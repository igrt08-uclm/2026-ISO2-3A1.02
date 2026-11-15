package es.uclm.esi.iso2.client.configuration.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.configuration.domain.AppointmentPolicy;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.Protocol;
import es.uclm.esi.iso2.common.configuration.domain.Schedule;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the whole screen {@link IU_ConfigureParameters} of UC17: its tabs, the button <em>Back</em>, the
 * permission (Q10) and how the inactive rows are drawn. The screen is only built, never shown, so the tests need no
 * display; the confirmation of a deactivation is the answer given by the test.
 */
@DisplayName("IU_ConfigureParameters")
class IU_ConfigureParametersTest {
    private static final String REQUESTER = "admin";
    private static final String DENIED = "⚠ You do not have permission to configure the system.";
    private static final List<String> TAB_TITLES =
            List.of("Test types", "Protocols", "Schedules", "Appointment policies", "Priorities");

    private FakeSystemParameterService service;

    @BeforeEach
    void setUp() {
        service = new FakeSystemParameterService();
        service.add(new Priority("HIGH", "High", "Urgent tests", true));
        service.add(new TestType("CT-HEAD", "Head CT", "CT", "Room CT-1", 20, "None", false, false, "None", "HIGH",
                false, true));
        service.add(new TestType("MR-KNEE", "Knee MRI", "MR", "Room MR-1", 30, "None", false, true, "Metal", "HIGH",
                true, false));
        service.add(new Protocol("P1", "Stroke", "Stroke protocol", true));
        service.add(new Schedule("Room CT-1", Set.of(DayOfWeek.MONDAY), LocalTime.of(8, 0), LocalTime.of(15, 0),
                true));
        service.add(new AppointmentPolicy("A1", "Default", "Default policy", true));
    }

    private IU_ConfigureParameters open(Runnable onBack) {
        return new IU_ConfigureParameters(service, REQUESTER, question -> true, onBack);
    }

    @Test
    @DisplayName("TC17C-01 Shows the five tabs in order and the test types with their state")
    void showsTheFiveTabsAndTheTestTypes() {
        // Arrange
        IU_ConfigureParameters screen = open(() -> { });

        // Act
        JTabbedPane tabs = Screen.tabs(screen);
        List<String> titles = new ArrayList<>();
        for (int index = 0; index < tabs.getTabCount(); index++) {
            titles.add(tabs.getTitleAt(index));
        }
        JTable testTypes = Screen.table(Screen.tab(screen, "Test types"));

        // Assert
        assertAll(
                () -> assertEquals(TAB_TITLES, titles),
                () -> assertEquals(List.of("CT-HEAD", "MR-KNEE"), Screen.column(testTypes, 0)),
                () -> assertEquals(List.of("Head CT", "Knee MRI"), Screen.column(testTypes, 1)),
                () -> assertEquals(List.of("active", "inactive"), Screen.column(testTypes, 2)),
                () -> assertEquals(" ", Screen.status(screen)));
    }

    @Test
    @DisplayName("TC17C-04 Back runs its action")
    void backRunsItsAction() {
        // Arrange
        int[] backs = {0};
        IU_ConfigureParameters screen = open(() -> backs[0]++);

        // Act
        Screen.button(screen, "Back").doClick();

        // Assert
        assertEquals(1, backs[0]);
    }

    @Test
    @DisplayName("TC17C-24 When the server denies at opening, only Back is enabled")
    void deniedAtOpeningDisablesEverythingButBack() {
        // Arrange
        service.denyAll();

        // Act
        IU_ConfigureParameters screen = open(() -> { });

        // Assert
        assertAll(
                () -> assertEquals(DENIED, Screen.status(screen)),
                () -> assertEquals(new Color(0xB00020), Screen.statusLabel(screen).getForeground()),
                () -> assertOnlyBackIsEnabled(screen));
    }

    @Test
    @DisplayName("TC17C-25 A permission revoked while the screen is open disables everything but Back on Save")
    void permissionRevokedWhileOpenDisablesEverythingOnSave() {
        // Arrange
        IU_ConfigureParameters screen = open(() -> { });
        Container testTypes = Screen.tab(screen, "Test types");
        Screen.table(testTypes).setRowSelectionInterval(0, 0);
        service.denyAll();

        // Act
        Screen.button(testTypes, "Save").doClick();

        // Assert
        assertAll(
                () -> assertEquals(DENIED, Screen.status(screen)),
                () -> assertEquals(0, service.changes()),
                () -> assertOnlyBackIsEnabled(screen));
    }

    @Test
    @DisplayName("TC17C-25 A permission revoked while open is also detected on Save in the other tabs")
    void permissionRevokedIsDetectedOnSaveInEveryTab() {
        for (String title : List.of("Protocols", "Schedules", "Appointment policies", "Priorities")) {
            // Arrange
            setUp();
            IU_ConfigureParameters screen = open(() -> { });
            Container tab = Screen.tab(screen, title);
            Screen.table(tab).setRowSelectionInterval(0, 0);
            service.denyAll();

            // Act
            Screen.button(tab, "Save").doClick();

            // Assert
            assertAll(title,
                    () -> assertEquals(DENIED, Screen.status(screen)),
                    () -> assertOnlyBackIsEnabled(screen));
        }
    }

    @Test
    @DisplayName("TC17C-25 A permission revoked while open is detected on Deactivate in every tab")
    void permissionRevokedIsDetectedOnDeactivate() {
        for (String title : List.of("Test types", "Protocols", "Schedules")) {
            // Arrange
            setUp();
            IU_ConfigureParameters screen = open(() -> { });
            Container tab = Screen.tab(screen, title);
            Screen.table(tab).setRowSelectionInterval(0, 0);
            service.denyAll();

            // Act
            Screen.button(tab, "Deactivate").doClick();

            // Assert
            assertAll(title,
                    () -> assertEquals(DENIED, Screen.status(screen)),
                    () -> assertNull(service.lastDeactivated()),
                    () -> assertOnlyBackIsEnabled(screen));
        }
    }

    @Test
    @DisplayName("TC17C-25 A permission revoked while open is detected when the test types tab is shown again")
    void permissionRevokedIsDetectedWhenTheTestTypesTabIsShown() {
        // Arrange
        IU_ConfigureParameters screen = open(() -> { });
        JTabbedPane tabs = Screen.tabs(screen);
        tabs.setSelectedIndex(TAB_TITLES.indexOf("Priorities"));
        service.denyAll();

        // Act
        tabs.setSelectedIndex(TAB_TITLES.indexOf("Test types"));

        // Assert
        assertAll(
                () -> assertEquals(DENIED, Screen.status(screen)),
                () -> assertOnlyBackIsEnabled(screen));
    }

    @Test
    @DisplayName("TC17C-26 A rejected deactivation shows the error and reads the table again, in every tab")
    void rejectedDeactivationShowsTheErrorAndReadsTheTableAgain() {
        for (String title : List.of("Test types", "Protocols", "Schedules")) {
            // Arrange
            setUp();
            IU_ConfigureParameters screen = open(() -> { });
            Container tab = Screen.tab(screen, title);
            JTable table = Screen.table(tab);
            int rowsBefore = table.getRowCount();
            table.setRowSelectionInterval(0, 0);
            service.rejectNextChange("The entry does not exist");
            // Meanwhile, another administrator adds an entry: it must appear when the table is read again.
            service.add(new TestType("XR-CHEST", "Chest X-ray", "XR", "Room XR-1", 10, "", false, false, "", "HIGH",
                    false, true));
            service.add(new Protocol("P2", "Trauma", "Trauma protocol", true));
            service.add(new Schedule("Room XR-1", Set.of(DayOfWeek.TUESDAY), LocalTime.of(9, 0),
                    LocalTime.of(14, 0), true));

            // Act
            Screen.button(tab, "Deactivate").doClick();

            // Assert
            assertAll(title,
                    () -> assertEquals("⚠ The entry does not exist.", Screen.status(screen)),
                    () -> assertEquals(new Color(0xB00020), Screen.statusLabel(screen).getForeground()),
                    () -> assertEquals(rowsBefore + 1, table.getRowCount()),
                    () -> assertEquals(-1, table.getSelectedRow()),
                    () -> assertNull(service.lastDeactivated()));
        }
    }

    @Test
    @DisplayName("TC17C-27 Inactive rows are drawn in grey and say inactive; active rows keep the normal colour")
    void inactiveRowsAreGreyAndSayInactive() {
        // Arrange
        IU_ConfigureParameters screen = open(() -> { });
        JTable table = Screen.table(Screen.tab(screen, "Test types"));

        // Act (the renderer reuses one component, so each colour is read at once)
        Color active = render(table, 0, 1, false).getForeground();
        Color inactive = render(table, 1, 1, false).getForeground();
        Color inactiveSelected = render(table, 1, 1, true).getForeground();

        // Assert
        assertAll(
                () -> assertEquals("inactive", table.getValueAt(1, 2)),
                () -> assertEquals(new Color(0x757575), inactive),
                () -> assertEquals(table.getForeground(), active),
                () -> assertEquals(table.getSelectionForeground(), inactiveSelected));
    }

    @Test
    @DisplayName("TC17C-27 Inactive rows are also grey in the generic and schedule tabs")
    void inactiveRowsAreGreyInTheOtherTabs() {
        // Arrange
        service.add(new Protocol("P0", "Old", "Old protocol", false));
        service.add(new Schedule("Room OLD", Set.of(DayOfWeek.SUNDAY), LocalTime.of(8, 0), LocalTime.of(9, 0),
                false));
        IU_ConfigureParameters screen = open(() -> { });

        for (String title : List.of("Protocols", "Schedules")) {
            JTable table = Screen.table(Screen.tab(screen, title));

            // Act
            Color inactive = render(table, 1, 0, false).getForeground();

            // Assert
            assertAll(title,
                    () -> assertEquals("inactive", table.getValueAt(1, 2)),
                    () -> assertEquals(new Color(0x757575), inactive));
        }
    }

    private static Component render(JTable table, int row, int column, boolean selected) {
        return table.getCellRenderer(row, column).getTableCellRendererComponent(table, table.getValueAt(row, column),
                selected, false, row, column);
    }

    private static void assertOnlyBackIsEnabled(IU_ConfigureParameters screen) {
        // The arrows of the scroll bars and of the combo box are buttons without text: they are not actions.
        List<String> enabled = new ArrayList<>();
        for (JButton button : Screen.all(screen, JButton.class)) {
            if (button.isEnabled() && !button.getText().isEmpty()) {
                enabled.add(button.getText());
            }
        }
        assertEquals(List.of("Back"), enabled);
        for (String title : TAB_TITLES) {
            Container tab = Screen.tab(screen, title);
            assertAll(title,
                    () -> assertFalse(Screen.enabled(tab, "New")),
                    () -> assertFalse(Screen.enabled(tab, "Save")),
                    () -> assertFalse(Screen.enabled(tab, "Deactivate")));
        }
        assertTrue(Screen.enabled(screen, "Back"));
    }
}
