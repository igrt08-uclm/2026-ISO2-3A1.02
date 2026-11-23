package es.uclm.esi.iso2.server.appointments.service;

import static org.junit.jupiter.api.Assertions.assertAll;
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
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeedStatus;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.appointments.persistence.InMemoryAppointmentRepository;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakeClinicalOrderService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakePatientService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakeRoleManagementService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakeSystemParameterService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.RecordingAuditRecorder;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.RecordingAuditRecorder.AuditEntry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of {@link AppointmentService} (UC13 server, C5 1.0.0, #131). Each test names the case of the test design
 * (`doc/testing/test-cases/UC13-server-test-design.md`, TC13S-xx) that it checks. The service uses an empty in-memory
 * repository, the test doubles of C1, C2, C3 and C4, and a fixed clock at 24/11/2026 08:00, a Tuesday, so the schedule
 * of Room 1 (Tuesdays from 09:00 to 11:00) is not in the past.
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
    private InMemoryAppointmentRepository appointments;
    private AppointmentService service;

    @BeforeEach
    void setUp() {
        roles = new FakeRoleManagementService();
        audit = new RecordingAuditRecorder();
        patients = new FakePatientService();
        parameters = new FakeSystemParameterService();
        orders = new FakeClinicalOrderService();
        appointments = new InMemoryAppointmentRepository();
        service = new AppointmentService(appointments, orders, patients, parameters, roles, audit, CLOCK);
        orders.addOrder(new DiagnosticOrder(ORDER, PATIENT, TEST_TYPE, PHYSICIAN, "Chest pain", "Rule out pneumonia",
                LocalDate.of(2026, 11, 23), "URGENT"));
        orders.addNeed(new SchedulingNeed(NEEDED, ORDER, LocalDate.of(2026, 11, 23), SchedulingNeedStatus.PENDING));
    }

    private static TimeSlot slot(int hour, int minute) {
        LocalDateTime start = LocalDateTime.of(TUESDAY, java.time.LocalTime.of(hour, minute));
        return new TimeSlot(ROOM, start, start.plusMinutes(15));
    }

    private static TimeSlot slotOf(String room, int hour, int minute) {
        LocalDateTime start = LocalDateTime.of(TUESDAY, java.time.LocalTime.of(hour, minute));
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

    @Nested
    @DisplayName("1. Permission")
    class Permission {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { ADMIN, PHYSICIAN, INACTIVE_USER, UNKNOWN })
        @DisplayName("TC13S-01 a user without MANAGE_APPOINTMENTS is denied in every operation")
        void userWithoutPermissionIsDenied(String requester) {
            assertDenied(requester, () -> service.getAppointments(requester), APPOINTMENTS);
            audit.clear();
            assertDenied(requester, () -> service.bookAppointment(requester, NEEDED, slot(9, 0)), APPOINTMENTS);
            audit.clear();
            assertDenied(requester, () -> service.cancelAppointment(requester, "APT-0001"), APPOINTMENTS);
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
        @DisplayName("TC13S-03 the slots of the schedule of the room, one every duration")
        void slotsOfTheSchedule() throws AccessDeniedException, ValidationException {
            List<TimeSlot> free = service.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, TUESDAY);
            assertAll(
                    () -> assertEquals(8, free.size()),
                    () -> assertEquals(LocalDateTime.of(TUESDAY, java.time.LocalTime.of(9, 0)),
                            free.get(0).getStart()),
                    () -> assertEquals(LocalDateTime.of(TUESDAY, java.time.LocalTime.of(9, 15)), free.get(1).getStart()),
                    () -> assertEquals(LocalDateTime.of(TUESDAY, java.time.LocalTime.of(11, 0)), free.get(8 - 1).getEnd()),
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
    }

    @Nested
    @DisplayName("3. Booking")
    class Booking {

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
        @DisplayName("TC13S-16 a slot outside the schedule is rejected")
        void slotOutsideSchedule() {
            List<String> errors = errorsOf(() -> service.bookAppointment(STAFF, NEEDED, slot(8, 0)));
            assertEquals(List.of("The slot 24/11/2026 08:00 is not inside the schedule of the room Room 1"), errors);
        }

        @Test
        @DisplayName("TC13S-17 a slot in the past is rejected")
        void slotInThePast() {
            TimeSlot past = new TimeSlot(ROOM, LocalDateTime.of(2026, 11, 24, 9, 0).minusDays(1),
                    LocalDateTime.of(2026, 11, 24, 9, 15).minusDays(1));
            List<String> errors = errorsOf(() -> service.bookAppointment(STAFF, NEEDED, past));
            assertTrue(errors.contains("The slot 23/11/2026 09:00 is in the past"));
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
    }

    @Nested
    @DisplayName("5. Cancelling")
    class Cancelling {

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
        @DisplayName("TC13S-27 an appointment without need change does not fail the cancellation")
        void cancelAlreadyPendingNeed() throws AccessDeniedException, ValidationException {
            orders.addNeed(new SchedulingNeed("NEED-0002", "ORD-0001", LocalDate.of(2026, 11, 23),
                    SchedulingNeedStatus.PENDING));
            Appointment other = service.bookAppointment(STAFF, "NEED-0002",
                    new TimeSlot(ROOM, LocalDateTime.of(TUESDAY, java.time.LocalTime.of(9, 0)),
                            LocalDateTime.of(TUESDAY, java.time.LocalTime.of(9, 15))));
            assertEquals(AppointmentStatus.CANCELLED,
                    service.cancelAppointment(STAFF, other.getAppointmentId()).getStatus());
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
            assertAll(
                    () -> assertEquals(1, service.getAppointments(STAFF).size()),
                    () -> assertEquals(List.of(new AuditEntry(STAFF, ACCESSED, APPOINTMENTS)), audit.entries()),
                    () -> assertEquals(1, service.getAppointmentsOfPatient(STAFF, PATIENT).size()),
                    () -> assertSame(booked, service.getAppointmentsOfPatient(STAFF, PATIENT).get(0)));
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
}
