package es.uclm.esi.iso2.server.configuration.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IAuditRecorder;
import es.uclm.esi.iso2.common.access.contracts.IAuthenticatorSystem;
import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.configuration.contracts.IIntegrationManagementService;
import es.uclm.esi.iso2.common.configuration.domain.ConnectionTestResult;
import es.uclm.esi.iso2.common.configuration.domain.HospitalSystemType;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationConfiguration;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationMode;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationStatus;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.configuration.integration.AdmissionsGatewaySimulator;
import es.uclm.esi.iso2.server.configuration.integration.EhrGatewaySimulator;
import es.uclm.esi.iso2.server.configuration.integration.IAdmissionsGateway;
import es.uclm.esi.iso2.server.configuration.integration.IEhrGateway;
import es.uclm.esi.iso2.server.configuration.integration.IRisPacsGateway;
import es.uclm.esi.iso2.server.configuration.integration.RisPacsGatewaySimulator;
import es.uclm.esi.iso2.server.configuration.persistence.IConfigurationRepository;
import es.uclm.esi.iso2.server.configuration.persistence.InMemoryConfigurationRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the integrations with the external hospital systems (UC20). Every operation checks that the requester is an
 * active user with the permission {@code CONFIGURE_INTEGRATIONS}, validates the settings, and registers every change
 * in the audit log. The connection test goes through the interface of each external system, so that nothing depends
 * on whether the system is real or simulated, and it never changes the stored configuration.
 */
public class IntegrationManagementService implements IIntegrationManagementService {
    private static final String CONFIGURE_INTEGRATIONS = "CONFIGURE_INTEGRATIONS";
    private static final String CHANGED = "INTEGRATION_CHANGED";
    private static final String ACCESS_DENIED = "INTEGRATION_ACCESS_DENIED";
    private static final String RESOURCE = "INTEGRATION";
    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final long NANOSECONDS_PER_MILLISECOND = 1_000_000L;

    private final IConfigurationRepository<IntegrationConfiguration> integrations;
    private final IRoleManagementService roleManagementService;
    private final IAuditRecorder auditRecorder;
    private final IAuthenticatorSystem sso;
    private final IEhrGateway ehr;
    private final IAdmissionsGateway admissions;
    private final IRisPacsGateway risPacs;

    /**
     * Creates the service with the given repository and external systems.
     *
     * @param integrations where the configuration of each integration is kept, by the name of its system
     * @param roleManagementService used to know the permissions of the requester (contract of C1)
     * @param auditRecorder where every change is registered (contract of C1)
     * @param sso the single sign-on, which C1 also uses to log in
     * @param ehr the electronic health record
     * @param admissions the admissions system
     * @param risPacs the RIS/PACS
     */
    public IntegrationManagementService(IConfigurationRepository<IntegrationConfiguration> integrations,
            IRoleManagementService roleManagementService, IAuditRecorder auditRecorder, IAuthenticatorSystem sso,
            IEhrGateway ehr, IAdmissionsGateway admissions, IRisPacsGateway risPacs) {
        this.integrations = integrations;
        this.roleManagementService = roleManagementService;
        this.auditRecorder = auditRecorder;
        this.sso = sso;
        this.ehr = ehr;
        this.admissions = admissions;
        this.risPacs = risPacs;
    }

    /**
     * Creates the service with an empty in-memory repository (D-030) and the simulators of the external systems
     * (A5), except the SSO, which is the one that C1 uses.
     *
     * @param roleManagementService used to know the permissions of the requester (contract of C1)
     * @param auditRecorder where every change is registered (contract of C1)
     * @param sso the single sign-on, which C1 also uses to log in
     */
    public IntegrationManagementService(IRoleManagementService roleManagementService, IAuditRecorder auditRecorder,
            IAuthenticatorSystem sso) {
        this(new InMemoryConfigurationRepository<>(configuration -> configuration.getSystem().name()),
                roleManagementService, auditRecorder, sso, new EhrGatewaySimulator(),
                new AdmissionsGatewaySimulator(), new RisPacsGatewaySimulator());
    }

    /**
     * Creates the four integrations, simulated, enabled and with an empty endpoint, only if there is none. It is
     * called when the program starts, not by a user, so it does not need a permission and it is not registered in
     * the audit log.
     */
    public void loadSeedData() {
        if (integrations.findAll().isEmpty()) {
            for (HospitalSystemType system : HospitalSystemType.values()) {
                integrations.save(new IntegrationConfiguration(system, "", Map.of(), true, IntegrationMode.SIMULATED,
                        IntegrationStatus.ACTIVE));
            }
        }
    }

    @Override
    public List<IntegrationConfiguration> getIntegrations(String requester) throws AccessDeniedException {
        checkPermission(requester, RESOURCE);
        return integrations.findAll().stream()
                .sorted(Comparator.comparing(IntegrationConfiguration::getSystem)).toList();
    }

    @Override
    public IntegrationConfiguration getIntegration(String requester, HospitalSystemType system)
            throws AccessDeniedException {
        checkPermission(requester, system == null ? RESOURCE : resourceOf(system));
        requireSystem(system);
        return integrations.findById(system.name());
    }

    @Override
    public IntegrationConfiguration configureIntegration(String requester, IntegrationConfiguration configuration)
            throws AccessDeniedException, ValidationException {
        HospitalSystemType system = configuration == null ? null : configuration.getSystem();
        String resource = system == null ? RESOURCE : resourceOf(system);
        checkPermission(requester, resource);
        List<String> errors = validate(configuration);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        String endpoint = configuration.getEndpoint() == null ? "" : configuration.getEndpoint().strip();
        Map<String, String> settings = null;
        if (configuration.getSettings() != null) {
            settings = new LinkedHashMap<>();
            for (Map.Entry<String, String> setting : configuration.getSettings().entrySet()) {
                settings.put(setting.getKey().strip(), setting.getValue().strip());
            }
        }
        IntegrationConfiguration saved = new IntegrationConfiguration(system, endpoint, settings,
                configuration.isEnabled(), configuration.getMode(),
                configuration.isEnabled() ? IntegrationStatus.ACTIVE : IntegrationStatus.INACTIVE);
        integrations.save(saved);
        auditRecorder.register(requester, CHANGED, resource);
        return saved;
    }

    @Override
    public ConnectionTestResult testConnection(String requester, HospitalSystemType system)
            throws AccessDeniedException {
        checkPermission(requester, system == null ? RESOURCE : resourceOf(system));
        requireSystem(system);
        IntegrationConfiguration configuration = integrations.findById(system.name());
        if (configuration == null) {
            return new ConnectionTestResult(false, 0, "integration not configured");
        }
        if (!configuration.isEnabled()) {
            return new ConnectionTestResult(false, 0, "integration disabled");
        }
        if (configuration.getMode() == IntegrationMode.REAL) {
            return new ConnectionTestResult(false, 0, "real mode not available in this version");
        }
        long start = System.nanoTime();
        boolean answered = ping(system);
        long milliseconds = (System.nanoTime() - start) / NANOSECONDS_PER_MILLISECOND;
        return answered ? new ConnectionTestResult(true, milliseconds, "Connection OK")
                : new ConnectionTestResult(false, milliseconds, "The system did not answer");
    }

    private boolean ping(HospitalSystemType system) {
        return switch (system) {
            case SSO -> sso.ping();
            case EHR -> ehr.ping();
            case ADMISSIONS -> admissions.ping();
            case RIS_PACS -> risPacs.ping();
        };
    }

    /** The requester must exist, be active and have CONFIGURE_INTEGRATIONS; if not, it is registered and denied. */
    private void checkPermission(String requester, String resource) throws AccessDeniedException {
        if (!roleManagementService.hasPermission(requester, CONFIGURE_INTEGRATIONS)) {
            auditRecorder.register(requester, ACCESS_DENIED, resource);
            throw new AccessDeniedException(requester);
        }
    }

    private static void requireSystem(HospitalSystemType system) {
        if (system == null) {
            throw new IllegalArgumentException("The system is required");
        }
    }

    private static String resourceOf(HospitalSystemType system) {
        return RESOURCE + ":" + system.name();
    }

    private List<String> validate(IntegrationConfiguration configuration) {
        List<String> errors = new ArrayList<>();
        if (configuration == null) {
            errors.add("The integration is required");
            return errors;
        }
        if (configuration.getSystem() == null) {
            errors.add("The system is required");
        }
        if (configuration.getMode() == null) {
            errors.add("The mode is required");
        }
        String endpoint = configuration.getEndpoint() == null ? "" : configuration.getEndpoint().strip();
        if (endpoint.isEmpty() && configuration.getMode() == IntegrationMode.REAL) {
            errors.add("The endpoint is required in real mode");
        } else if (!endpoint.isEmpty() && !endpoint.startsWith(HTTP) && !endpoint.startsWith(HTTPS)) {
            errors.add("The endpoint must start with http:// or https://");
        }
        if (configuration.getSettings() != null) {
            configuration.getSettings().forEach((key, value) -> {
                if (key == null || key.isBlank()) {
                    errors.add("A setting needs a key");
                } else if (value == null || value.isBlank()) {
                    errors.add("The setting " + key + " needs a value");
                }
            });
        }
        return errors;
    }
}
