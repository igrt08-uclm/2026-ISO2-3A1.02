package es.uclm.esi.iso2.client.appointments.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.shared.ValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the screen {@link IU_ManageWaitlist} of UC15 (#133). The cases are those of
 * {@code doc/testing/test-cases/UC15-client-test-design.md}. The screen is only built, never shown, so the tests need
 * no display; every question is answered by the test, which also records it.
 */
@DisplayName("IU_ManageWaitlist")
class IU_ManageWaitlistTest {
    private static final String REQUESTER = "lmartin";
    private static final String DAY = "30/11/2026";
    private static final String DENIED = "⚠ You do not have permission to manage the waitlist.";
    private static final String EMPTY = "The waitlist of that test type is empty.";
    private static final String NOBODY = "There is no patient who can fill this opening.";
    private static final List<String> ACTIONS =
            List.of("Find openings", "Add to waitlist", "Change tier", "Remove", "Propose patient", "Fill opening");

    private FakeWaitlistService waitlist;
    private FakeAppointmentService appointments;
    private List<String> questions;
    private boolean answer;
    private int backs;

    @BeforeEach
    void setUp() {
        waitlist = FakeWaitlistService.withUsualData();
        appointments = FakeAppointmentService.withUsualData();
        questions = new ArrayList<>();
        answer = true;
        backs = 0;
    }

    @Test
    @DisplayName("TC15C-01 opening shows the data of the first test type and today")
    void opening() {
        // Act
        IU_ManageWaitlist screen = open();

        // Assert
        assertAll(
                () -> assertEquals(3, combo(screen, 0).getItemCount()),
                () -> assertEquals(2, combo(screen, 1).getItemCount()),
                () -> assertEquals(2, screen.getNeedsTable().getRowCount()),
                () -> assertEquals(2, screen.getWaitlistTable().getRowCount()),
                () -> assertEquals(DAY, Screen.field(screen, "Day").getText()));
    }

    @Test
    @DisplayName("TC15C-02 an empty waitlist shows a notice")
    void emptyWaitlist() {
        // Arrange
        IU_ManageWaitlist screen = open();

        // Act
        combo(screen, 0).setSelectedItem("MRI-KNEE");

        // Assert
        assertEquals(0, screen.getWaitlistTable().getRowCount());
        assertEquals(EMPTY, screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC15C-03 the notice disappears with a waitlist that has entries")
    void noticeDisappears() {
        // Arrange
        IU_ManageWaitlist screen = open();
        combo(screen, 0).setSelectedItem("MRI-KNEE");

        // Act
        combo(screen, 0).setSelectedItem("XRAY-CHEST");

        // Assert
        assertEquals(2, screen.getWaitlistTable().getRowCount());
        assertTrue(screen.getStatusMessage().isBlank(), screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC15C-04 the entries keep the order of the service")
    void order() {
        // Act
        IU_ManageWaitlist screen = open();

        // Assert
        assertEquals(List.of("WL-0001", "WL-0002"),
                Screen.rows(screen.getWaitlistTable()).stream().map(row -> row.get(0)).toList());
    }

    @Test
    @DisplayName("TC15C-05 opening and changing the test type ask for the waitlist once each")
    void oneQueryEachTime() {
        // Arrange
        IU_ManageWaitlist screen = open();
        List<String> afterOpening = List.copyOf(waitlist.queries());

        // Act
        combo(screen, 0).setSelectedItem("MRI-KNEE");

        // Assert
        assertEquals(List.of("waitlist XRAY-CHEST"), afterOpening);
        assertEquals(List.of("waitlist XRAY-CHEST", "waitlist MRI-KNEE"), waitlist.queries());
    }

    @Test
    @DisplayName("TC15C-06 without permission the actions are disabled and Back is not")
    void deniedOnOpening() {
        // Arrange
        appointments.denyAll();

        // Act
        IU_ManageWaitlist screen = open();

        // Assert
        assertEquals(DENIED, screen.getStatusMessage());
        assertEquals(List.of("Back"), Screen.enabledButtons(screen));
    }

    @Test
    @DisplayName("TC15C-07 a rejected reading shows its errors")
    void rejectedReading() {
        // Arrange
        appointments.failReads(new ValidationException(List.of("The orders could not be read")));

        // Act
        IU_ManageWaitlist screen = open();

        // Assert
        assertEquals("⚠ The orders could not be read", screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC15C-08 a failed reading shows the error")
    void failedReading() {
        // Arrange
        appointments.failReads(new IllegalStateException("The server cannot be reached"));

        // Act
        IU_ManageWaitlist screen = open();

        // Assert
        assertErrorMentions(screen, "The server cannot be reached");
    }

    @Test
    @DisplayName("TC15C-09 a failed waitlist empties the table and shows the error")
    void failedWaitlist() {
        // Arrange
        IU_ManageWaitlist screen = open();
        waitlist.failWaitlist(new IllegalStateException("Storage unavailable"));

        // Act
        combo(screen, 0).setSelectedItem("MRI-KNEE");

        // Assert
        assertEquals(0, screen.getWaitlistTable().getRowCount());
        assertErrorMentions(screen, "Storage unavailable");
    }

    @Test
    @DisplayName("TC15C-10 a need is added and the lists are reloaded")
    void add() {
        // Arrange
        IU_ManageWaitlist screen = open();
        screen.getNeedsTable().setRowSelectionInterval(0, 0);
        combo(screen, 1).setSelectedItem("ROUTINE");

        // Act
        click(screen, "Add to waitlist");

        // Assert
        assertEquals(List.of("add NEED-0001 ROUTINE"), waitlist.changes());
        assertEquals("Entry WL-0003 added to the waitlist.", screen.getStatusMessage());
        assertEquals(3, screen.getWaitlistTable().getRowCount());
    }

    @Test
    @DisplayName("TC15C-11 adding without a need sends nothing")
    void addWithoutNeed() {
        // Arrange
        IU_ManageWaitlist screen = open();

        // Act
        click(screen, "Add to waitlist");

        // Assert
        assertError(screen);
        assertTrue(waitlist.changes().isEmpty());
    }

    @Test
    @DisplayName("TC15C-12 both errors of a rejected add are shown")
    void addRejected() {
        // Arrange
        IU_ManageWaitlist screen = open();
        screen.getNeedsTable().setRowSelectionInterval(0, 0);
        waitlist.rejectNext("First reason.", "Second reason.");

        // Act
        click(screen, "Add to waitlist");

        // Assert
        assertEquals("⚠ First reason. Second reason.", screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC15C-13 the tier of an entry is changed")
    void changeTier() {
        // Arrange
        IU_ManageWaitlist screen = open();
        screen.getWaitlistTable().setRowSelectionInterval(0, 0);
        combo(screen, 1).setSelectedItem("ROUTINE");

        // Act
        click(screen, "Change tier");

        // Assert
        assertEquals(List.of("change WL-0001 ROUTINE"), waitlist.changes());
        assertEquals("Entry WL-0001 moved to the tier ROUTINE.", screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC15C-14 changing without an entry sends nothing")
    void changeWithoutEntry() {
        // Arrange
        IU_ManageWaitlist screen = open();

        // Act
        click(screen, "Change tier");

        // Assert
        assertError(screen);
        assertTrue(waitlist.changes().isEmpty());
    }

    @Test
    @DisplayName("TC15C-15 an accepted removal removes the entry")
    void remove() {
        // Arrange
        IU_ManageWaitlist screen = open();
        screen.getWaitlistTable().setRowSelectionInterval(0, 0);

        // Act
        click(screen, "Remove");

        // Assert
        assertEquals(1, questions.size());
        assertEquals(List.of("remove WL-0001"), waitlist.changes());
        assertEquals("Entry WL-0001 removed from the waitlist.", screen.getStatusMessage());
        assertEquals(1, screen.getWaitlistTable().getRowCount());
    }

    @Test
    @DisplayName("TC15C-16 a refused removal sends nothing")
    void removeRefused() {
        // Arrange
        IU_ManageWaitlist screen = open();
        screen.getWaitlistTable().setRowSelectionInterval(0, 0);
        answer = false;

        // Act
        click(screen, "Remove");

        // Assert
        assertEquals(1, questions.size());
        assertTrue(waitlist.changes().isEmpty());
    }

    @Test
    @DisplayName("TC15C-17 removing without an entry shows an error")
    void removeWithoutEntry() {
        // Arrange
        IU_ManageWaitlist screen = open();

        // Act
        click(screen, "Remove");

        // Assert
        assertError(screen);
        assertTrue(questions.isEmpty());
    }

    @Test
    @DisplayName("TC15C-18 the openings of a day are listed and counted")
    void openings() {
        // Arrange
        IU_ManageWaitlist two = open();
        FakeWaitlistService both = waitlist;
        waitlist = FakeWaitlistService.withUsualData();
        waitlist.keepOneOpening();
        IU_ManageWaitlist one = open();

        // Act
        click(two, "Find openings");
        click(one, "Find openings");

        // Assert
        assertAll(
                () -> assertTrue(both.calls().contains("openings XRAY-CHEST 2026-11-30 2026-11-30")),
                () -> assertEquals(2, two.getOpeningsTable().getRowCount()),
                () -> assertEquals("2 openings.", two.getStatusMessage()),
                () -> assertEquals(1, one.getOpeningsTable().getRowCount()),
                () -> assertEquals("1 opening.", one.getStatusMessage()));
    }

    @Test
    @DisplayName("TC15C-19 a day without openings shows a notice")
    void noOpenings() {
        // Arrange
        IU_ManageWaitlist screen = open();
        Screen.field(screen, "Day").setText("01/12/2026");

        // Act
        click(screen, "Find openings");

        // Assert
        assertEquals(0, screen.getOpeningsTable().getRowCount());
        assertEquals("There are no openings on that day for that test type.", screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC15C-20 a blank or badly written day empties the openings and sends nothing")
    void badDay() {
        for (String day : List.of("", "2026-11-30", "32/11/2026")) {
            // Arrange
            waitlist = FakeWaitlistService.withUsualData();
            IU_ManageWaitlist screen = open();
            click(screen, "Find openings");
            waitlist.calls().clear();
            Screen.field(screen, "Day").setText(day);

            // Act
            click(screen, "Find openings");

            // Assert
            assertEquals("⚠ The day must be written as dd/MM/yyyy", screen.getStatusMessage(), day);
            assertEquals(0, screen.getOpeningsTable().getRowCount(), day);
            assertTrue(waitlist.calls().isEmpty(), day);
        }
    }

    @Test
    @DisplayName("TC15C-21 the proposed patient is selected and named")
    void propose() {
        // Arrange
        IU_ManageWaitlist screen = openWithOpening();

        // Act
        click(screen, "Propose patient");

        // Assert
        assertEquals(1, screen.getWaitlistTable().getSelectedRow());
        assertTrue(screen.getStatusMessage().contains("P-0002"), screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC15C-22 an empty proposal shows a notice and selects nothing")
    void proposeNobody() {
        // Arrange
        waitlist.proposeNobody();
        IU_ManageWaitlist screen = openWithOpening();
        screen.getWaitlistTable().setRowSelectionInterval(0, 0);

        // Act
        click(screen, "Propose patient");

        // Assert
        assertEquals(NOBODY, screen.getStatusMessage());
        assertEquals(-1, screen.getWaitlistTable().getSelectedRow());
    }

    @Test
    @DisplayName("TC15C-23 proposing without an opening shows an error")
    void proposeWithoutOpening() {
        // Arrange
        IU_ManageWaitlist screen = open();

        // Act
        click(screen, "Propose patient");

        // Assert
        assertError(screen);
        assertEquals(List.of("waitlist XRAY-CHEST"), waitlist.queries().subList(0, 1));
        assertTrue(waitlist.calls().stream().noneMatch(call -> call.startsWith("propose")));
    }

    @Test
    @DisplayName("TC15C-24 an accepted filling books the appointment and reloads")
    void fill() {
        // Arrange
        IU_ManageWaitlist screen = openWithOpening();
        screen.getWaitlistTable().setRowSelectionInterval(0, 0);

        // Act
        click(screen, "Fill opening");

        // Assert
        assertEquals(List.of("fill WL-0001 2026-11-30T09:00"), waitlist.changes());
        assertEquals("Opening filled: appointment APT-0009 booked on 30/11/2026 09:00.", screen.getStatusMessage());
        assertEquals(0, screen.getOpeningsTable().getRowCount());
        assertEquals(1, screen.getWaitlistTable().getRowCount());
    }

    @Test
    @DisplayName("TC15C-25 a refused filling sends nothing")
    void fillRefused() {
        // Arrange
        IU_ManageWaitlist screen = openWithOpening();
        screen.getWaitlistTable().setRowSelectionInterval(0, 0);
        answer = false;

        // Act
        click(screen, "Fill opening");

        // Assert
        assertEquals(1, questions.size());
        assertTrue(waitlist.changes().isEmpty());
    }

    @Test
    @DisplayName("TC15C-26 filling without an entry or an opening shows an error")
    void fillWithoutSelection() {
        // Arrange
        IU_ManageWaitlist withoutEntry = openWithOpening();
        IU_ManageWaitlist withoutOpening = open();
        withoutOpening.getWaitlistTable().setRowSelectionInterval(0, 0);

        // Act
        click(withoutEntry, "Fill opening");
        click(withoutOpening, "Fill opening");

        // Assert
        assertError(withoutEntry);
        assertError(withoutOpening);
        assertTrue(questions.isEmpty());
    }

    @Test
    @DisplayName("TC15C-27 a rejected filling shows the reason and keeps the entry")
    void fillRejected() {
        // Arrange
        IU_ManageWaitlist screen = openWithOpening();
        screen.getWaitlistTable().setRowSelectionInterval(0, 0);
        waitlist.rejectNext("The slot is not free");

        // Act
        click(screen, "Fill opening");

        // Assert
        assertEquals("⚠ The slot is not free", screen.getStatusMessage());
        assertEquals(2, screen.getWaitlistTable().getRowCount());
    }

    @Test
    @DisplayName("TC15C-28 a denied action disables the actions")
    void deniedAction() {
        for (String action : ACTIONS) {
            // Arrange
            waitlist = FakeWaitlistService.withUsualData();
            IU_ManageWaitlist screen = readyForEveryAction();
            waitlist.denyAll();

            // Act
            click(screen, action);

            // Assert
            assertEquals(DENIED, screen.getStatusMessage(), action);
            assertEquals(List.of("Back"), Screen.enabledButtons(screen), action);
        }
    }

    @Test
    @DisplayName("TC15C-29 an unexpected failure of every action is shown")
    void failedAction() {
        for (String action : ACTIONS) {
            // Arrange
            waitlist = FakeWaitlistService.withUsualData();
            IU_ManageWaitlist screen = readyForEveryAction();
            waitlist.failAll(new IllegalStateException("Connection lost"));

            // Act
            click(screen, action);

            // Assert
            assertErrorMentions(screen, "Connection lost");
        }
    }

    @Test
    @DisplayName("TC15C-30 a change with a failed reload shows the error, not the success")
    void failedReload() {
        // Arrange
        IU_ManageWaitlist adding = open();
        adding.getNeedsTable().setRowSelectionInterval(0, 0);
        appointments.failReads(new IllegalStateException("Needs unavailable"));

        // Act
        click(adding, "Add to waitlist");

        // Assert
        assertErrorMentions(adding, "Needs unavailable");

        // Arrange
        appointments = FakeAppointmentService.withUsualData();
        waitlist = FakeWaitlistService.withUsualData();
        IU_ManageWaitlist removing = open();
        removing.getWaitlistTable().setRowSelectionInterval(0, 0);
        waitlist.failWaitlist(new IllegalStateException("Storage unavailable"));

        // Act
        click(removing, "Remove");

        // Assert
        assertErrorMentions(removing, "Storage unavailable");
    }

    @Test
    @DisplayName("TC15C-31 Back goes back")
    void back() {
        // Arrange
        IU_ManageWaitlist screen = open();

        // Act
        click(screen, "Back");

        // Assert
        assertEquals(1, backs);
    }

    @Test
    @DisplayName("TC15C-34 no access key is repeated")
    void uniqueAccessKeys() {
        // Act
        IU_ManageWaitlist screen = open();
        List<Integer> keys = new ArrayList<>();
        Screen.all(screen, JButton.class).stream().map(JButton::getMnemonic).filter(key -> key != 0)
                .forEach(keys::add);
        Screen.all(screen, JLabel.class).stream().map(JLabel::getDisplayedMnemonic).filter(key -> key != 0)
                .forEach(keys::add);

        // Assert
        assertEquals(keys.stream().distinct().count(), keys.size(), keys.toString());
    }

    private IU_ManageWaitlist open() {
        Clock monday = Clock.fixed(Instant.parse("2026-11-30T09:00:00Z"), ZoneOffset.UTC);
        return new IU_ManageWaitlist(waitlist, appointments, new FakeParameterService(), REQUESTER, question -> {
            questions.add(question);
            return answer;
        }, () -> backs++, monday);
    }

    /** Opens the screen, finds the openings of today and selects the first one. */
    private IU_ManageWaitlist openWithOpening() {
        IU_ManageWaitlist screen = open();
        click(screen, "Find openings");
        screen.getOpeningsTable().setRowSelectionInterval(0, 0);
        return screen;
    }

    /** A screen with a need, an entry and an opening selected, so that every action reaches the server. */
    private IU_ManageWaitlist readyForEveryAction() {
        IU_ManageWaitlist screen = openWithOpening();
        screen.getNeedsTable().setRowSelectionInterval(0, 0);
        screen.getWaitlistTable().setRowSelectionInterval(0, 0);
        return screen;
    }

    private static JComboBox<?> combo(IU_ManageWaitlist screen, int index) {
        return Screen.all(screen, JComboBox.class).get(index);
    }

    private static void click(IU_ManageWaitlist screen, String button) {
        Screen.button(screen, button).doClick();
    }

    private static void assertError(IU_ManageWaitlist screen) {
        assertTrue(screen.getStatusMessage().startsWith("⚠ "), screen.getStatusMessage());
    }

    private static void assertErrorMentions(IU_ManageWaitlist screen, String text) {
        assertError(screen);
        assertTrue(screen.getStatusMessage().contains(text), screen.getStatusMessage());
    }
}
