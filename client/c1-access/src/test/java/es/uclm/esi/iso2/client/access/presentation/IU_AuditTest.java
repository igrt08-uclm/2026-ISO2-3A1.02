package es.uclm.esi.iso2.client.access.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import es.uclm.esi.iso2.common.access.domain.AuditLog;
import java.awt.Component;
import java.awt.Container;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests of {@link IU_Audit}. The controls are private, so they are found through the public {@code getComponents()}
 * method, as in IU_WelcomeTest. The panel is never shown, so no display is needed.
 */
@DisplayName("IU_Audit")
class IU_AuditTest {
    private static final int USER_FIELD = 0;
    private static final int TARGET_FIELD = 1;
    private static final int FROM_FIELD = 2;
    private static final int TO_FIELD = 3;

    private AuditLog login;
    private AuditLog failedLogin;
    private FakeAuditService service;

    @BeforeEach
    void setUp() {
        login = new AuditLog(2L, new Date(), "admin", "LOGIN_OK", "authentication");
        failedLogin = new AuditLog(1L, new Date(), "nobody", "LOGIN_FAILED", "authentication:UNKNOWN_USER");
        service = new FakeAuditService(login, failedLogin);
    }

    private IU_Audit openScreen() {
        return new IU_Audit(service, "admin", () -> { });
    }

    @Test
    @DisplayName("Opening the screen shows every entry for the logged user")
    void openingTheScreenShowsEveryEntry() {
        // Act
        IU_Audit audit = openScreen();

        // Assert
        assertAll(
                () -> assertEquals("admin", service.lastRequester()),
                () -> assertEquals("", service.lastFilters()),
                () -> assertEquals(2, table(audit).getRowCount()));
    }

    @Test
    @DisplayName("Each row shows the date, the user, the action and the resource")
    void eachRowShowsItsData() {
        // Arrange
        String expectedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(login.getTimestamp());

        // Act
        JTable table = table(openScreen());

        // Assert
        assertAll(
                () -> assertEquals(expectedDate, table.getValueAt(0, 0)),
                () -> assertEquals("admin", table.getValueAt(0, 1)),
                () -> assertEquals("LOGIN_OK", table.getValueAt(0, 2)),
                () -> assertEquals("authentication", table.getValueAt(0, 3)));
    }

    @Test
    @DisplayName("The status shows how many entries were found")
    void statusShowsHowManyEntriesWereFound() {
        // Arrange
        IU_Audit audit = openScreen();
        String several = status(audit);

        // Act
        audit.displayActivity(List.of(login));
        String one = status(audit);
        audit.displayActivity(List.of());
        String none = status(audit);

        // Assert
        assertAll(
                () -> assertEquals("2 entries", several),
                () -> assertEquals("1 entry", one),
                () -> assertEquals("No activity matches these filters.", none));
    }

    @ParameterizedTest(name = "field {0} = {1} sends {2}")
    @CsvSource({
        "0, admin,      author=admin",
        "1, PAT-1,      target=PAT-1",
        "2, 2026-11-01, from=2026-11-01",
        "3, 2026-11-30, to=2026-11-30"
    })
    @DisplayName("Each text filter sends its pair")
    void eachTextFilterSendsItsPair(int field, String value, String expectedFilters) {
        // Arrange
        IU_Audit audit = openScreen();
        textFields(audit).get(field).setText(value);

        // Act
        button(audit, "Search").doClick();

        // Assert
        assertEquals(expectedFilters, service.lastFilters());
    }

    @Test
    @DisplayName("The selected action is sent")
    void selectedActionIsSent() {
        // Arrange
        IU_Audit audit = openScreen();
        actionList(audit).setSelectedItem("LOGIN_FAILED");

        // Act
        button(audit, "Search").doClick();

        // Assert
        assertEquals("action=LOGIN_FAILED", service.lastFilters());
    }

    @Test
    @DisplayName("Every filter is sent, separated by ;")
    void everyFilterIsSent() {
        // Arrange
        IU_Audit audit = openScreen();
        textFields(audit).get(USER_FIELD).setText("admin");
        actionList(audit).setSelectedItem("LOGIN_OK");
        textFields(audit).get(TARGET_FIELD).setText("PAT-1");
        textFields(audit).get(FROM_FIELD).setText("2026-11-01");
        textFields(audit).get(TO_FIELD).setText("2026-11-30");

        // Act
        button(audit, "Search").doClick();

        // Assert
        assertEquals("author=admin;action=LOGIN_OK;target=PAT-1;from=2026-11-01;to=2026-11-30",
                service.lastFilters());
    }

    @Test
    @DisplayName("Values are trimmed, and empty fields and All actions are not sent")
    void valuesAreTrimmedAndEmptyOnesAreNotSent() {
        // Arrange
        IU_Audit audit = openScreen();
        textFields(audit).get(USER_FIELD).setText("  admin  ");
        textFields(audit).get(TARGET_FIELD).setText("   ");

        // Act
        button(audit, "Search").doClick();

        // Assert
        assertEquals("author=admin", service.lastFilters());
    }

    @ParameterizedTest(name = "field {0}")
    @CsvSource({"0", "1", "2", "3"})
    @DisplayName("Enter in a text field searches")
    void enterInATextFieldSearches(int field) {
        // Arrange
        IU_Audit audit = openScreen();
        JTextField textField = textFields(audit).get(field);
        textField.setText("2026-11-01");

        // Act
        textField.postActionEvent();

        // Assert
        assertEquals(2, service.receivedFilters().size());
    }

    // The server cannot tell a ; in a value from the one that separates the filters.
    @Test
    @DisplayName("Typed or pasted ; do not appear in the text fields")
    void separatorDoesNotAppearInTheTextFields() throws BadLocationException {
        // Arrange
        List<JTextField> fields = textFields(openScreen());

        // Act
        for (JTextField field : fields) {
            field.setText("ad;min");
            field.getDocument().insertString(0, ";x;", null);
        }

        // Assert
        for (JTextField field : fields) {
            assertEquals("xadmin", field.getText());
        }
    }

    @Test
    @DisplayName("Clear empties the filters and shows everything")
    void clearEmptiesTheFiltersAndShowsEverything() {
        // Arrange
        IU_Audit audit = openScreen();
        textFields(audit).forEach(field -> field.setText("x"));
        actionList(audit).setSelectedItem("LOGIN_OK");

        // Act
        button(audit, "Clear").doClick();

        // Assert
        assertAll(
                () -> textFields(audit).forEach(field -> assertEquals("", field.getText())),
                () -> assertEquals("All actions", actionList(audit).getSelectedItem()),
                () -> assertEquals("", service.lastFilters()));
    }

    @Test
    @DisplayName("A wrong date shows a message and keeps the table")
    void wrongDateShowsAMessageAndKeepsTheTable() {
        // Arrange
        IU_Audit audit = openScreen();
        service.rejectFilters(true);
        textFields(audit).get(FROM_FIELD).setText("yesterday");

        // Act
        button(audit, "Search").doClick();

        // Assert
        assertAll(
                () -> assertEquals("⚠ Check the dates: use the format yyyy-MM-dd.", status(audit)),
                () -> assertEquals(2, table(audit).getRowCount()));
    }

    @Test
    @DisplayName("A correct search after an error removes the message")
    void correctSearchAfterAnErrorRemovesTheMessage() {
        // Arrange
        IU_Audit audit = openScreen();
        service.rejectFilters(true);
        textFields(audit).get(FROM_FIELD).setText("yesterday");
        button(audit, "Search").doClick();

        // Act
        service.rejectFilters(false);
        textFields(audit).get(FROM_FIELD).setText("2026-11-01");
        button(audit, "Search").doClick();

        // Assert
        assertEquals("2 entries", status(audit));
    }

    @Test
    @DisplayName("Access denied shows a message and empties the table")
    void accessDeniedShowsAMessageAndEmptiesTheTable() {
        // Arrange
        IU_Audit audit = openScreen();
        service.denyAccess();

        // Act
        button(audit, "Search").doClick();

        // Assert
        assertAll(
                () -> assertEquals("⚠ You do not have permission to consult the audit log.", status(audit)),
                () -> assertEquals(0, table(audit).getRowCount()));
    }

    // D11: the audit log cannot be modified, not even in the screen.
    @Test
    @DisplayName("The cells cannot be edited")
    void cellsCannotBeEdited() {
        // Act
        JTable table = table(openScreen());

        // Assert
        assertFalse(table.isCellEditable(0, 0));
    }

    @Test
    @DisplayName("The Back button runs onBack")
    void backButtonRunsOnBack() {
        // Arrange
        int[] backs = {0};
        IU_Audit audit = new IU_Audit(service, "admin", () -> backs[0]++);

        // Act
        button(audit, "Back").doClick();

        // Assert
        assertEquals(1, backs[0]);
    }

    private static List<Component> allComponents(Container container) {
        List<Component> components = new ArrayList<>();
        for (Component component : container.getComponents()) {
            components.add(component);
            if (component instanceof Container inner) {
                components.addAll(allComponents(inner));
            }
        }
        return components;
    }

    private static JTable table(IU_Audit audit) {
        return allComponents(audit).stream().filter(JTable.class::isInstance).map(JTable.class::cast).findFirst()
                .orElseThrow();
    }

    /** The text fields, in the order of the screen: user, patient or resource, from and to. */
    private static List<JTextField> textFields(IU_Audit audit) {
        return allComponents(audit).stream().filter(JTextField.class::isInstance).map(JTextField.class::cast).toList();
    }

    @SuppressWarnings("unchecked")
    private static JComboBox<String> actionList(IU_Audit audit) {
        return allComponents(audit).stream().filter(JComboBox.class::isInstance).map(JComboBox.class::cast).findFirst()
                .orElseThrow();
    }

    private static JButton button(IU_Audit audit, String text) {
        return allComponents(audit).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    /** The status is the last label of the screen. */
    private static String status(IU_Audit audit) {
        List<JLabel> labels = allComponents(audit).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                .toList();
        return labels.get(labels.size() - 1).getText();
    }
}
