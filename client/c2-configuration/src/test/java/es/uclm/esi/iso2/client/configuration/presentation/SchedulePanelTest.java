package es.uclm.esi.iso2.client.configuration.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.configuration.domain.Schedule;
import java.awt.Container;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests of the tab <em>Schedules</em> of UC17 ({@link SchedulePanel}), used through the whole screen
 * {@link IU_ConfigureParameters}. A schedule is identified by its room or resource (Q7). The screen is never shown,
 * so no display is needed.
 */
@DisplayName("SchedulePanel")
class SchedulePanelTest {
    private static final String REQUESTER = "admin";
    private static final String RESOURCE = "Room or resource";
    private static final String START = "Start time (HH:mm)";
    private static final String END = "End time (HH:mm)";

    private FakeSystemParameterService service;
    private List<String> questions;
    private IU_ConfigureParameters screen;
    private Container tab;
    private JTable table;

    @BeforeEach
    void setUp() {
        service = new FakeSystemParameterService();
        service.add(new Schedule("Room MR-1", Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), LocalTime.of(9, 30),
                LocalTime.of(21, 0), true));
        questions = new ArrayList<>();
        screen = new IU_ConfigureParameters(service, REQUESTER, question -> {
            questions.add(question);
            return true;
        }, () -> { });
        tab = Screen.tab(screen, "Schedules");
        table = Screen.table(tab);
    }

    private void fillNew(String resource, String start, String end) {
        Screen.button(tab, "New").doClick();
        Screen.field(tab, RESOURCE).setText(resource);
        Screen.check(tab, "Monday").setSelected(true);
        Screen.check(tab, "Friday").setSelected(true);
        Screen.field(tab, START).setText(start);
        Screen.field(tab, END).setText(end);
    }

    @Test
    @DisplayName("TC17C-19 A room open on Monday and Friday from 08:00 to 15:00 is sent and listed")
    void scheduleIsSentAndListed() {
        // Arrange
        fillNew("Room CT-1", "08:00", " 15:00 ");

        // Act
        Screen.button(tab, "Save").doClick();

        // Assert
        Schedule sent = assertInstanceOf(Schedule.class, service.lastSaved());
        assertAll(
                () -> assertEquals("Room CT-1", sent.getResource()),
                () -> assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), sent.getDaysOfWeek()),
                () -> assertEquals(LocalTime.of(8, 0), sent.getStartTime()),
                () -> assertEquals(LocalTime.of(15, 0), sent.getEndTime()),
                () -> assertTrue(sent.isActive()),
                () -> assertEquals("✔ Schedule of Room CT-1 saved.", Screen.status(screen)),
                () -> assertEquals("Room CT-1", table.getValueAt(table.getSelectedRow(), 0)),
                () -> assertEquals("08:00 - 15:00", table.getValueAt(table.getSelectedRow(), 1)),
                () -> assertEquals("active", table.getValueAt(table.getSelectedRow(), 2)));
    }

    @Test
    @DisplayName("TC17C-19 Selecting a schedule shows its room, days and times")
    void selectingAScheduleShowsItsData() {
        // Act
        table.setRowSelectionInterval(0, 0);

        // Assert
        assertAll(
                () -> assertEquals("09:30 - 21:00", table.getValueAt(0, 1)),
                () -> assertEquals("Room MR-1", Screen.field(tab, RESOURCE).getText()),
                () -> assertTrue(Screen.check(tab, "Tuesday").isSelected()),
                () -> assertTrue(Screen.check(tab, "Thursday").isSelected()),
                () -> assertFalse(Screen.check(tab, "Monday").isSelected()),
                () -> assertEquals("09:30", Screen.field(tab, START).getText()),
                () -> assertEquals("21:00", Screen.field(tab, END).getText()));
    }

    @ParameterizedTest(name = "start \"{0}\", end \"{1}\"")
    @CsvSource({"8, 15:00", "25:00, 15:00", "08.30, 15:00", "08:00, 8", "08:00, 25:00", "08:00, 15.00", "24:00, 15:00", "08:00, 24:00"})
    @DisplayName("TC17C-20 Times that are not HH:mm are rejected without calling the server")
    void timesThatAreNotHhMmAreRejected(String start, String end) {
        // Arrange
        fillNew("Room CT-1", start, end);

        // Act
        Screen.button(tab, "Save").doClick();

        // Assert
        assertAll(
                () -> assertEquals("⚠ The times must be written as HH:mm, for example 08:30.", Screen.status(screen)),
                () -> assertNull(service.lastRequest()),
                () -> assertEquals(1, table.getRowCount()));
    }

    @Test
    @DisplayName("TC17C-21 Empty times are sent as no time and the error of the server is shown")
    void emptyTimesAreSentAsNoTime() {
        // Arrange
        fillNew("Room CT-1", "", "  ");
        service.rejectNextChange("The start time is required", "The end time is required");

        // Act
        Screen.button(tab, "Save").doClick();

        // Assert
        Schedule sent = assertInstanceOf(Schedule.class, service.lastRequest());
        assertAll(
                () -> assertNull(sent.getStartTime()),
                () -> assertNull(sent.getEndTime()),
                () -> assertEquals("⚠ The start time is required. The end time is required.", Screen.status(screen)),
                () -> assertEquals(0, service.changes()),
                () -> assertEquals(1, table.getRowCount()));
    }

    @Test
    @DisplayName("TC17C-22 New with a room that already has a schedule is rejected")
    void newWithARoomThatAlreadyHasAScheduleIsRejected() {
        // Arrange
        fillNew("Room MR-1", "08:00", "15:00");

        // Act
        Screen.button(tab, "Save").doClick();

        // Assert
        assertAll(
                () -> assertEquals("⚠ A schedule for Room MR-1 already exists. Select it in the table to edit it.",
                        Screen.status(screen)),
                () -> assertNull(service.lastRequest()),
                () -> assertEquals(0, service.changes()));
    }

    @Test
    @DisplayName("TC17C-23 Deactivating a schedule answered yes leaves the row inactive")
    void deactivatingASchedule() {
        // Arrange
        table.setRowSelectionInterval(0, 0);

        // Act
        Screen.button(tab, "Deactivate").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Deactivate the schedule of Room MR-1?"), questions),
                () -> assertEquals("Room MR-1", service.lastDeactivated()),
                () -> assertEquals("✔ Schedule of Room MR-1 deactivated.", Screen.status(screen)),
                () -> assertEquals(0, table.getSelectedRow()),
                () -> assertEquals("inactive", table.getValueAt(0, 2)),
                () -> assertFalse(Screen.field(tab, START).isEnabled()),
                () -> assertFalse(Screen.check(tab, "Monday").isEnabled()),
                () -> assertFalse(Screen.enabled(tab, "Save")),
                () -> assertFalse(Screen.enabled(tab, "Deactivate")));
    }

    @Test
    @DisplayName("TC17C-23 When editing, the room cannot be changed and the update keeps it")
    void whenEditingTheRoomCannotBeChanged() {
        // Arrange
        table.setRowSelectionInterval(0, 0);
        Screen.check(tab, "Saturday").setSelected(true);
        Screen.field(tab, END).setText("20:00");

        // Act
        Screen.button(tab, "Save").doClick();

        // Assert
        Schedule sent = assertInstanceOf(Schedule.class, service.lastSaved());
        assertAll(
                () -> assertFalse(Screen.field(tab, RESOURCE).isEnabled()),
                () -> assertEquals("Room MR-1", sent.getResource()),
                () -> assertEquals(Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
                        sent.getDaysOfWeek()),
                () -> assertEquals(LocalTime.of(20, 0), sent.getEndTime()),
                () -> assertEquals(1, table.getRowCount()),
                () -> assertEquals("09:30 - 20:00", table.getValueAt(0, 1)),
                () -> assertEquals(0, table.getSelectedRow()));
    }

    @Test
    @DisplayName("TC17C-23 Deactivate with no row selected asks nothing")
    void deactivateWithoutSelectionAsksNothing() {
        // Act
        Screen.button(tab, "Deactivate").doClick();

        // Assert
        assertAll(
                () -> assertTrue(questions.isEmpty()),
                () -> assertNull(service.lastDeactivated()));
    }

    @Test
    @DisplayName("TC17C-26 reload() reads the schedules again and keeps the selected one")
    void reloadKeepsTheSelectedSchedule() {
        // Arrange
        table.setRowSelectionInterval(0, 0);
        service.add(new Schedule("Room XR-1", Set.of(DayOfWeek.MONDAY), LocalTime.of(8, 0), LocalTime.of(14, 0),
                true));

        // Act
        ((SchedulePanel) tab).reload();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Room MR-1", "Room XR-1"), Screen.column(table, 0)),
                () -> assertEquals(0, table.getSelectedRow()));
    }
}
