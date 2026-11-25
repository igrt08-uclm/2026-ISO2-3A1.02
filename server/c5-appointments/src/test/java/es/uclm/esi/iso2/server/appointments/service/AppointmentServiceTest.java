package es.uclm.esi.iso2.server.appointments.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.AppointmentStatus;
import es.uclm.esi.iso2.common.appointments.domain.PendingNeedView;
import es.uclm.esi.iso2.common.appointments.domain.TimeSlot;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeedStatus;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.AppointmentRepositoryFake;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakeClinicalOrderService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakePatientService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakeRoleManagementService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakeSystemParameterService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.OrderWithoutPriority;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.RecordingAuditRecorder;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.RecordingAuditRecorder.AuditEntry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of {@link AppointmentService} (UC13 server, C5 1.0.0, #131). Each test names the case of the test design
 * (`doc/testing/test-cases/UC13-server-test-design.md`, TC13S-xx) that it checks. The service uses an empty in-memory
 * repository, the test doubles of C1, C2, C3 and C4, and a fixed clock at 24/11/2026 08:00, a Tuesday, so the schedule
 * of Room 1 (Tuesdays from 09:00 to 11:00) is not in the past. The cases about the past use a second service on the
 * same doubles and repository with the clock at 24/11/2026 10:00 ({@link #serviceAtTen()}).
 */
@DisplayName("AppointmentService")
class AppointmentServiceTest {
    private static final String STAFF = "lmartin";
    private static final String ADMIN = "admin";
    private static final String PHYSICIAN = "mgarcia";
    private static final String INACTIVE_USER = "pnavarro";
    private static final String UNKNOWN = "nobody";

    private static final String ACCESSED = "APPOINTMENT_ACCESSED";
    private static final String BOOKED = "APPOINTMENT_BOOKED";
    private static final String RESCHEDULED = "APPOINTMENT_RESCHEDULED";
    private static final String CANCELLED = "APPOINTMENT_CANCELLED";
    private static final String DENIED = "APPOINTMENT_ACCESS_DENIED";
    private static final String APPOINTMENTS = "appointments";

    private static final String PATIENT = "P-0001";
    private static final String TEST_TYPE = "TT-01";
    private static final String ROOM = "Room 1";
    private static final String NEEDED = "NEED-0001";
    private static final String ORDER = "ORD-0001";

    private static final LocalDate TUESDAY = LocalDate.of(2026, 11, 24);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-24T08:00:00Z"), ZoneOffset.UTC);

    private FakeRoleManagementService roles;
    private RecordingAuditRecorder audit;
    private FakePatientService patients;
    private FakeSystemParameterService parameters;
    private FakeClinicalOrderService orders;
    private AppointmentRepositoryFake appointments;
    private AppointmentService service;

    @BeforeEach
    void setUp() {
        roles = new FakeRoleManagementService();
        audit = new RecordingAuditRecorder();
        patients = new FakePatientService();
        parameters = new FakeSystemParameterService();
        orders = new FakeClinicalOrderService();
        appointments = new AppointmentRepositoryFake();
        service = new AppointmentService(appointments, orders, patients, parameters, roles, audit, CLOCK);
        orders.addOrder(new DiagnosticOrder(ORDER, PATIENT, TEST_TYPE, PHYSICIAN, "Chest pain", "Rule out pneumonia",
                LocalDate.of(2026, 11, 23), "URGENT"));
        orders.addNeed(new SchedulingNeed(NEEDED, ORDER, LocalDate.of(2026, 11, 23), SchedulingNeedStatus.PENDING));
    }

    private static TimeSlot slot(int hour, int minute) {
        LocalDateTime start = LocalDateTime.of(TUESDAY, LocalTime.of(hour, minute));
        return new TimeSlot(ROOM, start, start.plusMinutes(15));
    }

    private static TimeSlot slotOf(String room, int hour, int minute) {
        LocalDateTime start = LocalDateTime.of(TUESDAY, LocalTime.of(hour, minute));
        return new TimeSlot(room, start, start.plusMinutes(15));
    }

    private List<String> errorsOf(Executable call) {
        return assertThrows(ValidationException.class, call).getErrors();
    }

    private void assertDenied(String requester, Executable call, String resource) {
        AccessDeniedException denied = assertThrows(AccessDeniedException.class, call);
        assertEquals(requester, denied.getRequester());
        assertEquals(List.of(new AuditEntry(requester, DENIED, resource)), audit.entries());
    }

    private Appointment bookFirst() throws AccessDeniedException, ValidationException {
        return service.bookAppointment(STAFF, NEEDED, slot(9, 0));
    }

    /** A service on the same doubles and repository whose clock is 24/11/2026 10:00, in the middle of the schedule. */
    private AppointmentService serviceAtTen() {
        Clock ten = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC);
        return new AppointmentService(appointments, orders, patients, parameters, roles, audit, ten);
    }

    /** Adds a pending need {@code NEED-0002} of a diagnostic order {@code ORD-0002} of mgarcia. */
    private void addSecondNeed(String patientId, String testTypeId) {
        orders.addOrder(new DiagnosticOrder("ORD-0002", patientId, testTypeId, PHYSICIAN, "Control", "Control",
                LocalDate.of(2026, 11, 23), "ROUTINE"));
        orders.addNeed(new SchedulingNeed("NEED-0002", "ORD-0002", LocalDate.of(2026, 11, 23),
                SchedulingNeedStatus.PENDING));
    }

    private static List<LocalDateTime> startsOf(List<TimeSlot> slots) {
        return slots.stream().map(TimeSlot::getStart).toList();
    }

    private static LocalDateTime at(LocalDate day, int hour, int minute) {
        return LocalDateTime.of(day, LocalTime.of(hour, minute));
    }

    @Nested
    @DisplayName("1. Permission")
    class Permission {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { ADMIN, PHYSICIAN, INACTIVE_USER, UNKNOWN })
        @DisplayName("TC13S-01 a user without MANAGE_APPOINTMENTS is denied in every operation")
        void userWithoutPermissionIsDenied(String requester) {
            List<Executable> operations = List.of(
                    () -> service.getPendingSchedulingNeeds(requester),
                    () -> service.getFreeSlots(requester, TEST_TYPE, TUESDAY, TUESDAY),
                    () -> service.bookAppointment(requester, NEEDED, slot(9, 0)),
                    () -> service.rescheduleAppointment(requester, "APT-0001", slot(10, 0)),
                    () -> service.cancelAppointment(requester, "APT-0001"),
                    () -> service.getAppointments(requester),
                    () -> service.getAppointmentsOfPatient(requester, PATIENT));
            for (Executable operation : operations) {
                audit.clear();
                assertDenied(requester, operation, APPOINTMENTS);
            }
            assertTrue(appointments.findAll().isEmpty());
        }

        @Test
        @DisplayName("TC13S-02 a denied attempt reaches neither C2, C3 nor C4")
        void deniedAttemptDoesNotReachOtherComponents() {
            assertDenied(ADMIN, () -> service.bookAppointment(ADMIN, NEEDED, slot(9, 0)), APPOINTMENTS);
            assertAll(
                    () -> assertEquals(0, parameters.calls()),
                    () -> assertEquals(0, patients.getPatientCalls()),
                    () -> assertEquals(0, orders.pendingCalls()),
                    () -> assertTrue(appointments.findAll().isEmpty()));
        }
    }

    @Nested
    @DisplayName("2. The free slots of D40")
    class FreeSlots {

        @Test
        @DisplayName("TC13S-95 31 inclusive days include slots on the final Tuesday")
        void accepts31Days() throws AccessDeniedException, ValidationException {
            // Arrange
            LocalDate first = TUESDAY.plusDays(5);
            LocalDate last = first.plusDays(30);

            // Act
            List<TimeSlot> slots = service.getFreeSlots(STAFF, TEST_TYPE, first, last);

            // Assert
            assertEquals(40, slots.size());
            assertEquals(at(last, 11, 0), slots.getLast().getEnd());
        }

        @Test
        @DisplayName("TC13S-96 32 inclusive days are rejected")
        void rejects32Days() {
            // Arrange
            LocalDate last = TUESDAY.plusDays(31);

            // Act
            List<String> errors = errorsOf(() -> service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, last));

            // Assert
            assertEquals(List.of("The range cannot be longer than 31 days"), errors);
        }

        @Test
        @DisplayName("TC13S-97 an active test type with zero duration is rejected")
        void zeroDuration() {
            // Arrange
            parameters.addTestType(new TestType("TT-ZERO", "Zero duration", "X-ray", ROOM, 0, null, false,
                    false, null, "ROUTINE", false, true));
            LocalDate wednesday = TUESDAY.plusDays(1);

            // Act
            List<String> errors = errorsOf(() -> service.getFreeSlots(STAFF, "TT-ZERO", wednesday, wednesday));

            // Assert
            assertEquals(List.of("The test type TT-ZERO has no duration"), errors);
        }


        @Test
        @DisplayName("TC13S-03 the slots of the schedule of the room, one every duration")
        void slotsOfTheSchedule() throws AccessDeniedException, ValidationException {
            List<TimeSlot> free = service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, TUESDAY);
            assertAll(
                    () -> assertEquals(8, free.size()),
                    () -> assertEquals(LocalDateTime.of(TUESDAY, LocalTime.of(9, 0)),
                            free.get(0).getStart()),
                    () -> assertEquals(LocalDateTime.of(TUESDAY, LocalTime.of(9, 15)),
                            free.get(1).getStart()),
                    () -> assertEquals(LocalDateTime.of(TUESDAY, LocalTime.of(11, 0)),
                            free.get(7).getEnd()),
                    () -> assertEquals(ROOM, free.get(0).getRoom()));
        }

        @Test
        @DisplayName("TC13S-04 a day without schedule has no slots")
        void dayWithoutSchedule() throws AccessDeniedException, ValidationException {
            assertTrue(service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY.plusDays(1), TUESDAY.plusDays(1)).isEmpty());
        }

        @Test
        @DisplayName("TC13S-05 a booked slot is not offered and a cancelled one is")
        void bookedAndCancelledSlots() throws AccessDeniedException, ValidationException {
            Appointment booked = bookFirst();
            assertAll(
                    () -> assertTrue(service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, TUESDAY).stream()
                            .noneMatch(free -> free.getStart().equals(booked.getStart()))),
                    () -> assertEquals(7, service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, TUESDAY).size()));
            service.cancelAppointment(STAFF, booked.getAppointmentId());
            assertEquals(8, service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, TUESDAY).size());
        }

        @Test
        @DisplayName("TC13S-06 a range that ends before it starts is rejected")
        void wrongRange() {
            assertEquals(List.of("The last date cannot be before the first one"),
                    errorsOf(() -> service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, TUESDAY.minusDays(1))));
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("TC13S-07 a missing range is rejected")
        void missingRange(LocalDate date) {
            assertEquals(List.of("The date range is required"),
                    errorsOf(() -> service.getFreeSlots(STAFF, TEST_TYPE, date, TUESDAY)));
        }

        @Test
        @DisplayName("TC13S-08 an inactive test type has no slots")
        void inactiveTestType() {
            assertEquals(List.of("The test type TT-03 is not active"),
                    errorsOf(() -> service.getFreeSlots(STAFF, "TT-03", TUESDAY, TUESDAY)));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("TC13S-33 a blank test type is rejected")
        void blankTestType(String testTypeId) {
            assertEquals(List.of("The test type is required"),
                    errorsOf(() -> service.getFreeSlots(STAFF, testTypeId, TUESDAY, TUESDAY)));
        }

        @Test
        @DisplayName("TC13S-34 an unknown test type is rejected with the message of C2")
        void unknownTestType() {
            assertEquals(List.of("There is no test type with the id TT-99"),
                    errorsOf(() -> service.getFreeSlots(STAFF, "TT-99", TUESDAY, TUESDAY)));
        }

        @Test
        @DisplayName("TC13S-35 a missing last date is rejected")
        void missingLastDate() {
            assertEquals(List.of("The date range is required"),
                    errorsOf(() -> service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, null)));
        }

        @Test
        @DisplayName("TC13S-36 the slots of a second test type use its room and its duration")
        void slotsOfSecondTestType() throws AccessDeniedException, ValidationException {
            List<TimeSlot> free = service.getFreeSlots(STAFF, "TT-02", TUESDAY, TUESDAY);
            assertAll(
                    () -> assertEquals(8, free.size()),
                    () -> assertEquals(List.of(at(TUESDAY, 9, 0), at(TUESDAY, 9, 30), at(TUESDAY, 10, 0),
                            at(TUESDAY, 10, 30), at(TUESDAY, 11, 0), at(TUESDAY, 11, 30), at(TUESDAY, 12, 0),
                            at(TUESDAY, 12, 30)), startsOf(free)),
                    () -> assertEquals(at(TUESDAY, 13, 0), free.get(7).getEnd()),
                    () -> assertTrue(free.stream().allMatch(slot -> "Room 2".equals(slot.getRoom()))));
        }

        @Test
        @DisplayName("TC13S-37 a room whose only schedule is inactive has no slots")
        void inactiveSchedule() throws AccessDeniedException, ValidationException {
            assertTrue(service.getFreeSlots(STAFF, "TT-04", TUESDAY, TUESDAY).isEmpty());
        }

        @Test
        @DisplayName("TC13S-38 a two-day range gives the slots of the day with schedule")
        void twoDayRange() throws AccessDeniedException, ValidationException {
            List<TimeSlot> free = service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, TUESDAY.plusDays(1));
            assertAll(
                    () -> assertEquals(8, free.size()),
                    () -> assertTrue(free.stream().allMatch(slot -> TUESDAY.equals(slot.getStart().toLocalDate()))));
        }

        @Test
        @DisplayName("TC13S-39 a range of two Tuesdays gives the slots of both, in chronological order")
        void twoTuesdays() throws AccessDeniedException, ValidationException {
            LocalDate nextTuesday = TUESDAY.plusWeeks(1);
            List<TimeSlot> free = service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, nextTuesday);
            assertAll(
                    () -> assertEquals(16, free.size()),
                    () -> assertEquals(at(TUESDAY, 10, 45), free.get(7).getStart()),
                    () -> assertEquals(at(nextTuesday, 9, 0), free.get(8).getStart()),
                    () -> assertEquals(at(nextTuesday, 11, 0), free.get(15).getEnd()));
        }

        @Test
        @DisplayName("TC13S-40 the slot of a rescheduled appointment is not offered and its old slot is")
        void rescheduledSlotIsOccupied() throws AccessDeniedException, ValidationException {
            Appointment booked = bookFirst();
            service.rescheduleAppointment(STAFF, booked.getAppointmentId(), slot(10, 0));
            List<LocalDateTime> starts = startsOf(service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, TUESDAY));
            assertAll(
                    () -> assertEquals(7, starts.size()),
                    () -> assertTrue(starts.contains(at(TUESDAY, 9, 0))),
                    () -> assertTrue(!starts.contains(at(TUESDAY, 10, 0))));
        }

        @Test
        @DisplayName("TC13S-41 the slots of the past are not offered and the one starting now is")
        void slotsOfThePastAreSkipped() throws AccessDeniedException, ValidationException {
            List<TimeSlot> free = serviceAtTen().getFreeSlots(STAFF, TEST_TYPE, TUESDAY, TUESDAY);
            assertEquals(List.of(at(TUESDAY, 10, 0), at(TUESDAY, 10, 15), at(TUESDAY, 10, 30), at(TUESDAY, 10, 45)),
                    startsOf(free));
        }
    }

    @Nested
    @DisplayName("3. Booking")
    class Booking {

        @Test
        @DisplayName("TC13S-90 a failed save leaves the need pending")
        void bookingSaveFails() {
            // Arrange
            appointments.rejectSaves();

            // Act
            assertThrows(IllegalStateException.class, AppointmentServiceTest.this::bookFirst);

            // Assert
            assertTrue(appointments.findAll().isEmpty());
            assertEquals(SchedulingNeedStatus.PENDING, orders.need(NEEDED).getStatus());
        }


        @Test
        @DisplayName("TC13S-09 the booking keeps the need, the patient and the physician of the order")
        void validBooking() throws AccessDeniedException, ValidationException {
            Appointment created = service.bookAppointment(STAFF, NEEDED, slot(9, 0));
            assertAll(
                    () -> assertEquals("APT-0001", created.getAppointmentId()),
                    () -> assertEquals(NEEDED, created.getNeedId()),
                    () -> assertEquals(PATIENT, created.getPatientId()),
                    () -> assertEquals(TEST_TYPE, created.getTestTypeId()),
                    () -> assertEquals(ROOM, created.getRoom()),
                    () -> assertEquals(PHYSICIAN, created.getPhysician()),
                    () -> assertEquals(slot(9, 0).getStart(), created.getStart()),
                    () -> assertEquals(AppointmentStatus.BOOKED, created.getStatus()),
                    () -> assertSame(created, appointments.findById("APT-0001")));
        }

        @Test
        @DisplayName("TC13S-10 booking marks the need as booked in C4 and is audited")
        void needIsBookedInC4() throws AccessDeniedException, ValidationException {
            bookFirst();
            assertAll(
                    () -> assertEquals(SchedulingNeedStatus.BOOKED, orders.need(NEEDED).getStatus()),
                    () -> assertEquals(List.of(new AuditEntry(STAFF, BOOKED, "APPOINTMENT:APT-0001")),
                            audit.entries()));
        }

        @Test
        @DisplayName("TC13S-11 a second booking takes the next id")
        void secondBooking() throws AccessDeniedException, ValidationException {
            bookFirst();
            orders.addOrder(new DiagnosticOrder("ORD-0002", "P-0002", TEST_TYPE, PHYSICIAN, "Control", "Control",
                    LocalDate.of(2026, 11, 23), "ROUTINE"));
            orders.addNeed(new SchedulingNeed("NEED-0002", "ORD-0002", LocalDate.of(2026, 11, 23),
                    SchedulingNeedStatus.PENDING));
            assertEquals("APT-0002", service.bookAppointment(STAFF, "NEED-0002", slot(9, 15)).getAppointmentId());
        }

        @Test
        @DisplayName("TC13S-12 a need that is not pending is rejected")
        void needNotPending() {
            assertEquals(List.of("The scheduling need NEED-9999 is not pending"),
                    errorsOf(() -> service.bookAppointment(STAFF, "NEED-9999", slot(9, 0))));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("TC13S-13 a blank need is rejected")
        void blankNeed(String needId) {
            assertEquals(List.of("The scheduling need is required"),
                    errorsOf(() -> service.bookAppointment(STAFF, needId, slot(9, 0))));
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("TC13S-14 a missing slot is rejected")
        void missingSlot(TimeSlot slot) {
            assertEquals(List.of("The slot is required"),
                    errorsOf(() -> service.bookAppointment(STAFF, NEEDED, slot)));
        }

        @Test
        @DisplayName("TC13S-15 a slot of another room is rejected")
        void slotOfAnotherRoom() {
            assertEquals(List.of("The room Room 2 is not the room of the test type TT-01"),
                    errorsOf(() -> service.bookAppointment(STAFF, NEEDED, slotOf("Room 2", 9, 0))));
        }

        @Test
        @DisplayName("TC13S-16 a slot outside the schedule, on the grid, is rejected")
        void slotOutsideSchedule() {
            List<String> errors = errorsOf(() -> service.bookAppointment(STAFF, NEEDED, slot(12, 0)));
            assertEquals(List.of("The slot 24/11/2026 12:00 is not inside the schedule of the room Room 1"), errors);
        }

        @Test
        @DisplayName("TC13S-17 a slot in the past, inside the schedule and free, is rejected")
        void slotInThePast() {
            AppointmentService atTen = serviceAtTen();
            assertEquals(List.of("The slot 24/11/2026 09:00 is in the past"),
                    errorsOf(() -> atTen.bookAppointment(STAFF, NEEDED, slot(9, 0))));
        }

        @Test
        @DisplayName("TC13S-18 a slot already taken is rejected")
        void slotTaken() throws AccessDeniedException, ValidationException {
            bookFirst();
            orders.addOrder(new DiagnosticOrder("ORD-0002", "P-0002", TEST_TYPE, PHYSICIAN, "Control", "Control",
                    LocalDate.of(2026, 11, 23), "ROUTINE"));
            orders.addNeed(new SchedulingNeed("NEED-0002", "ORD-0002", LocalDate.of(2026, 11, 23),
                    SchedulingNeedStatus.PENDING));
            assertEquals(List.of("The slot 24/11/2026 09:00 is not free"),
                    errorsOf(() -> service.bookAppointment(STAFF, "NEED-0002", slot(9, 0))));
        }

        @Test
        @DisplayName("TC13S-19 an inactive patient is rejected")
        void inactivePatient() throws AccessDeniedException, ValidationException {
            orders.addOrder(new DiagnosticOrder("ORD-0002", "P-0004", TEST_TYPE, PHYSICIAN, "Control", "Control",
                    LocalDate.of(2026, 11, 23), "ROUTINE"));
            orders.addNeed(new SchedulingNeed("NEED-0002", "ORD-0002", LocalDate.of(2026, 11, 23),
                    SchedulingNeedStatus.PENDING));
            assertEquals(List.of("The patient P-0004 is not active"),
                    errorsOf(() -> service.bookAppointment(STAFF, "NEED-0002", slot(9, 0))));
        }

        @Test
        @DisplayName("TC13S-20 a rejected booking saves nothing and leaves the need pending")
        void rejectedBookingSavesNothing() {
            errorsOf(() -> service.bookAppointment(STAFF, NEEDED, slotOf("Room 2", 9, 0)));
            assertAll(
                    () -> assertTrue(appointments.findAll().isEmpty()),
                    () -> assertEquals(SchedulingNeedStatus.PENDING, orders.need(NEEDED).getStatus()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("TC13S-42 a slot that starts now is not in the past and is booked")
        void slotStartingNow() throws AccessDeniedException, ValidationException {
            Appointment created = serviceAtTen().bookAppointment(STAFF, NEEDED, slot(10, 0));
            assertAll(
                    () -> assertEquals("APT-0001", created.getAppointmentId()),
                    () -> assertEquals(at(TUESDAY, 10, 0), created.getStart()),
                    () -> assertEquals(AppointmentStatus.BOOKED, created.getStatus()));
        }

        @Test
        @DisplayName("TC13S-43 the last slot before now is in the past")
        void lastSlotBeforeNow() {
            AppointmentService atTen = serviceAtTen();
            assertEquals(List.of("The slot 24/11/2026 09:45 is in the past"),
                    errorsOf(() -> atTen.bookAppointment(STAFF, NEEDED, slot(9, 45))));
        }

        @Test
        @DisplayName("TC13S-44 the last slot of the schedule, ending when the schedule ends, is booked")
        void lastSlotOfTheSchedule() throws AccessDeniedException, ValidationException {
            Appointment created = service.bookAppointment(STAFF, NEEDED, slot(10, 45));
            assertAll(
                    () -> assertEquals(at(TUESDAY, 10, 45), created.getStart()),
                    () -> assertEquals(at(TUESDAY, 11, 0), created.getEnd()));
        }

        @Test
        @DisplayName("TC13S-45 a slot that ends after the schedule is rejected")
        void slotEndingAfterTheSchedule() {
            assertEquals(List.of("The slot 24/11/2026 11:00 is not inside the schedule of the room Room 1"),
                    errorsOf(() -> service.bookAppointment(STAFF, NEEDED, slot(11, 0))));
        }

        @Test
        @DisplayName("TC13S-46 a slot on a day without schedule is rejected")
        void slotOnADayWithoutSchedule() {
            LocalDateTime wednesday = at(TUESDAY.plusDays(1), 9, 0);
            TimeSlot slot = new TimeSlot(ROOM, wednesday, wednesday.plusMinutes(15));
            assertEquals(List.of("The start of the slot must be on the grid of the schedule of the room Room 1",
                    "The slot 25/11/2026 09:00 is not inside the schedule of the room Room 1"),
                    errorsOf(() -> service.bookAppointment(STAFF, NEEDED, slot)));
        }

        @Test
        @DisplayName("TC13S-47 a slot of a room whose schedule is inactive is rejected")
        void slotOfAnInactiveSchedule() {
            addSecondNeed("P-0002", "TT-04");
            assertEquals(List.of("The start of the slot must be on the grid of the schedule of the room Room 4",
                    "The slot 24/11/2026 09:00 is not inside the schedule of the room Room 4"),
                    errorsOf(() -> service.bookAppointment(STAFF, "NEED-0002", slotOf("Room 4", 9, 0))));
        }

        @Test
        @DisplayName("TC13S-48 a slot that starts when another appointment ends is free")
        void adjacentAfter() throws AccessDeniedException, ValidationException {
            bookFirst();
            addSecondNeed("P-0002", TEST_TYPE);
            assertEquals(at(TUESDAY, 9, 15),
                    service.bookAppointment(STAFF, "NEED-0002", slot(9, 15)).getStart());
        }

        @Test
        @DisplayName("TC13S-49 a slot that overlaps part of another appointment is not free")
        void partialOverlap() {
            appointments.save(new Appointment("APT-0009", "NEED-0009", "P-0002", TEST_TYPE, ROOM, PHYSICIAN,
                    at(TUESDAY, 9, 10), at(TUESDAY, 9, 25), AppointmentStatus.BOOKED));
            assertEquals(List.of("The slot 24/11/2026 09:15 is not free"),
                    errorsOf(() -> service.bookAppointment(STAFF, NEEDED, slot(9, 15))));
        }

        @Test
        @DisplayName("TC13S-50 a slot that ends when another appointment starts is free")
        void adjacentBefore() throws AccessDeniedException, ValidationException {
            service.bookAppointment(STAFF, NEEDED, slot(9, 15));
            addSecondNeed("P-0002", TEST_TYPE);
            assertEquals(at(TUESDAY, 9, 0),
                    service.bookAppointment(STAFF, "NEED-0002", slot(9, 0)).getStart());
        }

        @Test
        @DisplayName("TC13S-51 a need whose test type is inactive is rejected and nothing is saved")
        void needOfAnInactiveTestType() {
            addSecondNeed("P-0002", "TT-03");
            assertEquals(List.of("The test type TT-03 is not active"),
                    errorsOf(() -> service.bookAppointment(STAFF, "NEED-0002", slotOf("Room 3", 9, 0))));
            assertAll(
                    () -> assertTrue(appointments.findAll().isEmpty()),
                    () -> assertEquals(SchedulingNeedStatus.PENDING, orders.need("NEED-0002").getStatus()));
        }

        @Test
        @DisplayName("TC13S-52 the id skips the ids that the repository already has")
        void idSkipsExistingIds() throws AccessDeniedException, ValidationException {
            appointments.save(new Appointment("APT-0001", "NEED-0009", "P-0002", "TT-02", "Room 2", PHYSICIAN,
                    at(TUESDAY, 9, 0), at(TUESDAY, 9, 30), AppointmentStatus.BOOKED));
            assertEquals("APT-0002", bookFirst().getAppointmentId());
        }
    }

    @Nested
    @DisplayName("4. Rescheduling")
    class Rescheduling {

        @Test
        @DisplayName("TC13S-21 moving an appointment keeps its id and frees the old slot")
        void reschedule() throws AccessDeniedException, ValidationException {
            Appointment booked = bookFirst();
            Appointment moved = service.rescheduleAppointment(STAFF, booked.getAppointmentId(), slot(10, 0));
            assertAll(
                    () -> assertEquals("APT-0001", moved.getAppointmentId()),
                    () -> assertEquals(NEEDED, moved.getNeedId()),
                    () -> assertEquals(AppointmentStatus.RESCHEDULED, moved.getStatus()),
                    () -> assertEquals(slot(10, 0).getStart(), moved.getStart()),
                    () -> assertEquals(List.of(new AuditEntry(STAFF, BOOKED, "APPOINTMENT:APT-0001"),
                            new AuditEntry(STAFF, RESCHEDULED, "APPOINTMENT:APT-0001")), audit.entries()),
                    () -> assertEquals(7, service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, TUESDAY).size()));
        }

        @Test
        @DisplayName("TC13S-22 a cancelled appointment cannot be moved")
        void cancelledIsNotMovable() throws AccessDeniedException, ValidationException {
            Appointment booked = bookFirst();
            service.cancelAppointment(STAFF, booked.getAppointmentId());
            assertEquals(List.of("The appointment APT-0001 is cancelled"),
                    errorsOf(() -> service.rescheduleAppointment(STAFF, "APT-0001", slot(10, 0))));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = { "   " })
        @DisplayName("TC13S-23 a blank appointment is rejected")
        void blankAppointment(String appointmentId) {
            assertEquals(List.of("The appointment is required"),
                    errorsOf(() -> service.rescheduleAppointment(STAFF, appointmentId, slot(10, 0))));
        }

        @Test
        @DisplayName("TC13S-23b an unknown appointment is rejected")
        void unknownAppointment() {
            assertEquals(List.of("There is no appointment with the id APT-9999"),
                    errorsOf(() -> service.rescheduleAppointment(STAFF, "APT-9999", slot(10, 0))));
        }

        @Test
        @DisplayName("TC13S-24 moving to a taken slot is rejected and the appointment keeps its slot")
        void rescheduleToTakenSlot() throws AccessDeniedException, ValidationException {
            bookFirst();
            orders.addOrder(new DiagnosticOrder("ORD-0002", "P-0002", TEST_TYPE, PHYSICIAN, "Control", "Control",
                    LocalDate.of(2026, 11, 23), "ROUTINE"));
            orders.addNeed(new SchedulingNeed("NEED-0002", "ORD-0002", LocalDate.of(2026, 11, 23),
                    SchedulingNeedStatus.PENDING));
            service.bookAppointment(STAFF, "NEED-0002", slot(10, 0));
            assertEquals(List.of("The slot 24/11/2026 10:00 is not free"),
                    errorsOf(() -> service.rescheduleAppointment(STAFF, "APT-0001", slot(10, 0))));
            assertEquals(slot(9, 0).getStart(), appointments.findById("APT-0001").getStart());
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("TC13S-53 a missing new slot is rejected")
        void missingNewSlot(TimeSlot newSlot) throws AccessDeniedException, ValidationException {
            bookFirst();
            assertEquals(List.of("The slot is required"),
                    errorsOf(() -> service.rescheduleAppointment(STAFF, "APT-0001", newSlot)));
        }

        @Test
        @DisplayName("TC13S-54 moving to a slot of another room is rejected")
        void rescheduleToAnotherRoom() throws AccessDeniedException, ValidationException {
            bookFirst();
            assertEquals(List.of("The room Room 2 is not the room of the test type TT-01"),
                    errorsOf(() -> service.rescheduleAppointment(STAFF, "APT-0001", slotOf("Room 2", 10, 0))));
            assertEquals(ROOM, appointments.findById("APT-0001").getRoom());
        }

        @Test
        @DisplayName("TC13S-55 moving to a slot of the past is rejected")
        void rescheduleToThePast() throws AccessDeniedException, ValidationException {
            AppointmentService atTen = serviceAtTen();
            atTen.bookAppointment(STAFF, NEEDED, slot(10, 30));
            assertEquals(List.of("The slot 24/11/2026 09:00 is in the past"),
                    errorsOf(() -> atTen.rescheduleAppointment(STAFF, "APT-0001", slot(9, 0))));
            assertEquals(at(TUESDAY, 10, 30), appointments.findById("APT-0001").getStart());
        }

        @Test
        @DisplayName("TC13S-56 moving outside the schedule is rejected")
        void rescheduleOutsideTheSchedule() throws AccessDeniedException, ValidationException {
            bookFirst();
            assertEquals(List.of("The slot 24/11/2026 11:00 is not inside the schedule of the room Room 1"),
                    errorsOf(() -> service.rescheduleAppointment(STAFF, "APT-0001", slot(11, 0))));
            assertEquals(AppointmentStatus.BOOKED, appointments.findById("APT-0001").getStatus());
        }

        @Test
        @DisplayName("TC13S-57 moving an appointment onto its own slot is accepted")
        void rescheduleOntoItsOwnSlot() throws AccessDeniedException, ValidationException {
            bookFirst();
            Appointment moved = service.rescheduleAppointment(STAFF, "APT-0001", slot(9, 0));
            assertAll(
                    () -> assertEquals(at(TUESDAY, 9, 0), moved.getStart()),
                    () -> assertEquals(AppointmentStatus.RESCHEDULED, moved.getStatus()));
        }

        @Test
        @DisplayName("TC13S-58 after a reschedule the need is still booked in C4")
        void needStaysBookedAfterReschedule() throws AccessDeniedException, ValidationException {
            bookFirst();
            service.rescheduleAppointment(STAFF, "APT-0001", slot(10, 0));
            assertAll(
                    () -> assertEquals(SchedulingNeedStatus.BOOKED, orders.need(NEEDED).getStatus()),
                    () -> assertTrue(service.getPendingSchedulingNeeds(STAFF).isEmpty()));
        }

        @Test
        @DisplayName("TC13S-59 a rescheduled appointment can be moved again")
        void rescheduleTwice() throws AccessDeniedException, ValidationException {
            bookFirst();
            service.rescheduleAppointment(STAFF, "APT-0001", slot(10, 0));
            Appointment again = service.rescheduleAppointment(STAFF, "APT-0001", slot(10, 30));
            assertAll(
                    () -> assertEquals("APT-0001", again.getAppointmentId()),
                    () -> assertEquals(at(TUESDAY, 10, 30), again.getStart()),
                    () -> assertEquals(AppointmentStatus.RESCHEDULED, again.getStatus()));
        }
    }

    @Nested
    @DisplayName("5. Cancelling")
    class Cancelling {

        @Test
        @DisplayName("TC13S-91 a failed save leaves the appointment and need booked")
        void cancelSaveFails() throws AccessDeniedException, ValidationException {
            // Arrange
            String id = bookFirst().getAppointmentId();
            appointments.rejectSaves();

            // Act
            assertThrows(IllegalStateException.class, () -> service.cancelAppointment(STAFF, id));

            // Assert
            assertEquals(AppointmentStatus.BOOKED, appointments.findById(id).getStatus());
            assertEquals(SchedulingNeedStatus.BOOKED, orders.need(NEEDED).getStatus());
        }

        @Test
        @DisplayName("TC13S-92 cancellation accepts multiple already-pending errors")
        void allPending() throws AccessDeniedException, ValidationException {
            // Arrange
            String id = bookFirst().getAppointmentId();
            orders.forceStatus(NEEDED, SchedulingNeedStatus.PENDING);
            orders.rejectPendingWith(new ValidationException(List.of(
                    "The scheduling need " + NEEDED + " is already pending", "The need is already pending")));

            // Act
            Appointment result = service.cancelAppointment(STAFF, id);

            // Assert
            assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
            assertEquals(AppointmentStatus.CANCELLED, appointments.findById(id).getStatus());
        }

        @Test
        @DisplayName("TC13S-93 cancellation propagates an unrelated validation error after saving")
        void otherError() throws AccessDeniedException, ValidationException {
            // Arrange
            String id = bookFirst().getAppointmentId();
            ValidationException rejection = new ValidationException(List.of("The scheduling need is missing"));
            orders.rejectPendingWith(rejection);

            // Act
            ValidationException thrown = assertThrows(ValidationException.class,
                    () -> service.cancelAppointment(STAFF, id));

            // Assert
            assertSame(rejection, thrown);
            assertEquals(AppointmentStatus.CANCELLED, appointments.findById(id).getStatus());
        }

        @Test
        @DisplayName("TC13S-94 cancellation propagates mixed errors even when the first says already pending")
        void mixedErrors() throws AccessDeniedException, ValidationException {
            // Arrange
            String id = bookFirst().getAppointmentId();
            ValidationException rejection = new ValidationException(List.of(
                    "The need is already pending", "The need is already pending elsewhere"));
            orders.rejectPendingWith(rejection);

            // Act
            ValidationException thrown = assertThrows(ValidationException.class,
                    () -> service.cancelAppointment(STAFF, id));

            // Assert
            assertSame(rejection, thrown);
            assertEquals(AppointmentStatus.CANCELLED, appointments.findById(id).getStatus());
        }

        @Test
        @DisplayName("TC13S-98 cancellation does not ask C4 again when the need is already pending, whatever C4 says")
        void alreadyPendingNeedIsNotMarkedAgain() throws AccessDeniedException, ValidationException {
            // Arrange
            String id = bookFirst().getAppointmentId();
            orders.forceStatus(NEEDED, SchedulingNeedStatus.PENDING);
            orders.rejectPendingWith(new ValidationException(List.of("The need is broken")));

            // Act
            Appointment result = assertDoesNotThrow(() -> service.cancelAppointment(STAFF, id));

            // Assert
            assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
            assertEquals(AppointmentStatus.CANCELLED, appointments.findById(id).getStatus());
        }

        @Test
        @DisplayName("TC13S-25 cancelling frees the slot, leaves the need pending and is audited")
        void cancel() throws AccessDeniedException, ValidationException {
            Appointment booked = bookFirst();
            audit.clear();
            Appointment cancelled = service.cancelAppointment(STAFF, booked.getAppointmentId());
            assertAll(
                    () -> assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus()),
                    () -> assertEquals(SchedulingNeedStatus.PENDING, orders.need(NEEDED).getStatus()),
                    () -> assertEquals(List.of(new AuditEntry(STAFF, CANCELLED, "APPOINTMENT:APT-0001")),
                            audit.entries()));
        }

        @Test
        @DisplayName("TC13S-26 an appointment cannot be cancelled twice")
        void cancelTwice() throws AccessDeniedException, ValidationException {
            Appointment booked = bookFirst();
            service.cancelAppointment(STAFF, booked.getAppointmentId());
            assertEquals(List.of("The appointment APT-0001 is already cancelled"),
                    errorsOf(() -> service.cancelAppointment(STAFF, "APT-0001")));
        }

        @Test
        @DisplayName("TC13S-27 cancelling when C4 answers that the need is already pending does not fail")
        void cancelAlreadyPendingNeed() throws AccessDeniedException, ValidationException {
            Appointment booked = bookFirst();
            orders.forceStatus(NEEDED, SchedulingNeedStatus.PENDING);
            audit.clear();
            Appointment cancelled = assertDoesNotThrow(
                    () -> service.cancelAppointment(STAFF, booked.getAppointmentId()));
            assertAll(
                    () -> assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus()),
                    () -> assertEquals(SchedulingNeedStatus.PENDING, orders.need(NEEDED).getStatus()),
                    () -> assertEquals(List.of(new AuditEntry(STAFF, CANCELLED, "APPOINTMENT:APT-0001")),
                            audit.entries()));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("TC13S-60 cancelling a blank appointment is rejected")
        void cancelBlankAppointment(String appointmentId) {
            assertEquals(List.of("The appointment is required"),
                    errorsOf(() -> service.cancelAppointment(STAFF, appointmentId)));
        }

        @Test
        @DisplayName("TC13S-61 cancelling an unknown appointment is rejected")
        void cancelUnknownAppointment() {
            assertEquals(List.of("There is no appointment with the id APT-9999"),
                    errorsOf(() -> service.cancelAppointment(STAFF, "APT-9999")));
        }

        @Test
        @DisplayName("TC13S-62 cancelling a rescheduled appointment leaves its need pending and frees its slot")
        void cancelRescheduled() throws AccessDeniedException, ValidationException {
            bookFirst();
            service.rescheduleAppointment(STAFF, "APT-0001", slot(10, 0));
            Appointment cancelled = service.cancelAppointment(STAFF, "APT-0001");
            assertAll(
                    () -> assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus()),
                    () -> assertEquals(SchedulingNeedStatus.PENDING, orders.need(NEEDED).getStatus()),
                    () -> assertEquals(8, service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, TUESDAY).size()));
        }
    }

    @Nested
    @DisplayName("6. Reads and views")
    class ReadsAndViews {

        @Test
        @DisplayName("TC13S-28 the reads are audited and return the appointments")
        void reads() throws AccessDeniedException, ValidationException {
            Appointment booked = bookFirst();
            audit.clear();
            List<Appointment> all = service.getAppointments(STAFF);
            List<Appointment> ofPatient = service.getAppointmentsOfPatient(STAFF, PATIENT);
            assertAll(
                    () -> assertEquals(List.of(booked), all),
                    () -> assertEquals(1, ofPatient.size()),
                    () -> assertSame(booked, ofPatient.get(0)),
                    () -> assertEquals(List.of(new AuditEntry(STAFF, ACCESSED, APPOINTMENTS),
                            new AuditEntry(STAFF, ACCESSED, "PATIENT:" + PATIENT)), audit.entries()));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("TC13S-29 a blank patient is rejected")
        void blankPatient(String patientId) {
            assertEquals(List.of("The patient is required"),
                    errorsOf(() -> service.getAppointmentsOfPatient(STAFF, patientId)));
        }

        @Test
        @DisplayName("TC13S-30 the pending needs come with their order, patient and test type")
        void pendingNeedViews() throws AccessDeniedException, ValidationException {
            List<PendingNeedView> views = service.getPendingSchedulingNeeds(STAFF);
            assertAll(
                    () -> assertEquals(1, views.size()),
                    () -> assertEquals(NEEDED, views.get(0).getNeedId()),
                    () -> assertEquals(ORDER, views.get(0).getOrderId()),
                    () -> assertEquals(PATIENT, views.get(0).getPatientId()),
                    () -> assertEquals("Lucía Ejemplo Prueba", views.get(0).getPatientName()),
                    () -> assertEquals(TEST_TYPE, views.get(0).getTestTypeId()),
                    () -> assertEquals("Chest X-ray", views.get(0).getTestTypeName()),
                    () -> assertEquals("URGENT", views.get(0).getPriorityId()),
                    () -> assertEquals(LocalDate.of(2026, 11, 23), views.get(0).getDueDate()));
        }

        @Test
        @DisplayName("TC13S-63 a need of an order without priority has an empty priority")
        void needOfAnOrderWithoutPriority() throws AccessDeniedException, ValidationException {
            orders.addOrder(new OrderWithoutPriority("ORD-0002", "P-0002", "TT-02", PHYSICIAN,
                    LocalDate.of(2026, 11, 23)));
            orders.addNeed(new SchedulingNeed("NEED-0002", "ORD-0002", LocalDate.of(2027, 5, 23),
                    SchedulingNeedStatus.PENDING));
            List<PendingNeedView> views = service.getPendingSchedulingNeeds(STAFF);
            assertAll(
                    () -> assertEquals(2, views.size()),
                    () -> assertEquals("NEED-0002", views.get(1).getNeedId()),
                    () -> assertEquals("Mario Demo Simulado", views.get(1).getPatientName()),
                    () -> assertEquals("Blood test", views.get(1).getTestTypeName()),
                    () -> assertEquals("", views.get(1).getPriorityId()),
                    () -> assertEquals(LocalDate.of(2027, 5, 23), views.get(1).getDueDate()));
        }

        @Test
        @DisplayName("TC13S-31 a booked need is not among the pending ones")
        void bookedNeedIsNotPending() throws AccessDeniedException, ValidationException {
            bookFirst();
            assertAll(
                    () -> assertTrue(service.getPendingSchedulingNeeds(STAFF).isEmpty()),
                    () -> assertNull(appointments.findById("APT-0002")));
        }

        @Test
        @DisplayName("TC13S-32 the appointments of a patient without appointments are empty")
        void patientWithoutAppointments() throws AccessDeniedException, ValidationException {
            assertTrue(service.getAppointmentsOfPatient(STAFF, "P-0002").isEmpty());
        }
    }

    @Nested
    @DisplayName("7. Access denied by C2 (#208)")
    class DeniedByC2 {

        @Test
        @DisplayName("TC13S-67 the AccessDeniedException of C2 reaches the caller of getFreeSlots")
        void freeSlotsWhenC2DeniesTheSchedules() {
            parameters.denySchedulesTo(STAFF);
            AccessDeniedException denied = assertThrows(AccessDeniedException.class,
                    () -> service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, TUESDAY));
            assertEquals(STAFF, denied.getRequester());
        }

        @Test
        @DisplayName("TC13S-68 the AccessDeniedException of C2 reaches the caller of bookAppointment")
        void bookingWhenC2DeniesTheSchedules() {
            parameters.denySchedulesTo(STAFF);
            AccessDeniedException denied = assertThrows(AccessDeniedException.class, () -> bookFirst());
            assertAll(
                    () -> assertEquals(STAFF, denied.getRequester()),
                    () -> assertTrue(appointments.findAll().isEmpty()),
                    () -> assertEquals(SchedulingNeedStatus.PENDING, orders.need(NEEDED).getStatus()));
        }

        @Test
        @DisplayName("TC13S-69 the AccessDeniedException of C2 reaches the caller of rescheduleAppointment")
        void rescheduleWhenC2DeniesTheSchedules() throws AccessDeniedException, ValidationException {
            bookFirst();
            parameters.denySchedulesTo(STAFF);
            AccessDeniedException denied = assertThrows(AccessDeniedException.class,
                    () -> service.rescheduleAppointment(STAFF, "APT-0001", slot(10, 0)));
            Appointment kept = appointments.findById("APT-0001");
            assertAll(
                    () -> assertEquals(STAFF, denied.getRequester()),
                    () -> assertEquals(at(TUESDAY, 9, 0), kept.getStart()),
                    () -> assertEquals(AppointmentStatus.BOOKED, kept.getStatus()));
        }
        @Test
        @DisplayName("TC13S-70 a valid booking works when C2 denies a requester without permission")
        void bookingWithAC2ThatDeniesANullRequester() throws AccessDeniedException, ValidationException {
            parameters.denySchedulesTo((String) null);
            Appointment created = bookFirst();
            assertAll(
                    () -> assertEquals("APT-0001", created.getAppointmentId()),
                    () -> assertEquals(AppointmentStatus.BOOKED, created.getStatus()));
        }
    }

    @Nested
    @DisplayName("8. Seed data (#208)")
    class SeedData {

        private List<Appointment> seed() {
            return appointments.findAll().stream()
                    .sorted(Comparator.comparing(Appointment::getAppointmentId))
                    .toList();
        }

        private AppointmentService serviceAt(String instant) {
            Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
            return new AppointmentService(appointments, orders, patients, parameters, roles, audit, clock);
        }

        @Test
        @DisplayName("TC13S-71 the seed is loaded in an empty system")
        void seedIsLoadedInAnEmptySystem() {
            service.loadSeedData();
            List<Appointment> loaded = seed();
            assertAll(
                    () -> assertEquals(List.of("APT-0001", "APT-0002", "APT-0003"),
                            loaded.stream().map(Appointment::getAppointmentId).toList()),
                    () -> assertEquals(List.of("NEED-0001", "NEED-0002", "NEED-0003"),
                            loaded.stream().map(Appointment::getNeedId).toList()),
                    () -> assertEquals(List.of(AppointmentStatus.BOOKED, AppointmentStatus.CANCELLED,
                            AppointmentStatus.RESCHEDULED), loaded.stream().map(Appointment::getStatus).toList()),
                    () -> assertTrue(loaded.stream().allMatch(appointment ->
                            "CT-HEAD".equals(appointment.getTestTypeId())
                                    && "Room CT-1".equals(appointment.getRoom()))));
        }

        @Test
        @DisplayName("TC13S-74 the seed is not loaded when there is an appointment")
        void seedIsNotLoadedWhenThereIsData() throws AccessDeniedException, ValidationException {
            bookFirst();
            service.loadSeedData();
            assertEquals(List.of("APT-0001"), seed().stream().map(Appointment::getAppointmentId).toList());
            assertEquals(NEEDED, appointments.findById("APT-0001").getNeedId());
        }

        @Test
        @DisplayName("TC13S-75 loading the seed twice loads it only once, without audit entries")
        void seedIsLoadedOnlyOnceAndNotAudited() {
            service.loadSeedData();
            service.loadSeedData();
            assertAll(
                    () -> assertEquals(3, appointments.findAll().size()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("TC13S-76 after the seed, the next appointment is APT-0004")
        void idsContinueAfterTheSeed() throws AccessDeniedException, ValidationException {
            service.loadSeedData();
            assertEquals("APT-0004", bookFirst().getAppointmentId());
        }

        @Test
        @DisplayName("TC13S-72 started on a Monday, the seed is on the Tuesday after it")
        void seedOfAMondayIsOnTuesday() {
            serviceAt("2026-11-23T08:00:00Z").loadSeedData();
            assertTrue(seed().stream()
                    .allMatch(appointment -> LocalDate.of(2026, 11, 24).equals(appointment.getStart().toLocalDate())));
        }

        @Test
        @DisplayName("TC13S-73 started on a Friday, the seed is on the Monday after it")
        void seedOfAFridayIsOnMonday() {
            serviceAt("2026-11-27T08:00:00Z").loadSeedData();
            assertTrue(seed().stream()
                    .allMatch(appointment -> LocalDate.of(2026, 11, 30).equals(appointment.getStart().toLocalDate())));
        }

        @ParameterizedTest
        @CsvSource({"2026-11-23, 2026-11-24", "2026-11-26, 2026-11-27", "2026-11-27, 2026-11-30",
                "2026-11-28, 2026-11-30", "2026-11-29, 2026-11-30"})
        @DisplayName("TC13S-89 the next working day of Monday, Thursday, Friday, Saturday and Sunday")
        void nextWorkingDay(LocalDate day, LocalDate expected) {
            assertEquals(expected, AppointmentService.nextWorkingDay(day));
        }
    }

    @Nested
    @DisplayName("9. Rules of the slot (group decision, c775ff4)")
    class SlotRules {
        private static final String WRONG_DURATION =
                "The duration of the slot must match the duration of the test type TT-01";
        private static final String OFF_THE_GRID =
                "The start of the slot must be on the grid of the schedule of the room Room 1";

        private TimeSlot from(int hour, int minute, int lengthMinutes) {
            LocalDateTime start = at(TUESDAY, hour, minute);
            return new TimeSlot(ROOM, start, start.plusMinutes(lengthMinutes));
        }

        @Test
        @DisplayName("TC13S-77 a slot whose start is null is rejected")
        void startMissing() {
            TimeSlot slot = new TimeSlot(ROOM, null, at(TUESDAY, 9, 15));
            assertEquals(List.of("The start and the end of the slot are required"),
                    errorsOf(() -> service.bookAppointment(STAFF, NEEDED, slot)));
        }

        @Test
        @DisplayName("TC13S-78 a slot whose end is null is rejected")
        void endMissing() {
            TimeSlot slot = new TimeSlot(ROOM, at(TUESDAY, 9, 0), null);
            assertEquals(List.of("The start and the end of the slot are required"),
                    errorsOf(() -> service.bookAppointment(STAFF, NEEDED, slot)));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -15})
        @DisplayName("TC13S-79 a slot whose end is equal to or before its start is rejected")
        void endNotAfterStart(int lengthMinutes) {
            assertEquals(List.of("The end of the slot must be after its start"),
                    errorsOf(() -> service.bookAppointment(STAFF, NEEDED, from(9, 15, lengthMinutes))));
        }

        @Test
        @DisplayName("TC13S-80 a slot that spans two days is rejected")
        void slotOfTwoDays() {
            assertEquals(List.of("The slot must be inside a single day"),
                    errorsOf(() -> service.bookAppointment(STAFF, NEEDED, from(23, 55, 15))));
        }

        @Test
        @DisplayName("TC13S-81 a slot one minute shorter than the test type is rejected")
        void slotShorterThanTheDuration() {
            assertEquals(List.of(WRONG_DURATION),
                    errorsOf(() -> service.bookAppointment(STAFF, NEEDED, from(9, 0, 14))));
        }

        @Test
        @DisplayName("TC13S-82 a slot one minute longer than the test type is rejected")
        void slotLongerThanTheDuration() {
            assertEquals(List.of(WRONG_DURATION),
                    errorsOf(() -> service.bookAppointment(STAFF, NEEDED, from(9, 0, 16))));
        }

        @Test
        @DisplayName("TC13S-83 a slot exactly as long as the test type is booked")
        void lengthEqualToTheDuration() throws AccessDeniedException, ValidationException {
            Appointment created = service.bookAppointment(STAFF, NEEDED, from(9, 0, 15));
            assertAll(
                    () -> assertEquals(at(TUESDAY, 9, 0), created.getStart()),
                    () -> assertEquals(at(TUESDAY, 9, 15), created.getEnd()));
        }

        @Test
        @DisplayName("TC13S-84 a slot that starts one minute off the grid is rejected")
        void startOffTheGrid() {
            assertEquals(List.of(OFF_THE_GRID),
                    errorsOf(() -> service.bookAppointment(STAFF, NEEDED, from(9, 1, 15))));
        }

        @Test
        @DisplayName("TC13S-85 a slot that starts on the grid, after the first slot, is booked")
        void startOnTheGrid() throws AccessDeniedException, ValidationException {
            assertEquals(at(TUESDAY, 9, 15), service.bookAppointment(STAFF, NEEDED, from(9, 15, 15)).getStart());
        }

        @Test
        @DisplayName("TC13S-86 a slot before the schedule is off the grid and outside the schedule")
        void slotBeforeTheSchedule() {
            assertEquals(List.of(OFF_THE_GRID,
                    "The slot 24/11/2026 08:00 is not inside the schedule of the room Room 1"),
                    errorsOf(() -> service.bookAppointment(STAFF, NEEDED, from(8, 0, 15))));
        }

        @Test
        @DisplayName("TC13S-87 moving an appointment off the grid is rejected and it keeps its slot")
        void rescheduleOffTheGrid() throws AccessDeniedException, ValidationException {
            bookFirst();
            assertEquals(List.of(OFF_THE_GRID),
                    errorsOf(() -> service.rescheduleAppointment(STAFF, "APT-0001", from(10, 1, 15))));
            assertEquals(at(TUESDAY, 9, 0), appointments.findById("APT-0001").getStart());
        }

        @Test
        @DisplayName("TC13S-88 moving an appointment to a slot longer than the test type is rejected")
        void rescheduleWithAWrongDuration() throws AccessDeniedException, ValidationException {
            bookFirst();
            assertEquals(List.of(WRONG_DURATION),
                    errorsOf(() -> service.rescheduleAppointment(STAFF, "APT-0001", from(10, 0, 30))));
            assertEquals(AppointmentStatus.BOOKED, appointments.findById("APT-0001").getStatus());
        }
    }
}
