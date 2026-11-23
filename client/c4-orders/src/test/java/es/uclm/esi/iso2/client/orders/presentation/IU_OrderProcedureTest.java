package es.uclm.esi.iso2.client.orders.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.orders.domain.ClinicalOrder;
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the screen {@link IU_OrderProcedure} of UC06 (#67): the patients and their orders (Q3, Q4), the active
 * test types and priorities (Q5, D-022), the inactive patient, the validation of the client (Q6), the order placed
 * (Q7), the errors of the server and the denied access (Q8), the keys (Q10), the unsaved changes (Q11) and the orders
 * that cannot be edited (D-039). The cases are those of {@code doc/testing/test-cases/UC06-client-test-design.md}.
 * The screen is only built, never shown, so the tests need no display; every question is answered by the test, which
 * also records it.
 */
@DisplayName("IU_OrderProcedure")
class IU_OrderProcedureTest {
    private static final String REQUESTER = "mgarcia";
    private static final String TEST_TYPE = "Test type";
    private static final String PRIORITY = "Priority";
    private static final String INDICATION = "Clinical indication";
    private static final String JUSTIFICATION = "Justification";
    private static final String PLACE_ORDER = "Place order";
    private static final String DISCARD = "Discard the unsaved changes?";
    private static final String DENIED = "⚠ You do not have permission to order procedures.";
    private static final String NO_TYPES = "⚠ There are no active test types or priorities to order with.";
    private static final String INDICATION_REQUIRED = "The clinical indication is required";
    private static final String JUSTIFICATION_REQUIRED = "The justification is required";
    private static final Color GREEN = new Color(0, 128, 0);
    private static final Color RED = new Color(180, 0, 0);
    private static final int LUCIA = 0;
    private static final int MARIO = 1;
    private static final int INES = 2;
    private static final List<String> LUCIA_ORDER_1 =
            List.of("20/11/2026", "Chest X-ray", "Routine", "Persistent cough");
    private static final List<String> LUCIA_ORDER_2 =
            List.of("21/11/2026", "Knee MRI", "Urgent", "Knee pain after a fall");

    private FakeClinicalOrderService orders;
    private FakePatientService patients;
    private FakeSystemParameterService parameters;
    private List<String> questions;
    private boolean answer;
    private int backs;

    @BeforeEach
    void setUp() {
        orders = FakeClinicalOrderService.withTwoOrdersOfLucia();
        patients = FakePatientService.withThreePatients();
        parameters = FakeSystemParameterService.withSeedData();
        questions = new ArrayList<>();
        answer = true;
        backs = 0;
    }

    private IU_OrderProcedure open() {
        return new IU_OrderProcedure(orders, patients, parameters, REQUESTER, question -> {
            questions.add(question);
            return answer;
        }, () -> backs++);
    }

    private static IU_OrderProcedure select(IU_OrderProcedure screen, int row) {
        screen.getPatientsTable().setRowSelectionInterval(row, row);
        return screen;
    }

    private static JTextField field(IU_OrderProcedure screen, String label) {
        return Screen.field(screen, label);
    }

    private static JComboBox<Object> combo(IU_OrderProcedure screen, String label) {
        return Screen.combo(screen, label);
    }

    private static void fill(IU_OrderProcedure screen, String testType, String priority, String indication,
            String justification) {
        Screen.choose(combo(screen, TEST_TYPE), testType);
        Screen.choose(combo(screen, PRIORITY), priority);
        field(screen, INDICATION).setText(indication);
        field(screen, JUSTIFICATION).setText(justification);
    }

    private static void placeOrder(IU_OrderProcedure screen) {
        Screen.button(screen, PLACE_ORDER).doClick();
    }

    private static Color statusColour(IU_OrderProcedure screen) {
        return Screen.label(screen, screen.getStatusMessage()).getForeground();
    }

    private static boolean formEnabled(IU_OrderProcedure screen) {
        return combo(screen, TEST_TYPE).isEnabled() || combo(screen, PRIORITY).isEnabled()
                || field(screen, INDICATION).isEnabled() || field(screen, JUSTIFICATION).isEnabled()
                || screen.getPlaceOrderButton().isEnabled();
    }

    private static boolean formAllEnabled(IU_OrderProcedure screen) {
        return combo(screen, TEST_TYPE).isEnabled() && combo(screen, PRIORITY).isEnabled()
                && field(screen, INDICATION).isEnabled() && field(screen, JUSTIFICATION).isEnabled()
                && screen.getPlaceOrderButton().isEnabled();
    }

    /** A filled form for Lucía that the client accepts. */
    private IU_OrderProcedure validFormOfLucia() {
        IU_OrderProcedure screen = select(open(), LUCIA);
        fill(screen, "CT-HEAD · Head CT", "Urgent", "Headache after a fall", "Rule out bleeding");
        return screen;
    }

    @Test
    @DisplayName("TC06C-01 On opening, every patient is listed and the form stays disabled until one is chosen")
    void tc06c01OpeningListsThePatients() {
        // Act
        IU_OrderProcedure screen = open();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Identifier", "Full name", "Date of birth"),
                        Screen.columns(screen.getPatientsTable())),
                () -> assertEquals(List.of(List.of("HC-0000001", "Lucía Ejemplo Prueba", "14/03/1980"),
                        List.of("HC-0000002", "Mario Demo Simulado", "02/11/1975"),
                        List.of("HC-0000009", "Inés Inactiva Inventada", "")), Screen.rows(screen.getPatientsTable())),
                () -> assertEquals("3 patients found.", Screen.found(screen)),
                () -> assertEquals(List.of(""), patients.searches()),
                () -> assertNull(screen.getSelectedPatient()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertEquals(List.of("Date", "Test type", "Priority", "Indication"),
                        Screen.columns(screen.getOrdersTable())),
                () -> assertEquals(0, screen.getOrdersTable().getRowCount()),
                () -> assertTrue(Screen.button(screen, "Back").isEnabled()),
                () -> assertEquals(" ", screen.getStatusMessage()),
                () -> assertTrue(orders.requesters().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-02 Search finds one patient (button), none (Enter) and sends the text without end spaces")
    void tc06c02SearchFindsOneOrNone() {
        // Arrange
        IU_OrderProcedure screen = open();

        // Act
        screen.getSearchField().setText("  mario ");
        Screen.button(screen, "Search").doClick();
        List<List<String>> one = Screen.rows(screen.getPatientsTable());
        String foundOne = Screen.found(screen);
        screen.getSearchField().setText("zzz");
        screen.getSearchField().postActionEvent();

        // Assert
        assertAll(
                () -> assertEquals(List.of(List.of("HC-0000002", "Mario Demo Simulado", "02/11/1975")), one),
                () -> assertEquals("1 patient found.", foundOne),
                () -> assertEquals(0, screen.getPatientsTable().getRowCount()),
                () -> assertEquals("0 patients found.", Screen.found(screen)),
                () -> assertEquals(List.of("", "mario", "zzz"), patients.searches()));
    }

    @Test
    @DisplayName("TC06C-03 Selecting an active patient enables the form and lists the orders that they have")
    void tc06c03ActivePatientShowsTheFormAndTheOrders() {
        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertEquals("P-0001", screen.getSelectedPatient().getPatientId()),
                () -> assertTrue(formAllEnabled(screen)),
                () -> assertEquals(List.of(LUCIA_ORDER_1, LUCIA_ORDER_2), Screen.rows(screen.getOrdersTable())),
                () -> assertEquals(List.of("P-0001"), orders.reads()),
                () -> assertEquals(List.of(REQUESTER), orders.requesters()),
                () -> assertEquals("", field(screen, INDICATION).getText()),
                () -> assertEquals("", field(screen, JUSTIFICATION).getText()),
                () -> assertEquals(" ", screen.getStatusMessage()));
    }

    @Test
    @DisplayName("TC06C-04 An active patient without orders gets the form enabled and an empty list of orders")
    void tc06c04PatientWithoutOrders() {
        // Act
        IU_OrderProcedure screen = select(open(), MARIO);

        // Assert
        assertAll(
                () -> assertEquals("P-0002", screen.getSelectedPatient().getPatientId()),
                () -> assertTrue(formAllEnabled(screen)),
                () -> assertEquals(0, screen.getOrdersTable().getRowCount()));
    }

    @Test
    @DisplayName("TC06C-05 Only the active test types are offered, as id · name; the deactivated XRAY-OLD is not")
    void tc06c05OnlyActiveTestTypes() {
        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertEquals(List.of("XRAY-CHEST · Chest X-ray", "CT-HEAD · Head CT", "MRI-KNEE · Knee MRI",
                "US-ABDOMEN · Abdominal ultrasound", "MAMMO-BILATERAL · Bilateral mammography",
                "DEXA-SPINE · Bone densitometry"), Screen.items(combo(screen, TEST_TYPE)));
    }

    @Test
    @DisplayName("TC06C-06 Only the active priorities are offered, by name; the deactivated OLD-TIER is not")
    void tc06c06OnlyActivePriorities() {
        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertEquals(List.of("Urgent", "Preferential", "Routine"), Screen.items(combo(screen, PRIORITY)));
    }

    @Test
    @DisplayName("TC06C-07 Without active test types the form stays disabled and says why")
    void tc06c07NoActiveTestTypes() {
        // Arrange
        parameters.testTypes().replaceAll(TestType::deactivated);

        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertEquals(0, combo(screen, TEST_TYPE).getItemCount()),
                () -> assertEquals(3, combo(screen, PRIORITY).getItemCount()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertEquals(NO_TYPES, screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals(List.of(LUCIA_ORDER_1, LUCIA_ORDER_2), Screen.rows(screen.getOrdersTable())));
    }

    @Test
    @DisplayName("TC06C-08 Without active priorities the form stays disabled and says why")
    void tc06c08NoActivePriorities() {
        // Arrange
        parameters.priorities().replaceAll(Priority::deactivated);

        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertEquals(6, combo(screen, TEST_TYPE).getItemCount()),
                () -> assertEquals(0, combo(screen, PRIORITY).getItemCount()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertEquals(NO_TYPES, screen.getStatusMessage()));
    }

    @Test
    @DisplayName("TC06C-09 An inactive patient disables the form, empties the orders, says why and sends nothing")
    void tc06c09InactivePatient() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Act
        select(screen, INES);
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ The patient P-0009 is not active, so no order can be placed.",
                        screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals("P-0009", screen.getSelectedPatient().getPatientId()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertEquals(0, screen.getOrdersTable().getRowCount()),
                () -> assertEquals(List.of("P-0001"), orders.reads()),
                () -> assertTrue(orders.sent().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-10 An identifier that no patient has shows an error and leaves the form disabled")
    void tc06c10UnknownPatient() {
        // Arrange
        IU_OrderProcedure screen = open();

        // Act
        screen.onSelectPatient("HC-9999999");

        // Assert
        assertAll(
                () -> assertEquals("⚠ There is no patient with the identifier HC-9999999", screen.getStatusMessage()),
                () -> assertNull(screen.getSelectedPatient()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertTrue(orders.requesters().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-43 A part of an identifier finds patients, but none with exactly that identifier: error")
    void tc06c43PartialIdentifier() {
        // Arrange
        IU_OrderProcedure screen = open();

        // Act
        screen.onSelectPatient("HC-000000");

        // Assert
        assertAll(
                () -> assertEquals("⚠ There is no patient with the identifier HC-000000", screen.getStatusMessage()),
                () -> assertNull(screen.getSelectedPatient()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertTrue(orders.requesters().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-11Placing an order with no patient and an empty form shows the five errors, sends nothing")
    void tc06c11EmptyFormWithoutPatient() {
        // Arrange
        IU_OrderProcedure screen = open();

        // Act
        screen.onSubmitOrder();

        // Assert
        assertAll(
                () -> assertEquals("⚠ Select a patient before placing an order. The test type is required"
                        + " The priority is required " + INDICATION_REQUIRED + " " + JUSTIFICATION_REQUIRED,
                        screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertTrue(orders.sent().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-12 With the patient chosen, empty indication and justification give both errors together")
    void tc06c12EmptyIndicationAndJustification() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ " + INDICATION_REQUIRED + " " + JUSTIFICATION_REQUIRED,
                        screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertTrue(orders.sent().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-13 Indication and justification with only spaces are blank: both errors, nothing sent")
    void tc06c13BlankIndicationAndJustification() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);
        fill(screen, "CT-HEAD · Head CT", "Urgent", "   ", " ");

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ " + INDICATION_REQUIRED + " " + JUSTIFICATION_REQUIRED,
                        screen.getStatusMessage()),
                () -> assertTrue(orders.sent().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-14 Only the indication blank gives only its error, and the form keeps the justification")
    void tc06c14OnlyIndicationBlank() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);
        fill(screen, "CT-HEAD · Head CT", "Urgent", "  ", "Rule out bleeding");

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ " + INDICATION_REQUIRED, screen.getStatusMessage()),
                () -> assertEquals("Rule out bleeding", field(screen, JUSTIFICATION).getText()),
                () -> assertTrue(orders.sent().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-15 Only the justification empty gives only its error, and the form keeps the indication")
    void tc06c15OnlyJustificationEmpty() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);
        fill(screen, "CT-HEAD · Head CT", "Urgent", "Headache after a fall", "");

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ " + JUSTIFICATION_REQUIRED, screen.getStatusMessage()),
                () -> assertEquals("Headache after a fall", field(screen, INDICATION).getText()),
                () -> assertTrue(orders.sent().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-16 With nothing to choose in the lists, the test type and the priority are required")
    void tc06c16NoTestTypeAndNoPriority() {
        // Arrange
        parameters.testTypes().clear();
        parameters.priorities().clear();
        IU_OrderProcedure screen = select(open(), LUCIA);
        field(screen, INDICATION).setText("Headache after a fall");
        field(screen, JUSTIFICATION).setText("Rule out bleeding");

        // Act
        screen.onSubmitOrder();

        // Assert
        assertAll(
                () -> assertEquals("⚠ The test type is required The priority is required", screen.getStatusMessage()),
                () -> assertTrue(orders.sent().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-17 A valid order is sent once with its data, the success is shown and the list is reloaded")
    void tc06c17ValidOrder() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);
        fill(screen, "CT-HEAD · Head CT", "Urgent", "  Headache after a fall ", " Rule out bleeding  ");
        String today = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDate.now());

        // Act
        placeOrder(screen);

        // Assert
        DiagnosticOrder sent = orders.sent().get(0);
        assertAll(
                () -> assertEquals(1, orders.sent().size()),
                () -> assertNull(sent.getOrderId()),
                () -> assertEquals("P-0001", sent.getPatientId()),
                () -> assertEquals("CT-HEAD", sent.getTestTypeId()),
                () -> assertEquals("URGENT", sent.getPriorityId()),
                () -> assertEquals(REQUESTER, sent.getRequester()),
                () -> assertEquals("Headache after a fall", sent.getIndication()),
                () -> assertEquals("Rule out bleeding", sent.getJustification()),
                () -> assertNull(sent.getCreationDate()),
                () -> assertEquals(List.of(REQUESTER, REQUESTER, REQUESTER), orders.requesters()),
                () -> assertEquals("Order ORD-0003 created.", screen.getStatusMessage()),
                () -> assertEquals(GREEN, statusColour(screen)),
                () -> assertEquals("", field(screen, INDICATION).getText()),
                () -> assertEquals("", field(screen, JUSTIFICATION).getText()),
                () -> assertEquals("P-0001", screen.getSelectedPatient().getPatientId()),
                () -> assertEquals(List.of("P-0001", "P-0001"), orders.reads()),
                () -> assertEquals(List.of(LUCIA_ORDER_1, LUCIA_ORDER_2,
                        List.of(today, "Head CT", "Urgent", "Headache after a fall")),
                        Screen.rows(screen.getOrdersTable())));
    }

    @Test
    @DisplayName("TC06C-18 An indication and a justification of one character are accepted and sent")
    void tc06c18OneCharacterTexts() {
        // Arrange
        IU_OrderProcedure screen = select(open(), MARIO);
        fill(screen, "XRAY-CHEST · Chest X-ray", "Routine", "a", "b");

        // Act
        placeOrder(screen);

        // Assert
        DiagnosticOrder sent = orders.sent().get(0);
        assertAll(
                () -> assertEquals(1, orders.sent().size()),
                () -> assertEquals("P-0002", sent.getPatientId()),
                () -> assertEquals("XRAY-CHEST", sent.getTestTypeId()),
                () -> assertEquals("ROUTINE", sent.getPriorityId()),
                () -> assertEquals("a", sent.getIndication()),
                () -> assertEquals("b", sent.getJustification()),
                () -> assertEquals("Order ORD-0003 created.", screen.getStatusMessage()),
                () -> assertEquals(1, screen.getOrdersTable().getRowCount()));
    }

    @Test
    @DisplayName("TC06C-19 The errors of the server are shown together; the list and the form do not change")
    void tc06c19ServerRejectsWithTwoErrors() {
        // Arrange
        IU_OrderProcedure screen = validFormOfLucia();
        orders.rejectNextOrder("The patient P-0001 is not active", "The test type CT-HEAD is not active");

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ The patient P-0001 is not active The test type CT-HEAD is not active",
                        screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals(1, orders.sent().size()),
                () -> assertEquals(List.of("P-0001"), orders.reads()),
                () -> assertEquals(List.of(LUCIA_ORDER_1, LUCIA_ORDER_2), Screen.rows(screen.getOrdersTable())),
                () -> assertEquals("Headache after a fall", field(screen, INDICATION).getText()),
                () -> assertEquals("Rule out bleeding", field(screen, JUSTIFICATION).getText()),
                () -> assertTrue(formAllEnabled(screen)));
    }

    @Test
    @DisplayName("TC06C-20 A single error of the server is shown alone")
    void tc06c20ServerRejectsWithOneError() {
        // Arrange
        IU_OrderProcedure screen = validFormOfLucia();
        orders.rejectNextOrder("The priority URGENT is not active");

        // Act
        placeOrder(screen);

        // Assert
        assertEquals("⚠ The priority URGENT is not active", screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC06C-21 A failure of the server while ordering is explained and the list does not change")
    void tc06c21ServerFailsWhileOrdering() {
        // Arrange
        IU_OrderProcedure screen = validFormOfLucia();
        orders.failNextOrder(new IllegalStateException("The server cannot be reached"));

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ The order could not be created: The server cannot be reached",
                        screen.getStatusMessage()),
                () -> assertEquals(2, screen.getOrdersTable().getRowCount()),
                () -> assertEquals("Headache after a fall", field(screen, INDICATION).getText()));
    }

    @Test
    @DisplayName("TC06C-22 A denied order explains it, disables every action but Back, and nothing more is called")
    void tc06c22DeniedWhileOrdering() {
        // Arrange
        IU_OrderProcedure screen = validFormOfLucia();
        orders.denyAll();

        // Act
        placeOrder(screen);
        int ordersCalls = orders.requesters().size();
        int searches = patients.searches().size();
        screen.onSearch();
        screen.onSelectPatient("HC-0000002");
        screen.onSubmitOrder();

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertFalse(formEnabled(screen)),
                () -> assertFalse(screen.getPatientsTable().isEnabled()),
                () -> assertFalse(screen.getSearchField().isEnabled()),
                () -> assertTrue(Screen.button(screen, "Back").isEnabled()),
                () -> assertTrue(orders.sent().isEmpty()),
                () -> assertEquals(ordersCalls, orders.requesters().size()),
                () -> assertEquals(searches, patients.searches().size()),
                () -> assertEquals(2, screen.getOrdersTable().getRowCount()));
    }

    @Test
    @DisplayName("TC06C-23 A user without the permission gets the denied message on opening; Back still works")
    void tc06c23DeniedOnOpening() {
        // Arrange
        patients.denyAll();

        // Act
        IU_OrderProcedure screen = open();
        Screen.button(screen, "Back").doClick();

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertEquals(0, screen.getPatientsTable().getRowCount()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertFalse(screen.getPatientsTable().isEnabled()),
                () -> assertFalse(screen.getSearchField().isEnabled()),
                () -> assertEquals(1, backs),
                () -> assertTrue(questions.isEmpty()),
                () -> assertTrue(orders.requesters().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-24 A denied reading of the orders of the patient gives the denied message, form disabled")
    void tc06c24DeniedWhileReadingTheOrders() {
        // Arrange
        orders.denyAll();

        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertFalse(screen.getPatientsTable().isEnabled()));
    }

    @Test
    @DisplayName("TC06C-25 The errors of the server while reading the orders of the patient are shown")
    void tc06c25ValidationErrorWhileReadingTheOrders() {
        // Arrange
        orders.failReads(new ValidationException(List.of("There is no patient with the id P-0001")));

        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertEquals("⚠ There is no patient with the id P-0001", screen.getStatusMessage()),
                () -> assertFalse(formEnabled(screen)));
    }

    @Test
    @DisplayName("TC06C-26 A failure of the server while reading the orders is explained and the form stays disabled")
    void tc06c26FailureWhileReadingTheOrders() {
        // Arrange
        orders.failReads(new IllegalStateException("The server cannot be reached"));

        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertEquals("⚠ The orders could not be read: The server cannot be reached",
                        screen.getStatusMessage()),
                () -> assertFalse(formEnabled(screen)));
    }

    @Test
    @DisplayName("TC06C-27 A failure of the server while reading the patients is explained")
    void tc06c27FailureWhileReadingThePatients() {
        // Arrange
        patients.failSearches(new IllegalStateException("The server cannot be reached"));

        // Act
        IU_OrderProcedure screen = open();

        // Assert
        assertAll(
                () -> assertEquals("⚠ The patients could not be read: The server cannot be reached",
                        screen.getStatusMessage()),
                () -> assertEquals(0, screen.getPatientsTable().getRowCount()),
                () -> assertTrue(screen.getSearchField().isEnabled()));
    }

    @Test
    @DisplayName("TC06C-28 No cell of the tables can be edited and there is no button to edit or cancel an order")
    void tc06c28OrdersCannotBeEdited() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Act
        List<Boolean> editable = new ArrayList<>();
        for (JTable table : List.of(screen.getPatientsTable(), screen.getOrdersTable())) {
            for (int row = 0; row < table.getRowCount(); row++) {
                for (int column = 0; column < table.getColumnCount(); column++) {
                    editable.add(table.isCellEditable(row, column));
                }
            }
        }

        // Assert
        assertAll(
                () -> assertEquals(9 + 8, editable.size()),
                () -> assertFalse(editable.contains(true)),
                () -> assertEquals(List.of("Search", PLACE_ORDER, "Back"), Screen.buttonTexts(screen)));
    }

    @Test
    @DisplayName("TC06C-29 An order without date, of a test type not in the catalogue and not diagnostic is listed")
    void tc06c29UnusualOrdersAreListed() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);
        ClinicalOrder notDiagnostic = new ClinicalOrder("ORD-0005", "P-0001", "CT-HEAD", REQUESTER, "Control",
                "Yearly control", LocalDate.of(2026, 11, 22)) { };

        // Act
        screen.displayOrders(List.of(new DiagnosticOrder("ORD-0004", "P-0001", "PET-BODY", REQUESTER, "Staging",
                "Rule out metastasis", null, "VIP"), notDiagnostic));

        // Assert
        assertEquals(List.of(List.of("", "PET-BODY", "VIP", "Staging"),
                List.of("22/11/2026", "Head CT", "", "Control")), Screen.rows(screen.getOrdersTable()));
    }

    @Test
    @DisplayName("TC06C-30 Back with an empty form runs its action without asking")
    void tc06c30BackWithEmptyForm() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Act
        Screen.button(screen, "Back").doClick();

        // Assert
        assertAll(
                () -> assertEquals(1, backs),
                () -> assertTrue(questions.isEmpty()));
    }

    @Test
    @DisplayName("TC06C-31 Back with an unsaved indication asks; answering No stays and keeps the text")
    void tc06c31BackWithIndicationAnsweredNo() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);
        field(screen, INDICATION).setText("Headache after a fall");
        answer = false;

        // Act
        Screen.button(screen, "Back").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of(DISCARD), questions),
                () -> assertEquals(0, backs),
                () -> assertEquals("Headache after a fall", field(screen, INDICATION).getText()));
    }

    @Test
    @DisplayName("TC06C-32 Back with only an unsaved justification asks; answering Yes runs its action")
    void tc06c32BackWithJustificationAnsweredYes() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);
        field(screen, JUSTIFICATION).setText("Rule out bleeding");

        // Act
        Screen.button(screen, "Back").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of(DISCARD), questions),
                () -> assertEquals(1, backs));
    }

    @Test
    @DisplayName("TC06C-33 Back with text but no patient selected is not an unsaved change and does not ask")
    void tc06c33BackWithTextButNoPatient() {
        // Arrange
        IU_OrderProcedure screen = open();
        field(screen, INDICATION).setText("Headache after a fall");

        // Act
        Screen.button(screen, "Back").doClick();

        // Assert
        assertAll(
                () -> assertTrue(questions.isEmpty()),
                () -> assertEquals(1, backs));
    }

    @Test
    @DisplayName("TC06C-34 Choosing another patient with unsaved data asks; No keeps the patient, row and text")
    void tc06c34OtherPatientAnsweredNo() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);
        field(screen, INDICATION).setText("Headache after a fall");
        answer = false;

        // Act
        select(screen, MARIO);

        // Assert
        assertAll(
                () -> assertEquals(List.of(DISCARD), questions),
                () -> assertEquals("P-0001", screen.getSelectedPatient().getPatientId()),
                () -> assertEquals(LUCIA, screen.getPatientsTable().getSelectedRow()),
                () -> assertEquals("Headache after a fall", field(screen, INDICATION).getText()),
                () -> assertEquals(List.of("P-0001"), orders.reads()));
    }

    @Test
    @DisplayName("TC06C-35 Choosing another patient with unsaved data asks; Yes loads the new patient, form empty")
    void tc06c35OtherPatientAnsweredYes() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);
        field(screen, JUSTIFICATION).setText("Rule out bleeding");

        // Act
        select(screen, MARIO);

        // Assert
        assertAll(
                () -> assertEquals(List.of(DISCARD), questions),
                () -> assertEquals("P-0002", screen.getSelectedPatient().getPatientId()),
                () -> assertEquals("", field(screen, JUSTIFICATION).getText()),
                () -> assertEquals(0, screen.getOrdersTable().getRowCount()),
                () -> assertEquals(List.of("P-0001", "P-0002"), orders.reads()));
    }

    @Test
    @DisplayName("TC06C-36 Choosing another patient with an empty form does not ask")
    void tc06c36OtherPatientWithEmptyForm() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Act
        select(screen, MARIO);

        // Assert
        assertAll(
                () -> assertTrue(questions.isEmpty()),
                () -> assertEquals("P-0002", screen.getSelectedPatient().getPatientId()));
    }

    @Test
    @DisplayName("TC06C-37 The buttons have their keys: Place order Alt+P, Search Alt+H, Back Alt+B")
    void tc06c37Mnemonics() {
        // Act
        IU_OrderProcedure screen = open();

        // Assert
        assertAll(
                () -> assertEquals(KeyEvent.VK_P, Screen.button(screen, PLACE_ORDER).getMnemonic()),
                () -> assertEquals(KeyEvent.VK_H, Screen.button(screen, "Search").getMnemonic()),
                () -> assertEquals(KeyEvent.VK_B, Screen.button(screen, "Back").getMnemonic()));
    }
}
