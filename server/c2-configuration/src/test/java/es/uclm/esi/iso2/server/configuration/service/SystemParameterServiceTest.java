package es.uclm.esi.iso2.server.configuration.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.configuration.domain.AppointmentPolicy;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.Protocol;
import es.uclm.esi.iso2.common.configuration.domain.Schedule;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.configuration.service.RecordingAuditRecorder.AuditEntry;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of {@link SystemParameterService} (UC17 server, #40). Each test names the case of the test design
 * (TC17S-xx) that it checks. The service uses empty in-memory repositories, a fake of the role management of C1 and
 * a recording audit, so every test starts from the same state. A test type must name a priority that exists (Q9), so
 * the tests that save one first save the priority {@code URGENT} as part of their arrange step.
 */
@DisplayName("SystemParameterService")
class SystemParameterServiceTest {
    private static final String ADMIN = "admin";
    private static final String PHYSICIAN = "mgarcia";
    private static final String INACTIVE = "pnavarro";
    private static final String UNKNOWN = "nobody";
    private static final Set<DayOfWeek> WEEKDAYS = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

    private RecordingAuditRecorder audit;
    private SystemParameterService service;

    @BeforeEach
    void setUp() {
        audit = new RecordingAuditRecorder();
        service = new SystemParameterService(new FakeRoleManagementService(), audit);
    }

    /** The valid test type of the test design. */
    private static TestType headCt() {
        return testType("CT-HEAD", 20);
    }

    private static TestType testType(String id, int durationMinutes) {
        return testType(id, durationMinutes, "URGENT");
    }

    private static TestType testType(String id, int durationMinutes, String priority) {
        return new TestType(id, "Head CT", "CT", "Room CT-1", durationMinutes, "Fast for 4 hours", true, true,
                "Ask about allergies to contrast", priority, false, true);
    }

    /** Saves the priority that the valid test type uses, so that it can be created (Q9). */
    private void saveUrgentPriority() throws Exception {
        service.savePriority(ADMIN, new Priority("URGENT", "Urgent", "Hours", true));
    }

    private static Schedule schedule(LocalTime start, LocalTime end) {
        return new Schedule("Room CT-1", WEEKDAYS, start, end, true);
    }

    @Nested
    @DisplayName("Permission")
    class PermissionTests {

        @Test
        @DisplayName("TC17S-01: the administrator can read every group and the reads are not audited")
        void administratorReadsEveryGroupWithoutAudit() {
            assertAll(
                    () -> assertTrue(service.getTestTypes(ADMIN).isEmpty()),
                    () -> assertTrue(service.getProtocols(ADMIN).isEmpty()),
                    () -> assertTrue(service.getSchedules(ADMIN).isEmpty()),
                    () -> assertTrue(service.getAppointmentPolicies(ADMIN).isEmpty()),
                    () -> assertTrue(service.getPriorities(ADMIN).isEmpty()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("TC17S-02: an active user without CONFIGURE_SYSTEM cannot create, and the denial is audited")
        void userWithoutPermissionIsDeniedAndAudited() {
            // Act
            assertThrows(AccessDeniedException.class, () -> service.createTestType(PHYSICIAN, headCt()));

            // Assert
            assertAll(
                    () -> assertTrue(service.getTestTypes(ADMIN).isEmpty()),
                    () -> assertEquals(List.of(new AuditEntry(PHYSICIAN, "PARAMETER_ACCESS_DENIED", "TEST_TYPE")),
                            audit.entries()));
        }

        @Test
        @DisplayName("TC17S-03: an inactive user is denied even if he has CONFIGURE_SYSTEM")
        void inactiveUserIsDenied() {
            assertThrows(AccessDeniedException.class, () -> service.getTestTypes(INACTIVE));
        }

        @Test
        @DisplayName("TC17S-04: an unknown user is denied, and the audit names the group")
        void unknownUserIsDenied() {
            // Act
            assertThrows(AccessDeniedException.class,
                    () -> service.saveProtocol(UNKNOWN, new Protocol("P1", "Protocol", "", true)));

            // Assert
            assertEquals(List.of(new AuditEntry(UNKNOWN, "PARAMETER_ACCESS_DENIED", "PROTOCOL")), audit.entries());
        }

        @Test
        @DisplayName("TC17S-05: a null requester is denied without any other exception")
        void nullRequesterIsDenied() {
            assertThrows(AccessDeniedException.class, () -> service.getPriorities(null));
        }

        @Test
        @DisplayName("TC17S-06: every operation of every group checks the permission")
        void everyOperationChecksThePermission() {
            // Arrange: one call per operation, all made by the physician
            List<Executable> calls = List.of(
                    () -> service.getTestTypes(PHYSICIAN),
                    () -> service.getTestType(PHYSICIAN, "CT-HEAD"),
                    () -> service.createTestType(PHYSICIAN, headCt()),
                    () -> service.updateTestType(PHYSICIAN, "CT-HEAD", headCt()),
                    () -> service.deactivateTestType(PHYSICIAN, "CT-HEAD"),
                    () -> service.getProtocols(PHYSICIAN),
                    () -> service.saveProtocol(PHYSICIAN, new Protocol("P1", "Protocol", "", true)),
                    () -> service.deactivateProtocol(PHYSICIAN, "P1"),
                    () -> service.getSchedules(PHYSICIAN),
                    () -> service.saveSchedule(PHYSICIAN, schedule(LocalTime.of(8, 0), LocalTime.of(15, 0))),
                    () -> service.deactivateSchedule(PHYSICIAN, "Room CT-1"),
                    () -> service.getAppointmentPolicies(PHYSICIAN),
                    () -> service.saveAppointmentPolicy(PHYSICIAN, new AppointmentPolicy("A1", "Policy", "", true)),
                    () -> service.deactivateAppointmentPolicy(PHYSICIAN, "A1"),
                    () -> service.getPriorities(PHYSICIAN),
                    () -> service.savePriority(PHYSICIAN, new Priority("URGENT", "Urgent", "", true)),
                    () -> service.deactivatePriority(PHYSICIAN, "URGENT"));

            // Act and assert
            for (Executable call : calls) {
                assertThrows(AccessDeniedException.class, call);
            }
            List<String> groups = audit.entries().stream().map(AuditEntry::targetResource).distinct().toList();
            assertAll(
                    () -> assertEquals(calls.size(), audit.entries().size()),
                    () -> assertEquals(List.of("TEST_TYPE", "PROTOCOL", "SCHEDULE", "APPOINTMENT_POLICY", "PRIORITY"),
                            groups));
        }

        @Test
        @DisplayName("TC17S-07: a denial leaves nothing behind, so the administrator can do the same right after")
        void denialLeavesNothingBehind() throws Exception {
            // Arrange
            saveUrgentPriority();
            assertThrows(AccessDeniedException.class, () -> service.createTestType(PHYSICIAN, headCt()));

            // Act
            TestType created = service.createTestType(ADMIN, headCt());

            // Assert
            assertEquals("CT-HEAD", created.getId());
        }
    }

    @Nested
    @DisplayName("Test types")
    class TestTypeTests {

        @Test
        @DisplayName("TC17S-08: the administrator creates a valid test type, which is saved and audited")
        void createValidTestType() throws Exception {
            // Arrange
            saveUrgentPriority();

            // Act
            TestType created = service.createTestType(ADMIN, headCt());

            // Assert
            assertAll(
                    () -> assertEquals("CT-HEAD", created.getId()),
                    () -> assertSame(created, service.getTestType(ADMIN, "CT-HEAD")),
                    () -> assertEquals(1, service.getTestTypes(ADMIN).size()),
                    () -> assertEquals(List.of(
                            new AuditEntry(ADMIN, "PARAMETER_CREATED", "PRIORITY:URGENT"),
                            new AuditEntry(ADMIN, "PARAMETER_CREATED", "TEST_TYPE:CT-HEAD")), audit.entries()));
        }

        @Test
        @DisplayName("TC17S-09: all the validation errors are returned together and nothing is saved")
        void allValidationErrorsTogether() throws Exception {
            // Arrange
            TestType empty = new TestType("", "", "", "", 0, null, false, false, null, "", false, true);

            // Act
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.createTestType(ADMIN, empty));

            // Assert
            assertAll(
                    () -> assertEquals(6, error.getErrors().size()),
                    () -> assertTrue(error.getErrors().contains("The duration must be greater than zero")),
                    () -> assertTrue(service.getTestTypes(ADMIN).isEmpty()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("TC17S-10: a field made only of spaces counts as empty")
        void blankFieldsCountAsEmpty() {
            // Arrange
            TestType blank = new TestType("   ", "   ", "   ", "   ", 20, null, false, false, null, "   ", false, true);

            // Act
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.createTestType(ADMIN, blank));

            // Assert: id, name, modality, room and priority
            assertEquals(5, error.getErrors().size());
        }

        @ParameterizedTest(name = "duration {0} is rejected")
        @ValueSource(ints = {0, -1})
        @DisplayName("TC17S-11: a duration of zero or less is rejected")
        void nonPositiveDurationIsRejected(int duration) throws Exception {
            // Arrange
            saveUrgentPriority();

            // Act
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.createTestType(ADMIN, testType("CT-HEAD", duration)));

            // Assert
            assertEquals(List.of("The duration must be greater than zero"), error.getErrors());
        }

        @Test
        @DisplayName("TC17S-11: a duration of one minute, the lowest valid value, is accepted")
        void durationOfOneMinuteIsAccepted() throws Exception {
            // Arrange
            saveUrgentPriority();

            // Act and assert
            assertEquals(1, service.createTestType(ADMIN, testType("CT-HEAD", 1)).getDurationMinutes());
        }

        @Test
        @DisplayName("TC17S-12: a duplicated id is rejected and the first test type does not change")
        void duplicatedIdIsRejected() throws Exception {
            // Arrange
            saveUrgentPriority();
            TestType first = service.createTestType(ADMIN, headCt());

            // Act
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.createTestType(ADMIN, testType("CT-HEAD", 45)));

            // Assert
            assertAll(
                    () -> assertEquals(List.of("A test type with the id CT-HEAD already exists"), error.getErrors()),
                    () -> assertSame(first, service.getTestType(ADMIN, "CT-HEAD")));
        }

        @Test
        @DisplayName("TC17S-13: a null test type is rejected")
        void nullTestTypeIsRejected() {
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.createTestType(ADMIN, null));
            assertEquals(List.of("The test type is required"), error.getErrors());
        }

        @Test
        @DisplayName("TC17S-14: an existing test type is updated and the change is audited")
        void updateExistingTestType() throws Exception {
            // Arrange
            saveUrgentPriority();
            service.createTestType(ADMIN, headCt());

            // Act
            service.updateTestType(ADMIN, "CT-HEAD", testType("CT-HEAD", 30));

            // Assert
            assertAll(
                    () -> assertEquals(30, service.getTestType(ADMIN, "CT-HEAD").getDurationMinutes()),
                    () -> assertEquals(new AuditEntry(ADMIN, "PARAMETER_UPDATED", "TEST_TYPE:CT-HEAD"),
                            audit.entries().get(2)));
        }

        @Test
        @DisplayName("TC17S-15: the id of a test type cannot be changed")
        void idCannotBeChanged() throws Exception {
            // Arrange
            saveUrgentPriority();
            service.createTestType(ADMIN, headCt());

            // Act
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.updateTestType(ADMIN, "CT-HEAD", testType("CT-NEW", 20)));

            // Assert
            assertAll(
                    () -> assertEquals(List.of("The id of a test type cannot be changed"), error.getErrors()),
                    () -> assertEquals(20, service.getTestType(ADMIN, "CT-HEAD").getDurationMinutes()));
        }

        @Test
        @DisplayName("TC17S-16: reading or updating an id that does not exist, or a null id, is rejected")
        void unknownOrNullIdIsRejected() {
            assertAll(
                    () -> assertEquals(List.of("There is no test type with the id NOPE"),
                            assertThrows(ValidationException.class, () -> service.getTestType(ADMIN, "NOPE"))
                                    .getErrors()),
                    () -> assertThrows(ValidationException.class,
                            () -> service.updateTestType(ADMIN, "NOPE", testType("NOPE", 20))),
                    () -> assertThrows(ValidationException.class, () -> service.getTestType(ADMIN, null)));
        }

        @Test
        @DisplayName("TC17S-34: creating a test type whose priority does not exist is rejected and nothing is saved")
        void createWithUnknownPriorityIsRejected() throws Exception {
            // Arrange
            saveUrgentPriority();

            // Act
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.createTestType(ADMIN, testType("CT-HEAD", 20, "XYZ")));

            // Assert
            assertAll(
                    () -> assertEquals(List.of("The priority XYZ does not exist"), error.getErrors()),
                    () -> assertTrue(service.getTestTypes(ADMIN).isEmpty()),
                    () -> assertEquals(List.of(new AuditEntry(ADMIN, "PARAMETER_CREATED", "PRIORITY:URGENT")),
                            audit.entries()));
        }

        @Test
        @DisplayName("TC17S-34: with no priorities saved, the valid test type is rejected (URGENT does not exist)")
        void createWithoutAnyPriorityIsRejected() {
            // Act
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.createTestType(ADMIN, headCt()));

            // Assert
            assertAll(
                    () -> assertEquals(List.of("The priority URGENT does not exist"), error.getErrors()),
                    () -> assertTrue(service.getTestTypes(ADMIN).isEmpty()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("TC17S-34: an update with a priority that does not exist is rejected and changes nothing")
        void updateWithUnknownPriorityIsRejected() throws Exception {
            // Arrange
            saveUrgentPriority();
            service.createTestType(ADMIN, headCt());

            // Act
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.updateTestType(ADMIN, "CT-HEAD", testType("CT-HEAD", 30, "XYZ")));

            // Assert
            TestType stored = service.getTestType(ADMIN, "CT-HEAD");
            assertAll(
                    () -> assertEquals(List.of("The priority XYZ does not exist"), error.getErrors()),
                    () -> assertEquals("URGENT", stored.getPriority()),
                    () -> assertEquals(20, stored.getDurationMinutes()),
                    () -> assertFalse(audit.entries().stream().anyMatch(e -> e.action().equals("PARAMETER_UPDATED"))));
        }

        @Test
        @DisplayName("TC17S-34: a test type can be created and updated with another priority that exists")
        void existingPriorityIsAccepted() throws Exception {
            // Arrange
            saveUrgentPriority();
            service.savePriority(ADMIN, new Priority("ROUTINE", "Routine", "Weeks", true));
            service.createTestType(ADMIN, testType("CT-HEAD", 20, "ROUTINE"));

            // Act
            service.updateTestType(ADMIN, "CT-HEAD", testType("CT-HEAD", 20, "URGENT"));

            // Assert
            assertEquals("URGENT", service.getTestType(ADMIN, "CT-HEAD").getPriority());
        }

        @Test
        @DisplayName("TC17S-34: an existing test type can still be updated with its deactivated priority (Q9)")
        void deactivatedPriorityIsAccepted() throws Exception {
            // Arrange
            saveUrgentPriority();
            service.createTestType(ADMIN, headCt());
            service.deactivatePriority(ADMIN, "URGENT");

            // Act
            service.updateTestType(ADMIN, "CT-HEAD", testType("CT-HEAD", 30));

            // Assert
            assertAll(
                    () -> assertEquals(30, service.getTestType(ADMIN, "CT-HEAD").getDurationMinutes()),
                    () -> assertEquals("URGENT", service.getTestType(ADMIN, "CT-HEAD").getPriority()),
                    () -> assertFalse(service.getPriorities(ADMIN).get(0).isActive()));
        }

        @Test
        @DisplayName("TC17S-35: a new test type with a deactivated priority is rejected and nothing is saved (F-20)")
        void newTestTypeWithDeactivatedPriorityIsRejected() throws Exception {
            // Arrange
            saveUrgentPriority();
            service.deactivatePriority(ADMIN, "URGENT");
            int auditEntriesBefore = audit.entries().size();

            // Act
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.createTestType(ADMIN, testType("CT-NECK", 25)));

            // Assert
            assertAll(
                    () -> assertEquals(List.of("The priority URGENT is deactivated"), error.getErrors()),
                    () -> assertTrue(service.getTestTypes(ADMIN).isEmpty()),
                    () -> assertEquals(auditEntriesBefore, audit.entries().size()));
        }

        @Test
        @DisplayName("TC17S-35: a new test type with another active priority is still accepted")
        void newTestTypeWithActivePriorityIsAccepted() throws Exception {
            // Arrange
            saveUrgentPriority();
            service.savePriority(ADMIN, new Priority("ROUTINE", "Routine", "", true));
            service.deactivatePriority(ADMIN, "URGENT");

            // Act
            service.createTestType(ADMIN, testType("CT-NECK", 25, "ROUTINE"));

            // Assert
            assertEquals("ROUTINE", service.getTestType(ADMIN, "CT-NECK").getPriority());
        }

        @Test
        @DisplayName("TC17S-34: an empty priority only gets the error that it is required")
        void emptyPriorityOnlyGetsRequired() {
            // Act
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.createTestType(ADMIN, testType("CT-HEAD", 20, "")));

            // Assert
            assertEquals(List.of("The priority is required"), error.getErrors());
        }
    }

    @Nested
    @DisplayName("Protocols, schedules, appointment policies and priorities")
    class OtherGroupTests {

        @Test
        @DisplayName("TC17S-17: protocols, policies and priorities are saved and audited with their group")
        void saveSimpleEntries() throws Exception {
            // Act
            service.saveProtocol(ADMIN, new Protocol("P1", "Protocol", "Steps", true));
            service.saveAppointmentPolicy(ADMIN, new AppointmentPolicy("A1", "Policy", "Rule", true));
            service.savePriority(ADMIN, new Priority("URGENT", "Urgent", "Hours", true));

            // Assert
            assertAll(
                    () -> assertEquals(1, service.getProtocols(ADMIN).size()),
                    () -> assertEquals(1, service.getAppointmentPolicies(ADMIN).size()),
                    () -> assertEquals(1, service.getPriorities(ADMIN).size()),
                    () -> assertEquals(List.of(
                            new AuditEntry(ADMIN, "PARAMETER_CREATED", "PROTOCOL:P1"),
                            new AuditEntry(ADMIN, "PARAMETER_CREATED", "APPOINTMENT_POLICY:A1"),
                            new AuditEntry(ADMIN, "PARAMETER_CREATED", "PRIORITY:URGENT")), audit.entries()));
        }

        @Test
        @DisplayName("TC17S-18: saving again the same id replaces the entry and is audited as an update")
        void saveAgainReplaces() throws Exception {
            // Arrange
            service.saveProtocol(ADMIN, new Protocol("P1", "Old name", "", true));

            // Act
            service.saveProtocol(ADMIN, new Protocol("P1", "New name", "", true));

            // Assert
            assertAll(
                    () -> assertEquals(List.of("New name"),
                            service.getProtocols(ADMIN).stream().map(Protocol::getName).toList()),
                    () -> assertEquals(new AuditEntry(ADMIN, "PARAMETER_UPDATED", "PROTOCOL:P1"),
                            audit.entries().get(1)));
        }

        @Test
        @DisplayName("TC17S-19: a protocol, policy or priority without id and name, or null, is rejected")
        void simpleEntriesWithoutIdOrNameAreRejected() {
            assertAll(
                    () -> assertEquals(2, assertThrows(ValidationException.class,
                            () -> service.saveProtocol(ADMIN, new Protocol("", " ", null, true))).getErrors().size()),
                    () -> assertEquals(2, assertThrows(ValidationException.class,
                            () -> service.saveAppointmentPolicy(ADMIN, new AppointmentPolicy(null, "", null, true)))
                            .getErrors().size()),
                    () -> assertEquals(2, assertThrows(ValidationException.class,
                            () -> service.savePriority(ADMIN, new Priority("", "", null, true))).getErrors().size()),
                    () -> assertEquals(List.of("The protocol is required"), assertThrows(ValidationException.class,
                            () -> service.saveProtocol(ADMIN, null)).getErrors()),
                    () -> assertEquals(List.of("The appointment policy is required"),
                            assertThrows(ValidationException.class,
                                    () -> service.saveAppointmentPolicy(ADMIN, null)).getErrors()),
                    () -> assertEquals(List.of("The priority is required"), assertThrows(ValidationException.class,
                            () -> service.savePriority(ADMIN, null)).getErrors()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("TC17S-20: a valid schedule is saved and audited with its resource")
        void saveValidSchedule() throws Exception {
            // Act
            service.saveSchedule(ADMIN, schedule(LocalTime.of(8, 0), LocalTime.of(15, 0)));

            // Assert
            assertAll(
                    () -> assertEquals(WEEKDAYS, service.getSchedules(ADMIN).get(0).getDaysOfWeek()),
                    () -> assertEquals(List.of(new AuditEntry(ADMIN, "PARAMETER_CREATED", "SCHEDULE:Room CT-1")),
                            audit.entries()));
        }

        @Test
        @DisplayName("TC17S-21: the end time must be after the start time (boundary values)")
        void endTimeBoundaries() {
            LocalTime eight = LocalTime.of(8, 0);
            List<String> afterStart = List.of("The end time must be after the start time");
            assertAll(
                    () -> assertEquals(afterStart, assertThrows(ValidationException.class,
                            () -> service.saveSchedule(ADMIN, schedule(eight, eight))).getErrors()),
                    () -> assertEquals(afterStart, assertThrows(ValidationException.class,
                            () -> service.saveSchedule(ADMIN, schedule(LocalTime.of(15, 0), eight))).getErrors()),
                    () -> assertDoesNotThrow(() -> service.saveSchedule(ADMIN, schedule(eight, eight.plusMinutes(1)))));
        }

        @Test
        @DisplayName("TC17S-22: a schedule without resource, days or times gets one error for each problem")
        void incompleteScheduleGetsEveryError() {
            assertAll(
                    () -> assertEquals(3, assertThrows(ValidationException.class,
                            () -> service.saveSchedule(ADMIN, new Schedule("", Set.of(), null, null, true)))
                            .getErrors().size()),
                    () -> assertEquals(List.of("At least one day of the week is required"),
                            assertThrows(ValidationException.class, () -> service.saveSchedule(ADMIN,
                                    new Schedule("Room CT-1", null, LocalTime.of(8, 0), LocalTime.of(9, 0), true)))
                                    .getErrors()),
                    () -> assertEquals(List.of("The start and the end time are required"),
                            assertThrows(ValidationException.class, () -> service.saveSchedule(ADMIN,
                                    new Schedule("Room CT-1", WEEKDAYS, LocalTime.of(8, 0), null, true)))
                                    .getErrors()),
                    () -> assertEquals(List.of("The schedule is required"),
                            assertThrows(ValidationException.class, () -> service.saveSchedule(ADMIN, null))
                                    .getErrors()));
        }
    }

    @Nested
    @DisplayName("Deactivate instead of delete")
    class DeactivateTests {

        @Test
        @DisplayName("TC17S-23: every group is deactivated, kept with the same data, and audited")
        void deactivateEveryGroup() throws Exception {
            // Arrange: the priority first, because the test type uses it
            saveUrgentPriority();
            service.createTestType(ADMIN, headCt());
            service.saveProtocol(ADMIN, new Protocol("P1", "Protocol", "Steps", true));
            service.saveSchedule(ADMIN, schedule(LocalTime.of(8, 0), LocalTime.of(15, 0)));
            service.saveAppointmentPolicy(ADMIN, new AppointmentPolicy("A1", "Policy", "Rule", true));

            // Act
            service.deactivateTestType(ADMIN, "CT-HEAD");
            service.deactivateProtocol(ADMIN, "P1");
            service.deactivateSchedule(ADMIN, "Room CT-1");
            service.deactivateAppointmentPolicy(ADMIN, "A1");
            service.deactivatePriority(ADMIN, "URGENT");

            // Assert
            TestType testType = service.getTestType(ADMIN, "CT-HEAD");
            assertAll(
                    () -> assertFalse(testType.isActive()),
                    () -> assertEquals("Head CT", testType.getName()),
                    () -> assertEquals(20, testType.getDurationMinutes()),
                    () -> assertFalse(service.getProtocols(ADMIN).get(0).isActive()),
                    () -> assertFalse(service.getSchedules(ADMIN).get(0).isActive()),
                    () -> assertFalse(service.getAppointmentPolicies(ADMIN).get(0).isActive()),
                    () -> assertFalse(service.getPriorities(ADMIN).get(0).isActive()),
                    () -> assertEquals(List.of("TEST_TYPE:CT-HEAD", "PROTOCOL:P1", "SCHEDULE:Room CT-1",
                            "APPOINTMENT_POLICY:A1", "PRIORITY:URGENT"),
                            audit.entries().stream().filter(e -> e.action().equals("PARAMETER_DEACTIVATED"))
                                    .map(AuditEntry::targetResource).toList()));
        }

        @Test
        @DisplayName("TC17S-24: deactivating an entry that is already inactive changes nothing and is not audited again")
        void deactivateTwiceIsAuditedOnce() throws Exception {
            // Arrange
            saveUrgentPriority();
            service.createTestType(ADMIN, headCt());
            service.deactivateTestType(ADMIN, "CT-HEAD");

            // Act
            service.deactivateTestType(ADMIN, "CT-HEAD");

            // Assert
            assertEquals(1, audit.entries().stream().filter(e -> e.action().equals("PARAMETER_DEACTIVATED")).count());
        }

        @Test
        @DisplayName("TC17S-25: deactivating an id that does not exist is rejected")
        void deactivateUnknownIdIsRejected() {
            assertAll(
                    () -> assertEquals(List.of("There is no protocol with the id NOPE"),
                            assertThrows(ValidationException.class, () -> service.deactivateProtocol(ADMIN, "NOPE"))
                                    .getErrors()),
                    () -> assertThrows(ValidationException.class, () -> service.deactivateTestType(ADMIN, "NOPE")),
                    () -> assertThrows(ValidationException.class, () -> service.deactivateSchedule(ADMIN, "NOPE")),
                    () -> assertThrows(ValidationException.class,
                            () -> service.deactivateAppointmentPolicy(ADMIN, "NOPE")),
                    () -> assertThrows(ValidationException.class, () -> service.deactivatePriority(ADMIN, "NOPE")),
                    () -> assertTrue(audit.entries().isEmpty()));
        }
    }

    /**
     * Cases of the patch C2 1.1.2 (#174): the reads of the test types, schedules, appointment policies and priorities
     * also accept an active user with ORDER_PROCEDURES or MANAGE_APPOINTMENTS, while {@code getProtocols} and the
     * writes still need CONFIGURE_SYSTEM (test design {@code C2-server-patch-1.1.2-test-design.md}).
     */
    @Nested
    @DisplayName("Reads for ORDER_PROCEDURES and MANAGE_APPOINTMENTS (patch 1.1.2)")
    class ReadPermissionTests {
        private static final String ORDERING_PHYSICIAN = "druiz";
        private static final String ADMISSIONS = "aortega";
        private static final String INACTIVE_PHYSICIAN = "rmoreno";
        private static final String INACTIVE_ADMISSIONS = "cvega";
        private static final String CONFIGURATOR = "lmartin";
        private static final String DENIED = "PARAMETER_ACCESS_DENIED";
        private static final List<String> READ_GROUPS =
                List.of("TEST_TYPE", "TEST_TYPE", "SCHEDULE", "APPOINTMENT_POLICY", "PRIORITY");

        /** Saves, as the administrator, one entry of every group (five audit entries). */
        private void saveOneEntryOfEachGroup() throws Exception {
            saveUrgentPriority();
            service.createTestType(ADMIN, headCt());
            service.saveProtocol(ADMIN, new Protocol("P1", "Protocol", "Steps", true));
            service.saveSchedule(ADMIN, schedule(LocalTime.of(8, 0), LocalTime.of(15, 0)));
            service.saveAppointmentPolicy(ADMIN, new AppointmentPolicy("A1", "Policy", "Rule", true));
        }

        /** The five reads of the patch, in the order of {@link #READ_GROUPS}. */
        private List<Executable> reads(String requester) {
            return List.of(
                    () -> service.getTestTypes(requester),
                    () -> service.getTestType(requester, "CT-HEAD"),
                    () -> service.getSchedules(requester),
                    () -> service.getAppointmentPolicies(requester),
                    () -> service.getPriorities(requester));
        }

        /** The eleven writes, which keep CONFIGURE_SYSTEM. */
        private List<Executable> writes(String requester) {
            return List.of(
                    () -> service.createTestType(requester, testType("CT-NECK", 15)),
                    () -> service.updateTestType(requester, "CT-HEAD", testType("CT-HEAD", 30)),
                    () -> service.deactivateTestType(requester, "CT-HEAD"),
                    () -> service.saveProtocol(requester, new Protocol("P2", "Protocol", "", true)),
                    () -> service.deactivateProtocol(requester, "P1"),
                    () -> service.saveSchedule(requester, new Schedule("Room MR-1", WEEKDAYS,
                            LocalTime.of(8, 0), LocalTime.of(15, 0), true)),
                    () -> service.deactivateSchedule(requester, "Room CT-1"),
                    () -> service.saveAppointmentPolicy(requester, new AppointmentPolicy("A2", "Policy", "", true)),
                    () -> service.deactivateAppointmentPolicy(requester, "A1"),
                    () -> service.savePriority(requester, new Priority("ROUTINE", "Routine", "", true)),
                    () -> service.deactivatePriority(requester, "URGENT"));
        }

        /** Checks that the reads of {@code requester} return what the administrator reads. */
        private void assertReadsAsTheAdministrator(String requester) throws Exception {
            assertAll(
                    () -> assertEquals(service.getTestTypes(ADMIN), service.getTestTypes(requester)),
                    () -> assertSame(service.getTestType(ADMIN, "CT-HEAD"), service.getTestType(requester, "CT-HEAD")),
                    () -> assertEquals(service.getSchedules(ADMIN), service.getSchedules(requester)),
                    () -> assertEquals(service.getAppointmentPolicies(ADMIN),
                            service.getAppointmentPolicies(requester)),
                    () -> assertEquals(service.getPriorities(ADMIN), service.getPriorities(requester)),
                    () -> assertEquals(1, service.getTestTypes(requester).size()),
                    () -> assertEquals(1, service.getSchedules(requester).size()),
                    () -> assertEquals(1, service.getAppointmentPolicies(requester).size()),
                    () -> assertEquals(1, service.getPriorities(requester).size()));
        }

        @Test
        @DisplayName("TC17S-36: an active user with ORDER_PROCEDURES only reads the five groups, not audited")
        void orderingPhysicianReadsWithoutAudit() throws Exception {
            // Arrange
            saveOneEntryOfEachGroup();
            List<AuditEntry> before = List.copyOf(audit.entries());

            // Act and assert
            assertReadsAsTheAdministrator(ORDERING_PHYSICIAN);
            assertEquals(before, audit.entries());
        }

        @Test
        @DisplayName("TC17S-37: an active user with MANAGE_APPOINTMENTS only reads the five groups, not audited")
        void admissionsStaffReadsWithoutAudit() throws Exception {
            // Arrange
            saveOneEntryOfEachGroup();
            List<AuditEntry> before = List.copyOf(audit.entries());

            // Act and assert
            assertReadsAsTheAdministrator(ADMISSIONS);
            assertEquals(before, audit.entries());
        }

        @ParameterizedTest(name = "requester {0}")
        @ValueSource(strings = {ORDERING_PHYSICIAN, ADMISSIONS})
        @DisplayName("TC17S-38: ORDER_PROCEDURES or MANAGE_APPOINTMENTS cannot read the protocols, audited")
        void readPermissionCannotReadProtocols(String requester) throws Exception {
            // Arrange
            saveOneEntryOfEachGroup();
            int before = audit.entries().size();

            // Act
            assertThrows(AccessDeniedException.class, () -> service.getProtocols(requester));

            // Assert
            assertAll(
                    () -> assertEquals(before + 1, audit.entries().size()),
                    () -> assertEquals(new AuditEntry(requester, DENIED, "PROTOCOL"), audit.entries().get(before)));
        }

        @ParameterizedTest(name = "requester {0}")
        @ValueSource(strings = {ORDERING_PHYSICIAN, ADMISSIONS})
        @DisplayName("TC17S-39: ORDER_PROCEDURES or MANAGE_APPOINTMENTS cannot write, audited, nothing changes")
        void readPermissionCannotWrite(String requester) throws Exception {
            // Arrange
            saveOneEntryOfEachGroup();
            int before = audit.entries().size();
            List<Executable> calls = writes(requester);

            // Act
            for (Executable call : calls) {
                assertThrows(AccessDeniedException.class, call);
            }

            // Assert
            List<AuditEntry> denials = audit.entries().subList(before, audit.entries().size());
            assertAll(
                    () -> assertEquals(calls.size(), denials.size()),
                    () -> assertTrue(denials.stream().allMatch(
                            e -> e.author().equals(requester) && e.action().equals(DENIED))),
                    () -> assertEquals(List.of("TEST_TYPE", "TEST_TYPE", "TEST_TYPE", "PROTOCOL", "PROTOCOL",
                            "SCHEDULE", "SCHEDULE", "APPOINTMENT_POLICY", "APPOINTMENT_POLICY", "PRIORITY",
                            "PRIORITY"), denials.stream().map(AuditEntry::targetResource).toList()),
                    () -> assertEquals(20, service.getTestType(ADMIN, "CT-HEAD").getDurationMinutes()),
                    () -> assertTrue(service.getTestType(ADMIN, "CT-HEAD").isActive()),
                    () -> assertEquals(1, service.getTestTypes(ADMIN).size()),
                    () -> assertEquals(1, service.getProtocols(ADMIN).size()),
                    () -> assertTrue(service.getProtocols(ADMIN).get(0).isActive()),
                    () -> assertEquals(1, service.getSchedules(ADMIN).size()),
                    () -> assertTrue(service.getSchedules(ADMIN).get(0).isActive()),
                    () -> assertEquals(1, service.getAppointmentPolicies(ADMIN).size()),
                    () -> assertTrue(service.getAppointmentPolicies(ADMIN).get(0).isActive()),
                    () -> assertEquals(1, service.getPriorities(ADMIN).size()),
                    () -> assertTrue(service.getPriorities(ADMIN).get(0).isActive()));
        }

        @ParameterizedTest(name = "requester {0}")
        @NullSource
        @ValueSource(strings = {INACTIVE_PHYSICIAN, INACTIVE_ADMISSIONS, UNKNOWN})
        @DisplayName("TC17S-40: an inactive, unknown or null requester cannot read, and each denial names the group")
        void inactiveUnknownOrNullCannotRead(String requester) throws Exception {
            // Arrange
            saveOneEntryOfEachGroup();
            int before = audit.entries().size();
            List<Executable> calls = reads(requester);

            // Act
            for (Executable call : calls) {
                assertThrows(AccessDeniedException.class, call);
            }

            // Assert
            List<AuditEntry> expected = READ_GROUPS.stream()
                    .map(group -> new AuditEntry(requester, DENIED, group)).toList();
            assertEquals(expected, audit.entries().subList(before, audit.entries().size()));
        }

        @ParameterizedTest(name = "requester {0}")
        @NullSource
        @ValueSource(strings = {INACTIVE_PHYSICIAN, INACTIVE_ADMISSIONS, UNKNOWN})
        @DisplayName("TC17S-41: an inactive, unknown or null requester cannot read the protocols nor write, audited")
        void inactiveUnknownOrNullCannotReadProtocolsNorWrite(String requester) throws Exception {
            // Arrange
            saveOneEntryOfEachGroup();
            int before = audit.entries().size();
            List<Executable> calls = new ArrayList<>(writes(requester));
            calls.add(() -> service.getProtocols(requester));

            // Act
            for (Executable call : calls) {
                assertThrows(AccessDeniedException.class, call);
            }

            // Assert
            List<AuditEntry> denials = audit.entries().subList(before, audit.entries().size());
            assertAll(
                    () -> assertEquals(calls.size(), denials.size()),
                    () -> assertTrue(denials.stream().allMatch(
                            e -> Objects.equals(e.author(), requester) && e.action().equals(DENIED))),
                    () -> assertEquals(new AuditEntry(requester, DENIED, "PROTOCOL"), denials.get(calls.size() - 1)),
                    () -> assertEquals(1, service.getTestTypes(ADMIN).size()),
                    () -> assertEquals(1, service.getPriorities(ADMIN).size()));
        }

        @Test
        @DisplayName("TC17S-42: a user with CONFIGURE_SYSTEM only still reads and writes every group as before")
        void configuratorStillReadsAndWrites() throws Exception {
            // Arrange
            saveOneEntryOfEachGroup();
            int before = audit.entries().size();

            // Act
            service.saveProtocol(CONFIGURATOR, new Protocol("P2", "Protocol", "", true));
            service.deactivatePriority(CONFIGURATOR, "URGENT");

            // Assert
            assertReadsAsTheAdministrator(CONFIGURATOR);
            assertAll(
                    () -> assertEquals(2, service.getProtocols(CONFIGURATOR).size()),
                    () -> assertFalse(service.getPriorities(CONFIGURATOR).get(0).isActive()),
                    () -> assertEquals(List.of(
                            new AuditEntry(CONFIGURATOR, "PARAMETER_CREATED", "PROTOCOL:P2"),
                            new AuditEntry(CONFIGURATOR, "PARAMETER_DEACTIVATED", "PRIORITY:URGENT")),
                            audit.entries().subList(before, audit.entries().size())));
        }

        @ParameterizedTest(name = "requester {0}")
        @ValueSource(strings = {ORDERING_PHYSICIAN, ADMISSIONS})
        @DisplayName("TC17S-43: a test type that does not exist gives the same error as for the administrator")
        void unknownTestTypeGivesTheSameError(String requester) {
            // Act
            ValidationException error = assertThrows(ValidationException.class,
                    () -> service.getTestType(requester, "NOPE"));

            // Assert
            assertAll(
                    () -> assertEquals(List.of("There is no test type with the id NOPE"), error.getErrors()),
                    () -> assertEquals(assertThrows(ValidationException.class,
                            () -> service.getTestType(ADMIN, "NOPE")).getErrors(), error.getErrors()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }
    }
}
