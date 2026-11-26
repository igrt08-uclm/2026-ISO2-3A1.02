package es.uclm.esi.iso2.server.appointments.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.TimeSlot;
import es.uclm.esi.iso2.common.appointments.domain.WaitlistEntry;
import es.uclm.esi.iso2.common.appointments.domain.WaitlistStatus;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeedStatus;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.appointments.persistence.InMemoryWaitlistRepository;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.AppointmentRepositoryFake;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakeClinicalOrderService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakePatientService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakeRoleManagementService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.FakeSystemParameterService;
import es.uclm.esi.iso2.server.appointments.service.TestDoubles.RecordingAuditRecorder;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Tests of {@link WaitlistService} (UC15 server, C5 1.1.0, #134). Each test names the case of the test design
 * ({@code doc/testing/test-cases/UC15-server-test-design.md}, TC15S-xx) that it checks. The service uses an empty
 * in-memory waitlist, the {@link AppointmentService} of UC13, the test doubles of C1, C2, C3 and C4, and a fixed clock
 * at 24/11/2026 08:00, a Tuesday.
 */
@DisplayName("WaitlistService")
class WaitlistServiceTest {
    private static final String STAFF = "lmartin";
    private static final String PHYSICIAN = "mgarcia";
    private static final String TEST_TYPE = "TT-01";
    private static final LocalDate TUESDAY = LocalDate.of(2026, 11, 24);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-24T08:00:00Z"), ZoneOffset.UTC);

    private RecordingAuditRecorder audit;
    private FakeClinicalOrderService orders;
    private InMemoryWaitlistRepository waitlist;
    private AppointmentService appointmentService;
    private WaitlistService service;

    @BeforeEach
    void setUp() {
        FakeRoleManagementService roles = new FakeRoleManagementService();
        FakePatientService patients = new FakePatientService();
        FakeSystemParameterService parameters = new FakeSystemParameterService();
        parameters.usePriorities(tier("URGENT", true), tier("PREFERENTIAL", true), tier("ROUTINE", true),
                tier("OLD", false));
        audit = new RecordingAuditRecorder();
        orders = new FakeClinicalOrderService();
        waitlist = new InMemoryWaitlistRepository();
        appointmentService = new AppointmentService(new AppointmentRepositoryFake(), orders, patients, parameters,
                roles, audit, CLOCK);
        service = new WaitlistService(waitlist, appointmentService, orders, patients, parameters, roles, audit, CLOCK);
        addNeed("0001", "P-0001", SchedulingNeedStatus.PENDING);
        addNeed("0002", "P-0002", SchedulingNeedStatus.PENDING);
        addNeed("0003", "P-0004", SchedulingNeedStatus.PENDING);
        addNeed("0004", "P-0002", SchedulingNeedStatus.BOOKED);
    }

    @Test
    @DisplayName("TC15S-01 a requester without permission is denied in the seven operations")
    void deniedWithoutPermission() {
        // Arrange
        WaitlistEntry entry = waiting("WL-0001", "NEED-0001", "URGENT", 8);
        List<Executable> operations = List.of(
                () -> service.getWaitlist(PHYSICIAN, TEST_TYPE),
                () -> service.addToWaitlist(PHYSICIAN, "NEED-0002", "URGENT"),
                () -> service.changeWaitlistPriority(PHYSICIAN, "WL-0001", "ROUTINE"),
                () -> service.removeFromWaitlist(PHYSICIAN, "WL-0001"),
                () -> service.getCalendarOpenings(PHYSICIAN, TEST_TYPE, TUESDAY, TUESDAY),
                () -> service.proposePatientForOpening(PHYSICIAN, TEST_TYPE, slot(9, 0)),
                () -> service.fillOpening(PHYSICIAN, "WL-0001", slot(9, 0)));

        for (Executable operation : operations) {
            audit.clear();
            // Act
            assertThrows(AccessDeniedException.class, operation);
            // Assert
            assertAudited(PHYSICIAN, "WAITLIST_ACCESS_DENIED");
        }
        assertEquals(List.of(entry), waitlist.findAll());
    }

    @Test
    @DisplayName("TC15S-02 the waitlist is ordered by priority tier")
    void orderedByTier() throws AccessDeniedException, ValidationException {
        // Arrange
        waiting("WL-0001", "NEED-0001", "ROUTINE", 8);
        waiting("WL-0002", "NEED-0002", "URGENT", 9);
        waiting("WL-0003", "NEED-0003", "PREFERENTIAL", 10);

        // Act
        List<WaitlistEntry> result = service.getWaitlist(STAFF, TEST_TYPE);

        // Assert
        assertEquals(List.of("WL-0002", "WL-0003", "WL-0001"), idsOf(result));
    }

    @Test
    @DisplayName("TC15S-03 inside a tier the oldest entry goes first")
    void orderedByDateInsideATier() throws AccessDeniedException, ValidationException {
        // Arrange
        waiting("WL-0001", "NEED-0001", "URGENT", 10);
        waiting("WL-0002", "NEED-0002", "URGENT", 8);

        // Act
        List<WaitlistEntry> result = service.getWaitlist(STAFF, TEST_TYPE);

        // Assert
        assertEquals(List.of("WL-0002", "WL-0001"), idsOf(result));
    }

    @Test
    @DisplayName("TC15S-04 removed, booked and other test type entries are not returned")
    void onlyWaitingEntriesOfTheTestType() throws AccessDeniedException, ValidationException {
        // Arrange
        save("WL-0001", "NEED-0001", TEST_TYPE, "URGENT", 8, WaitlistStatus.REMOVED);
        save("WL-0002", "NEED-0002", TEST_TYPE, "URGENT", 8, WaitlistStatus.BOOKED);
        save("WL-0003", "NEED-0003", "TT-02", "URGENT", 8, WaitlistStatus.WAITING);

        // Act
        List<WaitlistEntry> result = service.getWaitlist(STAFF, TEST_TYPE);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TC15S-05 the test type is required to consult the waitlist")
    void consultWithoutTestType() {
        // Act and assert
        assertAll(
                () -> assertThrows(ValidationException.class, () -> service.getWaitlist(STAFF, null)),
                () -> assertThrows(ValidationException.class, () -> service.getWaitlist(STAFF, " ")));
    }

    @Test
    @DisplayName("TC15S-06 a pending need is added as a waiting entry and audited")
    void add() throws AccessDeniedException, ValidationException {
        // Act
        WaitlistEntry entry = service.addToWaitlist(STAFF, "NEED-0001", "URGENT");

        // Assert
        assertAll(
                () -> assertEquals("WL-0001", entry.getEntryId()),
                () -> assertEquals("NEED-0001", entry.getNeedId()),
                () -> assertEquals("P-0001", entry.getPatientId()),
                () -> assertEquals(TEST_TYPE, entry.getTestTypeId()),
                () -> assertEquals("URGENT", entry.getPriorityId()),
                () -> assertEquals(LocalDateTime.now(CLOCK), entry.getAddedAt()),
                () -> assertEquals(WaitlistStatus.WAITING, entry.getStatus()),
                () -> assertEquals(List.of(entry), waitlist.findAll()));
        assertAudited(STAFF, "WAITLIST_CHANGED");
    }

    @Test
    @DisplayName("TC15S-07 a missing need and tier are reported together")
    void addWithoutData() {
        // Act
        List<String> errors = errorsOf(() -> service.addToWaitlist(STAFF, null, " "));

        // Assert
        assertEquals(2, errors.size());
        assertTrue(waitlist.findAll().isEmpty());
    }

    @Test
    @DisplayName("TC15S-08 an unknown tier is rejected")
    void addWithUnknownTier() {
        // Act
        List<String> errors = errorsOf(() -> service.addToWaitlist(STAFF, "NEED-0001", "NONE"));

        // Assert
        assertMentions(errors, "NONE");
        assertTrue(waitlist.findAll().isEmpty());
    }

    @Test
    @DisplayName("TC15S-09 an inactive tier is rejected")
    void addWithInactiveTier() {
        // Act
        List<String> errors = errorsOf(() -> service.addToWaitlist(STAFF, "NEED-0001", "OLD"));

        // Assert
        assertMentions(errors, "OLD");
        assertTrue(waitlist.findAll().isEmpty());
    }

    @Test
    @DisplayName("TC15S-10 a need that is not pending is rejected")
    void addNeedNotPending() {
        // Act
        List<String> errors = errorsOf(() -> service.addToWaitlist(STAFF, "NEED-0004", "URGENT"));

        // Assert
        assertMentions(errors, "NEED-0004");
        assertTrue(waitlist.findAll().isEmpty());
    }

    @Test
    @DisplayName("TC15S-11 a need that already waits is rejected")
    void addNeedAlreadyWaiting() {
        // Arrange
        WaitlistEntry entry = waiting("WL-0001", "NEED-0001", "URGENT", 8);

        // Act
        List<String> errors = errorsOf(() -> service.addToWaitlist(STAFF, "NEED-0001", "URGENT"));

        // Assert
        assertMentions(errors, "NEED-0001");
        assertEquals(List.of(entry), waitlist.findAll());
    }

    @Test
    @DisplayName("TC15S-12 an invalid tier and a need that is not pending are reported together")
    void addWithEveryReason() {
        // Act
        List<String> errors = errorsOf(() -> service.addToWaitlist(STAFF, "NEED-0004", "NONE"));

        // Assert
        assertEquals(2, errors.size());
        assertMentions(errors, "NONE");
        assertMentions(errors, "NEED-0004");
        assertTrue(waitlist.findAll().isEmpty());
    }

    @Test
    @DisplayName("TC15S-13 changing the tier changes the order, keeps the date and is audited")
    void changeTier() throws AccessDeniedException, ValidationException {
        // Arrange
        waiting("WL-0001", "NEED-0001", "PREFERENTIAL", 8);
        WaitlistEntry second = waiting("WL-0002", "NEED-0002", "ROUTINE", 9);

        // Act
        WaitlistEntry changed = service.changeWaitlistPriority(STAFF, "WL-0002", "URGENT");

        // Assert
        assertEquals("URGENT", changed.getPriorityId());
        assertEquals(second.getAddedAt(), changed.getAddedAt());
        assertAudited(STAFF, "WAITLIST_CHANGED");
        assertEquals(List.of("WL-0002", "WL-0001"), idsOf(service.getWaitlist(STAFF, TEST_TYPE)));
    }

    @Test
    @DisplayName("TC15S-14 a missing entry id and tier are reported together")
    void changeTierWithoutData() {
        // Act
        List<String> errors = errorsOf(() -> service.changeWaitlistPriority(STAFF, " ", null));

        // Assert
        assertEquals(2, errors.size());
    }

    @Test
    @DisplayName("TC15S-15 the tier of an unknown or not waiting entry cannot be changed")
    void changeTierOfWrongEntry() {
        // Arrange
        save("WL-0001", "NEED-0001", TEST_TYPE, "URGENT", 8, WaitlistStatus.BOOKED);

        // Act and assert
        assertAll(
                () -> assertMentions(errorsOf(() -> service.changeWaitlistPriority(STAFF, "WL-0009", "ROUTINE")),
                        "WL-0009"),
                () -> assertMentions(errorsOf(() -> service.changeWaitlistPriority(STAFF, "WL-0001", "ROUTINE")),
                        "WL-0001"));
    }

    @Test
    @DisplayName("TC15S-16 an invalid tier does not change the entry")
    void changeToInvalidTier() {
        // Arrange
        WaitlistEntry entry = waiting("WL-0001", "NEED-0001", "URGENT", 8);

        // Act
        List<String> errors = errorsOf(() -> service.changeWaitlistPriority(STAFF, "WL-0001", "NONE"));

        // Assert
        assertMentions(errors, "NONE");
        assertEquals(List.of(entry), waitlist.findAll());
    }

    @Test
    @DisplayName("TC15S-17 a removed entry is kept as REMOVED and audited")
    void remove() throws AccessDeniedException, ValidationException {
        // Arrange
        waiting("WL-0001", "NEED-0001", "URGENT", 8);

        // Act
        WaitlistEntry removed = service.removeFromWaitlist(STAFF, "WL-0001");

        // Assert
        assertEquals(WaitlistStatus.REMOVED, removed.getStatus());
        assertEquals(List.of(removed), waitlist.findAll());
        assertAudited(STAFF, "WAITLIST_ENTRY_REMOVED");
    }

    @Test
    @DisplayName("TC15S-18 the entry id is required to remove")
    void removeWithoutId() {
        // Act and assert
        assertThrows(ValidationException.class, () -> service.removeFromWaitlist(STAFF, " "));
    }

    @Test
    @DisplayName("TC15S-19 an unknown or already removed entry cannot be removed")
    void removeWrongEntry() {
        // Arrange
        save("WL-0001", "NEED-0001", TEST_TYPE, "URGENT", 8, WaitlistStatus.REMOVED);

        // Act and assert
        assertAll(
                () -> assertMentions(errorsOf(() -> service.removeFromWaitlist(STAFF, "WL-0009")), "WL-0009"),
                () -> assertMentions(errorsOf(() -> service.removeFromWaitlist(STAFF, "WL-0001")), "WL-0001"));
    }

    @Test
    @DisplayName("TC15S-20 the openings are the free slots of UC13 and the access is audited")
    void openings() throws AccessDeniedException, ValidationException {
        // Arrange
        List<TimeSlot> free = appointmentService.getFreeSlots(STAFF, TEST_TYPE, TUESDAY, TUESDAY);
        audit.clear();

        // Act
        List<TimeSlot> openings = service.getCalendarOpenings(STAFF, TEST_TYPE, TUESDAY, TUESDAY);

        // Assert
        assertEquals(startsOf(free), startsOf(openings));
        assertAudited(STAFF, "WAITLIST_ACCESSED");
    }

    @Test
    @DisplayName("TC15S-21 the test type is required to find openings")
    void openingsWithoutTestType() {
        // Act and assert
        assertThrows(ValidationException.class, () -> service.getCalendarOpenings(STAFF, " ", TUESDAY, TUESDAY));
    }

    @Test
    @DisplayName("TC15S-22 a reversed range is rejected by UC13")
    void openingsWithReversedRange() {
        // Act and assert
        assertThrows(ValidationException.class,
                () -> service.getCalendarOpenings(STAFF, TEST_TYPE, TUESDAY, TUESDAY.minusDays(1)));
    }

    @Test
    @DisplayName("TC15S-23 the first valid entry is proposed")
    void proposeFirst() throws AccessDeniedException, ValidationException {
        // Arrange
        WaitlistEntry first = waiting("WL-0001", "NEED-0001", "URGENT", 8);
        waiting("WL-0002", "NEED-0002", "ROUTINE", 8);

        // Act
        WaitlistEntry proposed = service.proposePatientForOpening(STAFF, TEST_TYPE, slot(9, 0));

        // Assert
        assertEquals(first, proposed);
    }

    @Test
    @DisplayName("TC15S-24 an inactive patient is skipped")
    void proposeSkipsInactivePatient() throws AccessDeniedException, ValidationException {
        // Arrange
        waiting("WL-0001", "NEED-0003", "URGENT", 8);
        WaitlistEntry next = waiting("WL-0002", "NEED-0002", "ROUTINE", 8);

        // Act
        WaitlistEntry proposed = service.proposePatientForOpening(STAFF, TEST_TYPE, slot(9, 0));

        // Assert
        assertEquals(next, proposed);
    }

    @Test
    @DisplayName("TC15S-25 a need that is already booked is skipped")
    void proposeSkipsBookedNeed() throws AccessDeniedException, ValidationException {
        // Arrange
        waiting("WL-0001", "NEED-0004", "URGENT", 8);
        WaitlistEntry next = waiting("WL-0002", "NEED-0001", "ROUTINE", 8);

        // Act
        WaitlistEntry proposed = service.proposePatientForOpening(STAFF, TEST_TYPE, slot(9, 0));

        // Assert
        assertEquals(next, proposed);
    }

    @Test
    @DisplayName("TC15S-26 nobody is proposed when nobody waits")
    void proposeWithEmptyWaitlist() throws AccessDeniedException, ValidationException {
        // Act
        WaitlistEntry proposed = service.proposePatientForOpening(STAFF, TEST_TYPE, slot(9, 0));

        // Assert
        assertNull(proposed);
    }

    @Test
    @DisplayName("TC15S-27 nobody is proposed when no entry is valid")
    void proposeWithoutValidEntry() throws AccessDeniedException, ValidationException {
        // Arrange
        waiting("WL-0001", "NEED-0003", "URGENT", 8);
        waiting("WL-0002", "NEED-0004", "ROUTINE", 8);

        // Act
        WaitlistEntry proposed = service.proposePatientForOpening(STAFF, TEST_TYPE, slot(9, 0));

        // Assert
        assertNull(proposed);
    }

    @Test
    @DisplayName("TC15S-28 a missing test type and opening are reported together")
    void proposeWithoutData() {
        // Act
        List<String> errors = errorsOf(() -> service.proposePatientForOpening(STAFF, " ", null));

        // Assert
        assertEquals(2, errors.size());
    }

    @Test
    @DisplayName("TC15S-29 filling an opening books the appointment, closes the entry and is audited")
    void fill() throws AccessDeniedException, ValidationException {
        // Arrange
        waiting("WL-0001", "NEED-0001", "URGENT", 8);

        // Act
        Appointment booked = service.fillOpening(STAFF, "WL-0001", slot(9, 0));

        // Assert
        assertAll(
                () -> assertEquals("NEED-0001", booked.getNeedId()),
                () -> assertEquals(slot(9, 0).getStart(), booked.getStart()),
                () -> assertEquals(SchedulingNeedStatus.BOOKED, orders.need("NEED-0001").getStatus()),
                () -> assertEquals(WaitlistStatus.BOOKED, waitlist.findById("WL-0001").getStatus()),
                () -> assertTrue(audit.entries().stream().anyMatch(
                        entry -> entry.author().equals(STAFF) && entry.action().equals("OPENING_FILLED"))));
    }

    @Test
    @DisplayName("TC15S-30 a missing entry id and opening are reported together")
    void fillWithoutData() {
        // Act
        List<String> errors = errorsOf(() -> service.fillOpening(STAFF, " ", null));

        // Assert
        assertEquals(2, errors.size());
    }

    @Test
    @DisplayName("TC15S-31 an unknown or not waiting entry books nothing")
    void fillWithWrongEntry() throws AccessDeniedException {
        // Arrange
        save("WL-0001", "NEED-0001", TEST_TYPE, "URGENT", 8, WaitlistStatus.REMOVED);

        // Act and assert
        assertAll(
                () -> assertMentions(errorsOf(() -> service.fillOpening(STAFF, "WL-0009", slot(9, 0))), "WL-0009"),
                () -> assertMentions(errorsOf(() -> service.fillOpening(STAFF, "WL-0001", slot(9, 0))), "WL-0001"));
        assertTrue(appointmentService.getAppointments(STAFF).isEmpty());
    }

    @Test
    @DisplayName("TC15S-32 an opening that is already taken leaves the entry waiting")
    void fillTakenOpening() throws AccessDeniedException, ValidationException {
        // Arrange
        waiting("WL-0001", "NEED-0001", "URGENT", 8);
        appointmentService.bookAppointment(STAFF, "NEED-0002", slot(9, 0));

        // Act
        assertThrows(ValidationException.class, () -> service.fillOpening(STAFF, "WL-0001", slot(9, 0)));

        // Assert
        assertEquals(WaitlistStatus.WAITING, waitlist.findById("WL-0001").getStatus());
        assertEquals(1, appointmentService.getAppointments(STAFF).size());
    }

    @Test
    @DisplayName("TC15S-33 the seed is loaded only in an empty waitlist and waits for pending needs")
    void seed() {
        // Act
        service.loadSeedData();
        List<WaitlistEntry> first = waitlist.findAll();
        service.loadSeedData();

        // Assert: NEED-0002 and NEED-0004 are the pending needs of the seed of C4
        assertEquals(List.of("NEED-0002", "NEED-0004"), first.stream().map(WaitlistEntry::getNeedId).toList());
        assertEquals(first, waitlist.findAll());
    }

    private static Priority tier(String id, boolean active) {
        return new Priority(id, id, id, active);
    }

    /** Adds the need {@code NEED-<number>} of a diagnostic order of {@code TT-01}. */
    private void addNeed(String number, String patientId, SchedulingNeedStatus status) {
        LocalDate day = TUESDAY.minusDays(1);
        orders.addOrder(new DiagnosticOrder("ORD-" + number, patientId, TEST_TYPE, PHYSICIAN, "Control", "Control",
                day, "URGENT"));
        orders.addNeed(new SchedulingNeed("NEED-" + number, "ORD-" + number, day, status));
    }

    /** Saves an entry added the day before at that hour, with the patient of its need. */
    private WaitlistEntry save(String entryId, String needId, String testTypeId, String priorityId, int hour,
            WaitlistStatus status) {
        String patientId = switch (needId) {
            case "NEED-0001" -> "P-0001";
            case "NEED-0003" -> "P-0004";
            default -> "P-0002";
        };
        WaitlistEntry entry = new WaitlistEntry(entryId, needId, patientId, testTypeId, priorityId,
                LocalDateTime.of(TUESDAY.minusDays(1), LocalTime.of(hour, 0)), status);
        waitlist.save(entry);
        return entry;
    }

    private WaitlistEntry waiting(String entryId, String needId, String priorityId, int hour) {
        return save(entryId, needId, TEST_TYPE, priorityId, hour, WaitlistStatus.WAITING);
    }

    private static TimeSlot slot(int hour, int minute) {
        LocalDateTime start = LocalDateTime.of(TUESDAY, LocalTime.of(hour, minute));
        return new TimeSlot("Room 1", start, start.plusMinutes(15));
    }

    private static List<String> idsOf(List<WaitlistEntry> entries) {
        return entries.stream().map(WaitlistEntry::getEntryId).toList();
    }

    private static List<LocalDateTime> startsOf(List<TimeSlot> slots) {
        return slots.stream().map(TimeSlot::getStart).toList();
    }

    private static List<String> errorsOf(Executable call) {
        return assertThrows(ValidationException.class, call).getErrors();
    }

    private static void assertMentions(List<String> errors, String text) {
        assertTrue(errors.stream().anyMatch(error -> error.contains(text)), () -> text + " not in " + errors);
    }

    /** The audit log has exactly one entry, of that author and action. */
    private void assertAudited(String author, String action) {
        assertEquals(List.of(author + " " + action),
                audit.entries().stream().map(entry -> entry.author() + " " + entry.action()).toList());
    }
}
