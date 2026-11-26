package es.uclm.esi.iso2.server.appointments.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.AppointmentStatus;
import es.uclm.esi.iso2.common.appointments.domain.ScheduleEntry;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.appointments.persistence.InMemoryAppointmentRepository;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakePatientService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakeRoleManagementService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.RecordingAuditRecorder;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Tests of {@link DailyScheduleService} (UC14 server, C5 1.2.0, #137). Each test names the case of the test design
 * ({@code doc/testing/test-cases/UC14-server-test-design.md}, TC14S-xx) that it checks. The service reads an in-memory
 * repository with the appointments of the design and the test doubles of C1, C2 and C3.
 */
@DisplayName("DailyScheduleService")
class DailyScheduleServiceTest {
    private static final String STAFF = "lmartin";
    private static final String PHYSICIAN = "mgarcia";
    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);
    private static final LocalDate ONLY_CANCELLED = LocalDate.of(2026, 11, 26);
    private static final LocalDate EMPTY_DAY = LocalDate.of(2026, 11, 27);
    private static final TestType CHEST = testType("TT-01", "Chest X-ray", "Room 1", "Remove metal objects");
    private static final TestType BLOOD = testType("TT-02", "Blood test", "Room 2", "Fast for eight hours");

    private InMemoryAppointmentRepository appointments;
    private FakePatientService patients;
    private RecordingAuditRecorder audit;
    private int testTypeReads;
    private DailyScheduleService service;

    @BeforeEach
    void setUp() {
        appointments = new InMemoryAppointmentRepository();
        patients = new FakePatientService();
        audit = new RecordingAuditRecorder();
        testTypeReads = 0;
        service = new DailyScheduleService(appointments, patients, parameters(), new FakeRoleManagementService(),
                audit);
        save("APT-0001", DAY.atTime(9, 15), CHEST, "P-0002", PHYSICIAN, AppointmentStatus.BOOKED);
        save("APT-0002", DAY.atTime(9, 0), CHEST, "P-0001", PHYSICIAN, AppointmentStatus.RESCHEDULED);
        save("APT-0003", DAY.atTime(10, 0), BLOOD, "P-0001", "jfernandez", AppointmentStatus.BOOKED);
        save("APT-0004", DAY.atTime(9, 30), CHEST, "P-0002", PHYSICIAN, AppointmentStatus.CANCELLED);
        save("APT-0005", DAY.plusDays(1).atTime(9, 0), CHEST, "P-0001", PHYSICIAN, AppointmentStatus.BOOKED);
        save("APT-0006", ONLY_CANCELLED.atTime(9, 0), CHEST, "P-0001", PHYSICIAN, AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("TC14S-01 a requester without permission is denied in the three operations")
    void deniedWithoutPermission() {
        // Arrange
        List<Executable> operations = List.of(
                () -> service.getDailySchedule(PHYSICIAN, DAY),
                () -> service.getRoomWorklist(PHYSICIAN, DAY, "Room 1"),
                () -> service.getPhysicianWorklist(PHYSICIAN, DAY, PHYSICIAN));

        for (Executable operation : operations) {
            audit.clear();
            // Act
            assertThrows(AccessDeniedException.class, operation);
            // Assert
            assertAudited(PHYSICIAN, "SCHEDULE_ACCESS_DENIED");
        }
        assertAll(
                () -> assertEquals(0, patients.getPatientCalls()),
                () -> assertEquals(0, testTypeReads));
    }

    @Test
    @DisplayName("TC14S-02 the daily schedule is grouped by room and in time order")
    void dailySchedule() throws AccessDeniedException, ValidationException {
        // Act
        Map<String, List<ScheduleEntry>> schedule = service.getDailySchedule(STAFF, DAY);

        // Assert
        assertAll(
                () -> assertEquals(2, schedule.size()),
                () -> assertEquals(List.of("APT-0002", "APT-0001"), idsOf(schedule.get("Room 1"))),
                () -> assertEquals(List.of("APT-0003"), idsOf(schedule.get("Room 2"))),
                () -> assertAudited(STAFF, "SCHEDULE_ACCESSED", DAY.toString()));
    }

    @Test
    @DisplayName("TC14S-03 a line has the appointment, the patient, the test type and its preparation")
    void lineOfTheSchedule() throws AccessDeniedException, ValidationException {
        // Act
        ScheduleEntry line = service.getDailySchedule(STAFF, DAY).get("Room 2").get(0);

        // Assert
        assertAll(
                () -> assertSame(appointments.findById("APT-0003"), line.getAppointment()),
                () -> assertEquals("Lucía Ejemplo Prueba", line.getPatientName()),
                () -> assertEquals("Blood test", line.getTestTypeName()),
                () -> assertEquals("Fast for eight hours", line.getPreparation()));
    }

    @Test
    @DisplayName("TC14S-04 a day with only a cancelled appointment has an empty schedule")
    void dayWithOnlyACancelledAppointment() throws AccessDeniedException, ValidationException {
        // Act
        Map<String, List<ScheduleEntry>> schedule = service.getDailySchedule(STAFF, ONLY_CANCELLED);

        // Assert
        assertAll(
                () -> assertTrue(schedule.isEmpty()),
                () -> assertAudited(STAFF, "SCHEDULE_ACCESSED", ONLY_CANCELLED.toString()));
    }

    @Test
    @DisplayName("TC14S-05 a day without appointments has an empty schedule")
    void dayWithoutAppointments() throws AccessDeniedException, ValidationException {
        // Act
        Map<String, List<ScheduleEntry>> schedule = service.getDailySchedule(STAFF, EMPTY_DAY);

        // Assert
        assertAll(
                () -> assertTrue(schedule.isEmpty()),
                () -> assertAudited(STAFF, "SCHEDULE_ACCESSED", EMPTY_DAY.toString()));
    }

    @Test
    @DisplayName("TC14S-06 the daily schedule needs a day")
    void dailyScheduleWithoutDay() {
        assertRejected(() -> service.getDailySchedule(STAFF, null), 1);
    }

    @Test
    @DisplayName("TC14S-07 the worklist of a room is in time order")
    void roomWorklist() throws AccessDeniedException, ValidationException {
        // Act
        List<ScheduleEntry> worklist = service.getRoomWorklist(STAFF, DAY, "Room 1");

        // Assert
        assertAll(
                () -> assertEquals(List.of("APT-0002", "APT-0001"), idsOf(worklist)),
                () -> assertAudited(STAFF, "WORKLIST_ACCESSED", "Room 1", DAY.toString()));
    }

    @Test
    @DisplayName("TC14S-08 a room without appointments has an empty worklist")
    void roomWithoutAppointments() throws AccessDeniedException, ValidationException {
        // Act
        List<ScheduleEntry> worklist = service.getRoomWorklist(STAFF, DAY, "Room 9");

        // Assert
        assertAll(
                () -> assertTrue(worklist.isEmpty()),
                () -> assertAudited(STAFF, "WORKLIST_ACCESSED", "Room 9", DAY.toString()));
    }

    @Test
    @DisplayName("TC14S-09 the worklist of a room needs a day and a room")
    void roomWorklistWithoutData() {
        assertRejected(() -> service.getRoomWorklist(STAFF, null, null), 2);
    }

    @Test
    @DisplayName("TC14S-10 the worklist of a room rejects a blank room")
    void roomWorklistWithBlankRoom() {
        assertRejected(() -> service.getRoomWorklist(STAFF, DAY, " "), 1);
    }

    @Test
    @DisplayName("TC14S-11 the worklist of a physician is in time order")
    void physicianWorklist() throws AccessDeniedException, ValidationException {
        // Act
        List<ScheduleEntry> worklist = service.getPhysicianWorklist(STAFF, DAY, PHYSICIAN);

        // Assert
        assertAll(
                () -> assertEquals(List.of("APT-0002", "APT-0001"), idsOf(worklist)),
                () -> assertAudited(STAFF, "WORKLIST_ACCESSED", PHYSICIAN, DAY.toString()));
    }

    @Test
    @DisplayName("TC14S-12 a physician without appointments has an empty worklist")
    void physicianWithoutAppointments() throws AccessDeniedException, ValidationException {
        // Act
        List<ScheduleEntry> worklist = service.getPhysicianWorklist(STAFF, DAY, "nobody");

        // Assert
        assertAll(
                () -> assertTrue(worklist.isEmpty()),
                () -> assertAudited(STAFF, "WORKLIST_ACCESSED", "nobody", DAY.toString()));
    }

    @Test
    @DisplayName("TC14S-13 the worklist of a physician needs a day and a physician")
    void physicianWorklistWithoutData() {
        assertRejected(() -> service.getPhysicianWorklist(STAFF, null, " "), 2);
    }

    @Test
    @DisplayName("TC14S-14 an appointment of an unknown patient rejects the schedule and registers no access")
    void unknownPatient() {
        // Arrange
        save("APT-0007", DAY.atTime(11, 0), CHEST, "P-9999", PHYSICIAN, AppointmentStatus.BOOKED);

        // Act and assert
        assertRejected(() -> service.getDailySchedule(STAFF, DAY), 1);
    }

    @Test
    @DisplayName("TC14S-15 an appointment of an unknown test type rejects the worklist and registers no access")
    void unknownTestType() {
        // Arrange
        TestType unknown = testType("TT-99", "Unknown", "Room 1", null);
        save("APT-0007", DAY.atTime(11, 0), unknown, "P-0001", PHYSICIAN, AppointmentStatus.BOOKED);

        // Act and assert
        assertRejected(() -> service.getPhysicianWorklist(STAFF, DAY, PHYSICIAN), 1);
    }

    /** C2: it only reads {@code TT-01} and {@code TT-02}, each with a preparation, and counts the reads. */
    private ISystemParameterService parameters() {
        return (ISystemParameterService) Proxy.newProxyInstance(ISystemParameterService.class.getClassLoader(),
                new Class<?>[] {ISystemParameterService.class}, (proxy, method, arguments) -> {
                    if (!"getTestType".equals(method.getName())) {
                        throw new UnsupportedOperationException("Not used by UC14");
                    }
                    testTypeReads++;
                    return List.of(CHEST, BLOOD).stream()
                            .filter(testType -> testType.getId().equals(arguments[1]))
                            .findFirst()
                            .orElseThrow(() -> new ValidationException(
                                    List.of("There is no test type with the id " + arguments[1])));
                });
    }

    private static TestType testType(String id, String name, String room, String preparation) {
        return new TestType(id, name, "Modality", room, 15, preparation, false, false, null, "ROUTINE", false, true);
    }

    private void save(String id, LocalDateTime start, TestType testType, String patientId, String physician,
            AppointmentStatus status) {
        appointments.save(new Appointment(id, "NEED-" + id, patientId, testType.getId(), testType.getRoom(),
                physician, start, start.plusMinutes(15), status));
    }

    private static List<String> idsOf(List<ScheduleEntry> entries) {
        return entries.stream().map(entry -> entry.getAppointment().getAppointmentId()).toList();
    }

    private void assertRejected(Executable operation, int errors) {
        ValidationException exception = assertThrows(ValidationException.class, operation);
        assertAll(
                () -> assertEquals(errors, exception.getErrors().size()),
                () -> assertTrue(audit.entries().isEmpty()));
    }

    /** Checks that there is exactly one audit entry, with that author, action and parts of the resource. */
    private void assertAudited(String author, String action, String... resourceParts) {
        assertEquals(1, audit.entries().size());
        RecordingAuditRecorder.AuditEntry entry = audit.entries().get(0);
        assertAll(
                () -> assertEquals(author, entry.author()),
                () -> assertEquals(action, entry.action()),
                () -> {
                    for (String part : resourceParts) {
                        assertTrue(entry.targetResource().contains(part), entry.targetResource());
                    }
                });
    }
}
