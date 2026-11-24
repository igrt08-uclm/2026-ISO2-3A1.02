package es.uclm.esi.iso2.server.orders.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.orders.domain.ClinicalOrder;
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.orders.domain.FollowUpOrder;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeedStatus;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.orders.persistence.InMemoryClinicalOrderRepository;
import es.uclm.esi.iso2.server.orders.persistence.InMemorySchedulingNeedRepository;
import es.uclm.esi.iso2.server.orders.service.RecordingAuditRecorder.AuditEntry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of the follow-up orders of {@link ClinicalOrderService} (UC07 server, #71). Each test names the case of the
 * test design ({@code doc/testing/test-cases/UC07-server-test-design.md}, TC07S-xx) that it checks. The service uses
 * empty in-memory repositories, the fakes of C1, C2 and C3 of the test design and a fixed clock, so the creation date
 * of every order is 23/11/2026 (31/01/2027 and 29/02/2028 in the month-end cases) and the due dates of the needs are
 * written as literal dates that do not depend on the day the tests are run (D-040, plan Q4).
 */
@DisplayName("ClinicalOrderService · follow-up orders")
class OrderFollowUpTest {
    private static final String PHYSICIAN = "mgarcia";
    private static final String ADMISSIONS = "lmartin";
    private static final String ADMIN = "admin";
    private static final String PATIENT_USER = "jfernandez";
    private static final String INACTIVE = "pnavarro";
    private static final String UNKNOWN = "nobody";

    private static final String CREATED = "FOLLOW_UP_ORDER_CREATED";
    private static final String DENIED = "ORDER_ACCESS_DENIED";
    private static final String ORDERS = "orders";
    private static final String PATIENT = "P-0001";
    private static final String ELIGIBLE = "TT-02";
    private static final String NOT_ELIGIBLE = "TT-01";

    private static final String TEST_TYPE_REQUIRED = "The test type is required";
    private static final String INDICATION_REQUIRED = "The clinical indication is required";
    private static final String JUSTIFICATION_REQUIRED = "The justification is required";
    private static final String INTERVAL_REQUIRED = "The recurrence interval must be a positive number of months";
    private static final String INTERVAL_TOO_LONG = "The recurrence interval cannot be more than 12 months";

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    private FakeRoleManagementService roles;
    private RecordingAuditRecorder audit;
    private FakePatientService patients;
    private FakeSystemParameterService parameters;
    private InMemoryClinicalOrderRepository orders;
    private InMemorySchedulingNeedRepository needs;
    private ClinicalOrderService service;

    @BeforeEach
    void setUp() {
        roles = new FakeRoleManagementService();
        audit = new RecordingAuditRecorder();
        patients = new FakePatientService();
        parameters = new FakeSystemParameterService();
        orders = new InMemoryClinicalOrderRepository();
        needs = new InMemorySchedulingNeedRepository();
        service = new ClinicalOrderService(orders, needs, roles, audit, parameters, patients, CLOCK);
    }

    private static FollowUpOrder validOrder() {
        return order(PATIENT, ELIGIBLE, 6);
    }

    private static FollowUpOrder order(String patientId, String testTypeId, int months) {
        return new FollowUpOrder(null, patientId, testTypeId, PHYSICIAN, "Control of the nodule",
                "Follow-up of a known lesion", null, months);
    }

    private List<String> errorsOf(Executable call) {
        return assertThrows(ValidationException.class, call).getErrors();
    }

    private void assertDenied(String requester, Executable call) {
        AccessDeniedException denied = assertThrows(AccessDeniedException.class, call);
        assertEquals(requester, denied.getRequester());
        assertEquals(List.of(new AuditEntry(requester, DENIED, ORDERS)), audit.entries());
    }

    /** The due dates of the pending needs, in the order in which they were created. */
    private List<LocalDate> dueDates() {
        return needs.findPending().stream().map(SchedulingNeed::getDueDate).toList();
    }

    /** A service with empty repositories whose clock is fixed at 10:00 UTC of the given day. */
    private ClinicalOrderService serviceOn(String day) {
        Clock clock = Clock.fixed(Instant.parse(day + "T10:00:00Z"), ZoneOffset.UTC);
        return new ClinicalOrderService(orders, needs, roles, audit, parameters, patients, clock);
    }

    private static DiagnosticOrder diagnosticOrder() {
        return new DiagnosticOrder(null, PATIENT, NOT_ELIGIBLE, PHYSICIAN, "Chest pain", "Rule out pneumonia", null,
                "URGENT");
    }

    @Nested
    @DisplayName("1. Permission")
    class Permission {

        @Test
        @DisplayName("TC07S-01 the physician creates a follow-up order and it is audited")
        void physicianCreatesTheOrder() throws AccessDeniedException, ValidationException {
            FollowUpOrder created = service.orderFollowUp(PHYSICIAN, validOrder());
            assertAll(
                    () -> assertEquals("ORD-0001", created.getOrderId()),
                    () -> assertEquals(PHYSICIAN, created.getRequester()),
                    () -> assertEquals(TODAY, created.getCreationDate()),
                    () -> assertEquals(6, created.getRecurrenceIntervalMonths()),
                    () -> assertSame(created, orders.findById("ORD-0001")),
                    () -> assertEquals(List.of(new AuditEntry(PHYSICIAN, CREATED, "ORDER:ORD-0001")),
                            audit.entries()));
        }

        @ParameterizedTest
        @ValueSource(strings = { ADMISSIONS, ADMIN, PATIENT_USER })
        @DisplayName("TC07S-02 an active user without ORDER_PROCEDURES is denied")
        void activeUserWithoutPermissionIsDenied(String requester) {
            assertDenied(requester, () -> service.orderFollowUp(requester, validOrder()));
            assertAll(
                    () -> assertEquals(0, patients.getPatientCalls()),
                    () -> assertEquals(0, parameters.calls()),
                    () -> assertNull(orders.findById("ORD-0001")),
                    () -> assertTrue(needs.findPending().isEmpty()));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { INACTIVE, UNKNOWN })
        @DisplayName("TC07S-03 an inactive, unknown or null requester is denied")
        void inactiveUnknownOrNullRequesterIsDenied(String requester) {
            assertDenied(requester, () -> service.orderFollowUp(requester, validOrder()));
            assertNull(orders.findById("ORD-0001"));
        }

        /** A {@code null} order and an order with every field wrong. */
        static Stream<Arguments> invalidOrders() {
            return Stream.of(Arguments.of((FollowUpOrder) null),
                    Arguments.of(new FollowUpOrder(null, "P-9999", NOT_ELIGIBLE, ADMIN, "", "  ", null, 0)));
        }

        @ParameterizedTest
        @MethodSource("invalidOrders")
        @DisplayName("TC07S-32 admin with an invalid follow-up order is denied before the order is validated")
        void permissionBeforeValidation(FollowUpOrder sent) {
            assertDenied(ADMIN, () -> service.orderFollowUp(ADMIN, sent));
            assertAll(
                    () -> assertEquals(0, patients.getPatientCalls()),
                    () -> assertEquals(0, parameters.calls()),
                    () -> assertNull(orders.findById("ORD-0001")),
                    () -> assertTrue(needs.findPending().isEmpty()));
        }
    }

    @Nested
    @DisplayName("2. A valid follow-up order")
    class ValidFollowUpOrder {

        @Test
        @DisplayName("TC07S-21 the created order is the only audit entry")
        void createdOrderIsTheOnlyAuditEntry() throws AccessDeniedException, ValidationException {
            service.orderFollowUp(PHYSICIAN, validOrder());
            assertAll(
                    () -> assertEquals(List.of(new AuditEntry(PHYSICIAN, CREATED, "ORDER:ORD-0001")),
                            audit.entries()),
                    () -> assertTrue(audit.entries().stream()
                            .noneMatch(entry -> entry.action().startsWith("PATIENT_"))));
        }

        @Test
        @DisplayName("TC07S-22 the follow-up orders share the counter with the diagnostic ones")
        void sharedCounter() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, diagnosticOrder());
            FollowUpOrder created = service.orderFollowUp(PHYSICIAN, validOrder());
            assertEquals("ORD-0002", created.getOrderId());
        }

        @Test
        @DisplayName("TC07S-23 the reads return the diagnostic and the follow-up order")
        void readsReturnBothOrders() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, diagnosticOrder());
            service.orderFollowUp(PHYSICIAN, validOrder());
            List<ClinicalOrder> found = service.getOrdersOfPatient(PHYSICIAN, PATIENT);
            assertAll(
                    () -> assertEquals(2, found.size()),
                    () -> assertInstanceOf(DiagnosticOrder.class, found.get(0)),
                    () -> assertInstanceOf(FollowUpOrder.class, found.get(1)),
                    () -> assertEquals(6, ((FollowUpOrder) found.get(1)).getRecurrenceIntervalMonths()));
        }

        @Test
        @DisplayName("TC07S-25 the id, the requester and the date sent by the client are ignored")
        void clientDataIsIgnored() throws AccessDeniedException, ValidationException {
            FollowUpOrder sent = new FollowUpOrder("ORD-0099", PATIENT, ELIGIBLE, ADMIN, "Control of the nodule",
                    "Follow-up of a known lesion", LocalDate.of(2000, 1, 1), 6);
            FollowUpOrder created = service.orderFollowUp(PHYSICIAN, sent);
            assertAll(
                    () -> assertEquals("ORD-0001", created.getOrderId()),
                    () -> assertEquals(PHYSICIAN, created.getRequester()),
                    () -> assertEquals(TODAY, created.getCreationDate()));
        }
    }

    @Nested
    @DisplayName("3. The scheduling needs of D-040")
    class SchedulingNeedsOfTheNextTwelveMonths {

        private List<SchedulingNeed> needsOf(FollowUpOrder order) throws AccessDeniedException, ValidationException {
            service.orderFollowUp(PHYSICIAN, order);
            return needs.findPending();
        }

        @Test
        @DisplayName("TC07S-17 an interval of 6 months creates two pending needs")
        void sixMonths() throws AccessDeniedException, ValidationException {
            List<SchedulingNeed> pending = needsOf(validOrder());
            assertAll(
                    () -> assertEquals(2, pending.size()),
                    () -> assertEquals("NEED-0001", pending.get(0).getNeedId()),
                    () -> assertEquals("ORD-0001", pending.get(0).getOrderId()),
                    () -> assertEquals(LocalDate.of(2027, 5, 23), pending.get(0).getDueDate()),
                    () -> assertEquals("NEED-0002", pending.get(1).getNeedId()),
                    () -> assertEquals("ORD-0001", pending.get(1).getOrderId()),
                    () -> assertEquals(LocalDate.of(2027, 11, 23), pending.get(1).getDueDate()),
                    () -> assertEquals(SchedulingNeedStatus.PENDING, pending.get(0).getStatus()),
                    () -> assertEquals(SchedulingNeedStatus.PENDING, pending.get(1).getStatus()));
        }

        @Test
        @DisplayName("TC07S-18 an interval of 12 months creates one pending need")
        void twelveMonths() throws AccessDeniedException, ValidationException {
            List<SchedulingNeed> pending = needsOf(order(PATIENT, ELIGIBLE, 12));
            assertAll(
                    () -> assertEquals(1, pending.size()),
                    () -> assertEquals(LocalDate.of(2027, 11, 23), pending.get(0).getDueDate()));
        }

        @Test
        @DisplayName("TC07S-19 an interval of 5 months creates two pending needs")
        void fiveMonths() throws AccessDeniedException, ValidationException {
            List<SchedulingNeed> pending = needsOf(order(PATIENT, ELIGIBLE, 5));
            assertAll(
                    () -> assertEquals(2, pending.size()),
                    () -> assertEquals(LocalDate.of(2027, 4, 23), pending.get(0).getDueDate()),
                    () -> assertEquals(LocalDate.of(2027, 9, 23), pending.get(1).getDueDate()));
        }

        @Test
        @DisplayName("TC07S-20 an interval of 1 month creates twelve pending needs")
        void oneMonth() throws AccessDeniedException, ValidationException {
            needsOf(order(PATIENT, ELIGIBLE, 1));
            assertEquals(List.of(LocalDate.of(2026, 12, 23), LocalDate.of(2027, 1, 23), LocalDate.of(2027, 2, 23),
                    LocalDate.of(2027, 3, 23), LocalDate.of(2027, 4, 23), LocalDate.of(2027, 5, 23),
                    LocalDate.of(2027, 6, 23), LocalDate.of(2027, 7, 23), LocalDate.of(2027, 8, 23),
                    LocalDate.of(2027, 9, 23), LocalDate.of(2027, 10, 23), LocalDate.of(2027, 11, 23)), dueDates());
        }

        @Test
        @DisplayName("TC07S-27 an interval of 3 months creates four pending needs")
        void threeMonths() throws AccessDeniedException, ValidationException {
            needsOf(order(PATIENT, ELIGIBLE, 3));
            assertEquals(List.of(LocalDate.of(2027, 2, 23), LocalDate.of(2027, 5, 23), LocalDate.of(2027, 8, 23),
                    LocalDate.of(2027, 11, 23)), dueDates());
        }

        @Test
        @DisplayName("TC07S-28 an interval of 4 months creates three pending needs")
        void fourMonths() throws AccessDeniedException, ValidationException {
            needsOf(order(PATIENT, ELIGIBLE, 4));
            assertEquals(List.of(LocalDate.of(2027, 3, 23), LocalDate.of(2027, 7, 23), LocalDate.of(2027, 11, 23)),
                    dueDates());
        }

        @Test
        @DisplayName("TC07S-29 an interval of 11 months creates one pending need")
        void elevenMonths() throws AccessDeniedException, ValidationException {
            needsOf(order(PATIENT, ELIGIBLE, 11));
            assertEquals(List.of(LocalDate.of(2027, 10, 23)), dueDates());
        }

        @Test
        @DisplayName("TC07S-30 an order of 31/01/2027 with 1 month has its needs on the last day of short months")
        void monthEnd() throws AccessDeniedException, ValidationException {
            FollowUpOrder created = serviceOn("2027-01-31").orderFollowUp(PHYSICIAN, order(PATIENT, ELIGIBLE, 1));
            assertAll(
                    () -> assertEquals(LocalDate.of(2027, 1, 31), created.getCreationDate()),
                    () -> assertEquals(List.of(LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31),
                            LocalDate.of(2027, 4, 30), LocalDate.of(2027, 5, 31), LocalDate.of(2027, 6, 30),
                            LocalDate.of(2027, 7, 31), LocalDate.of(2027, 8, 31), LocalDate.of(2027, 9, 30),
                            LocalDate.of(2027, 10, 31), LocalDate.of(2027, 11, 30), LocalDate.of(2027, 12, 31),
                            LocalDate.of(2028, 1, 31)), dueDates()));
        }

        @Test
        @DisplayName("TC07S-31 an order created on 29/02/2028 with 12 months has its need on 28/02/2029")
        void leapDay() throws AccessDeniedException, ValidationException {
            FollowUpOrder created = serviceOn("2028-02-29").orderFollowUp(PHYSICIAN, order(PATIENT, ELIGIBLE, 12));
            assertAll(
                    () -> assertEquals(LocalDate.of(2028, 2, 29), created.getCreationDate()),
                    () -> assertEquals(List.of(LocalDate.of(2029, 2, 28)), dueDates()));
        }

        @Test
        @DisplayName("TC07S-33 two follow-up orders in a row continue the ids of the needs, each need to its own order")
        void twoFollowUpOrdersInARow() throws AccessDeniedException, ValidationException {
            service.orderFollowUp(PHYSICIAN, validOrder());
            service.orderFollowUp(PHYSICIAN, order("P-0002", ELIGIBLE, 12));
            List<SchedulingNeed> pending = needs.findPending();
            assertAll(
                    () -> assertEquals(List.of("NEED-0001", "NEED-0002", "NEED-0003"),
                            pending.stream().map(SchedulingNeed::getNeedId).toList()),
                    () -> assertEquals(List.of("ORD-0001", "ORD-0001", "ORD-0002"),
                            pending.stream().map(SchedulingNeed::getOrderId).toList()),
                    () -> assertEquals(List.of(LocalDate.of(2027, 5, 23), LocalDate.of(2027, 11, 23),
                            LocalDate.of(2027, 11, 23)), dueDates()));
        }

        @Test
        @DisplayName("TC07S-34 a diagnostic order and a follow-up order share the counter of the needs")
        void diagnosticAndFollowUpShareTheNeedCounter() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, diagnosticOrder());
            service.orderFollowUp(PHYSICIAN, validOrder());
            List<SchedulingNeed> pending = needs.findPending();
            assertAll(
                    () -> assertEquals(List.of("NEED-0001", "NEED-0002", "NEED-0003"),
                            pending.stream().map(SchedulingNeed::getNeedId).toList()),
                    () -> assertEquals(List.of("ORD-0001", "ORD-0002", "ORD-0002"),
                            pending.stream().map(SchedulingNeed::getOrderId).toList()),
                    () -> assertEquals(List.of(LocalDate.of(2026, 11, 23), LocalDate.of(2027, 5, 23),
                            LocalDate.of(2027, 11, 23)), dueDates()));
        }

        @Test
        @DisplayName("TC07S-26 the needs are pending for C5")
        void needsArePendingForC5() throws AccessDeniedException, ValidationException {
            service.orderFollowUp(PHYSICIAN, validOrder());
            assertEquals(2, service.getPendingSchedulingNeeds(ADMISSIONS).size());
        }
    }

    @Nested
    @DisplayName("4. A rejected follow-up order")
    class RejectedFollowUpOrder {

        @Test
        @DisplayName("TC07S-35 an interval of 13 months is rejected without saving the order or its needs")
        void thirteenMonths() {
            // Arrange
            FollowUpOrder sent = order(PATIENT, ELIGIBLE, 13);

            // Act
            List<String> errors = errorsOf(() -> service.orderFollowUp(PHYSICIAN, sent));

            // Assert
            assertAll(
                    () -> assertEquals(List.of(INTERVAL_TOO_LONG), errors),
                    () -> assertTrue(orders.findByPatient(PATIENT).isEmpty()),
                    () -> assertTrue(needs.findPending().isEmpty()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("TC07S-36 an interval of 120 months is rejected without saving the order or its needs")
        void oneHundredTwentyMonths() {
            // Arrange
            FollowUpOrder sent = order(PATIENT, ELIGIBLE, 120);

            // Act
            List<String> errors = errorsOf(() -> service.orderFollowUp(PHYSICIAN, sent));

            // Assert
            assertAll(
                    () -> assertEquals(List.of(INTERVAL_TOO_LONG), errors),
                    () -> assertTrue(orders.findByPatient(PATIENT).isEmpty()),
                    () -> assertTrue(needs.findPending().isEmpty()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("TC07S-04 a null order gives only its message")
        void nullOrder() {
            assertEquals(List.of("The order is required"), errorsOf(() -> service.orderFollowUp(PHYSICIAN, null)));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("TC07S-05 a blank patient is rejected")
        void blankPatient(String patientId) {
            assertEquals(List.of("The patient is required"),
                    errorsOf(() -> service.orderFollowUp(PHYSICIAN, order(patientId, ELIGIBLE, 6))));
        }

        @Test
        @DisplayName("TC07S-06 an unknown patient gives the message of C3")
        void unknownPatient() {
            assertEquals(List.of("There is no patient with the id P-9999"),
                    errorsOf(() -> service.orderFollowUp(PHYSICIAN, order("P-9999", ELIGIBLE, 6))));
        }

        @Test
        @DisplayName("TC07S-07 an inactive patient is rejected")
        void inactivePatient() {
            assertEquals(List.of("The patient P-0004 is not active"),
                    errorsOf(() -> service.orderFollowUp(PHYSICIAN, order("P-0004", ELIGIBLE, 6))));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("TC07S-08 a blank test type is rejected")
        void blankTestType(String testTypeId) {
            assertEquals(List.of(TEST_TYPE_REQUIRED),
                    errorsOf(() -> service.orderFollowUp(PHYSICIAN, order(PATIENT, testTypeId, 6))));
        }

        @Test
        @DisplayName("TC07S-09 an unknown test type gives the message of C2")
        void unknownTestType() {
            assertEquals(List.of("There is no test type with the id TT-99"),
                    errorsOf(() -> service.orderFollowUp(PHYSICIAN, order(PATIENT, "TT-99", 6))));
        }

        @Test
        @DisplayName("TC07S-10 an inactive test type is rejected")
        void inactiveTestType() {
            assertEquals(List.of("The test type TT-03 is not active"),
                    errorsOf(() -> service.orderFollowUp(PHYSICIAN, order(PATIENT, "TT-03", 6))));
        }

        @Test
        @DisplayName("TC07S-11 an active test type that is not eligible for follow-up is rejected")
        void notEligibleTestType() {
            assertEquals(List.of("The test type TT-01 is not eligible for follow-up"),
                    errorsOf(() -> service.orderFollowUp(PHYSICIAN, order(PATIENT, NOT_ELIGIBLE, 6))));
        }

        @Test
        @DisplayName("TC07S-12 an interval of 0 is rejected")
        void zeroInterval() {
            assertEquals(List.of(INTERVAL_REQUIRED),
                    errorsOf(() -> service.orderFollowUp(PHYSICIAN, order(PATIENT, ELIGIBLE, 0))));
        }

        @ParameterizedTest
        @ValueSource(ints = { -1, -6 })
        @DisplayName("TC07S-13 a negative interval is rejected")
        void negativeInterval(int months) {
            assertEquals(List.of(INTERVAL_REQUIRED),
                    errorsOf(() -> service.orderFollowUp(PHYSICIAN, order(PATIENT, ELIGIBLE, months))));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("TC07S-14 a blank clinical indication is rejected")
        void blankIndication(String text) {
            FollowUpOrder sent = new FollowUpOrder(null, PATIENT, ELIGIBLE, PHYSICIAN, text,
                    "Follow-up of a known lesion", null, 6);
            assertEquals(List.of(INDICATION_REQUIRED), errorsOf(() -> service.orderFollowUp(PHYSICIAN, sent)));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("TC07S-15 a blank justification is rejected")
        void blankJustification(String text) {
            FollowUpOrder sent = new FollowUpOrder(null, PATIENT, ELIGIBLE, PHYSICIAN, "Control of the nodule",
                    text, null, 6);
            assertEquals(List.of(JUSTIFICATION_REQUIRED), errorsOf(() -> service.orderFollowUp(PHYSICIAN, sent)));
        }

        @Test
        @DisplayName("TC07S-16 every error comes together and in the order of the plan")
        void everyErrorTogether() {
            FollowUpOrder sent = new FollowUpOrder(null, null, "", PHYSICIAN, "", "  ", null, 0);
            assertEquals(List.of("The patient is required", TEST_TYPE_REQUIRED, INTERVAL_REQUIRED,
                    INDICATION_REQUIRED, JUSTIFICATION_REQUIRED),
                    errorsOf(() -> service.orderFollowUp(PHYSICIAN, sent)));
        }

        @Test
        @DisplayName("TC07S-24 a rejected order saves nothing and does not use the id")
        void rejectedOrderSavesNothing() throws AccessDeniedException, ValidationException {
            errorsOf(() -> service.orderFollowUp(PHYSICIAN, order(PATIENT, NOT_ELIGIBLE, 6)));
            assertAll(
                    () -> assertTrue(orders.findByPatient(PATIENT).isEmpty()),
                    () -> assertTrue(needs.findPending().isEmpty()),
                    () -> assertTrue(audit.entries().isEmpty()));
            assertEquals("ORD-0001", service.orderFollowUp(PHYSICIAN, validOrder()).getOrderId());
        }
    }
}
