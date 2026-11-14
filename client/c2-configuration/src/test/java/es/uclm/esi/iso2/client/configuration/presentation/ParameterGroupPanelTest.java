package es.uclm.esi.iso2.client.configuration.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.configuration.domain.AppointmentPolicy;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.Protocol;
import java.awt.Color;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the generic tab {@link ParameterGroupPanel} of UC17, used for the protocols, the appointment policies and
 * the priorities. The tabs are used through the whole screen {@link IU_ConfigureParameters}, which is never shown, so
 * no display is needed.
 */
@DisplayName("ParameterGroupPanel")
class ParameterGroupPanelTest {
    private static final String REQUESTER = "admin";

    private FakeSystemParameterService service;
    private List<String> questions;
    private boolean answer;
    private IU_ConfigureParameters screen;

    @BeforeEach
    void setUp() {
        service = new FakeSystemParameterService();
        service.add(new Protocol("P1", "Stroke", "Stroke protocol", true));
        questions = new ArrayList<>();
        answer = true;
    }

    private void open() {
        screen = new IU_ConfigureParameters(service, REQUESTER, question -> {
            questions.add(question);
            return answer;
        }, () -> { });
    }

    private Container create(String tabTitle, String id, String name, String description) {
        Container tab = Screen.tab(screen, tabTitle);
        Screen.button(tab, "New").doClick();
        Screen.field(tab, "Id").setText(id);
        Screen.field(tab, "Name").setText(name);
        Screen.field(tab, "Description").setText(description);
        Screen.button(tab, "Save").doClick();
        return tab;
    }

    @Test
    @DisplayName("TC17C-15 Creating a protocol sends it and shows its message")
    void creatingAProtocol() {
        // Arrange
        service = new FakeSystemParameterService();
        open();

        // Act
        Container tab = create("Protocols", " P1 ", " Stroke ", " Stroke protocol ");

        // Assert
        Protocol sent = assertInstanceOf(Protocol.class, service.lastSaved());
        JTable table = Screen.table(tab);
        assertAll(
                () -> assertEquals("✔ Protocol P1 saved.", Screen.status(screen)),
                () -> assertEquals(new Color(0x1B5E20), Screen.statusLabel(screen).getForeground()),
                () -> assertEquals("P1", sent.getId()),
                () -> assertEquals("Stroke", sent.getName()),
                () -> assertEquals("Stroke protocol", sent.getDescription()),
                () -> assertTrue(sent.isActive()),
                () -> assertEquals("P1", table.getValueAt(table.getSelectedRow(), 0)),
                () -> assertEquals("active", table.getValueAt(table.getSelectedRow(), 2)));
    }

    @Test
    @DisplayName("TC17C-15 Creating an appointment policy sends it and shows its message")
    void creatingAnAppointmentPolicy() {
        // Arrange
        open();

        // Act
        create("Appointment policies", "A1", "Default", "Default policy");

        // Assert
        AppointmentPolicy sent = assertInstanceOf(AppointmentPolicy.class, service.lastSaved());
        assertAll(
                () -> assertEquals("✔ Appointment policy A1 saved.", Screen.status(screen)),
                () -> assertEquals("A1", sent.getId()),
                () -> assertEquals("Default", sent.getName()),
                () -> assertEquals("Default policy", sent.getDescription()),
                () -> assertTrue(sent.isActive()));
    }

    @Test
    @DisplayName("TC17C-15 Creating a priority sends it and shows its message")
    void creatingAPriority() {
        // Arrange
        open();

        // Act
        create("Priorities", "LOW", "Low", "Routine tests");

        // Assert
        Priority sent = assertInstanceOf(Priority.class, service.lastSaved());
        assertAll(
                () -> assertEquals("✔ Priority LOW saved.", Screen.status(screen)),
                () -> assertEquals("LOW", sent.getId()),
                () -> assertEquals("Low", sent.getName()),
                () -> assertEquals("Routine tests", sent.getDescription()),
                () -> assertTrue(sent.isActive()));
    }

    @Test
    @DisplayName("TC17C-15 Editing a selected protocol sends it with the same id")
    void editingAProtocolKeepsItsId() {
        // Arrange
        open();
        Container tab = Screen.tab(screen, "Protocols");
        JTable table = Screen.table(tab);
        table.setRowSelectionInterval(0, 0);
        Screen.field(tab, "Description").setText("Acute stroke protocol");

        // Act
        Screen.button(tab, "Save").doClick();

        // Assert
        Protocol sent = assertInstanceOf(Protocol.class, service.lastSaved());
        assertAll(
                () -> assertFalse(Screen.field(tab, "Id").isEnabled()),
                () -> assertEquals("P1", sent.getId()),
                () -> assertEquals("Acute stroke protocol", sent.getDescription()),
                () -> assertEquals(1, table.getRowCount()),
                () -> assertEquals(0, table.getSelectedRow()),
                () -> assertEquals("Acute stroke protocol", Screen.field(tab, "Description").getText()),
                () -> assertEquals("✔ Protocol P1 saved.", Screen.status(screen)));
    }

    @Test
    @DisplayName("TC17C-16 New with an id that is already in the table is rejected without calling the server")
    void newWithAnExistingIdIsRejected() {
        // Arrange
        open();

        // Act
        Container tab = create("Protocols", "P1", "Another", "Duplicated");

        // Assert
        assertAll(
                () -> assertEquals("⚠ A protocol with the id P1 already exists. Select it in the table to edit it.",
                        Screen.status(screen)),
                () -> assertNull(service.lastRequest()),
                () -> assertEquals(0, service.changes()),
                () -> assertEquals(1, Screen.table(tab).getRowCount()));
    }

    @Test
    @DisplayName("TC17C-16 The message of a duplicated appointment policy starts with \"An\"")
    void duplicatedAppointmentPolicyUsesAn() {
        // Arrange
        service.add(new AppointmentPolicy("A1", "Cancellation notice", "24 hours", true));
        open();

        // Act
        create("Appointment policies", "A1", "Another", "Duplicated");

        // Assert
        assertEquals("\u26a0 An appointment policy with the id A1 already exists. Select it in the table to edit it.",
                Screen.status(screen));
    }

    @Test
    @DisplayName("TC17C-17 Deactivating a protocol answered yes leaves the row inactive and the buttons disabled")
    void deactivatingAProtocol() {
        // Arrange
        open();
        Container tab = Screen.tab(screen, "Protocols");
        JTable table = Screen.table(tab);
        table.setRowSelectionInterval(0, 0);

        // Act
        Screen.button(tab, "Deactivate").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Deactivate the protocol P1?"), questions),
                () -> assertEquals("P1", service.lastDeactivated()),
                () -> assertEquals("✔ Protocol P1 deactivated.", Screen.status(screen)),
                () -> assertEquals(0, table.getSelectedRow()),
                () -> assertEquals("inactive", table.getValueAt(0, 2)),
                () -> assertFalse(Screen.field(tab, "Name").isEnabled()),
                () -> assertFalse(Screen.field(tab, "Description").isEnabled()),
                () -> assertFalse(Screen.enabled(tab, "Save")),
                () -> assertFalse(Screen.enabled(tab, "Deactivate")),
                () -> assertTrue(Screen.enabled(tab, "New")));
    }

    @Test
    @DisplayName("TC17C-17 Deactivating a protocol answered no sends nothing to the server")
    void deactivatingAProtocolAnsweredNo() {
        // Arrange
        answer = false;
        open();
        Container tab = Screen.tab(screen, "Protocols");
        Screen.table(tab).setRowSelectionInterval(0, 0);

        // Act
        Screen.button(tab, "Deactivate").doClick();

        // Assert
        assertAll(
                () -> assertEquals(1, questions.size()),
                () -> assertNull(service.lastDeactivated()),
                () -> assertEquals("active", Screen.table(tab).getValueAt(0, 2)));
    }

    @Test
    @DisplayName("TC17C-17 Deactivate with no row selected asks nothing")
    void deactivateWithoutSelectionAsksNothing() {
        // Arrange
        open();
        Container tab = Screen.tab(screen, "Protocols");

        // Act
        Screen.button(tab, "Deactivate").doClick();

        // Assert
        assertAll(
                () -> assertFalse(Screen.enabled(tab, "Deactivate")),
                () -> assertTrue(questions.isEmpty()),
                () -> assertNull(service.lastDeactivated()));
    }

    @Test
    @DisplayName("TC17C-18 The errors of the server for a protocol are shown in red")
    void errorsOfTheServerAreShownInRed() {
        // Arrange
        open();
        service.rejectNextChange("The name is required", "The description is too long");

        // Act
        Container tab = create("Protocols", "P2", "", "Too long");

        // Assert
        assertAll(
                () -> assertEquals("⚠ The name is required. The description is too long.", Screen.status(screen)),
                () -> assertEquals(new Color(0xB00020), Screen.statusLabel(screen).getForeground()),
                () -> assertEquals(0, service.changes()),
                () -> assertEquals(1, Screen.table(tab).getRowCount()));
    }

    @Test
    @DisplayName("TC17C-26 reload() reads the entries again and keeps the selected one")
    void reloadKeepsTheSelectedEntry() {
        // Arrange
        open();
        Container tab = Screen.tab(screen, "Protocols");
        JTable table = Screen.table(tab);
        table.setRowSelectionInterval(0, 0);
        service.add(new Protocol("P2", "Trauma", "Trauma protocol", true));

        // Act
        ((ParameterGroupPanel<?>) tab).reload();

        // Assert
        assertAll(
                () -> assertEquals(List.of("P1", "P2"), Screen.column(table, 0)),
                () -> assertEquals(0, table.getSelectedRow()),
                () -> assertEquals("Stroke", Screen.field(tab, "Name").getText()));
    }
}
