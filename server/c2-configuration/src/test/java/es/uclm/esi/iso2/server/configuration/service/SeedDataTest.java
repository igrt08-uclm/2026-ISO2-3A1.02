package es.uclm.esi.iso2.server.configuration.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of the seed data of the examination catalogue, loaded only when the system starts with no data (D-003). */
@DisplayName("SystemParameterService seed data")
class SeedDataTest {
    private static final String ADMIN = "admin";

    private RecordingAuditRecorder audit;
    private SystemParameterService service;

    @BeforeEach
    void setUp() {
        audit = new RecordingAuditRecorder();
        service = new SystemParameterService(new FakeRoleManagementService(), audit);
    }

    @Test
    @DisplayName("TC17S-26: an empty system gets the 6 test types, 3 priorities and one example of each other group")
    void seedDataIsLoadedOnAnEmptySystem() throws Exception {
        // Act
        service.loadSeedData();

        // Assert
        List<TestType> testTypes = service.getTestTypes(ADMIN);
        assertAll(
                () -> assertEquals(List.of("XRAY-CHEST", "CT-HEAD", "MRI-KNEE", "US-ABDOMEN", "MAMMO-BILATERAL",
                        "DEXA-SPINE"), testTypes.stream().map(TestType::getId).toList()),
                () -> assertEquals(List.of("URGENT", "PREFERENTIAL", "ROUTINE"),
                        service.getPriorities(ADMIN).stream().map(Priority::getId).toList()),
                () -> assertEquals(1, service.getProtocols(ADMIN).size()),
                () -> assertEquals(1, service.getSchedules(ADMIN).size()),
                () -> assertEquals(1, service.getAppointmentPolicies(ADMIN).size()),
                () -> assertTrue(testTypes.stream().allMatch(TestType::isActive)),
                // It is loaded by the program, not by a user, so it is not audited
                () -> assertTrue(audit.entries().isEmpty()));
    }

    @Test
    @DisplayName("TC17S-27: loading the seed data twice does not duplicate it")
    void seedDataIsLoadedOnlyOnce() throws Exception {
        // Act
        service.loadSeedData();
        service.loadSeedData();

        // Assert
        assertAll(
                () -> assertEquals(6, service.getTestTypes(ADMIN).size()),
                () -> assertEquals(3, service.getPriorities(ADMIN).size()));
    }

    @Test
    @DisplayName("TC17S-28: if there is already data, the seed data is not loaded")
    void seedDataIsNotLoadedWhenThereIsData() throws Exception {
        // Arrange
        service.savePriority(ADMIN, new Priority("LOW", "Low", "Own priority", true));

        // Act
        service.loadSeedData();

        // Assert
        assertAll(
                () -> assertTrue(service.getTestTypes(ADMIN).isEmpty()),
                () -> assertEquals(1, service.getPriorities(ADMIN).size()));
    }

    @Test
    @DisplayName("TC17S-29: every seed test type has its required fields and a priority of the seed")
    void seedTestTypesAreComplete() throws Exception {
        // Arrange
        service.loadSeedData();
        Set<String> priorities = service.getPriorities(ADMIN).stream().map(Priority::getId)
                .collect(Collectors.toSet());

        // Assert
        for (TestType testType : service.getTestTypes(ADMIN)) {
            assertAll(testType.getId(),
                    () -> assertFalse(testType.getName().isBlank()),
                    () -> assertFalse(testType.getModality().isBlank()),
                    () -> assertFalse(testType.getRoom().isBlank()),
                    () -> assertTrue(testType.getDurationMinutes() > 0),
                    () -> assertTrue(priorities.contains(testType.getPriority())));
        }
    }
}
