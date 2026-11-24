package es.uclm.esi.iso2.client.appointments.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.AppointmentStatus;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the screen {@link IU_ManageAppointments} of UC13 (#130): the pending needs and the appointments, the free
 * slots of a day, the booking, the move and the cancellation of an appointment, the checks of the screen, the errors
 * of the server, the denied access, the keys and <em>Back</em>. The cases are those of
 * {@code doc/testing/test-cases/UC13-client-test-design.md}. The screen is only built, never shown, so the tests need
 * no display; every question is answered by the test, which also records it.
 */
@DisplayName("IU_ManageAppointments")
class IU_ManageAppointmentsTest {
    private static final String REQUESTER = "lmartin";
    private static final String DAY = "30/11/2026";
    private static final String DENIED = "⚠ You do not have permission to manage appointments.";
    private static final String SELECT_NEED_AND_SLOT = "⚠ Select a pending need and one of its free slots.";
    private static final String SELECT_APPOINTMENT_AND_SLOT = "⚠ Select an appointment and one of the free slots.";
    private static final String BAD_DAY = "⚠ The day must be written as dd/MM/yyyy";
    private static final Color GREEN = new Color(0, 128, 0);
    private static final Color RED = new Color(180, 0, 0);
    private static final int LUCIA = 0;
    private static final int MARIO = 1;
    private static final int APT_0001 = 0;
    private static final int APT_0003 = 2;
    private static final List<String> ACTIONS = List.of("Find slots", "Book", "Move", "Cancel appointment");

    private FakeAppointmentService service;
    private List<String> questions;
    private boolean answer;
    private int backs;

    @BeforeEach
    void setUp() {
        service = FakeAppointmentService.withUsualData();
        questions = new ArrayList<>();
        answer = true;
        backs = 0;
    }

    private IU_ManageAppointments open() {
        return new IU_ManageAppointments(service, REQUESTER, question -> {
            questions.add(question);
            return answer;
        }, () -> backs++);
    }

    /** Opens the screen with a clock fixed at 09:00 of Monday 30/11/2026 in Europe/Madrid (F-05). */
    private IU_ManageAppointments openOnMonday() {
        ZoneId madrid = ZoneId.of("Europe/Madrid");
        Clock monday = Clock.fixed(LocalDateTime.of(2026, 11, 30, 9, 0).atZone(madrid).toInstant(), madrid);
        return new IU_ManageAppointments(service, REQUESTER, question -> {
            questions.add(question);
            return answer;
        }, () -> backs++, monday);
    }

    private static void select(JTable table, int row) {
        table.setRowSelectionInterval(row, row);
    }

    private static void click(IU_ManageAppointments screen, String button) {
        Screen.button(screen, button).doClick();
    }

    /** Writes the day and presses <em>Find slots</em>. */
    private static void findSlots(IU_ManageAppointments screen, String day) {
        Screen.field(screen, "Day").setText(day);
        click(screen, "Find slots");
    }

    /** Opens the screen, selects the need of the row, finds the slots of 30/11/2026 and selects the slot of the row. */
    private IU_ManageAppointments needAndSlot(int need, int slot) {
        IU_ManageAppointments screen = open();
        select(screen.getNeedsTable(), need);
        findSlots(screen, DAY);
        select(screen.getSlotsTable(), slot);
        return screen;
    }

    /**
     * Opens the screen, selects the appointment of the row, finds the slots of its test type on 30/11/2026 and selects
     * the slot of the row (decision F-02 of Design, PR #210).
     */
    private IU_ManageAppointments appointmentAndSlot(int appointment, int slot) {
        IU_ManageAppointments screen = open();
        select(screen.getAppointmentsTable(), appointment);
        findSlots(screen, DAY);
        select(screen.getSlotsTable(), slot);
        return screen;
    }

    private static Color statusColour(IU_ManageAppointments screen) {
        return Screen.label(screen, screen.getStatusMessage()).getForeground();
    }

    private static boolean tablesEnabled(IU_ManageAppointments screen) {
        return screen.getNeedsTable().isEnabled() || screen.getSlotsTable().isEnabled()
                || screen.getAppointmentsTable().isEnabled();
    }

    private static List<String> appointmentRow(IU_ManageAppointments screen, int row) {
        return Screen.rows(screen.getAppointmentsTable()).get(row);
    }

    @Test
    @DisplayName("TC13C-01 Opening lists the pending needs and every appointment, with no slot")
    void tc13c01OpeningListsTheNeedsAndTheAppointments() {
        // Act
        IU_ManageAppointments screen = open();

        // Assert
        JTable needs = screen.getNeedsTable();
        assertAll(
                () -> assertEquals(2, needs.getRowCount()),
                () -> assertEquals(List.of("NEED-0001", "Lucía Ejemplo Prueba", "Chest X-ray", "URGENT"),
                        List.of(Screen.cell(needs, LUCIA, "Need"), Screen.cell(needs, LUCIA, "Patient"),
                                Screen.cell(needs, LUCIA, "Test type"), Screen.cell(needs, LUCIA, "Priority"))),
                () -> assertEquals(List.of("NEED-0002", "Mario Demo Simulado", "Knee MRI", "ROUTINE"),
                        List.of(Screen.cell(needs, MARIO, "Need"), Screen.cell(needs, MARIO, "Patient"),
                                Screen.cell(needs, MARIO, "Test type"), Screen.cell(needs, MARIO, "Priority"))),
                () -> assertTrue(Screen.rows(needs).get(LUCIA).contains("27/11/2026")),
                () -> assertTrue(Screen.rows(needs).get(MARIO).contains("")),
                () -> assertEquals(List.of("Appointment", "Patient", "Test type", "Start", "Status"),
                        Screen.columns(screen.getAppointmentsTable())),
                () -> assertEquals(List.of(
                        List.of("APT-0001", "P-0003", "XRAY-CHEST", "30/11/2026 08:00", "BOOKED"),
                        List.of("APT-0002", "P-0002", "MRI-KNEE", "30/11/2026 11:00", "CANCELLED"),
                        List.of("APT-0003", "P-0004", "CT-HEAD", "30/11/2026 12:30", "BOOKED")),
                        Screen.rows(screen.getAppointmentsTable())),
                () -> assertEquals(List.of("Room", "Start", "End"), Screen.columns(screen.getSlotsTable())),
                () -> assertEquals(0, screen.getSlotsTable().getRowCount()),
                () -> assertEquals(List.of("needs", "appointments"), service.calls()),
                () -> assertEquals(List.of(REQUESTER, REQUESTER), service.requesters()),
                () -> assertEquals(" ", screen.getStatusMessage()),
                () -> assertEquals(ACTIONS.size() + 1, Screen.enabledButtons(screen).size()));
    }

    @Test
    @DisplayName("TC13C-02 The day starts as today and a reload keeps the day written by the user")
    void tc13c02DayStartsAsTodayAndIsKept() {
        // Arrange
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String before = format.format(LocalDate.now());

        // Act
        IU_ManageAppointments screen = open();
        String shown = Screen.field(screen, "Day").getText();
        String after = format.format(LocalDate.now());
        Screen.field(screen, "Day").setText("01/12/2026");
        screen.loadData();

        // Assert
        assertAll(
                () -> assertTrue(shown.equals(before) || shown.equals(after), shown),
                () -> assertEquals("01/12/2026", Screen.field(screen, "Day").getText()),
                () -> assertEquals(List.of("needs", "appointments", "needs", "appointments"), service.calls()));
    }

    @Test
    @DisplayName("TC13C-03 Find slots of Lucía's need lists the two free slots of the chest X-ray that day")
    void tc13c03FindSlotsOfTheSelectedNeed() {
        // Act
        IU_ManageAppointments screen = open();
        select(screen.getNeedsTable(), LUCIA);
        findSlots(screen, DAY);

        // Assert
        assertAll(
                () -> assertEquals(List.of("XRAY-CHEST 2026-11-30 2026-11-30"), service.slotQueries()),
                () -> assertEquals(List.of(
                        List.of("XRAY-1", "30/11/2026 09:00", "30/11/2026 09:15"),
                        List.of("XRAY-1", "30/11/2026 09:15", "30/11/2026 09:30")),
                        Screen.rows(screen.getSlotsTable())),
                () -> assertEquals("2 free slots.", screen.getStatusMessage()),
                () -> assertEquals(REQUESTER, service.requesters().get(2)),
                () -> assertTrue(service.changes().isEmpty()));
    }

    @Test
    @DisplayName("TC13C-04 One free slot (Mario's knee MRI, day with spaces) says 1 free slot.")
    void tc13c04OneFreeSlotAndTrimmedDay() {
        // Act
        IU_ManageAppointments screen = open();
        select(screen.getNeedsTable(), MARIO);
        findSlots(screen, "  30/11/2026 ");

        // Assert
        assertAll(
                () -> assertEquals(List.of("MRI-KNEE 2026-11-30 2026-11-30"), service.slotQueries()),
                () -> assertEquals(List.of(List.of("MRI-1", "30/11/2026 10:00", "30/11/2026 10:45")),
                        Screen.rows(screen.getSlotsTable())),
                () -> assertEquals("1 free slot.", screen.getStatusMessage()));
    }

    @Test
    @DisplayName("TC13C-05 A day without free slots empties the slots and says so in red")
    void tc13c05DayWithoutFreeSlots() {
        // Arrange
        IU_ManageAppointments screen = open();
        select(screen.getNeedsTable(), LUCIA);
        findSlots(screen, DAY);

        // Act
        findSlots(screen, "01/12/2026");

        // Assert
        assertAll(
                () -> assertEquals(0, screen.getSlotsTable().getRowCount()),
                () -> assertEquals("⚠ There are no free slots on that day for that test type.",
                        screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals("XRAY-CHEST 2026-12-01 2026-12-01", service.slotQueries().get(1)));
    }

    @Test
    @DisplayName("TC13C-06 Find slots without a selected need or appointment asks for one and sends nothing")
    void tc13c06FindSlotsWithoutNeed() {
        // Act
        IU_ManageAppointments screen = open();
        findSlots(screen, DAY);

        // Assert
        assertAll(
                () -> assertEquals("⚠ Select a pending need or an appointment before looking for slots.",
                        screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertTrue(service.slotQueries().isEmpty()),
                () -> assertEquals(List.of("needs", "appointments"), service.calls()));
    }

    @Test
    @DisplayName("TC13C-07 A day that is not dd/MM/yyyy is rejected and nothing is sent")
    void tc13c07InvalidDay() {
        // Arrange
        IU_ManageAppointments screen = open();
        select(screen.getNeedsTable(), LUCIA);
        List<String> shown = new ArrayList<>();

        // Act
        for (String day : List.of("", "   ", "2026-11-30", "32/11/2026", "30/13/2026", "1/12/2026", "abc")) {
            screen.showConfirmation("reset");
            findSlots(screen, day);
            shown.add(screen.getStatusMessage());
        }

        // Assert
        assertAll(
                () -> assertEquals(List.of(BAD_DAY, BAD_DAY, BAD_DAY, BAD_DAY, BAD_DAY, BAD_DAY, BAD_DAY), shown),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertTrue(service.slotQueries().isEmpty()));
    }

    @Test
    @DisplayName("TC13C-08 The errors of the server for the slots are shown together in red")
    void tc13c08ServerRejectsTheSlots() {
        // Arrange
        service.rejectSlots("The test type XRAY-CHEST has no room", "The day 30/11/2026 is closed");
        IU_ManageAppointments screen = open();
        select(screen.getNeedsTable(), LUCIA);

        // Act
        findSlots(screen, DAY);

        // Assert
        assertAll(
                () -> assertEquals("⚠ The test type XRAY-CHEST has no room The day 30/11/2026 is closed",
                        screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals(0, screen.getSlotsTable().getRowCount()));
    }

    @Test
    @DisplayName("TC13C-09 Book asks, sends one booking with the need and the slot, and reloads the lists")
    void tc13c09BookTheSelectedSlot() {
        // Arrange
        IU_ManageAppointments screen = needAndSlot(LUCIA, 1);

        // Act
        click(screen, "Book");

        // Assert
        assertAll(
                () -> assertEquals(List.of("Book the patient Lucía Ejemplo Prueba on 30/11/2026 09:15?"), questions),
                () -> assertEquals(List.of("book NEED-0001 XRAY-1 2026-11-30T09:15 2026-11-30T09:30"),
                        service.changes()),
                () -> assertEquals(REQUESTER, service.requesters().get(3)),
                () -> assertEquals(List.of("needs", "appointments", "slots XRAY-CHEST",
                        "book NEED-0001 XRAY-1 2026-11-30T09:15 2026-11-30T09:30", "needs", "appointments"),
                        service.calls()),
                () -> assertEquals(1, screen.getNeedsTable().getRowCount()),
                () -> assertEquals("NEED-0002", Screen.cell(screen.getNeedsTable(), 0, "Need")),
                () -> assertEquals(List.of("APT-0004", "P-0001", "XRAY-CHEST", "30/11/2026 09:15", "BOOKED"),
                        appointmentRow(screen, 3)),
                () -> assertEquals(-1, screen.getNeedsTable().getSelectedRow()));
    }

    @Test
    @DisplayName("TC13C-10 Book answered No sends nothing and changes nothing")
    void tc13c10BookAnsweredNo() {
        // Arrange
        IU_ManageAppointments screen = needAndSlot(MARIO, 0);
        answer = false;

        // Act
        click(screen, "Book");

        // Assert
        assertAll(
                () -> assertEquals(List.of("Book the patient Mario Demo Simulado on 30/11/2026 10:00?"), questions),
                () -> assertTrue(service.changes().isEmpty()),
                () -> assertEquals(2, screen.getNeedsTable().getRowCount()),
                () -> assertEquals(3, screen.getAppointmentsTable().getRowCount()),
                () -> assertEquals("1 free slot.", screen.getStatusMessage()));
    }

    @Test
    @DisplayName("TC13C-11 Book without a need, without a slot or without both asks for them and sends nothing")
    void tc13c11BookWithoutNeedOrSlot() {
        // Arrange
        IU_ManageAppointments nothing = open();
        IU_ManageAppointments needOnly = open();
        select(needOnly.getNeedsTable(), LUCIA);
        findSlots(needOnly, DAY);
        IU_ManageAppointments slotOnly = needAndSlot(LUCIA, 0);
        slotOnly.getNeedsTable().clearSelection();

        // Act
        click(nothing, "Book");
        click(needOnly, "Book");
        click(slotOnly, "Book");

        // Assert
        assertAll(
                () -> assertEquals(SELECT_NEED_AND_SLOT, nothing.getStatusMessage()),
                () -> assertEquals(SELECT_NEED_AND_SLOT, needOnly.getStatusMessage()),
                () -> assertEquals(SELECT_NEED_AND_SLOT, slotOnly.getStatusMessage()),
                () -> assertEquals(RED, statusColour(slotOnly)),
                () -> assertTrue(questions.isEmpty()),
                () -> assertTrue(service.changes().isEmpty()));
    }

    @Test
    @DisplayName("TC13C-12 One error of the server while booking is shown in red and nothing is reloaded")
    void tc13c12ServerRejectsTheBookingWithOneError() {
        // Arrange
        IU_ManageAppointments screen = needAndSlot(LUCIA, 0);
        service.rejectNext("The slot is no longer free");
        int callsBefore = service.calls().size();

        // Act
        click(screen, "Book");

        // Assert
        assertAll(
                () -> assertEquals("⚠ The slot is no longer free", screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals(callsBefore + 1, service.calls().size()),
                () -> assertEquals(2, screen.getNeedsTable().getRowCount()),
                () -> assertEquals(3, screen.getAppointmentsTable().getRowCount()));
    }

    @Test
    @DisplayName("TC13C-13 Two errors of the server while booking are shown together")
    void tc13c13ServerRejectsTheBookingWithTwoErrors() {
        // Arrange
        IU_ManageAppointments screen = needAndSlot(LUCIA, 0);
        service.rejectNext("The patient P-0001 is not active", "The slot is in the past");

        // Act
        click(screen, "Book");

        // Assert
        assertEquals("⚠ The patient P-0001 is not active The slot is in the past", screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC13C-14 Denied while booking: the message, every action and table disabled, Back enabled")
    void tc13c14DeniedWhileBooking() {
        // Arrange
        IU_ManageAppointments screen = needAndSlot(LUCIA, 0);
        service.denyAll();

        // Act
        click(screen, "Book");

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals(List.of("Back"), Screen.enabledButtons(screen)),
                () -> assertFalse(tablesEnabled(screen)));
    }

    @Test
    @DisplayName("TC13C-15 Denied on opening: no data, only Back enabled, and Back runs its action")
    void tc13c15DeniedOnOpening() {
        // Arrange
        service.denyAll();

        // Act
        IU_ManageAppointments screen = open();
        click(screen, "Back");

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals(0, screen.getNeedsTable().getRowCount()),
                () -> assertEquals(0, screen.getAppointmentsTable().getRowCount()),
                () -> assertEquals(List.of("Back"), Screen.enabledButtons(screen)),
                () -> assertFalse(tablesEnabled(screen)),
                () -> assertEquals(List.of("needs"), service.calls()),
                () -> assertEquals(1, backs),
                () -> assertTrue(questions.isEmpty()));
    }

    @Test
    @DisplayName("TC13C-16 Denied when the appointments are read: the denied message and only Back enabled")
    void tc13c16DeniedWhileReadingTheAppointments() {
        // Arrange
        service.denyAppointments();

        // Act
        IU_ManageAppointments screen = open();

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertEquals(List.of("Back"), Screen.enabledButtons(screen)),
                () -> assertFalse(tablesEnabled(screen)),
                () -> assertEquals(0, screen.getAppointmentsTable().getRowCount()));
    }

    @Test
    @DisplayName("TC13C-17 Denied while finding slots: the denied message and only Back enabled")
    void tc13c17DeniedWhileFindingSlots() {
        // Arrange
        IU_ManageAppointments screen = open();
        select(screen.getNeedsTable(), LUCIA);
        service.denyAll();

        // Act
        findSlots(screen, DAY);

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertEquals(List.of("Back"), Screen.enabledButtons(screen)),
                () -> assertEquals(0, screen.getSlotsTable().getRowCount()));
    }

    @Test
    @DisplayName("TC13C-18 A rejection of the reading of the needs is shown in red")
    void tc13c18ValidationErrorWhileReading() {
        // Arrange
        service.failReads(new ValidationException(List.of("The orders of C4 could not be read")));

        // Act
        IU_ManageAppointments screen = open();

        // Assert
        assertAll(
                () -> assertEquals("⚠ The orders of C4 could not be read", screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals(0, screen.getNeedsTable().getRowCount()),
                () -> assertEquals(5, Screen.enabledButtons(screen).size()));
    }

    @Test
    @DisplayName("TC13C-19 A failure of the reading is explained with its cause (colour: TC13C-42)")
    void tc13c19FailureWhileReading() {
        // Arrange
        service.failReads(new IllegalStateException("The server cannot be reached"));

        // Act
        IU_ManageAppointments screen = open();

        // Assert
        assertAll(
                () -> assertTrue(screen.getStatusMessage()
                        .endsWith("The data could not be read: The server cannot be reached"),
                        screen.getStatusMessage()),
                () -> assertEquals(0, screen.getNeedsTable().getRowCount()),
                () -> assertEquals(0, screen.getAppointmentsTable().getRowCount()));
    }

    @Test
    @DisplayName("TC13C-20 Move asks, sends one move of the selected appointment to its slot, and never a booking")
    void tc13c20MoveTheSelectedAppointment() {
        // Arrange
        IU_ManageAppointments screen = open();
        select(screen.getNeedsTable(), LUCIA);
        select(screen.getAppointmentsTable(), APT_0001);
        findSlots(screen, DAY);
        select(screen.getSlotsTable(), 0);

        // Act
        click(screen, "Move");

        // Assert
        assertAll(
                () -> assertEquals(List.of("reschedule APT-0001 XRAY-1 2026-11-30T09:00"), service.changes()),
                () -> assertEquals(List.of("Move the appointment APT-0001 to 30/11/2026 09:00?"), questions),
                () -> assertEquals(List.of("XRAY-CHEST 2026-11-30 2026-11-30"), service.slotQueries()),
                () -> assertEquals(REQUESTER, service.requesters().get(3)),
                () -> assertEquals(List.of("APT-0001", "P-0003", "XRAY-CHEST", "30/11/2026 09:00", "RESCHEDULED"),
                        appointmentRow(screen, APT_0001)),
                () -> assertEquals(2, screen.getNeedsTable().getRowCount()),
                () -> assertEquals(List.of("needs", "appointments"), service.calls().subList(4, 6)));
    }

    @Test
    @DisplayName("TC13C-21 Move without an appointment or without a slot asks for them and sends nothing")
    void tc13c21MoveWithoutAppointmentOrSlot() {
        // Arrange
        IU_ManageAppointments slotOnly = needAndSlot(LUCIA, 0);
        IU_ManageAppointments appointmentOnly = open();
        select(appointmentOnly.getAppointmentsTable(), APT_0001);

        // Act
        click(slotOnly, "Move");
        click(appointmentOnly, "Move");

        // Assert
        assertAll(
                () -> assertEquals(SELECT_APPOINTMENT_AND_SLOT, slotOnly.getStatusMessage()),
                () -> assertEquals(SELECT_APPOINTMENT_AND_SLOT, appointmentOnly.getStatusMessage()),
                () -> assertEquals(RED, statusColour(appointmentOnly)),
                () -> assertTrue(service.changes().isEmpty()));
    }

    @Test
    @DisplayName("TC13C-22 A rejected move shows the reason in red and changes nothing")
    void tc13c22ServerRejectsTheMove() {
        // Arrange
        IU_ManageAppointments screen = appointmentAndSlot(APT_0001, 0);
        service.rejectNext("The slot is outside the schedule of the room");

        // Act
        click(screen, "Move");

        // Assert
        assertAll(
                () -> assertEquals("⚠ The slot is outside the schedule of the room", screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals("BOOKED", Screen.cell(screen.getAppointmentsTable(), APT_0001, "Status")));
    }

    @Test
    @DisplayName("TC13C-23 Denied while moving: the denied message and only Back enabled")
    void tc13c23DeniedWhileMoving() {
        // Arrange
        IU_ManageAppointments screen = appointmentAndSlot(APT_0001, 0);
        service.denyAll();

        // Act
        click(screen, "Move");

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertEquals(List.of("Back"), Screen.enabledButtons(screen)),
                () -> assertFalse(tablesEnabled(screen)));
    }

    @Test
    @DisplayName("TC13C-24 Cancel answered Yes sends one cancellation and the appointment is shown cancelled")
    void tc13c24CancelAnsweredYes() {
        // Arrange
        IU_ManageAppointments screen = open();
        select(screen.getAppointmentsTable(), APT_0003);

        // Act
        click(screen, "Cancel appointment");

        // Assert
        assertAll(
                () -> assertEquals(List.of("Cancel the appointment APT-0003?"), questions),
                () -> assertEquals(List.of("cancel APT-0003"), service.changes()),
                () -> assertEquals(REQUESTER, service.requesters().get(2)),
                () -> assertEquals("CANCELLED", Screen.cell(screen.getAppointmentsTable(), APT_0003, "Status")),
                () -> assertEquals(List.of("needs", "appointments"), service.calls().subList(3, 5)));
    }

    @Test
    @DisplayName("TC13C-25 Cancel answered No sends nothing")
    void tc13c25CancelAnsweredNo() {
        // Arrange
        IU_ManageAppointments screen = open();
        select(screen.getAppointmentsTable(), APT_0001);
        answer = false;

        // Act
        click(screen, "Cancel appointment");

        // Assert
        assertAll(
                () -> assertEquals(List.of("Cancel the appointment APT-0001?"), questions),
                () -> assertTrue(service.changes().isEmpty()),
                () -> assertEquals("BOOKED", Screen.cell(screen.getAppointmentsTable(), APT_0001, "Status")));
    }

    @Test
    @DisplayName("TC13C-26 Cancel without a selected appointment asks for one, without a question")
    void tc13c26CancelWithoutAppointment() {
        // Arrange
        IU_ManageAppointments screen = open();
        select(screen.getNeedsTable(), LUCIA);

        // Act
        click(screen, "Cancel appointment");

        // Assert
        assertAll(
                () -> assertEquals("⚠ Select the appointment to cancel.", screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertTrue(questions.isEmpty()),
                () -> assertTrue(service.changes().isEmpty()));
    }

    @Test
    @DisplayName("TC13C-27 A rejected cancellation shows the reason; a denied one leaves only Back")
    void tc13c27CancelRejectedAndDenied() {
        // Arrange
        IU_ManageAppointments rejected = open();
        select(rejected.getAppointmentsTable(), APT_0001);
        service.rejectNext("The appointment APT-0001 is already cancelled");

        // Act
        click(rejected, "Cancel appointment");
        String rejectedMessage = rejected.getStatusMessage();
        service.denyAll();
        rejected.onCancelAppointment("APT-0001");

        // Assert
        assertAll(
                () -> assertEquals("⚠ The appointment APT-0001 is already cancelled", rejectedMessage),
                () -> assertEquals(DENIED, rejected.getStatusMessage()),
                () -> assertEquals(List.of("Back"), Screen.enabledButtons(rejected)),
                () -> assertEquals("BOOKED", Screen.cell(rejected.getAppointmentsTable(), APT_0001, "Status")));
    }

    @Test
    @DisplayName("TC13C-28 No cell of the three tables is editable and each table selects one row")
    void tc13c28CellsAreNotEditable() {
        // Arrange
        IU_ManageAppointments screen = needAndSlot(LUCIA, 0);
        List<Boolean> editable = new ArrayList<>();
        int cells = 0;

        // Act
        for (JTable table : List.of(screen.getNeedsTable(), screen.getSlotsTable(), screen.getAppointmentsTable())) {
            for (int row = 0; row < table.getRowCount(); row++) {
                for (int column = 0; column < table.getColumnCount(); column++) {
                    editable.add(table.isCellEditable(row, column));
                    cells++;
                }
            }
        }
        int total = cells;

        // Assert
        assertAll(
                () -> assertTrue(total >= 2 * 4 + 2 * 3 + 3 * 5, "cells " + total),
                () -> assertFalse(editable.contains(true)),
                () -> assertEquals(ListSelectionModel.SINGLE_SELECTION,
                        screen.getNeedsTable().getSelectionModel().getSelectionMode()),
                () -> assertEquals(ListSelectionModel.SINGLE_SELECTION,
                        screen.getSlotsTable().getSelectionModel().getSelectionMode()),
                () -> assertEquals(ListSelectionModel.SINGLE_SELECTION,
                        screen.getAppointmentsTable().getSelectionModel().getSelectionMode()));
    }

    @Test
    @DisplayName("TC13C-29 The buttons and the keys of the code: Alt+F, Alt+B, Alt+M, Alt+C and Back Alt+B (N-02)")
    void tc13c29ButtonsAndMnemonics() {
        // Act
        IU_ManageAppointments screen = open();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Find slots", "Book", "Move", "Cancel appointment", "Back"),
                        Screen.buttonTexts(screen)),
                () -> assertEquals(KeyEvent.VK_F, Screen.button(screen, "Find slots").getMnemonic()),
                () -> assertEquals(KeyEvent.VK_B, Screen.button(screen, "Book").getMnemonic()),
                () -> assertEquals(KeyEvent.VK_M, Screen.button(screen, "Move").getMnemonic()),
                () -> assertEquals(KeyEvent.VK_C, Screen.button(screen, "Cancel appointment").getMnemonic()),
                () -> assertEquals(KeyEvent.VK_B, Screen.button(screen, "Back").getMnemonic()));
    }

    @Test
    @DisplayName("TC13C-30 Back runs its action once, without a question and without calling C5")
    void tc13c30Back() {
        // Arrange
        IU_ManageAppointments screen = needAndSlot(LUCIA, 0);
        int callsBefore = service.calls().size();

        // Act
        click(screen, "Back");

        // Assert
        assertAll(
                () -> assertEquals(1, backs),
                () -> assertTrue(questions.isEmpty()),
                () -> assertEquals(callsBefore, service.calls().size()));
    }

    @Test
    @DisplayName("TC13C-31 The public callbacks: showConfirmation in green, showErrors joined in red")
    void tc13c31Callbacks() {
        // Arrange
        IU_ManageAppointments screen = open();

        // Act
        screen.showConfirmation("Appointment APT-0004 booked on 30/11/2026 09:15.");
        Color green = statusColour(screen);
        String confirmation = screen.getStatusMessage();
        screen.showErrors(List.of("First error", "Second error"));

        // Assert
        assertAll(
                () -> assertEquals("Appointment APT-0004 booked on 30/11/2026 09:15.", confirmation),
                () -> assertEquals(GREEN, green),
                () -> assertEquals("⚠ First error Second error", screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)));
    }

    @Test
    @DisplayName("TC13C-32 isCancelled is true only for a cancelled appointment")
    void tc13c32IsCancelled() {
        // Arrange
        List<Boolean> cancelled = new ArrayList<>();

        // Act
        for (AppointmentStatus status : AppointmentStatus.values()) {
            cancelled.add(IU_ManageAppointments.isCancelled(new Appointment("APT-0001", "NEED-0009", "P-0003",
                    "XRAY-CHEST", "XRAY-1", "mgarcia", null, null, status)));
        }

        // Assert
        assertEquals(List.of(false, false, true), cancelled);
    }

    @Test
    @DisplayName("TC13C-38 With only an appointment selected, Find slots asks for the slots of its test type")
    void tc13c38FindSlotsOfTheSelectedAppointment() {
        // Arrange
        IU_ManageAppointments screen = open();
        select(screen.getAppointmentsTable(), APT_0003);

        // Act
        findSlots(screen, DAY);

        // Assert
        assertAll(
                () -> assertEquals(List.of("CT-HEAD 2026-11-30 2026-11-30"), service.slotQueries()),
                () -> assertEquals(List.of(List.of("CT-1", "30/11/2026 12:00", "30/11/2026 12:30")),
                        Screen.rows(screen.getSlotsTable())),
                () -> assertEquals("1 free slot.", screen.getStatusMessage()));
    }

    @Test
    @DisplayName("TC13C-39 With a need and an appointment selected, Find slots uses the test type of the appointment")
    void tc13c39AppointmentWinsOverTheNeedWhenFindingSlots() {
        // Arrange
        IU_ManageAppointments screen = open();
        select(screen.getNeedsTable(), LUCIA);
        select(screen.getAppointmentsTable(), APT_0003);

        // Act
        findSlots(screen, DAY);

        // Assert
        assertEquals(List.of("CT-HEAD 2026-11-30 2026-11-30"), service.slotQueries());
    }

    @Test
    @DisplayName("TC13C-40 An appointment is moved to a slot of its own test type without selecting any need")
    void tc13c40MoveWithoutAnyNeed() {
        // Arrange
        IU_ManageAppointments screen = open();
        select(screen.getAppointmentsTable(), APT_0003);
        findSlots(screen, DAY);
        select(screen.getSlotsTable(), 0);

        // Act
        click(screen, "Move");

        // Assert
        assertAll(
                () -> assertEquals(List.of("reschedule APT-0003 CT-1 2026-11-30T12:00"), service.changes()),
                () -> assertEquals(List.of("APT-0003", "P-0004", "CT-HEAD", "30/11/2026 12:00", "RESCHEDULED"),
                        appointmentRow(screen, APT_0003)));
    }

    @Test
    @DisplayName("TC13C-41 The needs table shows the order date and then the due date of each need")
    void tc13c41NeedsShowTheOrderDateAndTheDueDate() {
        // Act
        IU_ManageAppointments screen = open();

        // Assert
        List<String> columns = Screen.columns(screen.getNeedsTable()).stream().map(String::toLowerCase).toList();
        List<String> lucia = Screen.rows(screen.getNeedsTable()).get(LUCIA);
        List<String> mario = Screen.rows(screen.getNeedsTable()).get(MARIO);
        assertAll(
                () -> assertTrue(columns.stream().anyMatch(name -> name.contains("order")), columns.toString()),
                () -> assertTrue(columns.stream().anyMatch(name -> name.contains("due")), columns.toString()),
                () -> assertTrue(lucia.contains("20/11/2026"), lucia.toString()),
                () -> assertTrue(lucia.indexOf("20/11/2026") < lucia.indexOf("27/11/2026"), lucia.toString()),
                () -> assertTrue(mario.contains("21/11/2026"), mario.toString()),
                () -> assertTrue(mario.indexOf("21/11/2026") < mario.lastIndexOf(""), mario.toString()));
    }

    @Test
    @DisplayName("TC13C-42 A failure of the reading is shown in the error style (red)")
    void tc13c42FailureWhileReadingIsRed() {
        // Arrange
        service.failReads(new IllegalStateException("The server cannot be reached"));

        // Act
        IU_ManageAppointments screen = open();

        // Assert
        assertAll(
                () -> assertTrue(screen.getStatusMessage()
                        .contains("The data could not be read: The server cannot be reached")),
                () -> assertEquals(RED, statusColour(screen)));
    }

    @Test
    @DisplayName("TC13C-45 Move answered No sends nothing and the appointment stays booked (N-08)")
    void tc13c45MoveAnsweredNo() {
        // Arrange
        IU_ManageAppointments screen = appointmentAndSlot(APT_0001, 0);
        answer = false;

        // Act
        click(screen, "Move");

        // Assert
        assertAll(
                () -> assertEquals(List.of("Move the appointment APT-0001 to 30/11/2026 09:00?"), questions),
                () -> assertTrue(service.changes().isEmpty()),
                () -> assertEquals("BOOKED", Screen.cell(screen.getAppointmentsTable(), APT_0001, "Status")),
                () -> assertEquals("2 free slots.", screen.getStatusMessage()));
    }

    @Test
    @DisplayName("TC13C-43 After a booking the confirmation stays and is shown in green (F-01)")
    void tc13c43BookingConfirmationIsShown() {
        // Arrange
        IU_ManageAppointments screen = needAndSlot(LUCIA, 1);

        // Act
        click(screen, "Book");

        // Assert
        assertAll(
                () -> assertEquals(List.of("book NEED-0001 XRAY-1 2026-11-30T09:15 2026-11-30T09:30"),
                        service.changes()),
                () -> assertEquals("Appointment APT-0004 booked on 30/11/2026 09:15.", screen.getStatusMessage()),
                () -> assertEquals(GREEN, statusColour(screen)));
    }

    @Test
    @DisplayName("TC13C-44 After a move and after a cancellation the confirmation stays and is shown in green (F-01)")
    void tc13c44MoveAndCancelConfirmationsAreShown() {
        // Arrange
        IU_ManageAppointments moving = appointmentAndSlot(APT_0001, 0);
        IU_ManageAppointments cancelling = open();
        select(cancelling.getAppointmentsTable(), APT_0003);

        // Act
        click(moving, "Move");
        String moved = moving.getStatusMessage();
        Color movedColour = statusColour(moving);
        click(cancelling, "Cancel appointment");

        // Assert
        assertAll(
                () -> assertEquals(List.of("reschedule APT-0001 XRAY-1 2026-11-30T09:00", "cancel APT-0003"),
                        service.changes()),
                () -> assertEquals("Appointment APT-0001 moved to 30/11/2026 09:00.", moved),
                () -> assertEquals(GREEN, movedColour),
                () -> assertEquals("Appointment APT-0003 cancelled.", cancelling.getStatusMessage()),
                () -> assertEquals(GREEN, statusColour(cancelling)));
    }

    @Test
    @DisplayName("TC13C-46 The reading fails after a booking: one booking sent and the error in red, no confirmation")
    void tc13c46ReloadFailsAfterBooking() {
        // Arrange
        IU_ManageAppointments screen = needAndSlot(LUCIA, 1);
        service.failReads(new IllegalStateException("The server cannot be reached"));

        // Act
        click(screen, "Book");

        // Assert
        assertAll(
                () -> assertEquals(List.of("book NEED-0001 XRAY-1 2026-11-30T09:15 2026-11-30T09:30"),
                        service.changes()),
                () -> assertEquals("⚠ The data could not be read: The server cannot be reached",
                        screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)));
    }

    @Test
    @DisplayName("TC13C-47 With the clock fixed on Monday 30/11/2026 the day starts as that day and finds its slots")
    void tc13c47DayStartsAsTheDayOfTheClock() {
        // Arrange
        IU_ManageAppointments screen = openOnMonday();
        String shown = Screen.field(screen, "Day").getText();
        select(screen.getNeedsTable(), LUCIA);

        // Act
        click(screen, "Find slots");

        // Assert
        assertAll(
                () -> assertEquals(DAY, shown),
                () -> assertEquals(List.of("XRAY-CHEST 2026-11-30 2026-11-30"), service.slotQueries()));
    }

    @Test
    @DisplayName("TC13C-48 With a fixed clock the day written by the user is kept after a reload")
    void tc13c48DayWrittenIsKeptWithAFixedClock() {
        // Arrange
        IU_ManageAppointments screen = openOnMonday();
        Screen.field(screen, "Day").setText("01/12/2026");

        // Act
        screen.loadData();

        // Assert
        assertAll(
                () -> assertEquals("01/12/2026", Screen.field(screen, "Day").getText()),
                () -> assertEquals(List.of("needs", "appointments", "needs", "appointments"), service.calls()));
    }

    @Test
    @DisplayName("TC13C-49 The four-parameter constructor uses the system clock: the day starts as today")
    void tc13c49FourParameterConstructorUsesTheSystemClock() {
        // Arrange
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String before = format.format(LocalDate.now());

        // Act
        IU_ManageAppointments screen = open();
        String shown = Screen.field(screen, "Day").getText();
        String after = format.format(LocalDate.now());

        // Assert
        assertTrue(shown.equals(before) || shown.equals(after), shown);
    }
}
