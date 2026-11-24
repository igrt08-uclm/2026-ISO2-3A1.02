package es.uclm.esi.iso2.client.orders.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.orders.domain.FollowUpOrder;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the screen {@link IU_OrderFollowUp} of UC07 (#70): the patients and their orders with the interval, the
 * test types that are active and eligible for follow-up, the inactive patient, the recurrence interval (a spinner from
 * 0 to 120 months), the validation of the client, the order placed, the errors of the server and the denied access, the
 * unsaved changes, the keys and the orders that cannot be edited (D-039). The cases are those of
 * {@code doc/testing/test-cases/UC07-client-test-design.md}. The screen is only built, never shown, so the tests need
 * no display; every question is answered by the test, which also records it.
 */
@DisplayName("IU_OrderFollowUp")
class IU_OrderFollowUpTest {
    private static final String REQUESTER = "mgarcia";
    private static final String TEST_TYPE = "Test type";
    private static final String INTERVAL = "Interval (months)";
    private static final String INDICATION = "Clinical indication";
    private static final String JUSTIFICATION = "Justification";
    private static final String PLACE_ORDER = "Place order";
    private static final String DISCARD = "Discard the unsaved changes?";
    private static final String DENIED = "⚠ You do not have permission to order procedures.";
    private static final String NO_TYPES = "⚠ There is no active test type eligible for follow-up.";
    private static final String INTERVAL_NOT_POSITIVE = "The recurrence interval must be a positive number of months";
    private static final String INDICATION_REQUIRED = "The clinical indication is required";
    private static final String JUSTIFICATION_REQUIRED = "The justification is required";
    private static final String MRI_KNEE = "MRI-KNEE · Knee MRI";
    private static final Color GREEN = new Color(0, 128, 0);
    private static final Color RED = new Color(180, 0, 0);
    private static final int LUCIA = 0;
    private static final int MARIO = 1;
    private static final int INES = 2;
    private static final List<String> LUCIA_FOLLOW_UP =
            List.of("20/11/2026", "Bone densitometry", "6", "Osteoporosis treatment");
    private static final List<String> LUCIA_DIAGNOSTIC = List.of("21/11/2026", "Chest X-ray", "", "Persistent cough");

    private FollowUpOrderServiceFake orders;
    private FollowUpPatientServiceFake patients;
    private FollowUpParameterServiceFake parameters;
    private List<String> questions;
    private boolean answer;
    private int backs;

    @BeforeEach
    void setUp() {
        orders = FollowUpOrderServiceFake.withTwoOrdersOfLucia();
        patients = FollowUpPatientServiceFake.withThreePatients();
        parameters = FollowUpParameterServiceFake.withSeedData();
        questions = new ArrayList<>();
        answer = true;
        backs = 0;
    }

    private IU_OrderFollowUp open() {
        return new IU_OrderFollowUp(orders, patients, parameters, REQUESTER, question -> {
            questions.add(question);
            return answer;
        }, () -> backs++);
    }

    private static IU_OrderFollowUp select(IU_OrderFollowUp screen, int row) {
        screen.getPatientsTable().setRowSelectionInterval(row, row);
        return screen;
    }

    private static JTextField field(IU_OrderFollowUp screen, String label) {
        return FollowUpScreen.field(screen, label);
    }

    private static JComboBox<Object> combo(IU_OrderFollowUp screen) {
        return FollowUpScreen.combo(screen, TEST_TYPE);
    }

    private static JSpinner spinner(IU_OrderFollowUp screen) {
        return FollowUpScreen.spinner(screen, INTERVAL);
    }

    private static void fill(IU_OrderFollowUp screen, String testType, int interval, String indication,
            String justification) {
        FollowUpScreen.choose(combo(screen), testType);
        spinner(screen).setValue(interval);
        field(screen, INDICATION).setText(indication);
        field(screen, JUSTIFICATION).setText(justification);
    }

    private static void placeOrder(IU_OrderFollowUp screen) {
        FollowUpScreen.button(screen, PLACE_ORDER).doClick();
    }

    private static Color statusColour(IU_OrderFollowUp screen) {
        return FollowUpScreen.label(screen, screen.getStatusMessage()).getForeground();
    }

    private static boolean formEnabled(IU_OrderFollowUp screen) {
        return combo(screen).isEnabled() || spinner(screen).isEnabled() || field(screen, INDICATION).isEnabled()
                || field(screen, JUSTIFICATION).isEnabled() || screen.getPlaceOrderButton().isEnabled();
    }

    private static boolean formAllEnabled(IU_OrderFollowUp screen) {
        return combo(screen).isEnabled() && spinner(screen).isEnabled() && field(screen, INDICATION).isEnabled()
                && field(screen, JUSTIFICATION).isEnabled() && screen.getPlaceOrderButton().isEnabled();
    }

    /** Types a text in the box of the spinner and commits it, as the user does when the box loses the focus. */
    private static void typeInterval(IU_OrderFollowUp screen, String text) throws ParseException {
        JFormattedTextField box = ((JSpinner.DefaultEditor) spinner(screen).getEditor()).getTextField();
        box.setText(text);
        box.commitEdit();
    }

    /** A filled form for Lucía that the client accepts. */
    private IU_OrderFollowUp validFormOfLucia() {
        IU_OrderFollowUp screen = select(open(), LUCIA);
        fill(screen, MRI_KNEE, 6, "Meniscus repair", "Check the evolution");
        return screen;
    }

    @Test
    @DisplayName("TC07C-01 On opening, every patient is listed and the form stays disabled until one is chosen")
    void tc07c01OpeningListsThePatients() {
        // Act
        IU_OrderFollowUp screen = open();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Identifier", "Full name", "Date of birth"),
                        FollowUpScreen.columns(screen.getPatientsTable())),
                () -> assertEquals(List.of(List.of("HC-0000001", "Lucía Ejemplo Prueba", "14/03/1980"),
                        List.of("HC-0000002", "Mario Demo Simulado", "02/11/1975"),
                        List.of("HC-0000009", "Inés Inactiva Inventada", "")),
                        FollowUpScreen.rows(screen.getPatientsTable())),
                () -> assertEquals("3 patients found.", FollowUpScreen.found(screen)),
                () -> assertEquals(List.of(""), patients.searches()),
                () -> assertNull(screen.getSelectedPatient()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertEquals(List.of("Date", "Test type", INTERVAL, "Indication"),
                        FollowUpScreen.columns(screen.getOrdersTable())),
                () -> assertEquals(0, screen.getOrdersTable().getRowCount()),
                () -> assertTrue(FollowUpScreen.button(screen, "Back").isEnabled()),
                () -> assertEquals(" ", screen.getStatusMessage()),
                () -> assertTrue(orders.requesters().isEmpty()));
    }

    @Test
    @DisplayName("TC07C-02 Search finds one patient (button), none (Enter) and sends the text without end spaces")
    void tc07c02SearchFindsOneOrNone() {
        // Arrange
        IU_OrderFollowUp screen = open();

        // Act
        screen.getSearchField().setText("  mario ");
        FollowUpScreen.button(screen, "Search").doClick();
        List<List<String>> one = FollowUpScreen.rows(screen.getPatientsTable());
        String foundOne = FollowUpScreen.found(screen);
        screen.getSearchField().setText("zzz");
        screen.getSearchField().postActionEvent();

        // Assert
        assertAll(
                () -> assertEquals(List.of(List.of("HC-0000002", "Mario Demo Simulado", "02/11/1975")), one),
                () -> assertEquals("1 patient found.", foundOne),
                () -> assertEquals(0, screen.getPatientsTable().getRowCount()),
                () -> assertEquals("0 patients found.", FollowUpScreen.found(screen)),
                () -> assertEquals(List.of("", "mario", "zzz"), patients.searches()));
    }

    @Test
    @DisplayName("TC07C-03 An active patient enables the form and lists their orders with the interval column")
    void tc07c03ActivePatientShowsTheOrdersWithTheInterval() {
        // Act
        IU_OrderFollowUp screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertEquals("P-0001", screen.getSelectedPatient().getPatientId()),
                () -> assertTrue(formAllEnabled(screen)),
                () -> assertEquals(List.of(LUCIA_FOLLOW_UP, LUCIA_DIAGNOSTIC),
                        FollowUpScreen.rows(screen.getOrdersTable())),
                () -> assertEquals(List.of("P-0001"), orders.reads()),
                () -> assertEquals(List.of(REQUESTER), orders.requesters()),
                () -> assertEquals(6, spinner(screen).getValue()),
                () -> assertEquals("", field(screen, INDICATION).getText()),
                () -> assertEquals("", field(screen, JUSTIFICATION).getText()),
                () -> assertEquals(" ", screen.getStatusMessage()));
    }

    @Test
    @DisplayName("TC07C-04 An active patient without orders gets the form enabled and an empty list of orders")
    void tc07c04PatientWithoutOrders() {
        // Act
        IU_OrderFollowUp screen = select(open(), MARIO);

        // Assert
        assertAll(
                () -> assertEquals("P-0002", screen.getSelectedPatient().getPatientId()),
                () -> assertTrue(formAllEnabled(screen)),
                () -> assertEquals(0, screen.getOrdersTable().getRowCount()));
    }

    @Test
    @DisplayName("TC07C-05 Only the test types that are active and eligible for follow-up are offered, as id · name")
    void tc07c05OnlyActiveAndEligibleTestTypes() {
        // Act
        IU_OrderFollowUp screen = select(open(), LUCIA);

        // Assert
        assertEquals(List.of("XRAY-CHEST · Chest X-ray", MRI_KNEE, "US-ABDOMEN · Abdominal ultrasound",
                "MAMMO-BILATERAL · Bilateral mammography", "DEXA-SPINE · Bone densitometry"),
                FollowUpScreen.items(combo(screen)));
    }

    @Test
    @DisplayName("TC07C-06 Without an active test type eligible for follow-up the form stays disabled and says why")
    void tc07c06NoActiveEligibleTestType() {
        // Arrange
        parameters.testTypes().replaceAll(type -> type.isFollowUpEligible() ? type.deactivated() : type);

        // Act
        IU_OrderFollowUp screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertEquals(0, combo(screen).getItemCount()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertEquals(NO_TYPES, screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals(List.of(LUCIA_FOLLOW_UP, LUCIA_DIAGNOSTIC),
                        FollowUpScreen.rows(screen.getOrdersTable())));
    }

    @Test
    @DisplayName("TC07C-07 The form has test type, interval, indication and justification, and no priority")
    void tc07c07FormHasNoPriority() {
        // Act
        IU_OrderFollowUp screen = open();
        List<String> labels = FollowUpScreen.all(screen, JLabel.class).stream().map(JLabel::getText).toList();

        // Assert
        assertAll(
                () -> assertTrue(labels.containsAll(List.of(TEST_TYPE, INTERVAL, INDICATION, JUSTIFICATION))),
                () -> assertFalse(labels.contains("Priority")),
                () -> assertEquals(1, FollowUpScreen.all(screen, JComboBox.class).size()));
    }

    @Test
    @DisplayName("TC07C-08 An inactive patient is explained, the form is disabled and no order can be placed")
    void tc07c08InactivePatientBlocksTheOrder() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);

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
                () -> assertTrue(orders.followUpsSent().isEmpty()));
    }

    @Test
    @DisplayName("TC07C-09 An identifier that no patient has shows an error and leaves the form disabled")
    void tc07c09UnknownPatient() {
        // Arrange
        IU_OrderFollowUp screen = open();

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
    @DisplayName("TC07C-10 Submitting with no patient and an empty form lists every error and sends nothing")
    void tc07c10EmptyFormWithoutPatient() {
        // Arrange
        IU_OrderFollowUp screen = open();

        // Act
        screen.onSubmitOrder();

        // Assert
        assertAll(
                () -> assertEquals("⚠ Select a patient before placing an order. The test type is required "
                        + INDICATION_REQUIRED + " " + JUSTIFICATION_REQUIRED, screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertTrue(orders.followUpsSent().isEmpty()));
    }

    @Test
    @DisplayName("TC07C-11 An empty indication and justification give both errors together and send nothing")
    void tc07c11EmptyIndicationAndJustification() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ " + INDICATION_REQUIRED + " " + JUSTIFICATION_REQUIRED,
                        screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertTrue(orders.followUpsSent().isEmpty()));
    }

    @Test
    @DisplayName("TC07C-12 An indication and a justification of only spaces are rejected as empty")
    void tc07c12BlankIndicationAndJustification() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);
        fill(screen, MRI_KNEE, 6, "   ", " ");

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ " + INDICATION_REQUIRED + " " + JUSTIFICATION_REQUIRED,
                        screen.getStatusMessage()),
                () -> assertTrue(orders.followUpsSent().isEmpty()));
    }

    @Test
    @DisplayName("TC07C-13 Only the indication blank gives only its error and keeps the justification")
    void tc07c13OnlyIndicationBlank() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);
        fill(screen, MRI_KNEE, 6, "  ", "Check the evolution");

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ " + INDICATION_REQUIRED, screen.getStatusMessage()),
                () -> assertEquals("Check the evolution", field(screen, JUSTIFICATION).getText()),
                () -> assertTrue(orders.followUpsSent().isEmpty()));
    }

    @Test
    @DisplayName("TC07C-14 Only the justification empty gives only its error and keeps the indication")
    void tc07c14OnlyJustificationEmpty() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);
        fill(screen, MRI_KNEE, 6, "Meniscus repair", "");

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ " + JUSTIFICATION_REQUIRED, screen.getStatusMessage()),
                () -> assertEquals("Meniscus repair", field(screen, INDICATION).getText()),
                () -> assertTrue(orders.followUpsSent().isEmpty()));
    }

    @Test
    @DisplayName("TC07C-15 An interval of 0 months shows the text of the server, sends nothing, keeps the data")
    void tc07c15IntervalZeroIsRejected() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);
        fill(screen, MRI_KNEE, 0, "Meniscus repair", "Check the evolution");

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ " + INTERVAL_NOT_POSITIVE, screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertTrue(orders.followUpsSent().isEmpty()),
                () -> assertEquals(0, spinner(screen).getValue()),
                () -> assertEquals("Meniscus repair", field(screen, INDICATION).getText()),
                () -> assertEquals("Check the evolution", field(screen, JUSTIFICATION).getText()));
    }

    @Test
    @DisplayName("TC07C-16 An interval of 0 and empty texts give the three errors in the order of the form")
    void tc07c16IntervalZeroAndEmptyTexts() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);
        spinner(screen).setValue(0);

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ " + INTERVAL_NOT_POSITIVE + " " + INDICATION_REQUIRED + " "
                        + JUSTIFICATION_REQUIRED, screen.getStatusMessage()),
                () -> assertTrue(orders.followUpsSent().isEmpty()));
    }

    @Test
    @DisplayName("TC07C-17 The spinner goes from 0 to 120 months in steps of 1: no value below 0 or above 120")
    void tc07c17SpinnerBoundaries() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);
        SpinnerNumberModel model = (SpinnerNumberModel) spinner(screen).getModel();

        // Act
        model.setValue(0);
        Object belowZero = model.getPreviousValue();
        Object aboveZero = model.getNextValue();
        model.setValue(120);
        Object below120 = model.getPreviousValue();
        Object above120 = model.getNextValue();

        // Assert
        assertAll(
                () -> assertEquals(spinner(screen), screen.getIntervalSpinner()),
                () -> assertEquals(0, model.getMinimum()),
                () -> assertEquals(120, model.getMaximum()),
                () -> assertEquals(1, model.getStepSize()),
                () -> assertNull(belowZero),
                () -> assertEquals(1, aboveZero),
                () -> assertEquals(119, below120),
                () -> assertNull(above120));
    }

    @Test
    @DisplayName("TC07C-18 Typed in the spinner, -1, 121, abc and 99999999999 are refused; 0 and 120 are taken")
    void tc07c18TypedIntervals() throws ParseException {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);
        List<Object> kept = new ArrayList<>();

        // Act
        for (String refused : List.of("-1", "121", "abc", "99999999999")) {
            assertThrows(ParseException.class, () -> typeInterval(screen, refused), refused);
            kept.add(spinner(screen).getValue());
        }
        typeInterval(screen, "0");
        Object zero = spinner(screen).getValue();
        typeInterval(screen, "120");

        // Assert
        assertAll(
                () -> assertEquals(List.of(6, 6, 6, 6), kept),
                () -> assertEquals(0, zero),
                () -> assertEquals(120, spinner(screen).getValue()),
                () -> assertTrue(orders.followUpsSent().isEmpty()));
    }

    @Test
    @DisplayName("TC07C-19 A valid order of 1 month sends exactly one orderFollowUp and reloads the list of orders")
    void tc07c19ValidOrder() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);
        fill(screen, MRI_KNEE, 1, "  Meniscus repair ", " Check the evolution  ");
        String today = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDate.now());

        // Act
        placeOrder(screen);

        // Assert
        FollowUpOrder sent = orders.followUpsSent().get(0);
        assertAll(
                () -> assertEquals(1, orders.followUpsSent().size()),
                () -> assertTrue(orders.diagnosticsSent().isEmpty()),
                () -> assertNull(sent.getOrderId()),
                () -> assertEquals("P-0001", sent.getPatientId()),
                () -> assertEquals("MRI-KNEE", sent.getTestTypeId()),
                () -> assertEquals(REQUESTER, sent.getRequester()),
                () -> assertEquals("Meniscus repair", sent.getIndication()),
                () -> assertEquals("Check the evolution", sent.getJustification()),
                () -> assertNull(sent.getCreationDate()),
                () -> assertEquals(1, sent.getRecurrenceIntervalMonths()),
                () -> assertEquals(List.of(REQUESTER, REQUESTER, REQUESTER), orders.requesters()),
                () -> assertEquals("Order ORD-0003 created.", screen.getStatusMessage()),
                () -> assertEquals(GREEN, statusColour(screen)),
                () -> assertEquals("", field(screen, INDICATION).getText()),
                () -> assertEquals("", field(screen, JUSTIFICATION).getText()),
                () -> assertEquals(6, spinner(screen).getValue()),
                () -> assertEquals("P-0001", screen.getSelectedPatient().getPatientId()),
                () -> assertEquals(List.of("P-0001", "P-0001"), orders.reads()),
                () -> assertEquals(List.of(LUCIA_FOLLOW_UP, LUCIA_DIAGNOSTIC,
                        List.of(today, "Knee MRI", "1", "Meniscus repair")),
                        FollowUpScreen.rows(screen.getOrdersTable())));
    }

    @Test
    @DisplayName("TC07C-20 An interval of 12 months, the year of D-040, and texts of one character are sent")
    void tc07c20TwelveMonthsAndOneCharacterTexts() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), MARIO);
        fill(screen, "DEXA-SPINE · Bone densitometry", 12, "a", "b");

        // Act
        placeOrder(screen);

        // Assert
        FollowUpOrder sent = orders.followUpsSent().get(0);
        assertAll(
                () -> assertEquals(1, orders.followUpsSent().size()),
                () -> assertEquals("P-0002", sent.getPatientId()),
                () -> assertEquals("DEXA-SPINE", sent.getTestTypeId()),
                () -> assertEquals(12, sent.getRecurrenceIntervalMonths()),
                () -> assertEquals("a", sent.getIndication()),
                () -> assertEquals("b", sent.getJustification()),
                () -> assertEquals("Order ORD-0003 created.", screen.getStatusMessage()),
                () -> assertEquals(1, screen.getOrdersTable().getRowCount()),
                () -> assertEquals("12", FollowUpScreen.rows(screen.getOrdersTable()).get(0).get(2)));
    }

    @Test
    @DisplayName("TC07C-21 The errors of the server are shown together; the list and the form do not change")
    void tc07c21ServerRejectsWithTwoErrors() {
        // Arrange
        IU_OrderFollowUp screen = validFormOfLucia();
        orders.rejectNextOrder("The patient P-0001 is not active",
                "The test type MRI-KNEE is not eligible for follow-up");

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ The patient P-0001 is not active The test type MRI-KNEE is not eligible for "
                        + "follow-up", screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals(1, orders.followUpsSent().size()),
                () -> assertEquals(List.of("P-0001"), orders.reads()),
                () -> assertEquals(List.of(LUCIA_FOLLOW_UP, LUCIA_DIAGNOSTIC),
                        FollowUpScreen.rows(screen.getOrdersTable())),
                () -> assertEquals("Meniscus repair", field(screen, INDICATION).getText()),
                () -> assertEquals("Check the evolution", field(screen, JUSTIFICATION).getText()),
                () -> assertEquals(6, spinner(screen).getValue()));
    }

    @Test
    @DisplayName("TC07C-22 One error of the server is shown alone, as it comes")
    void tc07c22ServerRejectsWithOneError() {
        // Arrange
        IU_OrderFollowUp screen = validFormOfLucia();
        orders.rejectNextOrder(INTERVAL_NOT_POSITIVE);

        // Act
        placeOrder(screen);

        // Assert
        assertEquals("⚠ " + INTERVAL_NOT_POSITIVE, screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC07C-23 A failure of the server while ordering is shown; the list and the form do not change")
    void tc07c23ServerFailsWhileOrdering() {
        // Arrange
        IU_OrderFollowUp screen = validFormOfLucia();
        orders.failNextOrder(new IllegalStateException("The server cannot be reached"));

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ The order could not be created: The server cannot be reached",
                        screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals(2, screen.getOrdersTable().getRowCount()),
                () -> assertEquals("Meniscus repair", field(screen, INDICATION).getText()));
    }

    @Test
    @DisplayName("TC07C-24 A denied order shows the denied message and nothing more reaches the services")
    void tc07c24DeniedWhileOrdering() {
        // Arrange
        IU_OrderFollowUp screen = validFormOfLucia();
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
                () -> assertTrue(FollowUpScreen.button(screen, "Back").isEnabled()),
                () -> assertTrue(orders.followUpsSent().isEmpty()),
                () -> assertEquals(ordersCalls, orders.requesters().size()),
                () -> assertEquals(searches, patients.searches().size()),
                () -> assertEquals(2, screen.getOrdersTable().getRowCount()));
    }

    @Test
    @DisplayName("TC07C-25 A user without the permission gets the denied message on opening; Back still works")
    void tc07c25DeniedOnOpening() {
        // Arrange
        patients.denyAll();

        // Act
        IU_OrderFollowUp screen = open();
        FollowUpScreen.button(screen, "Back").doClick();

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
    @DisplayName("TC07C-26 A denied reading of the orders of the patient gives the denied message, form disabled")
    void tc07c26DeniedWhileReadingTheOrders() {
        // Arrange
        orders.denyAll();

        // Act
        IU_OrderFollowUp screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertFalse(screen.getPatientsTable().isEnabled()),
                () -> assertTrue(FollowUpScreen.button(screen, "Back").isEnabled()));
    }

    @Test
    @DisplayName("TC07C-27 A reading of the orders rejected by the server shows its reason and the form stays disabled")
    void tc07c27ValidationErrorWhileReadingTheOrders() {
        // Arrange
        orders.failReads(new ValidationException(List.of("There is no patient with the id P-0001")));

        // Act
        IU_OrderFollowUp screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertEquals("⚠ There is no patient with the id P-0001", screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertFalse(formEnabled(screen)));
    }

    @Test
    @DisplayName("TC07C-28 A failure while reading the orders is shown and the form stays disabled")
    void tc07c28FailureWhileReadingTheOrders() {
        // Arrange
        orders.failReads(new IllegalStateException("The server cannot be reached"));

        // Act
        IU_OrderFollowUp screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertEquals("⚠ The orders could not be read: The server cannot be reached",
                        screen.getStatusMessage()),
                () -> assertFalse(formEnabled(screen)));
    }

    @Test
    @DisplayName("TC07C-29 A failure while searching the patients is shown and the search box stays enabled")
    void tc07c29FailureWhileReadingThePatients() {
        // Arrange
        patients.failSearches(new IllegalStateException("The server cannot be reached"));

        // Act
        IU_OrderFollowUp screen = open();

        // Assert
        assertAll(
                () -> assertEquals("⚠ The patients could not be read: The server cannot be reached",
                        screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertEquals(0, screen.getPatientsTable().getRowCount()),
                () -> assertTrue(screen.getSearchField().isEnabled()));
    }

    @Test
    @DisplayName("TC07C-30 No cell of the tables is editable and no button edits or cancels an order (D-039)")
    void tc07c30OrdersCannotBeEdited() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);

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
                () -> assertEquals(List.of("Search", PLACE_ORDER, "Back"), FollowUpScreen.buttonTexts(screen)));
    }

    @Test
    @DisplayName("TC07C-31 An order without date and of a test type not in the catalogue is listed with its id")
    void tc07c31UnusualOrdersAreListed() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);

        // Act
        screen.displayOrders(List.of(new FollowUpOrder("ORD-0004", "P-0001", "PET-BODY", REQUESTER, "Staging",
                "Yearly control", null, 3)));

        // Assert
        assertEquals(List.of(List.of("", "PET-BODY", "3", "Staging")), FollowUpScreen.rows(screen.getOrdersTable()));
    }

    @Test
    @DisplayName("TC07C-32 Back with an empty form runs its action without asking")
    void tc07c32BackWithEmptyForm() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);

        // Act
        FollowUpScreen.button(screen, "Back").doClick();

        // Assert
        assertAll(
                () -> assertTrue(questions.isEmpty()),
                () -> assertEquals(1, backs));
    }

    @Test
    @DisplayName("TC07C-33 Back with the indication written asks; No stays on the screen and keeps the text")
    void tc07c33BackWithIndicationAnsweredNo() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);
        field(screen, INDICATION).setText("Meniscus repair");
        answer = false;

        // Act
        FollowUpScreen.button(screen, "Back").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of(DISCARD), questions),
                () -> assertEquals(0, backs),
                () -> assertEquals("Meniscus repair", field(screen, INDICATION).getText()));
    }

    @Test
    @DisplayName("TC07C-34 Back with only the justification written asks; Yes leaves the screen")
    void tc07c34BackWithJustificationAnsweredYes() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);
        field(screen, JUSTIFICATION).setText("Check the evolution");

        // Act
        FollowUpScreen.button(screen, "Back").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of(DISCARD), questions),
                () -> assertEquals(1, backs));
    }

    @Test
    @DisplayName("TC07C-35 Back with text but no patient selected does not ask")
    void tc07c35BackWithTextButNoPatient() {
        // Arrange
        IU_OrderFollowUp screen = open();
        field(screen, INDICATION).setText("Meniscus repair");

        // Act
        FollowUpScreen.button(screen, "Back").doClick();

        // Assert
        assertAll(
                () -> assertTrue(questions.isEmpty()),
                () -> assertEquals(1, backs));
    }

    @Test
    @DisplayName("TC07C-36 Choosing another patient with unsaved data asks; No keeps the patient and the text")
    void tc07c36OtherPatientAnsweredNo() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);
        field(screen, INDICATION).setText("Meniscus repair");
        answer = false;

        // Act
        select(screen, MARIO);

        // Assert
        assertAll(
                () -> assertEquals(List.of(DISCARD), questions),
                () -> assertEquals("P-0001", screen.getSelectedPatient().getPatientId()),
                () -> assertEquals(LUCIA, screen.getPatientsTable().getSelectedRow()),
                () -> assertEquals("Meniscus repair", field(screen, INDICATION).getText()),
                () -> assertEquals(List.of("P-0001"), orders.reads()));
    }

    @Test
    @DisplayName("TC07C-37 Choosing another patient with unsaved data asks; Yes loads the new patient, form empty")
    void tc07c37OtherPatientAnsweredYes() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);
        field(screen, JUSTIFICATION).setText("Check the evolution");
        spinner(screen).setValue(3);

        // Act
        select(screen, MARIO);

        // Assert
        assertAll(
                () -> assertEquals(List.of(DISCARD), questions),
                () -> assertEquals("P-0002", screen.getSelectedPatient().getPatientId()),
                () -> assertEquals("", field(screen, JUSTIFICATION).getText()),
                () -> assertEquals(6, spinner(screen).getValue()),
                () -> assertEquals(List.of("P-0001", "P-0002"), orders.reads()));
    }

    @Test
    @DisplayName("TC07C-38 Choosing another patient with an empty form does not ask")
    void tc07c38OtherPatientWithEmptyForm() {
        // Arrange
        IU_OrderFollowUp screen = select(open(), LUCIA);

        // Act
        select(screen, MARIO);

        // Assert
        assertAll(
                () -> assertTrue(questions.isEmpty()),
                () -> assertEquals("P-0002", screen.getSelectedPatient().getPatientId()));
    }

    @Test
    @DisplayName("TC07C-39 The buttons have their keys: Place order Alt+P, Search Alt+H, Back Alt+B")
    void tc07c39Mnemonics() {
        // Act
        IU_OrderFollowUp screen = open();

        // Assert
        assertAll(
                () -> assertEquals(KeyEvent.VK_P, FollowUpScreen.button(screen, PLACE_ORDER).getMnemonic()),
                () -> assertEquals(KeyEvent.VK_H, FollowUpScreen.button(screen, "Search").getMnemonic()),
                () -> assertEquals(KeyEvent.VK_B, FollowUpScreen.button(screen, "Back").getMnemonic()));
    }

    @Test
    @DisplayName("TC07C-45 A part of an identifier that every patient has is not a patient: error, nothing read")
    void tc07c45PartialIdentifier() {
        // Arrange
        IU_OrderFollowUp screen = open();

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
    @DisplayName("TC07C-46 The permission of C3 is lost after opening: selecting a patient shows the denied message")
    void tc07c46PermissionLostBeforeSelecting() {
        // Arrange
        IU_OrderFollowUp screen = open();
        patients.denyAll();

        // Act
        select(screen, LUCIA);

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertEquals(RED, statusColour(screen)),
                () -> assertNull(screen.getSelectedPatient()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertFalse(screen.getPatientsTable().isEnabled()),
                () -> assertFalse(screen.getSearchField().isEnabled()),
                () -> assertFalse(FollowUpScreen.button(screen, "Search").isEnabled()),
                () -> assertTrue(FollowUpScreen.button(screen, "Back").isEnabled()),
                () -> assertTrue(orders.requesters().isEmpty()));
    }

    @Test
    @DisplayName("TC07C-47 After a denied order the button Search is disabled and only Back stays enabled")
    void tc07c47SearchDisabledAfterDeniedOrder() {
        // Arrange
        IU_OrderFollowUp screen = validFormOfLucia();
        orders.denyAll();

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertFalse(FollowUpScreen.button(screen, "Search").isEnabled()),
                () -> assertFalse(screen.getPlaceOrderButton().isEnabled()),
                () -> assertTrue(FollowUpScreen.button(screen, "Back").isEnabled()));
    }

    @Test
    @DisplayName("TC07C-48 Denied on opening, the button Search is disabled and only Back stays enabled")
    void tc07c48SearchDisabledWhenDeniedOnOpening() {
        // Arrange
        patients.denyAll();

        // Act
        IU_OrderFollowUp screen = open();

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertFalse(FollowUpScreen.button(screen, "Search").isEnabled()),
                () -> assertFalse(screen.getPlaceOrderButton().isEnabled()),
                () -> assertTrue(FollowUpScreen.button(screen, "Back").isEnabled()));
    }
}
