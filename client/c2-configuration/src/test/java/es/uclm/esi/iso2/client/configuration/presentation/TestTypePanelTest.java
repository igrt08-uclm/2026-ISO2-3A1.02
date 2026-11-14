package es.uclm.esi.iso2.client.configuration.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of the tab <em>Test types</em> of UC17 ({@link TestTypePanel}), used through the whole screen
 * {@link IU_ConfigureParameters}, as the user does. The screen is never shown, so no display is needed.
 */
@DisplayName("TestTypePanel")
class TestTypePanelTest {
    private static final String REQUESTER = "admin";

    private FakeSystemParameterService service;
    private List<String> questions;
    private boolean answer;
    private IU_ConfigureParameters screen;
    private Container tab;
    private JTable table;

    @BeforeEach
    void setUp() {
        service = new FakeSystemParameterService();
        service.add(new Priority("HIGH", "High", "Urgent tests", true));
        service.add(new Priority("OLD", "Old normal", "No longer used", false));
        service.add(new TestType("CT-HEAD", "Head CT", "CT", "Room CT-1", 20, "Fasting 4 h", true, false,
                "Pregnancy", "HIGH", false, true));
        service.add(new TestType("MR-KNEE", "Knee MRI", "MR", "Room MR-1", 30, "None", false, true, "Metal", "OLD",
                true, true));
        service.add(new TestType("XR-HAND", "Hand X-ray", "XR", "Room XR-1", 10, "None", false, false, "None", "HIGH",
                false, false));
        questions = new ArrayList<>();
        answer = true;
    }

    private void open() {
        screen = new IU_ConfigureParameters(service, REQUESTER, question -> {
            questions.add(question);
            return answer;
        }, () -> { });
        tab = Screen.tab(screen, "Test types");
        table = Screen.table(tab);
    }

    private void select(String id) {
        int row = Screen.column(table, 0).indexOf(id);
        table.setRowSelectionInterval(row, row);
    }

    private JComboBox<Priority> priority() {
        return Screen.combo(tab, "Priority");
    }

    private List<String> priorityIds() {
        List<String> ids = new ArrayList<>();
        for (int index = 0; index < priority().getItemCount(); index++) {
            ids.add(priority().getItemAt(index).getId());
        }
        return ids;
    }

    private String shownName(Object value) {
        JLabel cell = (JLabel) priority().getRenderer().getListCellRendererComponent(new JList<>(), (Priority) value, 0, false,
                false);
        return cell.getText();
    }

    private void fillNew(String id, String duration) {
        Screen.button(tab, "New").doClick();
        Screen.field(tab, "Id").setText(id);
        Screen.field(tab, "Name").setText("Chest CT");
        Screen.field(tab, "Modality").setText("CT");
        Screen.field(tab, "Room").setText("Room CT-2");
        Screen.field(tab, "Duration (minutes)").setText(duration);
        Screen.field(tab, "Preparation").setText("Fasting 2 h");
        Screen.field(tab, "Safety information").setText("Allergies");
        Screen.check(tab, "Requires contrast").setSelected(true);
        Screen.check(tab, "Requires consent").setSelected(true);
        Screen.check(tab, "Eligible for follow-up").setSelected(true);
        priority().setSelectedIndex(0);
    }

    @Test
    @DisplayName("TC17C-05 Selecting a row shows its data, the priority by its name, and the id cannot be edited")
    void selectingARowShowsItsData() {
        // Arrange
        open();

        // Act
        select("CT-HEAD");

        // Assert
        assertAll(
                () -> assertEquals("CT-HEAD", Screen.field(tab, "Id").getText()),
                () -> assertFalse(Screen.field(tab, "Id").isEnabled()),
                () -> assertEquals("Head CT", Screen.field(tab, "Name").getText()),
                () -> assertTrue(Screen.field(tab, "Name").isEnabled()),
                () -> assertEquals("CT", Screen.field(tab, "Modality").getText()),
                () -> assertEquals("Room CT-1", Screen.field(tab, "Room").getText()),
                () -> assertEquals("20", Screen.field(tab, "Duration (minutes)").getText()),
                () -> assertEquals("Fasting 4 h", Screen.field(tab, "Preparation").getText()),
                () -> assertEquals("Pregnancy", Screen.field(tab, "Safety information").getText()),
                () -> assertTrue(Screen.check(tab, "Requires contrast").isSelected()),
                () -> assertFalse(Screen.check(tab, "Requires consent").isSelected()),
                () -> assertFalse(Screen.check(tab, "Eligible for follow-up").isSelected()),
                () -> assertEquals("HIGH", ((Priority) priority().getSelectedItem()).getId()),
                () -> assertEquals("High", shownName(priority().getSelectedItem())),
                () -> assertEquals(" ", shownName(null)));
    }

    @Test
    @DisplayName("TC17C-05 With no row selected the form is empty and the id can be written")
    void withoutSelectionTheFormIsEmpty() {
        // Arrange
        open();
        select("CT-HEAD");

        // Act
        Screen.button(tab, "New").doClick();

        // Assert
        assertAll(
                () -> assertEquals(-1, table.getSelectedRow()),
                () -> assertEquals("", Screen.field(tab, "Id").getText()),
                () -> assertTrue(Screen.field(tab, "Id").isEnabled()),
                () -> assertEquals("", Screen.field(tab, "Duration (minutes)").getText()),
                () -> assertNull(priority().getSelectedItem()),
                () -> assertTrue(Screen.enabled(tab, "Save")),
                () -> assertFalse(Screen.enabled(tab, "Deactivate")));
    }

    @Test
    @DisplayName("TC17C-06 New, fill the form and Save sends the test type and selects the new row")
    void newTestTypeIsSentAndSelected() {
        // Arrange
        open();
        fillNew("CT-CHEST", " 25 ");

        // Act
        Screen.button(tab, "Save").doClick();

        // Assert
        TestType sent = assertInstanceOf(TestType.class, service.lastSaved());
        assertAll(
                () -> assertEquals("createTestType", service.lastCall()),
                () -> assertEquals("CT-CHEST", sent.getId()),
                () -> assertEquals("Chest CT", sent.getName()),
                () -> assertEquals("CT", sent.getModality()),
                () -> assertEquals("Room CT-2", sent.getRoom()),
                () -> assertEquals(25, sent.getDurationMinutes()),
                () -> assertEquals("Fasting 2 h", sent.getPreparation()),
                () -> assertEquals("Allergies", sent.getSafetyInfo()),
                () -> assertTrue(sent.isRequiresContrast()),
                () -> assertTrue(sent.isRequiresConsent()),
                () -> assertTrue(sent.isFollowUpEligible()),
                () -> assertEquals("HIGH", sent.getPriority()),
                () -> assertTrue(sent.isActive()),
                () -> assertEquals("✔ Test type CT-CHEST saved.", Screen.status(screen)),
                () -> assertEquals("CT-CHEST", table.getValueAt(table.getSelectedRow(), 0)),
                () -> assertEquals("active", table.getValueAt(table.getSelectedRow(), 2)));
    }

    @Test
    @DisplayName("TC17C-06 A new test type without priority is sent with an empty priority")
    void newTestTypeWithoutPriorityIsSentWithEmptyPriority() {
        // Arrange
        open();
        fillNew("CT-CHEST", "25");
        priority().setSelectedIndex(-1);

        // Act
        Screen.button(tab, "Save").doClick();

        // Assert
        TestType sent = assertInstanceOf(TestType.class, service.lastSaved());
        assertEquals("", sent.getPriority());
    }

    @ParameterizedTest(name = "duration \"{0}\"")
    @ValueSource(strings = {"abc", "20.5", "", "   "})
    @DisplayName("TC17C-07 A duration that is not a whole number is rejected without calling the server")
    void durationThatIsNotAWholeNumberIsRejected(String duration) {
        // Arrange
        open();
        fillNew("CT-CHEST", duration);

        // Act
        Screen.button(tab, "Save").doClick();

        // Assert
        assertAll(
                () -> assertEquals("⚠ The duration must be a whole number of minutes.", Screen.status(screen)),
                () -> assertNull(service.lastRequest()),
                () -> assertEquals(0, service.changes()));
    }

    @Test
    @DisplayName("TC17C-08 Two errors of the server are shown together, each ending with a full stop")
    void twoErrorsOfTheServerAreShownTogether() {
        // Arrange
        open();
        fillNew("CT-CHEST", "0");
        service.rejectNextChange("The name is required", "The duration must be greater than 0");

        // Act
        Screen.button(tab, "Save").doClick();

        // Assert
        assertAll(
                () -> assertEquals("⚠ The name is required. The duration must be greater than 0.",
                        Screen.status(screen)),
                () -> assertEquals(0, service.changes()),
                () -> assertEquals(3, table.getRowCount()));
    }

    @Test
    @DisplayName("TC17C-09 Editing a selected test type sends an update with the same id")
    void editingSendsAnUpdateWithTheSameId() {
        // Arrange
        open();
        select("CT-HEAD");
        Screen.field(tab, "Name").setText("Head CT without contrast");

        // Act
        Screen.button(tab, "Save").doClick();

        // Assert
        TestType sent = assertInstanceOf(TestType.class, service.lastSaved());
        assertAll(
                () -> assertEquals("updateTestType", service.lastCall()),
                () -> assertEquals("CT-HEAD", sent.getId()),
                () -> assertEquals("Head CT without contrast", sent.getName()),
                () -> assertEquals("HIGH", sent.getPriority()),
                () -> assertEquals(3, table.getRowCount()),
                () -> assertEquals("Head CT without contrast", table.getValueAt(table.getSelectedRow(), 1)),
                () -> assertEquals("✔ Test type CT-HEAD saved.", Screen.status(screen)));
    }

    @Test
    @DisplayName("TC17C-10 Deactivate answered yes deactivates it, the row says inactive and the buttons are disabled")
    void deactivateAnsweredYes() {
        // Arrange
        open();
        select("CT-HEAD");

        // Act
        Screen.button(tab, "Deactivate").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Deactivate the test type CT-HEAD?"), questions),
                () -> assertEquals("CT-HEAD", service.lastDeactivated()),
                () -> assertEquals("✔ Test type CT-HEAD deactivated.", Screen.status(screen)),
                () -> assertEquals("CT-HEAD", table.getValueAt(table.getSelectedRow(), 0)),
                () -> assertEquals("inactive", table.getValueAt(table.getSelectedRow(), 2)),
                () -> assertFalse(Screen.enabled(tab, "Save")),
                () -> assertFalse(Screen.enabled(tab, "Deactivate")),
                () -> assertTrue(Screen.enabled(tab, "New")));
    }

    @Test
    @DisplayName("TC17C-11 Deactivate answered no sends nothing to the server")
    void deactivateAnsweredNo() {
        // Arrange
        answer = false;
        open();
        select("CT-HEAD");

        // Act
        Screen.button(tab, "Deactivate").doClick();

        // Assert
        assertAll(
                () -> assertEquals(1, questions.size()),
                () -> assertNull(service.lastDeactivated()),
                () -> assertEquals(0, service.changes()),
                () -> assertEquals("active", table.getValueAt(table.getSelectedRow(), 2)),
                () -> assertEquals(" ", Screen.status(screen)));
    }

    @Test
    @DisplayName("TC17C-12 An inactive test type can be read, but Save and Deactivate are disabled")
    void inactiveTestTypeCanOnlyBeRead() {
        // Arrange
        open();

        // Act
        select("XR-HAND");

        // Assert
        assertAll(
                () -> assertEquals("Hand X-ray", Screen.field(tab, "Name").getText()),
                () -> assertFalse(Screen.field(tab, "Name").isEnabled()),
                () -> assertFalse(Screen.field(tab, "Duration (minutes)").isEnabled()),
                () -> assertFalse(priority().isEnabled()),
                () -> assertFalse(Screen.check(tab, "Requires contrast").isEnabled()),
                () -> assertFalse(Screen.enabled(tab, "Save")),
                () -> assertFalse(Screen.enabled(tab, "Deactivate")),
                () -> assertTrue(Screen.enabled(tab, "New")));
    }

    @Test
    @DisplayName("TC17C-13 The priorities offered are the active ones, plus the inactive one of the test type")
    void prioritiesOfferedAreTheActiveOnesPlusTheOneOfTheTestType() {
        // Arrange
        open();

        // Act
        select("CT-HEAD");
        List<String> forActive = priorityIds();
        select("MR-KNEE");
        List<String> forInactivePriority = priorityIds();
        Priority chosen = (Priority) priority().getSelectedItem();
        Screen.button(tab, "New").doClick();
        List<String> forNew = priorityIds();

        // Assert
        assertAll(
                () -> assertEquals(List.of("HIGH"), forActive),
                () -> assertEquals(List.of("HIGH", "OLD"), forInactivePriority),
                () -> assertEquals("OLD", chosen.getId()),
                () -> assertEquals("Old normal", shownName(chosen)),
                () -> assertEquals(List.of("HIGH"), forNew));
    }

    @Test
    @DisplayName("TC17C-13 A priority that the server no longer lists is still offered, by its id")
    void unknownPriorityIsOfferedByItsId() {
        // Arrange
        service.add(new TestType("US-ABD", "Abdominal ultrasound", "US", "Room US-1", 15, "None", false, false,
                "None", "GONE", false, true));
        open();

        // Act
        select("US-ABD");

        // Assert
        assertAll(
                () -> assertEquals(List.of("HIGH", "GONE"), priorityIds()),
                () -> assertEquals("GONE", shownName(priority().getSelectedItem())));
    }

    @Test
    @DisplayName("TC17C-14 A priority saved in its tab appears in the test-type form when going back")
    void prioritySavedInItsTabAppearsInTheTestTypeForm() {
        // Arrange
        open();
        select("CT-HEAD");
        JTabbedPane tabs = Screen.tabs(screen);
        tabs.setSelectedIndex(tabs.indexOfTab("Priorities"));
        Container priorities = Screen.tab(screen, "Priorities");
        Screen.button(priorities, "New").doClick();
        Screen.field(priorities, "Id").setText("LOW");
        Screen.field(priorities, "Name").setText("Low");
        Screen.field(priorities, "Description").setText("Routine tests");
        Screen.button(priorities, "Save").doClick();

        // Act
        tabs.setSelectedIndex(tabs.indexOfTab("Test types"));

        // Assert
        assertAll(
                () -> assertEquals("✔ Priority LOW saved.", Screen.status(screen)),
                () -> assertEquals(List.of("HIGH", "LOW"), priorityIds()),
                () -> assertEquals("HIGH", ((Priority) priority().getSelectedItem()).getId()));
    }

    @Test
    @DisplayName("TC17C-14 Going back to the test types with nothing chosen keeps the priority empty")
    void goingBackWithNothingChosenKeepsThePriorityEmpty() {
        // Arrange
        open();
        JTabbedPane tabs = Screen.tabs(screen);
        tabs.setSelectedIndex(tabs.indexOfTab("Protocols"));
        service.add(new Priority("LOW", "Low", "Routine tests", true));

        // Act
        tabs.setSelectedIndex(tabs.indexOfTab("Test types"));

        // Assert
        assertAll(
                () -> assertEquals(List.of("HIGH", "LOW"), priorityIds()),
                () -> assertNull(priority().getSelectedItem()));
    }

    @Test
    @DisplayName("TC17C-26 reload() reads the test types again and keeps the selected one")
    void reloadKeepsTheSelectedTestType() {
        // Arrange
        TestTypePanel panel = new TestTypePanel(service, REQUESTER, question -> true, message -> { },
                message -> { }, () -> { });
        JTable panelTable = Screen.table(panel);
        panelTable.setRowSelectionInterval(1, 1);
        service.add(new TestType("AA-FIRST", "First", "XR", "Room XR-1", 5, "", false, false, "", "HIGH", false,
                true));

        // Act
        panel.reload();

        // Assert
        assertAll(
                () -> assertEquals(4, panelTable.getRowCount()),
                () -> assertEquals("MR-KNEE", panelTable.getValueAt(panelTable.getSelectedRow(), 0)),
                () -> assertEquals("MR-KNEE", Screen.field(panel, "Id").getText()));
    }
}
