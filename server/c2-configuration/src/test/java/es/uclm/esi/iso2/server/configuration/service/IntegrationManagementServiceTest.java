package es.uclm.esi.iso2.server.configuration.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.configuration.domain.ConnectionTestResult;
import es.uclm.esi.iso2.common.configuration.domain.HospitalSystemType;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationConfiguration;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationMode;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationStatus;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.configuration.persistence.InMemoryConfigurationRepository;
import es.uclm.esi.iso2.server.configuration.service.RecordingAuditRecorder.AuditEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of {@link IntegrationManagementService} (UC20 server, #43). Each test names the case of the test design
 * (TC20S-xx) that it checks. The service uses an empty in-memory repository, a fake of the role management of C1, a
 * recording audit and one {@link FakeHospitalSystem} for each external system, so every test starts from the same
 * state and can see which gateway was asked. The expected results come from the answers Q4 to Q12 and Q17 of the plan
 * {@code doc/plans/UC20-server.md} and the decisions of its step 5.
 */
@DisplayName("IntegrationManagementService")
class IntegrationManagementServiceTest {
    private static final String ADMIN = "admin";
    private static final String PHYSICIAN = "mgarcia";
    private static final String SYSTEM_CONFIGURATOR = "lmartin";
    private static final String INACTIVE = "pnavarro";
    private static final String UNKNOWN = "nobody";
    private static final String DENIED = "INTEGRATION_ACCESS_DENIED";
    private static final String CHANGED = "INTEGRATION_CHANGED";
    private static final String ENDPOINT_REQUIRED = "The endpoint is required in real mode";
    private static final String ENDPOINT_PREFIX = "The endpoint must start with http:// or https://";
    private static final String ENDPOINT_HOST = "The endpoint needs a host after http:// or https://";

    private RecordingAuditRecorder audit;
    private FakeHospitalSystem sso;
    private FakeHospitalSystem ehr;
    private FakeHospitalSystem admissions;
    private FakeHospitalSystem risPacs;
    private IntegrationManagementService service;

    @BeforeEach
    void setUp() {
        audit = new RecordingAuditRecorder();
        createService(new FakeHospitalSystem(true), new FakeHospitalSystem(true), new FakeHospitalSystem(true),
                new FakeHospitalSystem(true));
    }

    private void createService(FakeHospitalSystem newSso, FakeHospitalSystem newEhr,
            FakeHospitalSystem newAdmissions, FakeHospitalSystem newRisPacs) {
        sso = newSso;
        ehr = newEhr;
        admissions = newAdmissions;
        risPacs = newRisPacs;
        service = new IntegrationManagementService(
                new InMemoryConfigurationRepository<>(configuration -> configuration.getSystem().name()),
                new FakeRoleManagementService(), audit, sso, ehr, admissions, risPacs);
    }

    /** The gateway double that the service uses for a system. */
    private FakeHospitalSystem gatewayOf(HospitalSystemType system) {
        return switch (system) {
            case SSO -> sso;
            case EHR -> ehr;
            case ADMISSIONS -> admissions;
            case RIS_PACS -> risPacs;
        };
    }

    private int totalPings() {
        return sso.pings() + ehr.pings() + admissions.pings() + risPacs.pings();
    }

    private static IntegrationConfiguration simulated(HospitalSystemType system, boolean enabled) {
        return new IntegrationConfiguration(system, "", Map.of(), enabled, IntegrationMode.SIMULATED,
                IntegrationStatus.ACTIVE);
    }

    private static IntegrationConfiguration real(HospitalSystemType system, String endpoint, boolean enabled) {
        return new IntegrationConfiguration(system, endpoint, Map.of(), enabled, IntegrationMode.REAL,
                IntegrationStatus.ACTIVE);
    }

    private static IntegrationConfiguration withEndpoint(String endpoint, IntegrationMode mode) {
        return new IntegrationConfiguration(HospitalSystemType.EHR, endpoint, Map.of(), true, mode,
                IntegrationStatus.ACTIVE);
    }

    private static IntegrationConfiguration withSettings(Map<String, String> settings) {
        return new IntegrationConfiguration(HospitalSystemType.EHR, "", settings, true, IntegrationMode.SIMULATED,
                IntegrationStatus.ACTIVE);
    }

    /** Runs a configuration that must be rejected and returns its errors. */
    private List<String> errorsOf(IntegrationConfiguration configuration) {
        return assertThrows(ValidationException.class, () -> service.configureIntegration(ADMIN, configuration))
                .getErrors();
    }

    /** Checks that two configurations have the same data. */
    private static void assertSameData(IntegrationConfiguration expected, IntegrationConfiguration actual) {
        assertAll(
                () -> assertEquals(expected.getSystem(), actual.getSystem()),
                () -> assertEquals(expected.getEndpoint(), actual.getEndpoint()),
                () -> assertEquals(expected.getSettings(), actual.getSettings()),
                () -> assertEquals(expected.isEnabled(), actual.isEnabled()),
                () -> assertEquals(expected.getMode(), actual.getMode()),
                () -> assertEquals(expected.getStatus(), actual.getStatus()));
    }

    /** The four operations of the contract, each one with the resource that a denied access must register. */
    enum Operation {
        GET_INTEGRATIONS("INTEGRATION"),
        GET_INTEGRATION("INTEGRATION:EHR"),
        CONFIGURE_INTEGRATION("INTEGRATION:ADMISSIONS"),
        TEST_CONNECTION("INTEGRATION:RIS_PACS");

        private final String resource;

        Operation(String resource) {
            this.resource = resource;
        }

        void run(IntegrationManagementService service, String requester) throws Exception {
            switch (this) {
                case GET_INTEGRATIONS -> service.getIntegrations(requester);
                case GET_INTEGRATION -> service.getIntegration(requester, HospitalSystemType.EHR);
                case CONFIGURE_INTEGRATION -> service.configureIntegration(requester,
                        simulated(HospitalSystemType.ADMISSIONS, false));
                case TEST_CONNECTION -> service.testConnection(requester, HospitalSystemType.RIS_PACS);
            }
        }
    }

    /** Checks that a requester is denied in an operation, that only the denial is audited and nothing changed. */
    private void assertDenied(String requester, Operation operation) {
        // Arrange
        service.loadSeedData();

        // Act
        AccessDeniedException denied = assertThrows(AccessDeniedException.class,
                () -> operation.run(service, requester));

        // Assert
        assertAll(
                () -> assertEquals(requester, denied.getRequester()),
                () -> assertEquals(List.of(new AuditEntry(requester, DENIED, operation.resource)), audit.entries()),
                () -> assertTrue(service.getIntegration(ADMIN, HospitalSystemType.ADMISSIONS).isEnabled()),
                () -> assertEquals(0, totalPings()));
    }

    @Nested
    @DisplayName("Permission")
    class PermissionTests {

        @ParameterizedTest(name = "{0}")
        @EnumSource(Operation.class)
        @DisplayName("TC20S-01: the administrator can call every operation, and only a change is audited")
        void administratorCanCallEveryOperation(Operation operation) throws Exception {
            // Arrange
            service.loadSeedData();

            // Act
            operation.run(service, ADMIN);

            // Assert
            List<AuditEntry> expected = operation == Operation.CONFIGURE_INTEGRATION
                    ? List.of(new AuditEntry(ADMIN, CHANGED, "INTEGRATION:ADMISSIONS"))
                    : List.of();
            assertEquals(expected, audit.entries());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(Operation.class)
        @DisplayName("TC20S-02: an active user without any permission is denied in every operation, and it is audited")
        void userWithoutPermissionIsDenied(Operation operation) {
            assertDenied(PHYSICIAN, operation);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(Operation.class)
        @DisplayName("TC20S-03: CONFIGURE_SYSTEM alone does not give access to the integrations")
        void configureSystemIsNotEnough(Operation operation) {
            assertDenied(SYSTEM_CONFIGURATOR, operation);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(Operation.class)
        @DisplayName("TC20S-04: an inactive user is denied even if he has CONFIGURE_INTEGRATIONS")
        void inactiveUserIsDenied(Operation operation) {
            assertDenied(INACTIVE, operation);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(Operation.class)
        @DisplayName("TC20S-05: an unknown user is denied in every operation")
        void unknownUserIsDenied(Operation operation) {
            assertDenied(UNKNOWN, operation);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(Operation.class)
        @DisplayName("TC20S-06: a null requester gets AccessDeniedException and no other exception")
        void nullRequesterIsDenied(Operation operation) {
            assertDenied(null, operation);
        }

        @Test
        @DisplayName("TC20S-07: the permission is checked before the validation, and a null configuration names "
                + "INTEGRATION")
        void permissionIsCheckedBeforeValidation() {
            // Arrange
            IntegrationConfiguration invalid = real(HospitalSystemType.SSO, "ftp://sso.test", true);

            // Act
            assertThrows(AccessDeniedException.class, () -> service.configureIntegration(PHYSICIAN, invalid));
            assertThrows(AccessDeniedException.class, () -> service.configureIntegration(PHYSICIAN, null));

            // Assert
            assertEquals(List.of(new AuditEntry(PHYSICIAN, DENIED, "INTEGRATION:SSO"),
                    new AuditEntry(PHYSICIAN, DENIED, "INTEGRATION")), audit.entries());
        }

        @Test
        @DisplayName("TC20S-08: after a denial, the administrator can make the same call")
        void denialLeavesNothingBehind() throws Exception {
            // Arrange
            service.loadSeedData();
            assertThrows(AccessDeniedException.class,
                    () -> service.testConnection(PHYSICIAN, HospitalSystemType.SSO));

            // Act
            ConnectionTestResult result = service.testConnection(ADMIN, HospitalSystemType.SSO);

            // Assert
            assertAll(
                    () -> assertTrue(result.isSuccess()),
                    () -> assertEquals(1, audit.entries().size()));
        }
    }

    @Nested
    @DisplayName("Reading the integrations")
    class ReadTests {

        @Test
        @DisplayName("TC20S-09: getIntegrations returns SSO, EHR, admissions and RIS/PACS in that order")
        void integrationsAreInTheOrderOfTheSystems() throws Exception {
            // Arrange
            service.loadSeedData();

            // Act
            List<IntegrationConfiguration> integrations = service.getIntegrations(ADMIN);

            // Assert
            assertEquals(List.of(HospitalSystemType.SSO, HospitalSystemType.EHR, HospitalSystemType.ADMISSIONS,
                    HospitalSystemType.RIS_PACS), integrations.stream().map(IntegrationConfiguration::getSystem)
                    .toList());
        }

        @Test
        @DisplayName("TC20S-10: the order is the one of the systems, not the one in which they were saved")
        void orderDoesNotDependOnTheSaves() throws Exception {
            // Arrange: an empty service, RIS/PACS saved before admissions and SSO
            service.configureIntegration(ADMIN, simulated(HospitalSystemType.RIS_PACS, true));
            service.configureIntegration(ADMIN, simulated(HospitalSystemType.ADMISSIONS, true));
            service.configureIntegration(ADMIN, simulated(HospitalSystemType.SSO, true));

            // Act
            List<IntegrationConfiguration> integrations = service.getIntegrations(ADMIN);

            // Assert
            assertEquals(List.of(HospitalSystemType.SSO, HospitalSystemType.ADMISSIONS, HospitalSystemType.RIS_PACS),
                    integrations.stream().map(IntegrationConfiguration::getSystem).toList());
        }

        @Test
        @DisplayName("TC20S-11: with no integration stored, getIntegrations returns an empty list")
        void noIntegrationsGiveAnEmptyList() throws Exception {
            assertTrue(service.getIntegrations(ADMIN).isEmpty());
        }

        @Test
        @DisplayName("TC20S-12: getIntegration returns the integration of the system asked for")
        void getIntegrationReturnsThatSystem() throws Exception {
            // Arrange
            service.loadSeedData();

            // Act
            IntegrationConfiguration integration = service.getIntegration(ADMIN, HospitalSystemType.ADMISSIONS);

            // Assert
            assertEquals(HospitalSystemType.ADMISSIONS, integration.getSystem());
        }

        @Test
        @DisplayName("TC20S-13: getIntegration with a null system throws IllegalArgumentException, not audited")
        void getIntegrationWithNullSystem() {
            // Act
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.getIntegration(ADMIN, null));

            // Assert
            assertAll(
                    () -> assertEquals("The system is required", error.getMessage()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("TC20S-14: getIntegration of a system that is not stored returns null")
        void getIntegrationOfAMissingSystem() throws Exception {
            // Arrange
            service.configureIntegration(ADMIN, simulated(HospitalSystemType.SSO, true));

            // Act and Assert
            assertNull(service.getIntegration(ADMIN, HospitalSystemType.EHR));
        }
    }

    @Nested
    @DisplayName("Configuring an integration: valid data")
    class ValidConfigurationTests {

        @Test
        @DisplayName("TC20S-15: a simulated integration with an empty endpoint is saved, returned and audited")
        void simulatedWithEmptyEndpointIsSaved() throws Exception {
            // Arrange
            IntegrationConfiguration configuration = new IntegrationConfiguration(HospitalSystemType.EHR, "",
                    Map.of("timeout", "30"), true, IntegrationMode.SIMULATED, IntegrationStatus.ACTIVE);

            // Act
            IntegrationConfiguration saved = service.configureIntegration(ADMIN, configuration);

            // Assert
            assertAll(
                    () -> assertSameData(configuration, saved),
                    () -> assertSameData(saved, service.getIntegration(ADMIN, HospitalSystemType.EHR)),
                    () -> assertEquals(List.of(new AuditEntry(ADMIN, CHANGED, "INTEGRATION:EHR")),
                            audit.entries()));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"https://ehr.hospital.test/api", "http://ehr.hospital.test/api"})
        @DisplayName("TC20S-16: a real integration with an http:// or https:// endpoint is saved")
        void realWithValidEndpointIsSaved(String endpoint) throws Exception {
            // Act
            IntegrationConfiguration saved = service.configureIntegration(ADMIN,
                    real(HospitalSystemType.EHR, endpoint, true));

            // Assert
            assertAll(
                    () -> assertEquals(endpoint, saved.getEndpoint()),
                    () -> assertEquals(IntegrationMode.REAL, saved.getMode()),
                    () -> assertEquals(IntegrationMode.REAL,
                            service.getIntegration(ADMIN, HospitalSystemType.EHR).getMode()));
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"HTTPS://ehr.hospital.test", "Http://ehr.hospital.test", "http://ehr.hospital.test:8443/api"})
        @DisplayName("TC20S-17: the scheme ignores case and a host with a port and a path is accepted, saved as written")
        void endpointWithHostIsAccepted(String endpoint) throws Exception {
            // Act
            IntegrationConfiguration realSaved = service.configureIntegration(ADMIN,
                    withEndpoint(endpoint, IntegrationMode.REAL));
            IntegrationConfiguration simulatedSaved = service.configureIntegration(ADMIN,
                    withEndpoint(endpoint, IntegrationMode.SIMULATED));

            // Assert
            assertAll(
                    () -> assertEquals(endpoint, realSaved.getEndpoint()),
                    () -> assertEquals(endpoint, simulatedSaved.getEndpoint()),
                    () -> assertEquals(endpoint, service.getIntegration(ADMIN, HospitalSystemType.EHR).getEndpoint()));
        }

        @Test
        @DisplayName("TC20S-18: the settings are kept in the order in which they were written")
        void settingsKeepTheirOrder() throws Exception {
            // Arrange
            Map<String, String> settings = new LinkedHashMap<>();
            settings.put("timeout", "30");
            settings.put("application", "RIS");
            settings.put("facility", "HGUCR");

            // Act
            service.configureIntegration(ADMIN, withSettings(settings));

            // Assert
            assertEquals(List.of("timeout", "application", "facility"), List.copyOf(
                    service.getIntegration(ADMIN, HospitalSystemType.EHR).getSettings().keySet()));
        }

        @ParameterizedTest(name = "status sent {0}")
        @EnumSource(IntegrationStatus.class)
        @NullSource
        @DisplayName("TC20S-19: the server sets ACTIVE or INACTIVE from enabled and ignores the status sent")
        void statusIsSetByTheServer(IntegrationStatus sent) throws Exception {
            // Arrange
            IntegrationConfiguration enabled = new IntegrationConfiguration(HospitalSystemType.SSO, "", Map.of(),
                    true, IntegrationMode.SIMULATED, sent);
            IntegrationConfiguration disabled = new IntegrationConfiguration(HospitalSystemType.EHR, "", Map.of(),
                    false, IntegrationMode.SIMULATED, sent);

            // Act
            IntegrationConfiguration savedEnabled = service.configureIntegration(ADMIN, enabled);
            IntegrationConfiguration savedDisabled = service.configureIntegration(ADMIN, disabled);

            // Assert
            assertAll(
                    () -> assertEquals(IntegrationStatus.ACTIVE, savedEnabled.getStatus()),
                    () -> assertEquals(IntegrationStatus.INACTIVE, savedDisabled.getStatus()),
                    () -> assertEquals(IntegrationStatus.ACTIVE,
                            service.getIntegration(ADMIN, HospitalSystemType.SSO).getStatus()),
                    () -> assertEquals(IntegrationStatus.INACTIVE,
                            service.getIntegration(ADMIN, HospitalSystemType.EHR).getStatus()));
        }

        @Test
        @DisplayName("TC20S-20: the endpoint is saved without the spaces around it")
        void endpointIsStripped() throws Exception {
            // Act
            IntegrationConfiguration saved = service.configureIntegration(ADMIN,
                    withEndpoint("  https://ehr.hospital.test  ", IntegrationMode.REAL));

            // Assert
            assertAll(
                    () -> assertEquals("https://ehr.hospital.test", saved.getEndpoint()),
                    () -> assertEquals("https://ehr.hospital.test",
                            service.getIntegration(ADMIN, HospitalSystemType.EHR).getEndpoint()));
        }

        @ParameterizedTest(name = "endpoint \"{0}\"")
        @ValueSource(strings = {"   ", "\t"})
        @NullSource
        @DisplayName("TC20S-21: in simulated mode a null or blank endpoint is saved as an empty endpoint")
        void blankEndpointInSimulatedModeIsSavedEmpty(String endpoint) throws Exception {
            // Act
            IntegrationConfiguration saved = service.configureIntegration(ADMIN,
                    withEndpoint(endpoint, IntegrationMode.SIMULATED));

            // Assert
            assertEquals("", saved.getEndpoint());
        }

        @Test
        @DisplayName("TC20S-22: null or empty settings are accepted")
        void missingSettingsAreAccepted() throws Exception {
            // Act
            IntegrationConfiguration savedNull = service.configureIntegration(ADMIN, withSettings(null));
            IntegrationConfiguration savedEmpty = service.configureIntegration(ADMIN, withSettings(Map.of()));

            // Assert
            assertAll(
                    () -> assertNull(savedNull.getSettings()),
                    () -> assertTrue(savedEmpty.getSettings().isEmpty()));
        }

        @Test
        @DisplayName("TC20S-23: saving a system again replaces its integration, and each save is audited")
        void savingAgainReplaces() throws Exception {
            // Arrange
            service.loadSeedData();

            // Act
            service.configureIntegration(ADMIN, real(HospitalSystemType.RIS_PACS, "https://pacs.test", false));
            service.configureIntegration(ADMIN, real(HospitalSystemType.RIS_PACS, "https://pacs2.test", true));

            // Assert
            IntegrationConfiguration stored = service.getIntegration(ADMIN, HospitalSystemType.RIS_PACS);
            assertAll(
                    () -> assertEquals(4, service.getIntegrations(ADMIN).size()),
                    () -> assertEquals("https://pacs2.test", stored.getEndpoint()),
                    () -> assertTrue(stored.isEnabled()),
                    () -> assertEquals(List.of(new AuditEntry(ADMIN, CHANGED, "INTEGRATION:RIS_PACS"),
                            new AuditEntry(ADMIN, CHANGED, "INTEGRATION:RIS_PACS")), audit.entries()));
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(HospitalSystemType.class)
        @DisplayName("TC20S-24: the change is audited with the author and the resource INTEGRATION:<SYSTEM>")
        void changeIsAuditedWithTheSystem(HospitalSystemType system) throws Exception {
            // Act
            service.configureIntegration(ADMIN, simulated(system, true));

            // Assert
            assertEquals(List.of(new AuditEntry(ADMIN, CHANGED, "INTEGRATION:" + system.name())),
                    audit.entries());
        }
    }

    @Nested
    @DisplayName("Configuring an integration: invalid data")
    class InvalidConfigurationTests {

        @Test
        @DisplayName("TC20S-25: a null configuration is rejected with \"The integration is required\"")
        void nullConfiguration() {
            assertAll(
                    () -> assertEquals(List.of("The integration is required"), errorsOf(null)),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("TC20S-26: a null system is rejected with \"The system is required\"")
        void nullSystem() {
            // Arrange
            IntegrationConfiguration configuration = new IntegrationConfiguration(null, "", Map.of(), true,
                    IntegrationMode.SIMULATED, IntegrationStatus.ACTIVE);

            // Act and Assert
            assertEquals(List.of("The system is required"), errorsOf(configuration));
        }

        @Test
        @DisplayName("TC20S-27: a null mode is rejected with \"The mode is required\"")
        void nullMode() {
            assertEquals(List.of("The mode is required"), errorsOf(withEndpoint("", null)));
        }

        @ParameterizedTest(name = "endpoint \"{0}\"")
        @ValueSource(strings = {"", "   "})
        @NullSource
        @DisplayName("TC20S-28: real mode with a null, empty or blank endpoint is rejected")
        void realModeNeedsAnEndpoint(String endpoint) {
            assertEquals(List.of(ENDPOINT_REQUIRED), errorsOf(withEndpoint(endpoint, IntegrationMode.REAL)));
        }

        @ParameterizedTest(name = "endpoint \"{0}\"")
        @ValueSource(strings = {"ftp://ehr.hospital.test", "ehr.hospital.test", "http:/ehr.hospital.test",
                "http:/", "https:", "www.http://ehr.test"})
        @DisplayName("TC20S-29: an endpoint that does not start with http:// or https:// is rejected in both modes")
        void endpointWithoutTheRightPrefix(String endpoint) {
            assertAll(
                    () -> assertEquals(List.of(ENDPOINT_PREFIX),
                            errorsOf(withEndpoint(endpoint, IntegrationMode.REAL))),
                    () -> assertEquals(List.of(ENDPOINT_PREFIX),
                            errorsOf(withEndpoint(endpoint, IntegrationMode.SIMULATED))));
        }

        @ParameterizedTest(name = "endpoint \"{0}\"")
        @ValueSource(strings = {"http://", "https://", "HTTPS://", "http://:8443", "http:///path", "http://user@",
                "  https://  "})
        @DisplayName("TC20S-17: BV, an endpoint with no host after http:// or https:// is rejected in both modes")
        void endpointWithoutHostIsRejected(String endpoint) {
            assertAll(
                    () -> assertEquals(List.of(ENDPOINT_HOST),
                            errorsOf(withEndpoint(endpoint, IntegrationMode.REAL))),
                    () -> assertEquals(List.of(ENDPOINT_HOST),
                            errorsOf(withEndpoint(endpoint, IntegrationMode.SIMULATED))));
        }

        @ParameterizedTest(name = "key \"{0}\"")
        @ValueSource(strings = {"", "  "})
        @NullSource
        @DisplayName("TC20S-30: a setting with a null, empty or blank key is rejected")
        void settingWithoutKey(String key) {
            // Arrange
            Map<String, String> settings = new LinkedHashMap<>();
            settings.put(key, "30");

            // Act and Assert
            assertEquals(List.of("A setting needs a key"), errorsOf(withSettings(settings)));
        }

        @ParameterizedTest(name = "value \"{0}\"")
        @ValueSource(strings = {"", "  "})
        @NullSource
        @DisplayName("TC20S-31: a setting with a null, empty or blank value is rejected and named")
        void settingWithoutValue(String value) {
            // Arrange
            Map<String, String> settings = new LinkedHashMap<>();
            settings.put("timeout", value);

            // Act and Assert
            assertEquals(List.of("The setting timeout needs a value"), errorsOf(withSettings(settings)));
        }

        @Test
        @DisplayName("TC20S-32: every error comes together, in order, and nothing is saved or audited")
        void everyErrorComesTogether() {
            // Arrange
            Map<String, String> settings = new LinkedHashMap<>();
            settings.put("", "30");
            settings.put("facility", "");
            settings.put("application", " ");
            IntegrationConfiguration configuration = new IntegrationConfiguration(null, "ftp://x", settings, true,
                    null, IntegrationStatus.ERROR);

            // Act
            List<String> errors = errorsOf(configuration);

            // Assert
            assertAll(
                    () -> assertEquals(List.of("The system is required", "The mode is required", ENDPOINT_PREFIX,
                            "A setting needs a key", "The setting facility needs a value",
                            "The setting application needs a value"), errors),
                    () -> assertTrue(service.getIntegrations(ADMIN).isEmpty()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("TC20S-33: an invalid change of a stored integration leaves the stored one as it was")
        void invalidChangeKeepsTheStoredIntegration() throws Exception {
            // Arrange
            service.loadSeedData();
            IntegrationConfiguration before = service.getIntegration(ADMIN, HospitalSystemType.EHR);
            Map<String, String> settings = new LinkedHashMap<>();
            settings.put("timeout", "");

            // Act
            List<String> errors = errorsOf(new IntegrationConfiguration(HospitalSystemType.EHR, "", settings, false,
                    IntegrationMode.REAL, IntegrationStatus.INACTIVE));

            // Assert
            assertAll(
                    () -> assertEquals(List.of(ENDPOINT_REQUIRED, "The setting timeout needs a value"), errors),
                    () -> assertSameData(before, service.getIntegration(ADMIN, HospitalSystemType.EHR)),
                    () -> assertTrue(audit.entries().isEmpty()));
        }
    }

    @Nested
    @DisplayName("Connection test")
    class ConnectionTestTests {

        @ParameterizedTest(name = "{0}")
        @EnumSource(HospitalSystemType.class)
        @DisplayName("TC20S-34: a simulated, enabled integration answers \"Connection OK\" through its own gateway")
        void simulatedEnabledIntegrationAnswers(HospitalSystemType system) throws Exception {
            // Arrange
            service.loadSeedData();

            // Act
            ConnectionTestResult result = service.testConnection(ADMIN, system);

            // Assert
            assertAll(
                    () -> assertTrue(result.isSuccess()),
                    () -> assertEquals("Connection OK", result.getMessage()),
                    () -> assertTrue(result.getResponseTimeMs() >= 0),
                    () -> assertEquals(1, gatewayOf(system).pings()),
                    () -> assertEquals(1, totalPings()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(HospitalSystemType.class)
        @DisplayName("TC20S-35: a gateway that answers false gives \"The system did not answer\"")
        void gatewayThatDoesNotAnswer(HospitalSystemType system) throws Exception {
            // Arrange: only the gateway of the system under test answers false
            createService(new FakeHospitalSystem(system != HospitalSystemType.SSO),
                    new FakeHospitalSystem(system != HospitalSystemType.EHR),
                    new FakeHospitalSystem(system != HospitalSystemType.ADMISSIONS),
                    new FakeHospitalSystem(system != HospitalSystemType.RIS_PACS));
            service.loadSeedData();

            // Act
            ConnectionTestResult result = service.testConnection(ADMIN, system);

            // Assert
            assertAll(
                    () -> assertFalse(result.isSuccess()),
                    () -> assertEquals("The system did not answer", result.getMessage()),
                    () -> assertTrue(result.getResponseTimeMs() >= 0),
                    () -> assertEquals(1, gatewayOf(system).pings()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("TC20S-36: the response time is measured around the ping")
        void responseTimeIsMeasured() throws Exception {
            // Arrange: the EHR takes at least 50 ms to answer
            createService(new FakeHospitalSystem(true), new FakeHospitalSystem(true, 50),
                    new FakeHospitalSystem(true), new FakeHospitalSystem(true));
            service.loadSeedData();

            // Act
            ConnectionTestResult result = service.testConnection(ADMIN, HospitalSystemType.EHR);

            // Assert
            assertAll(
                    () -> assertTrue(result.isSuccess()),
                    () -> assertTrue(result.getResponseTimeMs() >= 50,
                            "measured " + result.getResponseTimeMs() + " ms"));
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(IntegrationMode.class)
        @DisplayName("TC20S-37: a disabled integration gives \"integration disabled\", also in real mode")
        void disabledIntegration(IntegrationMode mode) throws Exception {
            // Arrange
            service.configureIntegration(ADMIN, new IntegrationConfiguration(HospitalSystemType.ADMISSIONS,
                    "https://adm.test", Map.of(), false, mode, IntegrationStatus.ACTIVE));

            // Act
            ConnectionTestResult result = service.testConnection(ADMIN, HospitalSystemType.ADMISSIONS);

            // Assert
            assertAll(
                    () -> assertFalse(result.isSuccess()),
                    () -> assertEquals("integration disabled", result.getMessage()),
                    () -> assertEquals(0, result.getResponseTimeMs()),
                    () -> assertEquals(0, totalPings()));
        }

        @Test
        @DisplayName("TC20S-38: an enabled integration in real mode gives \"real mode not available in this "
                + "version\"")
        void realModeIsNotAvailable() throws Exception {
            // Arrange
            service.configureIntegration(ADMIN, real(HospitalSystemType.SSO, "https://sso.test", true));

            // Act
            ConnectionTestResult result = service.testConnection(ADMIN, HospitalSystemType.SSO);

            // Assert
            assertAll(
                    () -> assertFalse(result.isSuccess()),
                    () -> assertEquals("real mode not available in this version", result.getMessage()),
                    () -> assertEquals(0, result.getResponseTimeMs()),
                    () -> assertEquals(0, totalPings()));
        }

        @Test
        @DisplayName("TC20S-39: an integration that is not stored gives \"integration not configured\"")
        void integrationNotConfigured() throws Exception {
            // Act
            ConnectionTestResult result = service.testConnection(ADMIN, HospitalSystemType.RIS_PACS);

            // Assert
            assertAll(
                    () -> assertFalse(result.isSuccess()),
                    () -> assertEquals("integration not configured", result.getMessage()),
                    () -> assertEquals(0, result.getResponseTimeMs()),
                    () -> assertEquals(0, totalPings()));
        }

        @Test
        @DisplayName("TC20S-40: testConnection with a null system throws IllegalArgumentException")
        void testConnectionWithNullSystem() {
            // Act
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> service.testConnection(ADMIN, null));

            // Assert
            assertAll(
                    () -> assertEquals("The system is required", error.getMessage()),
                    () -> assertEquals(0, totalPings()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("TC20S-41: the test never changes the stored settings, also when it fails")
        void testNeverChangesTheStoredSettings() throws Exception {
            // Arrange: SSO answers, EHR does not, admissions is disabled and RIS/PACS is real
            createService(new FakeHospitalSystem(true), new FakeHospitalSystem(false), new FakeHospitalSystem(true),
                    new FakeHospitalSystem(true));
            service.configureIntegration(ADMIN, new IntegrationConfiguration(HospitalSystemType.SSO, "",
                    Map.of("realm", "HGUCR"), true, IntegrationMode.SIMULATED, null));
            service.configureIntegration(ADMIN, simulated(HospitalSystemType.EHR, true));
            service.configureIntegration(ADMIN, simulated(HospitalSystemType.ADMISSIONS, false));
            service.configureIntegration(ADMIN, real(HospitalSystemType.RIS_PACS, "https://pacs.test", true));
            List<IntegrationConfiguration> before = service.getIntegrations(ADMIN);
            int auditedBefore = audit.entries().size();

            // Act
            for (HospitalSystemType system : HospitalSystemType.values()) {
                service.testConnection(ADMIN, system);
            }

            // Assert: same data, and the status is never ERROR
            List<IntegrationConfiguration> after = service.getIntegrations(ADMIN);
            assertEquals(before.size(), after.size());
            for (int i = 0; i < before.size(); i++) {
                assertSameData(before.get(i), after.get(i));
                assertFalse(after.get(i).getStatus() == IntegrationStatus.ERROR);
            }
            assertEquals(auditedBefore, audit.entries().size());
        }
    }

    @Nested
    @DisplayName("Seed data")
    class SeedDataTests {

        @Test
        @DisplayName("TC20S-42: an empty service gets the four integrations, simulated, enabled, ACTIVE and empty")
        void seedDataIsLoaded() throws Exception {
            // Arrange: the constructor that Main uses, with the simulators
            IntegrationManagementService defaultService = new IntegrationManagementService(
                    new FakeRoleManagementService(), audit, sso);

            // Act
            defaultService.loadSeedData();

            // Assert
            List<IntegrationConfiguration> integrations = defaultService.getIntegrations(ADMIN);
            assertEquals(List.of(HospitalSystemType.SSO, HospitalSystemType.EHR, HospitalSystemType.ADMISSIONS,
                    HospitalSystemType.RIS_PACS), integrations.stream().map(IntegrationConfiguration::getSystem)
                    .toList());
            for (IntegrationConfiguration integration : integrations) {
                assertAll(integration.getSystem().name(),
                        () -> assertEquals(IntegrationMode.SIMULATED, integration.getMode()),
                        () -> assertTrue(integration.isEnabled()),
                        () -> assertEquals(IntegrationStatus.ACTIVE, integration.getStatus()),
                        () -> assertEquals("", integration.getEndpoint()),
                        () -> assertTrue(integration.getSettings().isEmpty()));
            }
            assertTrue(audit.entries().isEmpty());
        }

        @Test
        @DisplayName("TC20S-43: with the seed data and the simulators, every system answers \"Connection OK\"")
        void seedDataWithTheSimulatorsAnswers() throws Exception {
            // Arrange
            IntegrationManagementService defaultService = new IntegrationManagementService(
                    new FakeRoleManagementService(), audit, sso);
            defaultService.loadSeedData();

            // Act and Assert
            for (HospitalSystemType system : HospitalSystemType.values()) {
                ConnectionTestResult result = defaultService.testConnection(ADMIN, system);
                assertAll(system.name(),
                        () -> assertTrue(result.isSuccess()),
                        () -> assertEquals("Connection OK", result.getMessage()));
            }
        }

        @Test
        @DisplayName("TC20S-44: calling loadSeedData twice loads nothing more")
        void seedDataTwice() throws Exception {
            // Act
            service.loadSeedData();
            service.loadSeedData();

            // Assert
            assertEquals(4, service.getIntegrations(ADMIN).size());
        }

        @Test
        @DisplayName("TC20S-45: loadSeedData after a change does not overwrite it, and loads nothing if there is data")
        void seedDataDoesNotOverwrite() throws Exception {
            // Arrange: a first seed changed by the administrator, and another service with one integration only
            service.loadSeedData();
            service.configureIntegration(ADMIN, real(HospitalSystemType.EHR, "https://ehr.test", false));
            IntegrationManagementService other = new IntegrationManagementService(new FakeRoleManagementService(),
                    audit, sso);
            other.configureIntegration(ADMIN, real(HospitalSystemType.SSO, "https://sso.test", true));

            // Act
            service.loadSeedData();
            other.loadSeedData();

            // Assert
            IntegrationConfiguration ehrIntegration = service.getIntegration(ADMIN, HospitalSystemType.EHR);
            assertAll(
                    () -> assertEquals("https://ehr.test", ehrIntegration.getEndpoint()),
                    () -> assertFalse(ehrIntegration.isEnabled()),
                    () -> assertEquals(IntegrationMode.REAL, ehrIntegration.getMode()),
                    () -> assertEquals(List.of(HospitalSystemType.SSO), other.getIntegrations(ADMIN).stream()
                            .map(IntegrationConfiguration::getSystem).toList()),
                    () -> assertEquals(IntegrationMode.REAL,
                            other.getIntegration(ADMIN, HospitalSystemType.SSO).getMode()));
        }
    }

    @Nested
    @DisplayName("Fixes after the findings of #122")
    class FindingsOf122Tests {

        @ParameterizedTest(name = "requester {0}")
        @ValueSource(strings = {PHYSICIAN, SYSTEM_CONFIGURATOR, INACTIVE, UNKNOWN})
        @DisplayName("TC20S-59: F-14, getIntegration with a null system and no permission is denied on INTEGRATION")
        void getIntegrationWithNullSystemWithoutPermissionIsDenied(String requester) {
            // Arrange
            service.loadSeedData();

            // Act
            AccessDeniedException denied = assertThrows(AccessDeniedException.class,
                    () -> service.getIntegration(requester, null));

            // Assert
            assertAll(
                    () -> assertEquals(requester, denied.getRequester()),
                    () -> assertEquals(List.of(new AuditEntry(requester, DENIED, "INTEGRATION")), audit.entries()));
        }

        @ParameterizedTest(name = "requester {0}")
        @ValueSource(strings = {PHYSICIAN, SYSTEM_CONFIGURATOR, INACTIVE, UNKNOWN})
        @DisplayName("TC20S-60: F-14, testConnection with a null system and no permission is denied on INTEGRATION, "
                + "and no system is asked")
        void testConnectionWithNullSystemWithoutPermissionIsDenied(String requester) {
            // Arrange
            service.loadSeedData();

            // Act
            AccessDeniedException denied = assertThrows(AccessDeniedException.class,
                    () -> service.testConnection(requester, null));

            // Assert
            assertAll(
                    () -> assertEquals(requester, denied.getRequester()),
                    () -> assertEquals(List.of(new AuditEntry(requester, DENIED, "INTEGRATION")), audit.entries()),
                    () -> assertEquals(0, totalPings()));
        }

        @Test
        @DisplayName("TC20S-61: F-14, with the permission a null system is still \"The system is required\", "
                + "also after a denial, and only the denial is audited")
        void nullSystemWithPermissionStillFailsTheValidation() {
            // Arrange
            service.loadSeedData();
            assertThrows(AccessDeniedException.class, () -> service.getIntegration(PHYSICIAN, null));

            // Act
            IllegalArgumentException read = assertThrows(IllegalArgumentException.class,
                    () -> service.getIntegration(ADMIN, null));
            IllegalArgumentException test = assertThrows(IllegalArgumentException.class,
                    () -> service.testConnection(ADMIN, null));

            // Assert
            assertAll(
                    () -> assertEquals("The system is required", read.getMessage()),
                    () -> assertEquals("The system is required", test.getMessage()),
                    () -> assertEquals(List.of(new AuditEntry(PHYSICIAN, DENIED, "INTEGRATION")), audit.entries()),
                    () -> assertEquals(0, totalPings()));
        }

        @Test
        @DisplayName("TC20S-62: F-15, the keys and the values of the settings are saved without the spaces around "
                + "them, in their order")
        void settingsAreSavedStripped() throws Exception {
            // Arrange
            Map<String, String> settings = new LinkedHashMap<>();
            settings.put(" timeout ", " 30 ");
            settings.put("application", "  RIS");
            settings.put("\tretries\t", "2\n");
            settings.put("facility", "HGUCR");
            Map<String, String> expected = new LinkedHashMap<>();
            expected.put("timeout", "30");
            expected.put("application", "RIS");
            expected.put("retries", "2");
            expected.put("facility", "HGUCR");

            // Act
            IntegrationConfiguration saved = service.configureIntegration(ADMIN, withSettings(settings));

            // Assert
            IntegrationConfiguration stored = service.getIntegration(ADMIN, HospitalSystemType.EHR);
            assertAll(
                    () -> assertEquals(expected, saved.getSettings()),
                    () -> assertEquals(expected, stored.getSettings()),
                    () -> assertEquals(List.of("timeout", "application", "retries", "facility"),
                            List.copyOf(stored.getSettings().keySet())),
                    () -> assertEquals(List.of(new AuditEntry(ADMIN, CHANGED, "INTEGRATION:EHR")), audit.entries()));
        }
    }
}
