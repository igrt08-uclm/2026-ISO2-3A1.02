package es.uclm.esi.iso2.client.orders.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.orders.domain.FollowUpOrder;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cases of the UC06 screen {@link IU_OrderProcedure} run again in the branch of UC07, which was created before the
 * tests of UC06 were written: without them the module {@code client-c4-orders} would not reach the coverage of D-033.
 * They are cases of {@code doc/testing/test-cases/UC06-client-test-design.md} (same ids) whose expected result was
 * not changed by the fixes F-01 to F-05 of UC06: the priority is chosen by its position and never read as a text, the
 * button <em>Search</em> and the borders are not checked, so the class stays green when it meets the fixed screen. It
 * also checks that a follow-up order of UC07 is listed in the screen of UC06 without a priority. They use the fakes of
 * UC07; the screen is only built, never shown.
 */
@DisplayName("IU_OrderProcedure in the branch of UC07")
class IU_OrderProcedureInUc07Test {
    private static final String REQUESTER = "mgarcia";
    private static final String INDICATION = "Clinical indication";
    private static final String JUSTIFICATION = "Justification";
    private static final String DISCARD = "Discard the unsaved changes?";
    private static final String DENIED = "⚠ You do not have permission to order procedures.";
    private static final String NO_TYPES = "⚠ There are no active test types or priorities to order with.";
    private static final int URGENT = 0;
    private static final int LUCIA = 0;
    private static final int MARIO = 1;
    private static final int INES = 2;

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
        return FollowUpScreen.field(screen, label);
    }

    private static JComboBox<Object> combo(IU_OrderProcedure screen, String label) {
        return FollowUpScreen.combo(screen, label);
    }

    private static boolean formEnabled(IU_OrderProcedure screen) {
        return combo(screen, "Test type").isEnabled() || combo(screen, "Priority").isEnabled()
                || field(screen, INDICATION).isEnabled() || field(screen, JUSTIFICATION).isEnabled()
                || screen.getPlaceOrderButton().isEnabled();
    }

    private static void placeOrder(IU_OrderProcedure screen) {
        FollowUpScreen.button(screen, "Place order").doClick();
    }

    /** A filled form for Lucía that the client accepts: a head CT, urgent. */
    private IU_OrderProcedure validFormOfLucia() {
        IU_OrderProcedure screen = select(open(), LUCIA);
        FollowUpScreen.choose(combo(screen, "Test type"), "CT-HEAD · Head CT");
        combo(screen, "Priority").setSelectedIndex(URGENT);
        field(screen, INDICATION).setText("  Headache after a fall ");
        field(screen, JUSTIFICATION).setText(" Rule out bleeding  ");
        return screen;
    }

    @Test
    @DisplayName("TC06C-01 On opening, every patient is listed and the form stays disabled until one is chosen")
    void tc06c01OpeningListsThePatients() {
        // Act
        IU_OrderProcedure screen = open();

        // Assert
        assertAll(
                () -> assertEquals(3, screen.getPatientsTable().getRowCount()),
                () -> assertEquals("3 patients found.", FollowUpScreen.found(screen)),
                () -> assertNull(screen.getSelectedPatient()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertEquals(" ", screen.getStatusMessage()),
                () -> assertTrue(orders.requesters().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-03 An active patient enables the form; a follow-up order of UC07 is listed without priority")
    void tc06c03ActivePatientListsAFollowUpOrder() {
        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertEquals("P-0001", screen.getSelectedPatient().getPatientId()),
                () -> assertTrue(formEnabled(screen)),
                () -> assertEquals(List.of("20/11/2026", "Bone densitometry", "", "Osteoporosis treatment"),
                        FollowUpScreen.rows(screen.getOrdersTable()).get(0)),
                () -> assertEquals(2, screen.getOrdersTable().getRowCount()),
                () -> assertEquals(List.of("P-0001"), orders.reads()));
    }

    @Test
    @DisplayName("TC06C-05 Only the active test types are offered, eligible for follow-up or not")
    void tc06c05OnlyActiveTestTypes() {
        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertEquals(List.of("XRAY-CHEST · Chest X-ray", "CT-HEAD · Head CT", "MRI-KNEE · Knee MRI",
                "US-ABDOMEN · Abdominal ultrasound", "MAMMO-BILATERAL · Bilateral mammography",
                "DEXA-SPINE · Bone densitometry"), FollowUpScreen.items(combo(screen, "Test type")));
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
                () -> assertFalse(formEnabled(screen)),
                () -> assertEquals(NO_TYPES, screen.getStatusMessage()));
    }

    @Test
    @DisplayName("TC06C-08 Without active priorities the form stays disabled and says why")
    void tc06c08NoActivePriorities() {
        // Arrange
        parameters.priorities().clear();

        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertFalse(formEnabled(screen)),
                () -> assertEquals(NO_TYPES, screen.getStatusMessage()));
    }

    @Test
    @DisplayName("TC06C-09 An inactive patient is explained and no order can be placed")
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
                () -> assertFalse(formEnabled(screen)),
                () -> assertTrue(orders.diagnosticsSent().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-10 An identifier that no patient has shows an error")
    void tc06c10UnknownPatient() {
        // Arrange
        IU_OrderProcedure screen = open();

        // Act
        screen.onSelectPatient("HC-9999999");

        // Assert
        assertAll(
                () -> assertEquals("⚠ There is no patient with the identifier HC-9999999", screen.getStatusMessage()),
                () -> assertTrue(orders.requesters().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-11 Submitting with no patient and an empty form lists every error and sends nothing")
    void tc06c11EmptyFormWithoutPatient() {
        // Arrange
        IU_OrderProcedure screen = open();

        // Act
        screen.onSubmitOrder();

        // Assert
        assertAll(
                () -> assertEquals("⚠ Select a patient before placing an order. The test type is required The "
                        + "priority is required The clinical indication is required The justification is required",
                        screen.getStatusMessage()),
                () -> assertTrue(orders.diagnosticsSent().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-14 Only the indication blank gives only its error")
    void tc06c14OnlyIndicationBlank() {
        // Arrange
        IU_OrderProcedure screen = validFormOfLucia();
        field(screen, INDICATION).setText("  ");

        // Act
        placeOrder(screen);

        // Assert
        assertAll(
                () -> assertEquals("⚠ The clinical indication is required", screen.getStatusMessage()),
                () -> assertTrue(orders.diagnosticsSent().isEmpty()));
    }

    @Test
    @DisplayName("TC06C-17 A valid order sends one diagnostic order, never a follow-up, and reloads the list")
    void tc06c17ValidOrder() {
        // Arrange
        IU_OrderProcedure screen = validFormOfLucia();

        // Act
        placeOrder(screen);

        // Assert
        DiagnosticOrder sent = orders.diagnosticsSent().get(0);
        assertAll(
                () -> assertEquals(1, orders.diagnosticsSent().size()),
                () -> assertTrue(orders.followUpsSent().isEmpty()),
                () -> assertEquals("P-0001", sent.getPatientId()),
                () -> assertEquals("CT-HEAD", sent.getTestTypeId()),
                () -> assertEquals("URGENT", sent.getPriorityId()),
                () -> assertEquals("Headache after a fall", sent.getIndication()),
                () -> assertEquals("Rule out bleeding", sent.getJustification()),
                () -> assertEquals("Order ORD-0003 created.", screen.getStatusMessage()),
                () -> assertEquals("", field(screen, INDICATION).getText()),
                () -> assertEquals(List.of("P-0001", "P-0001"), orders.reads()),
                () -> assertEquals(3, screen.getOrdersTable().getRowCount()));
    }

    @Test
    @DisplayName("TC06C-19 The errors of the server are shown together")
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
                () -> assertEquals(List.of("P-0001"), orders.reads()));
    }

    @Test
    @DisplayName("TC06C-21 A failure of the server while ordering is shown")
    void tc06c21ServerFailsWhileOrdering() {
        // Arrange
        IU_OrderProcedure screen = validFormOfLucia();
        orders.failNextOrder(new IllegalStateException("The server cannot be reached"));

        // Act
        placeOrder(screen);

        // Assert
        assertEquals("⚠ The order could not be created: The server cannot be reached", screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC06C-22 A denied order shows the denied message and nothing more reaches the services")
    void tc06c22DeniedWhileOrdering() {
        // Arrange
        IU_OrderProcedure screen = validFormOfLucia();
        orders.denyAll();

        // Act
        placeOrder(screen);
        int ordersCalls = orders.requesters().size();
        screen.onSearch();
        screen.onSelectPatient("HC-0000002");
        screen.onSubmitOrder();

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertFalse(formEnabled(screen)),
                () -> assertFalse(screen.getSearchField().isEnabled()),
                () -> assertEquals(ordersCalls, orders.requesters().size()));
    }

    @Test
    @DisplayName("TC06C-23 A user without the permission gets the denied message on opening; Back still works")
    void tc06c23DeniedOnOpening() {
        // Arrange
        patients.denyAll();

        // Act
        IU_OrderProcedure screen = open();
        FollowUpScreen.button(screen, "Back").doClick();

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertEquals(1, backs),
                () -> assertTrue(questions.isEmpty()));
    }

    @Test
    @DisplayName("TC06C-24 A denied reading of the orders of the patient gives the denied message")
    void tc06c24DeniedWhileReadingTheOrders() {
        // Arrange
        orders.denyAll();

        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertAll(
                () -> assertEquals(DENIED, screen.getStatusMessage()),
                () -> assertFalse(formEnabled(screen)));
    }

    @Test
    @DisplayName("TC06C-25 A reading of the orders rejected by the server shows its reason")
    void tc06c25ValidationErrorWhileReadingTheOrders() {
        // Arrange
        orders.failReads(new ValidationException(List.of("There is no patient with the id P-0001")));

        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertEquals("⚠ There is no patient with the id P-0001", screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC06C-26 A failure while reading the orders is shown")
    void tc06c26FailureWhileReadingTheOrders() {
        // Arrange
        orders.failReads(new IllegalStateException("The server cannot be reached"));

        // Act
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Assert
        assertEquals("⚠ The orders could not be read: The server cannot be reached", screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC06C-27 A failure while searching the patients is shown")
    void tc06c27FailureWhileReadingThePatients() {
        // Arrange
        patients.failSearches(new IllegalStateException("The server cannot be reached"));

        // Act
        IU_OrderProcedure screen = open();

        // Assert
        assertEquals("⚠ The patients could not be read: The server cannot be reached", screen.getStatusMessage());
    }

    @Test
    @DisplayName("TC06C-29 An order without date and of a test type not in the catalogue is listed with its id")
    void tc06c29UnusualOrdersAreListed() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);

        // Act
        screen.displayOrders(List.of(new DiagnosticOrder("ORD-0004", "P-0001", "PET-BODY", REQUESTER, "Staging",
                "Rule out metastasis", null, "VIP"), new FollowUpOrder("ORD-0005", "P-0001", "CT-HEAD", REQUESTER,
                "Control", "Yearly control", LocalDate.of(2026, 11, 22), 12)));

        // Assert
        assertEquals(List.of(List.of("", "PET-BODY", "VIP", "Staging"),
                List.of("22/11/2026", "Head CT", "", "Control")), FollowUpScreen.rows(screen.getOrdersTable()));
    }

    @Test
    @DisplayName("TC06C-31 Back with the indication written asks; No stays on the screen")
    void tc06c31BackWithIndicationAnsweredNo() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);
        field(screen, INDICATION).setText("Headache after a fall");
        answer = false;

        // Act
        FollowUpScreen.button(screen, "Back").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of(DISCARD), questions),
                () -> assertEquals(0, backs));
    }

    @Test
    @DisplayName("TC06C-32 Back with only the justification written asks; Yes leaves the screen")
    void tc06c32BackWithJustificationAnsweredYes() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);
        field(screen, JUSTIFICATION).setText("Rule out bleeding");

        // Act
        FollowUpScreen.button(screen, "Back").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of(DISCARD), questions),
                () -> assertEquals(1, backs));
    }

    @Test
    @DisplayName("TC06C-34 Choosing another patient with unsaved data asks; No keeps the patient")
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
                () -> assertEquals("P-0001", screen.getSelectedPatient().getPatientId()));
    }

    @Test
    @DisplayName("TC06C-35 Choosing another patient with unsaved data asks; Yes loads the new patient")
    void tc06c35OtherPatientAnsweredYes() {
        // Arrange
        IU_OrderProcedure screen = select(open(), LUCIA);
        field(screen, JUSTIFICATION).setText("Rule out bleeding");

        // Act
        select(screen, MARIO);

        // Assert
        assertAll(
                () -> assertEquals(List.of(DISCARD), questions),
                () -> assertEquals("P-0002", screen.getSelectedPatient().getPatientId()));
    }
}
