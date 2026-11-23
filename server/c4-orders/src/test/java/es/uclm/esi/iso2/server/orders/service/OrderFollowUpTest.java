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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of the follow-up orders of {@link ClinicalOrderService} (UC07 server, #71). Each test names the case of the test
 * design (`doc/testing/test-cases/UC07-server-test-design.md`, TC07S-xx) that it checks. The service uses empty in-memory
 * repositories, the fakes of C1, C2 and C3 of the test design and a fixed clock, so the creation date of every order is
 * 23/11/2026 and the due dates of the needs do not depend on the day the tests are run.
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
                    () -> assertTrue(audit.entries().stream().noneMatch(entry -> entry.action().startsWith("PATIENT_"))));
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
                    () -> assertEquals(TODAY.plusMonths(6), pending.get(0).getDueDate()),
                    () -> assertEquals("NEED-0002", pending.get(1).getNeedId()),
                    () -> assertEquals(TODAY.plusMonths(12), pending.get(1).getDueDate()),
                    () -> assertEquals(SchedulingNeedStatus.PENDING, pending.get(1).getStatus()));
        }

        @Test
        @DisplayName("TC07S-18 an interval of 12 months creates one pending need")
        void twelveMonths() throws AccessDeniedException, ValidationException {
            List<SchedulingNeed> pending = needsOf(order(PATIENT, ELIGIBLE, 12));
            assertAll(
                    () -> assertEquals(1, pending.size()),
                    () -> assertEquals(TODAY.plusMonths(12), pending.get(0).getDueDate()));
        }

        @Test
        @DisplayName("TC07S-19 an interval of 5 months creates two pending needs")
        void fiveMonths() throws AccessDeniedException, ValidationException {
            List<SchedulingNeed> pending = needsOf(order(PATIENT, ELIGIBLE, 5));
            assertAll(
                    () -> assertEquals(2, pending.size()),
                    () -> assertEquals(TODAY.plusMonths(5), pending.get(0).getDueDate()),
                    () -> assertEquals(TODAY.plusMonths(10), pending.get(1).getDueDate()));
        }

        @Test
        @DisplayName("TC07S-20 an interval of 1 month creates twelve pending needs")
        void oneMonth() throws AccessDeniedException, ValidationException {
            List<SchedulingNeed> pending = needsOf(order(PATIENT, ELIGIBLE, 1));
            assertAll(
                    () -> assertEquals(12, pending.size()),
                    () -> assertEquals(TODAY.plusMonths(1), pending.get(0).getDueDate()),
                    () -> assertEquals(TODAY.plusMonths(12), pending.get(11).getDueDate()));
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
        @DisplayName("TC07S-14 a blank indication or justification is rejected")
        void blankTexts(String text) {
            FollowUpOrder noIndication = new FollowUpOrder(null, PATIENT, ELIGIBLE, PHYSICIAN, text,
                    "Follow-up of a known lesion", null, 6);
            FollowUpOrder noJustification = new FollowUpOrder(null, PATIENT, ELIGIBLE, PHYSICIAN, "Control of the nodule",
                    text, null, 6);
            assertAll(
                    () -> assertEquals(List.of(INDICATION_REQUIRED),
                            errorsOf(() -> service.orderFollowUp(PHYSICIAN, noIndication))),
                    () -> assertEquals(List.of(JUSTIFICATION_REQUIRED),
                            errorsOf(() -> service.orderFollowUp(PHYSICIAN, noJustification))));
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
