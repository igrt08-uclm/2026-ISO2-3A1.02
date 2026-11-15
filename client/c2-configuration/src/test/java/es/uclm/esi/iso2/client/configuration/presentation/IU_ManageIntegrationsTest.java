package es.uclm.esi.iso2.client.configuration.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.configuration.domain.ConnectionTestResult;
import es.uclm.esi.iso2.common.configuration.domain.HospitalSystemType;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationConfiguration;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationMode;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationStatus;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of the screen {@link IU_ManageIntegrations} of UC20 (#42): the table of the four integrations, the form of
 * the selected one, the reading and validation of the form (Q1, Q8), the confirmations (Q3), <em>Save</em>,
 * <em>Test connection</em> and <em>Revert</em> (Q2, Q11, Q12), the question when another row is pressed (Q15) and the
 * denied access. The screen is only built, never shown, so the tests need no display; every confirmation is answered
 * by the test, which also records the question.
 */
@DisplayName("IU_ManageIntegrations")
class IU_ManageIntegrationsTest {
    private static final String REQUESTER = "admin";
    private static final String ENDPOINT = "Endpoint (required in real mode)";
    private static final String SETTINGS = "Settings (key=value, one per line)";
    private static final String SIMULATED_LINE = "SIMULATED: no real system is contacted";
    private static final String UNSAVED_LINE = "Unsaved changes: save or revert to test the connection";
    private static final String DENIED = "⚠ You do not have permission to configure the integrations.";
    private static final String SSO = "Single sign-on (SSO)";
    private static final String EHR = "Electronic health record (EHR)";
    private static final Color AMBER = new Color(0x8A5A00);
    private static final Color GREEN = new Color(0x1B5E20);
    private static final Color RED = new Color(0xB00020);
    private static final int SSO_ROW = 0;
    private static final int EHR_ROW = 1;
    private static final int ADMISSIONS_ROW = 2;
    private static final int RIS_PACS_ROW = 3;

    private FakeIntegrationManagementService service;
    private List<String> questions;
    private boolean answer;
    private int backs;

    @BeforeEach
    void setUp() {
        service = FakeIntegrationManagementService.withFourIntegrations();
        questions = new ArrayList<>();
        answer = true;
        backs = 0;
    }

    private IU_ManageIntegrations open() {
        return new IU_ManageIntegrations(service, REQUESTER, question -> {
            questions.add(question);
            return answer;
        }, () -> backs++);
    }

    private static void select(IU_ManageIntegrations screen, int row) {
        Screen.table(screen).setRowSelectionInterval(row, row);
    }

    private static JTextField endpoint(IU_ManageIntegrations screen) {
        return Screen.field(screen, ENDPOINT);
    }

    private static JTextArea settings(IU_ManageIntegrations screen) {
        return Screen.area(screen, SETTINGS);
    }

    private static void save(IU_ManageIntegrations screen) {
        Screen.button(screen, "Save").doClick();
    }

    private static void testConnection(IU_ManageIntegrations screen) {
        Screen.button(screen, "Test connection").doClick();
    }

    private static boolean shows(IU_ManageIntegrations screen, String text) {
        return Screen.label(screen, text).isPresent();
    }

    private IntegrationConfiguration lastRequest() {
        return service.requests().get(service.requests().size() - 1);
    }

    private static Map<String, String> ordered(String... keysAndValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int index = 0; index < keysAndValues.length; index += 2) {
            map.put(keysAndValues[index], keysAndValues[index + 1]);
        }
        return map;
    }

    @Nested
    @DisplayName("Display")
    class Display {
        @Test
        @DisplayName("TC20C-01 Lists the four integrations in the order of the server, with plain names, and selects the first")
        void listsTheFourIntegrations() {
            // Act
            IU_ManageIntegrations screen = open();
            JTable table = Screen.table(screen);

            // Assert
            assertAll(
                    () -> assertEquals(List.of("System", "Mode", "Enabled", "Status"),
                            List.of(table.getColumnName(0), table.getColumnName(1), table.getColumnName(2),
                                    table.getColumnName(3))),
                    () -> assertEquals(List.of(SSO, EHR, "Admissions", "RIS/PACS"), Screen.column(table, 0)),
                    () -> assertEquals(List.of("SIMULATED", "SIMULATED", "SIMULATED", "REAL"),
                            Screen.column(table, 1)),
                    () -> assertEquals(List.of("Yes", "No", "Yes", "Yes"), Screen.column(table, 2)),
                    () -> assertEquals(List.of("Active", "Inactive", "Active", "Active"), Screen.column(table, 3)),
                    () -> assertEquals(SSO_ROW, table.getSelectedRow()),
                    () -> assertFalse(table.isCellEditable(0, 0)),
                    () -> assertEquals(" ", Screen.status(screen)),
                    () -> assertEquals(1, service.reads()));
        }

        @Test
        @DisplayName("TC20C-01 The rows follow the order in which the server returns them, and Error is a status")
        void rowsFollowTheOrderOfTheServer() {
            // Arrange
            service = new FakeIntegrationManagementService();
            service.add(new IntegrationConfiguration(HospitalSystemType.RIS_PACS, "", Map.of(), true,
                    IntegrationMode.SIMULATED, IntegrationStatus.ERROR));
            service.add(FakeIntegrationManagementService.integration(HospitalSystemType.SSO, "", Map.of(), true,
                    IntegrationMode.SIMULATED));

            // Act
            JTable table = Screen.table(open());

            // Assert
            assertAll(
                    () -> assertEquals(List.of("RIS/PACS", SSO), Screen.column(table, 0)),
                    () -> assertEquals(List.of("Error", "Active"), Screen.column(table, 3)));
        }

        @Test
        @DisplayName("TC20C-01 With no integrations the table is empty and the form cannot be used")
        void noIntegrations() {
            // Arrange
            service = new FakeIntegrationManagementService();

            // Act
            IU_ManageIntegrations screen = open();

            // Assert
            assertAll(
                    () -> assertEquals(0, Screen.table(screen).getRowCount()),
                    () -> assertFalse(Screen.enabled(screen, "Save")),
                    () -> assertFalse(Screen.enabled(screen, "Test connection")),
                    () -> assertFalse(shows(screen, SIMULATED_LINE)));
        }

        @Test
        @DisplayName("TC20C-02 SIMULATED is bold and dark amber in the table; REAL and the other columns are plain")
        void simulatedIsBoldAndAmberInTheTable() {
            // Arrange
            JTable table = Screen.table(open());

            // Act
            Component simulated = table.prepareRenderer(table.getCellRenderer(EHR_ROW, 1), EHR_ROW, 1);
            Font simulatedFont = simulated.getFont();
            Color simulatedColour = simulated.getForeground();
            Component real = table.prepareRenderer(table.getCellRenderer(RIS_PACS_ROW, 1), RIS_PACS_ROW, 1);
            Font realFont = real.getFont();
            Color realColour = real.getForeground();
            Component name = table.prepareRenderer(table.getCellRenderer(EHR_ROW, 0), EHR_ROW, 0);
            Font nameFont = name.getFont();
            Component selected = table.prepareRenderer(table.getCellRenderer(SSO_ROW, 1), SSO_ROW, 1);

            // Assert
            assertAll(
                    () -> assertTrue(simulatedFont.isBold()),
                    () -> assertEquals(AMBER, simulatedColour),
                    () -> assertFalse(realFont.isBold()),
                    () -> assertEquals(table.getForeground(), realColour),
                    () -> assertFalse(nameFont.isBold()),
                    () -> assertTrue(selected.getFont().isBold()));
        }

        @Test
        @DisplayName("TC20C-03 The line SIMULATED above the form is shown only in simulated mode, bold and amber")
        void simulatedLineOnlyInSimulatedMode() {
            // Arrange
            IU_ManageIntegrations screen = open();
            JLabel line = Screen.label(screen, SIMULATED_LINE).orElseThrow();
            boolean onSso = shows(screen, SIMULATED_LINE);

            // Act
            select(screen, RIS_PACS_ROW);
            boolean onRisPacs = shows(screen, SIMULATED_LINE);
            Screen.radio(screen, "Simulated").doClick();
            boolean afterSimulated = shows(screen, SIMULATED_LINE);
            Screen.radio(screen, "Real").doClick();
            boolean afterReal = shows(screen, SIMULATED_LINE);

            // Assert
            assertAll(
                    () -> assertTrue(onSso),
                    () -> assertFalse(onRisPacs),
                    () -> assertTrue(afterSimulated),
                    () -> assertFalse(afterReal),
                    () -> assertTrue(line.getFont().isBold()),
                    () -> assertEquals(AMBER, line.getForeground()));
        }

        @Test
        @DisplayName("TC20C-04 Selecting a row fills the form with its endpoint, settings, enabled and mode")
        void selectingARowFillsTheForm() {
            // Arrange
            IU_ManageIntegrations screen = open();
            String ssoEndpoint = endpoint(screen).getText();
            String ssoSettings = settings(screen).getText();
            boolean ssoEnabled = Screen.check(screen, "Enabled").isSelected();
            boolean ssoSimulated = Screen.radio(screen, "Simulated").isSelected();

            // Act
            select(screen, ADMISSIONS_ROW);
            String admissionsEndpoint = endpoint(screen).getText();
            String admissionsSettings = settings(screen).getText();
            select(screen, EHR_ROW);
            boolean ehrEnabled = Screen.check(screen, "Enabled").isSelected();
            select(screen, RIS_PACS_ROW);

            // Assert
            assertAll(
                    () -> assertEquals("", ssoEndpoint),
                    () -> assertEquals("timeout=30", ssoSettings),
                    () -> assertTrue(ssoEnabled),
                    () -> assertTrue(ssoSimulated),
                    () -> assertEquals("http://admissions.hospital.test", admissionsEndpoint),
                    () -> assertEquals("timeout=15\nretries=2", admissionsSettings),
                    () -> assertFalse(ehrEnabled),
                    () -> assertEquals("https://pacs.hospital.test", endpoint(screen).getText()),
                    () -> assertTrue(Screen.radio(screen, "Real").isSelected()),
                    () -> assertFalse(Screen.radio(screen, "Simulated").isSelected()),
                    () -> assertTrue(Screen.enabled(screen, "Save")),
                    () -> assertTrue(Screen.enabled(screen, "Test connection")),
                    () -> assertFalse(Screen.enabled(screen, "Revert")),
                    () -> assertFalse(shows(screen, UNSAVED_LINE)),
                    () -> assertTrue(questions.isEmpty()));
        }

        @Test
        @DisplayName("TC20C-05 Back runs its action and sends nothing")
        void backRunsItsAction() {
            // Arrange
            IU_ManageIntegrations screen = open();

            // Act
            Screen.button(screen, "Back").doClick();

            // Assert
            assertAll(
                    () -> assertEquals(1, backs),
                    () -> assertTrue(service.requests().isEmpty()));
        }

        @Test
        @DisplayName("TC20C-06 The buttons and the labels of the fields have their keyboard shortcuts")
        void keyboardShortcuts() {
            // Act
            IU_ManageIntegrations screen = open();

            // Assert
            assertAll(
                    () -> assertEquals(KeyEvent.VK_S, Screen.button(screen, "Save").getMnemonic()),
                    () -> assertEquals(KeyEvent.VK_T, Screen.button(screen, "Test connection").getMnemonic()),
                    () -> assertEquals(KeyEvent.VK_R, Screen.button(screen, "Revert").getMnemonic()),
                    () -> assertEquals(KeyEvent.VK_B, Screen.button(screen, "Back").getMnemonic()),
                    () -> assertEquals(KeyEvent.VK_N, Screen.check(screen, "Enabled").getMnemonic()),
                    () -> assertEquals(KeyEvent.VK_E,
                            Screen.label(screen, ENDPOINT).orElseThrow().getDisplayedMnemonic()),
                    () -> assertEquals(KeyEvent.VK_G,
                            Screen.label(screen, SETTINGS).orElseThrow().getDisplayedMnemonic()),
                    () -> assertEquals(KeyEvent.VK_M, Screen.label(screen, "Mode").orElseThrow().getDisplayedMnemonic()),
                    () -> assertEquals(Screen.radio(screen, "Simulated"),
                            Screen.label(screen, "Mode").orElseThrow().getLabelFor()));
        }
    }

    @Nested
    @DisplayName("Reading of the settings")
    class ReadingOfTheSettings {
        @Test
        @DisplayName("TC20C-10 Blank lines are ignored, spaces removed, split at the first = and the order kept")
        void readsTheSettings() {
            // Arrange
            IU_ManageIntegrations screen = open();
            settings(screen).setText("  zeta = 1 \n\n   \nalpha=2\n url = http://x.test/?a=b \n");

            // Act
            save(screen);

            // Assert
            Map<String, String> sent = lastRequest().getSettings();
            assertAll(
                    () -> assertEquals(1, service.requests().size()),
                    () -> assertEquals(ordered("zeta", "1", "alpha", "2", "url", "http://x.test/?a=b"), sent),
                    () -> assertEquals(List.of("zeta", "alpha", "url"), new ArrayList<>(sent.keySet())),
                    () -> assertEquals("✔ Integration " + SSO + " saved.", Screen.status(screen)));
        }

        @Test
        @DisplayName("TC20C-11 A line without = is an error with its number, and nothing is sent")
        void lineWithoutEquals() {
            // Arrange
            IU_ManageIntegrations screen = open();
            settings(screen).setText("timeout=30\nretries 2");

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ Line 2: use key=value.", Screen.status(screen)),
                    () -> assertEquals(RED, Screen.statusLabel(screen).getForeground()),
                    () -> assertTrue(service.requests().isEmpty()));
        }

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {"=5", "  = 5", "timeout=", "timeout=   ", "="})
        @DisplayName("TC20C-12 An empty key or an empty value is an error, and nothing is sent")
        void emptyKeyOrValue(String line) {
            // Arrange
            IU_ManageIntegrations screen = open();
            settings(screen).setText(line);

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ Line 1: use key=value.", Screen.status(screen)),
                    () -> assertTrue(service.requests().isEmpty()));
        }

        @Test
        @DisplayName("TC20C-13 A repeated key is an error, even with different spaces, and nothing is sent")
        void repeatedKey() {
            // Arrange
            IU_ManageIntegrations screen = open();
            settings(screen).setText("timeout=30\n timeout = 40");

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The setting timeout is repeated.", Screen.status(screen)),
                    () -> assertTrue(service.requests().isEmpty()));
        }

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {"", "\n\n", "   "})
        @DisplayName("TC20C-14 Empty settings are sent as no settings")
        void emptySettings(String text) {
            // Arrange
            IU_ManageIntegrations screen = open();
            settings(screen).setText(text);

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(1, service.requests().size()),
                    () -> assertEquals(Map.of(), lastRequest().getSettings()));
        }
    }

    @Nested
    @DisplayName("Validation of the endpoint")
    class ValidationOfTheEndpoint {
        @Test
        @DisplayName("TC20C-15 An empty endpoint is accepted in simulated mode")
        void emptyEndpointInSimulatedMode() {
            // Arrange
            IU_ManageIntegrations screen = open();
            settings(screen).setText("timeout=60");

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(1, service.requests().size()),
                    () -> assertEquals("", lastRequest().getEndpoint()),
                    () -> assertEquals(IntegrationMode.SIMULATED, lastRequest().getMode()));
        }

        @Test
        @DisplayName("TC20C-16 An empty endpoint is an error in real mode, and nothing is asked or sent")
        void emptyEndpointInRealMode() {
            // Arrange
            IU_ManageIntegrations screen = open();
            Screen.radio(screen, "Real").doClick();

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The endpoint is required in real mode.", Screen.status(screen)),
                    () -> assertTrue(questions.isEmpty()),
                    () -> assertTrue(service.requests().isEmpty()));
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {"http://", "https://", "http://sso.hospital.test", "https://sso.hospital.test:8443/api"})
        @DisplayName("TC20C-17 An endpoint that starts with http:// or https:// is accepted in real mode")
        void validEndpointInRealMode(String value) {
            // Arrange
            IU_ManageIntegrations screen = open();
            endpoint(screen).setText(value);
            Screen.radio(screen, "Real").doClick();

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(1, service.requests().size()),
                    () -> assertEquals(value, lastRequest().getEndpoint()),
                    () -> assertEquals(IntegrationMode.REAL, lastRequest().getMode()));
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {"ftp://sso.hospital.test", "sso.hospital.test", "HTTP://sso.hospital.test",
                "http:/sso.hospital.test", "https:sso.hospital.test", "httpx://sso"})
        @DisplayName("TC20C-18 Any other endpoint is an error in real mode, and nothing is asked or sent")
        void invalidEndpointInRealMode(String value) {
            // Arrange
            IU_ManageIntegrations screen = open();
            endpoint(screen).setText(value);
            Screen.radio(screen, "Real").doClick();

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The endpoint must start with http:// or https://.", Screen.status(screen)),
                    () -> assertTrue(questions.isEmpty()),
                    () -> assertTrue(service.requests().isEmpty()));
        }

        @Test
        @DisplayName("TC20C-19 A bad endpoint is also an error in simulated mode")
        void badEndpointInSimulatedMode() {
            // Arrange
            IU_ManageIntegrations screen = open();
            endpoint(screen).setText("ftp://sso.hospital.test");

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The endpoint must start with http:// or https://.", Screen.status(screen)),
                    () -> assertTrue(service.requests().isEmpty()));
        }

        @Test
        @DisplayName("TC20C-20 Every error is shown together, each with its full stop, and the form keeps the text")
        void everyErrorTogether() {
            // Arrange
            IU_ManageIntegrations screen = open();
            String typed = "timeout=30\nbad\n\ntimeout=5\n=3";
            endpoint(screen).setText("ftp://sso");
            settings(screen).setText(typed);

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The endpoint must start with http:// or https://. Line 2: use key=value. "
                            + "The setting timeout is repeated. Line 5: use key=value.", Screen.status(screen)),
                    () -> assertEquals(RED, Screen.statusLabel(screen).getForeground()),
                    () -> assertEquals("ftp://sso", endpoint(screen).getText()),
                    () -> assertEquals(typed, settings(screen).getText()),
                    () -> assertTrue(service.requests().isEmpty()));
        }

        @Test
        @DisplayName("TC20C-20 In real mode, the missing endpoint and a bad line are shown together")
        void missingEndpointAndBadLineTogether() {
            // Arrange
            IU_ManageIntegrations screen = open();
            Screen.radio(screen, "Real").doClick();
            settings(screen).setText("timeout");

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The endpoint is required in real mode. Line 1: use key=value.",
                            Screen.status(screen)),
                    () -> assertTrue(service.requests().isEmpty()));
        }

        @Test
        @DisplayName("TC20C-21 The spaces around the endpoint are removed before it is checked and sent")
        void endpointIsStripped() {
            // Arrange
            IU_ManageIntegrations screen = open();
            endpoint(screen).setText("   https://sso.hospital.test  ");

            // Act
            save(screen);

            // Assert
            assertEquals("https://sso.hospital.test", lastRequest().getEndpoint());
        }
    }

    @Nested
    @DisplayName("Confirmations")
    class Confirmations {
        @Test
        @DisplayName("TC20C-22 Disabling an integration asks first and, if accepted, saves it")
        void disablingAsks() {
            // Arrange
            IU_ManageIntegrations screen = open();
            Screen.check(screen, "Enabled").doClick();

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(List.of("Disable the integration with " + SSO + "?"), questions),
                    () -> assertEquals(1, service.requests().size()),
                    () -> assertFalse(lastRequest().isEnabled()),
                    () -> assertEquals(IntegrationMode.SIMULATED, lastRequest().getMode()));
        }

        @Test
        @DisplayName("TC20C-23 Switching to real mode asks first and, if accepted, saves it")
        void switchingToRealAsks() {
            // Arrange
            IU_ManageIntegrations screen = open();
            select(screen, ADMISSIONS_ROW);
            Screen.radio(screen, "Real").doClick();

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(List.of("Switch Admissions to real mode?"), questions),
                    () -> assertEquals(1, service.requests().size()),
                    () -> assertEquals(HospitalSystemType.ADMISSIONS, lastRequest().getSystem()),
                    () -> assertEquals(IntegrationMode.REAL, lastRequest().getMode()),
                    () -> assertTrue(lastRequest().isEnabled()));
        }

        @Test
        @DisplayName("TC20C-24 Disabling and switching to real mode at once asks one question")
        void bothAskOneQuestion() {
            // Arrange
            IU_ManageIntegrations screen = open();
            endpoint(screen).setText("https://sso.hospital.test");
            Screen.check(screen, "Enabled").doClick();
            Screen.radio(screen, "Real").doClick();

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(List.of("Disable the integration with " + SSO + " and switch it to real mode?"),
                            questions),
                    () -> assertEquals(1, service.requests().size()));
        }

        @Test
        @DisplayName("TC20C-25 Other changes are saved without asking: settings, enabling, real to simulated, real to real")
        void otherChangesDoNotAsk() {
            // Arrange
            IU_ManageIntegrations screen = open();

            // Act
            settings(screen).setText("timeout=45");
            save(screen);
            select(screen, EHR_ROW);
            Screen.check(screen, "Enabled").doClick();
            save(screen);
            select(screen, RIS_PACS_ROW);
            endpoint(screen).setText("https://pacs2.hospital.test");
            save(screen);
            Screen.radio(screen, "Simulated").doClick();
            save(screen);

            // Assert
            assertAll(
                    () -> assertTrue(questions.isEmpty()),
                    () -> assertEquals(4, service.requests().size()),
                    () -> assertTrue(service.stored(HospitalSystemType.EHR).isEnabled()),
                    () -> assertEquals(IntegrationMode.SIMULATED,
                            service.stored(HospitalSystemType.RIS_PACS).getMode()));
        }

        @Test
        @DisplayName("TC20C-26 Cancelling the question sends nothing, clears the message and keeps the changes")
        void cancellingSendsNothing() {
            // Arrange
            answer = false;
            IU_ManageIntegrations screen = open();
            settings(screen).setText("bad");
            save(screen);
            settings(screen).setText("timeout=10");
            Screen.check(screen, "Enabled").doClick();

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(1, questions.size()),
                    () -> assertTrue(service.requests().isEmpty()),
                    () -> assertEquals(" ", Screen.status(screen)),
                    () -> assertFalse(Screen.check(screen, "Enabled").isSelected()),
                    () -> assertEquals("timeout=10", settings(screen).getText()),
                    () -> assertTrue(shows(screen, UNSAVED_LINE)));
        }
    }

    @Nested
    @DisplayName("Save")
    class Save {
        @Test
        @DisplayName("TC20C-27 Save sends the form, shows the green message and reads the table again")
        void saveSendsTheForm() {
            // Arrange
            IU_ManageIntegrations screen = open();
            endpoint(screen).setText("https://sso.hospital.test");
            settings(screen).setText("timeout=45\nrealm=hospital");
            Screen.check(screen, "Enabled").doClick();

            // Act
            save(screen);

            // Assert
            IntegrationConfiguration sent = lastRequest();
            JTable table = Screen.table(screen);
            assertAll(
                    () -> assertEquals(HospitalSystemType.SSO, sent.getSystem()),
                    () -> assertEquals("https://sso.hospital.test", sent.getEndpoint()),
                    () -> assertEquals(ordered("timeout", "45", "realm", "hospital"), sent.getSettings()),
                    () -> assertFalse(sent.isEnabled()),
                    () -> assertEquals(IntegrationMode.SIMULATED, sent.getMode()),
                    () -> assertEquals("✔ Integration " + SSO + " saved.", Screen.status(screen)),
                    () -> assertEquals(GREEN, Screen.statusLabel(screen).getForeground()),
                    () -> assertEquals(2, service.reads()),
                    () -> assertEquals("No", table.getValueAt(SSO_ROW, 2)),
                    () -> assertEquals("Inactive", table.getValueAt(SSO_ROW, 3)),
                    () -> assertEquals(SSO_ROW, table.getSelectedRow()),
                    () -> assertEquals("timeout=45\nrealm=hospital", settings(screen).getText()),
                    () -> assertFalse(shows(screen, UNSAVED_LINE)),
                    () -> assertTrue(Screen.enabled(screen, "Test connection")),
                    () -> assertFalse(Screen.enabled(screen, "Revert")));
        }

        @Test
        @DisplayName("TC20C-27 After saving another row, that row stays selected")
        void savedRowStaysSelected() {
            // Arrange
            IU_ManageIntegrations screen = open();
            select(screen, ADMISSIONS_ROW);
            settings(screen).setText("timeout=20");

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals(ADMISSIONS_ROW, Screen.table(screen).getSelectedRow()),
                    () -> assertEquals("✔ Integration Admissions saved.", Screen.status(screen)),
                    () -> assertEquals(Map.of("timeout", "20"),
                            service.stored(HospitalSystemType.ADMISSIONS).getSettings()));
        }

        @Test
        @DisplayName("TC20C-28 The errors of the server are shown together, in red, and the form keeps the text")
        void serverErrorsTogether() {
            // Arrange
            IU_ManageIntegrations screen = open();
            service.rejectNextChange("The setting timeout needs a value", "The mode is required");
            settings(screen).setText("timeout=45");

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The setting timeout needs a value. The mode is required.",
                            Screen.status(screen)),
                    () -> assertEquals(RED, Screen.statusLabel(screen).getForeground()),
                    () -> assertEquals("timeout=45", settings(screen).getText()),
                    () -> assertEquals(Map.of("timeout", "30"), service.stored(HospitalSystemType.SSO).getSettings()),
                    () -> assertTrue(shows(screen, UNSAVED_LINE)));
        }
    }

    @Nested
    @DisplayName("Test connection")
    class TestConnection {
        @Test
        @DisplayName("TC20C-29 A connection that works is shown in green with its time")
        void connectionOk() {
            // Arrange
            IU_ManageIntegrations screen = open();

            // Act
            testConnection(screen);

            // Assert
            assertAll(
                    () -> assertEquals("✔ Connection test of " + SSO + ": Connection OK (3 ms).",
                            Screen.status(screen)),
                    () -> assertEquals(GREEN, Screen.statusLabel(screen).getForeground()));
        }

        @Test
        @DisplayName("TC20C-29 The time of the answer is the one of the server, 0 included")
        void connectionOkWithZeroMilliseconds() {
            // Arrange
            service.answerIn(0);
            IU_ManageIntegrations screen = open();
            select(screen, ADMISSIONS_ROW);

            // Act
            testConnection(screen);

            // Assert
            assertEquals("✔ Connection test of Admissions: Connection OK (0 ms).", Screen.status(screen));
        }

        @Test
        @DisplayName("TC20C-30 A disabled integration is shown in red with the reason")
        void disabledIntegration() {
            // Arrange
            IU_ManageIntegrations screen = open();
            select(screen, EHR_ROW);

            // Act
            testConnection(screen);

            // Assert
            assertAll(
                    () -> assertEquals("⚠ Connection test of " + EHR + ": integration disabled.",
                            Screen.status(screen)),
                    () -> assertEquals(RED, Screen.statusLabel(screen).getForeground()));
        }

        @Test
        @DisplayName("TC20C-31 Real mode is not available in this version")
        void realModeNotAvailable() {
            // Arrange
            IU_ManageIntegrations screen = open();
            select(screen, RIS_PACS_ROW);

            // Act
            testConnection(screen);

            // Assert
            assertEquals("⚠ Connection test of RIS/PACS: real mode not available in this version.",
                    Screen.status(screen));
        }

        @Test
        @DisplayName("TC20C-32 An integration that is not configured in the server")
        void notConfigured() {
            // Arrange
            service.notConfigured(HospitalSystemType.ADMISSIONS);
            IU_ManageIntegrations screen = open();
            select(screen, ADMISSIONS_ROW);

            // Act
            testConnection(screen);

            // Assert
            assertEquals("⚠ Connection test of Admissions: integration not configured.", Screen.status(screen));
        }

        @Test
        @DisplayName("TC20C-33 The test is of the selected system, changes nothing and tests what was saved")
        void testsTheSavedConfigurationOfTheSelectedSystem() {
            // Arrange
            IU_ManageIntegrations screen = open();
            Screen.check(screen, "Enabled").doClick();
            save(screen);

            // Act
            testConnection(screen);
            select(screen, ADMISSIONS_ROW);
            testConnection(screen);

            // Assert
            assertAll(
                    () -> assertEquals(List.of(HospitalSystemType.SSO, HospitalSystemType.ADMISSIONS),
                            service.tested()),
                    () -> assertEquals(1, service.requests().size()),
                    () -> assertEquals("✔ Connection test of Admissions: Connection OK (3 ms).",
                            Screen.status(screen)));
        }

        @Test
        @DisplayName("TC20C-33 After saving a disabled integration its test says it is disabled")
        void testAfterDisabling() {
            // Arrange
            IU_ManageIntegrations screen = open();
            Screen.check(screen, "Enabled").doClick();
            save(screen);

            // Act
            testConnection(screen);

            // Assert
            assertEquals("⚠ Connection test of " + SSO + ": integration disabled.", Screen.status(screen));
        }

        @Test
        @DisplayName("TC20C-33 showTestResult with no selected row names the system in general")
        void showTestResultWithoutSelection() {
            // Arrange
            service = new FakeIntegrationManagementService();
            IU_ManageIntegrations screen = open();

            // Act
            screen.showTestResult(new ConnectionTestResult(false, 0,
                    "The system did not answer"));

            // Assert
            assertEquals("⚠ Connection test of the system: The system did not answer.", Screen.status(screen));
        }
    }

    @Nested
    @DisplayName("Unsaved changes")
    class UnsavedChanges {
        @Test
        @DisplayName("TC20C-34 Changing any field shows the line, disables Test connection and enables Revert")
        void anyChangeIsUnsaved() {
            // Arrange
            IU_ManageIntegrations screen = open();
            List<Boolean> dirty = new ArrayList<>();
            List<Boolean> testEnabled = new ArrayList<>();
            List<Boolean> revertEnabled = new ArrayList<>();
            List<Runnable> changes = List.of(
                    () -> endpoint(screen).setText("http://sso.hospital.test"),
                    () -> settings(screen).setText("timeout=31"),
                    () -> Screen.check(screen, "Enabled").doClick(),
                    () -> Screen.radio(screen, "Real").doClick());

            // Act
            for (Runnable change : changes) {
                Screen.button(screen, "Revert").doClick();
                change.run();
                dirty.add(shows(screen, UNSAVED_LINE));
                testEnabled.add(Screen.enabled(screen, "Test connection"));
                revertEnabled.add(Screen.enabled(screen, "Revert"));
            }

            // Assert
            assertAll(
                    () -> assertEquals(List.of(true, true, true, true), dirty),
                    () -> assertEquals(List.of(false, false, false, false), testEnabled),
                    () -> assertEquals(List.of(true, true, true, true), revertEnabled),
                    () -> assertTrue(Screen.enabled(screen, "Save")));
        }

        @Test
        @DisplayName("TC20C-34 Undoing a change by hand, or only adding spaces, is not an unsaved change")
        void undoingByHandIsClean() {
            // Arrange
            IU_ManageIntegrations screen = open();

            // Act
            settings(screen).setText("timeout=31");
            boolean changed = shows(screen, UNSAVED_LINE);
            settings(screen).setText("timeout=30\n");
            endpoint(screen).setText("   ");
            Screen.check(screen, "Enabled").doClick();
            Screen.check(screen, "Enabled").doClick();

            // Assert
            assertAll(
                    () -> assertTrue(changed),
                    () -> assertFalse(shows(screen, UNSAVED_LINE)),
                    () -> assertTrue(Screen.enabled(screen, "Test connection")),
                    () -> assertFalse(Screen.enabled(screen, "Revert")));
        }

        @Test
        @DisplayName("TC20C-35 Revert loads the saved configuration again and sends nothing")
        void revert() {
            // Arrange
            IU_ManageIntegrations screen = open();
            select(screen, ADMISSIONS_ROW);
            endpoint(screen).setText("ftp://x");
            settings(screen).setText("bad");
            Screen.check(screen, "Enabled").doClick();
            Screen.radio(screen, "Real").doClick();

            // Act
            Screen.button(screen, "Revert").doClick();

            // Assert
            assertAll(
                    () -> assertEquals("http://admissions.hospital.test", endpoint(screen).getText()),
                    () -> assertEquals("timeout=15\nretries=2", settings(screen).getText()),
                    () -> assertTrue(Screen.check(screen, "Enabled").isSelected()),
                    () -> assertTrue(Screen.radio(screen, "Simulated").isSelected()),
                    () -> assertTrue(shows(screen, SIMULATED_LINE)),
                    () -> assertFalse(shows(screen, UNSAVED_LINE)),
                    () -> assertTrue(Screen.enabled(screen, "Test connection")),
                    () -> assertFalse(Screen.enabled(screen, "Revert")),
                    () -> assertTrue(service.requests().isEmpty()),
                    () -> assertTrue(questions.isEmpty()));
        }

        @Test
        @DisplayName("TC20C-36 Pressing another row with unsaved changes asks and, if accepted, shows the new row")
        void otherRowAcceptDiscards() {
            // Arrange
            IU_ManageIntegrations screen = open();
            settings(screen).setText("timeout=99");

            // Act
            select(screen, ADMISSIONS_ROW);

            // Assert
            assertAll(
                    () -> assertEquals(List.of("Discard the unsaved changes?"), questions),
                    () -> assertEquals(ADMISSIONS_ROW, Screen.table(screen).getSelectedRow()),
                    () -> assertEquals("timeout=15\nretries=2", settings(screen).getText()),
                    () -> assertFalse(shows(screen, UNSAVED_LINE)),
                    () -> assertEquals(Map.of("timeout", "30"), service.stored(HospitalSystemType.SSO).getSettings()),
                    () -> assertTrue(service.requests().isEmpty()));
        }

        @Test
        @DisplayName("TC20C-37 If the user cancels, the selection goes back to its row with the changes")
        void otherRowCancelKeepsTheChanges() {
            // Arrange
            answer = false;
            IU_ManageIntegrations screen = open();
            settings(screen).setText("timeout=99");

            // Act
            select(screen, RIS_PACS_ROW);

            // Assert
            assertAll(
                    () -> assertEquals(List.of("Discard the unsaved changes?"), questions),
                    () -> assertEquals(SSO_ROW, Screen.table(screen).getSelectedRow()),
                    () -> assertEquals("timeout=99", settings(screen).getText()),
                    () -> assertTrue(shows(screen, UNSAVED_LINE)),
                    () -> assertTrue(service.requests().isEmpty()));
        }

        @Test
        @DisplayName("TC20C-38 Pressing another row without changes does not ask")
        void otherRowWithoutChangesDoesNotAsk() {
            // Arrange
            IU_ManageIntegrations screen = open();

            // Act
            select(screen, EHR_ROW);
            select(screen, EHR_ROW);
            select(screen, SSO_ROW);

            // Assert
            assertAll(
                    () -> assertTrue(questions.isEmpty()),
                    () -> assertEquals(SSO_ROW, Screen.table(screen).getSelectedRow()));
        }
    }

    @Nested
    @DisplayName("Permission")
    class Permission {
        private void assertOnlyBackEnabled(IU_ManageIntegrations screen) {
            assertAll(
                    () -> assertEquals(DENIED, Screen.status(screen)),
                    () -> assertEquals(RED, Screen.statusLabel(screen).getForeground()),
                    () -> assertFalse(Screen.enabled(screen, "Save")),
                    () -> assertFalse(Screen.enabled(screen, "Test connection")),
                    () -> assertFalse(Screen.enabled(screen, "Revert")),
                    () -> assertFalse(endpoint(screen).isEnabled()),
                    () -> assertFalse(settings(screen).isEnabled()),
                    () -> assertFalse(Screen.check(screen, "Enabled").isEnabled()),
                    () -> assertFalse(Screen.radio(screen, "Simulated").isEnabled()),
                    () -> assertFalse(Screen.radio(screen, "Real").isEnabled()),
                    () -> assertTrue(Screen.enabled(screen, "Back")));
        }

        @Test
        @DisplayName("TC20C-39 If the server denies when the screen opens, only Back stays enabled")
        void deniedWhenOpening() {
            // Arrange
            service.denyAll();

            // Act
            IU_ManageIntegrations screen = open();
            Screen.button(screen, "Back").doClick();

            // Assert
            assertAll(
                    () -> assertOnlyBackEnabled(screen),
                    () -> assertEquals(0, Screen.table(screen).getRowCount()),
                    () -> assertFalse(shows(screen, SIMULATED_LINE)),
                    () -> assertEquals(1, backs));
        }

        @Test
        @DisplayName("TC20C-40 If the permission is revoked while the screen is open, Save is denied")
        void revokedThenSave() {
            // Arrange
            IU_ManageIntegrations screen = open();
            settings(screen).setText("timeout=45");
            service.denyAll();

            // Act
            save(screen);

            // Assert
            assertAll(
                    () -> assertOnlyBackEnabled(screen),
                    () -> assertTrue(service.requests().isEmpty()),
                    () -> assertFalse(shows(screen, UNSAVED_LINE)));
        }

        @Test
        @DisplayName("TC20C-41 If the permission is revoked while the screen is open, Test connection is denied")
        void revokedThenTestConnection() {
            // Arrange
            IU_ManageIntegrations screen = open();
            service.denyAll();

            // Act
            testConnection(screen);

            // Assert
            assertAll(
                    () -> assertOnlyBackEnabled(screen),
                    () -> assertTrue(service.tested().isEmpty()));
        }
    }

    @Nested
    @DisplayName("Fixes after the findings of #122")
    class FindingsOf122 {
        @Test
        @DisplayName("TC20C-42 F-17 A configuration with a null endpoint is shown with an empty endpoint and no unsaved changes")
        void nullEndpointIsNotAnUnsavedChange() {
            // Arrange
            service.add(FakeIntegrationManagementService.integration(HospitalSystemType.SSO, null,
                    Map.of("timeout", "30"), true, IntegrationMode.SIMULATED));
            IU_ManageIntegrations screen = open();
            boolean dirtyWhenOpened = shows(screen, UNSAVED_LINE);

            // Act
            select(screen, EHR_ROW);
            select(screen, SSO_ROW);

            // Assert
            assertAll(
                    () -> assertFalse(dirtyWhenOpened),
                    () -> assertEquals(SSO_ROW, Screen.table(screen).getSelectedRow()),
                    () -> assertEquals("", endpoint(screen).getText()),
                    () -> assertFalse(shows(screen, UNSAVED_LINE)),
                    () -> assertTrue(Screen.enabled(screen, "Test connection")),
                    () -> assertFalse(Screen.enabled(screen, "Revert")),
                    () -> assertTrue(questions.isEmpty()));
        }

        @Test
        @DisplayName("TC20C-43 F-18 After a denial the table is disabled and pressing a row does nothing and never asks")
        void afterADenialTheTableIgnoresTheRows() {
            // Arrange
            IU_ManageIntegrations screen = open();
            settings(screen).setText("timeout=45");
            service.denyAll();
            save(screen);
            JTable table = Screen.table(screen);

            // Act
            select(screen, ADMISSIONS_ROW);
            select(screen, RIS_PACS_ROW);

            // Assert
            assertAll(
                    () -> assertFalse(table.isEnabled()),
                    () -> assertEquals(0, questions.size()),
                    () -> assertEquals("timeout=45", settings(screen).getText()),
                    () -> assertEquals("", endpoint(screen).getText()),
                    () -> assertEquals(DENIED, Screen.status(screen)),
                    () -> assertFalse(Screen.enabled(screen, "Save")),
                    () -> assertFalse(Screen.enabled(screen, "Test connection")),
                    () -> assertTrue(service.requests().isEmpty()));
        }

        @Test
        @DisplayName("TC20C-44 F-19 Revert clears the message area, so an old error disappears")
        void revertClearsTheMessage() {
            // Arrange
            IU_ManageIntegrations screen = open();
            select(screen, ADMISSIONS_ROW);
            endpoint(screen).setText("ftp://x");
            save(screen);
            String error = Screen.status(screen);

            // Act
            Screen.button(screen, "Revert").doClick();

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The endpoint must start with http:// or https://.", error),
                    () -> assertEquals(" ", Screen.status(screen)),
                    () -> assertEquals("http://admissions.hospital.test", endpoint(screen).getText()),
                    () -> assertFalse(shows(screen, UNSAVED_LINE)),
                    () -> assertTrue(service.requests().isEmpty()));
        }

        @Test
        @DisplayName("TC20C-44 F-19 Revert also clears an error of the server")
        void revertClearsAnErrorOfTheServer() {
            // Arrange
            IU_ManageIntegrations screen = open();
            settings(screen).setText("timeout=0");
            service.rejectNextChange("The setting timeout is not valid");
            save(screen);
            String error = Screen.status(screen);

            // Act
            Screen.button(screen, "Revert").doClick();

            // Assert
            assertAll(
                    () -> assertEquals("⚠ The setting timeout is not valid.", error),
                    () -> assertEquals(" ", Screen.status(screen)),
                    () -> assertEquals("timeout=30", settings(screen).getText()),
                    () -> assertEquals(1, service.requests().size()));
        }
    }
}
