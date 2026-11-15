package es.uclm.esi.iso2.client.configuration.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.configuration.contracts.IIntegrationManagementService;
import es.uclm.esi.iso2.common.configuration.domain.ConnectionTestResult;
import es.uclm.esi.iso2.common.configuration.domain.HospitalSystemType;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationConfiguration;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationMode;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationStatus;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test double of {@link IIntegrationManagementService} for the screen of UC20. It keeps the integrations in memory,
 * in the order in which they are added, and does not validate them: a test asks for a rejection with
 * {@link #rejectNextChange}. The connection test answers as the server does (not configured, disabled, real mode not
 * available, or OK with the time given by {@link #answerIn}). It can also deny every call, as the server does when
 * the user has no permission.
 */
class FakeIntegrationManagementService implements IIntegrationManagementService {
    private final Map<HospitalSystemType, IntegrationConfiguration> integrations = new LinkedHashMap<>();
    private final Set<HospitalSystemType> notConfigured = EnumSet.noneOf(HospitalSystemType.class);
    private final List<IntegrationConfiguration> requests = new ArrayList<>();
    private final List<HospitalSystemType> tested = new ArrayList<>();

    private boolean denyAll;
    private List<String> nextErrors;
    private long responseTimeMs = 3;
    private int reads;

    /** The usual data: the four systems, in the order of the server. */
    static FakeIntegrationManagementService withFourIntegrations() {
        FakeIntegrationManagementService service = new FakeIntegrationManagementService();
        service.add(integration(HospitalSystemType.SSO, "", Map.of("timeout", "30"), true,
                IntegrationMode.SIMULATED));
        service.add(integration(HospitalSystemType.EHR, "", Map.of(), false, IntegrationMode.SIMULATED));
        LinkedHashMap<String, String> admissions = new LinkedHashMap<>();
        admissions.put("timeout", "15");
        admissions.put("retries", "2");
        service.add(integration(HospitalSystemType.ADMISSIONS, "http://admissions.hospital.test", admissions, true,
                IntegrationMode.SIMULATED));
        service.add(integration(HospitalSystemType.RIS_PACS, "https://pacs.hospital.test", Map.of(), true,
                IntegrationMode.REAL));
        return service;
    }

    /** A configuration whose status follows {@code enabled}, as the server stores it. */
    static IntegrationConfiguration integration(HospitalSystemType system, String endpoint,
            Map<String, String> settings, boolean enabled, IntegrationMode mode) {
        return new IntegrationConfiguration(system, endpoint, settings, enabled, mode,
                enabled ? IntegrationStatus.ACTIVE : IntegrationStatus.INACTIVE);
    }

    void add(IntegrationConfiguration integration) {
        integrations.put(integration.getSystem(), integration);
    }

    /** From now on every call is denied. */
    void denyAll() {
        denyAll = true;
    }

    /** The next configuration is rejected with these errors. */
    void rejectNextChange(String... errors) {
        nextErrors = List.of(errors);
    }

    /** The connection test of this system answers that it is not configured. */
    void notConfigured(HospitalSystemType system) {
        notConfigured.add(system);
    }

    /** The time that a successful connection test takes. */
    void answerIn(long milliseconds) {
        responseTimeMs = milliseconds;
    }

    /** Every configuration that reached the service, even if it was rejected. */
    List<IntegrationConfiguration> requests() {
        return requests;
    }

    /** The systems whose connection was tested, in order. */
    List<HospitalSystemType> tested() {
        return tested;
    }

    /** How many times the list of integrations was read. */
    int reads() {
        return reads;
    }

    IntegrationConfiguration stored(HospitalSystemType system) {
        return integrations.get(system);
    }

    private void check(String requester) throws AccessDeniedException {
        if (denyAll) {
            throw new AccessDeniedException(requester);
        }
    }

    @Override
    public List<IntegrationConfiguration> getIntegrations(String requester) throws AccessDeniedException {
        check(requester);
        reads++;
        return new ArrayList<>(integrations.values());
    }

    @Override
    public IntegrationConfiguration getIntegration(String requester, HospitalSystemType system)
            throws AccessDeniedException {
        check(requester);
        return integrations.get(system);
    }

    @Override
    public IntegrationConfiguration configureIntegration(String requester, IntegrationConfiguration configuration)
            throws AccessDeniedException, ValidationException {
        check(requester);
        requests.add(configuration);
        if (nextErrors != null) {
            List<String> errors = nextErrors;
            nextErrors = null;
            throw new ValidationException(errors);
        }
        IntegrationConfiguration saved = integration(configuration.getSystem(), configuration.getEndpoint(),
                configuration.getSettings(), configuration.isEnabled(), configuration.getMode());
        integrations.put(saved.getSystem(), saved);
        return saved;
    }

    @Override
    public ConnectionTestResult testConnection(String requester, HospitalSystemType system)
            throws AccessDeniedException {
        check(requester);
        tested.add(system);
        IntegrationConfiguration configuration = integrations.get(system);
        if (configuration == null || notConfigured.contains(system)) {
            return new ConnectionTestResult(false, 0, "integration not configured");
        }
        if (!configuration.isEnabled()) {
            return new ConnectionTestResult(false, 0, "integration disabled");
        }
        if (configuration.getMode() == IntegrationMode.REAL) {
            return new ConnectionTestResult(false, 0, "real mode not available in this version");
        }
        return new ConnectionTestResult(true, responseTimeMs, "Connection OK");
    }
}
