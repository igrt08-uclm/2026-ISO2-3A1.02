package es.uclm.esi.iso2.server.orders.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.orders.contracts.IClinicalOrderService;
import es.uclm.esi.iso2.common.orders.domain.ClinicalOrder;
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeedStatus;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.orders.persistence.InMemoryClinicalOrderRepository;
import es.uclm.esi.iso2.server.orders.persistence.InMemorySchedulingNeedRepository;
import es.uclm.esi.iso2.server.orders.service.RecordingAuditRecorder.AuditEntry;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.lang.reflect.Method;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
 * Tests of {@link ClinicalOrderService} (UC06 server, #68). Each test names the case of the test design
 * (`doc/testing/test-cases/UC06-server-test-design.md`, TC06S-xx) that it checks. The service uses empty in-memory
 * repositories, the fakes of C1, C2 and C3 of the test design and a fixed clock, so the creation date of every order
 * is 23/11/2026 and the tests do not depend on the day they are run. Only TC06S-55 uses the system clock, and it
 * accepts the date of the day before or after the call.
 */
@DisplayName("ClinicalOrderService")
class ClinicalOrderServiceTest {
    private static final String PHYSICIAN = "mgarcia";
    private static final String ADMISSIONS = "lmartin";
    private static final String ADMIN = "admin";
    private static final String PATIENT_USER = "jfernandez";
    private static final String INACTIVE = "pnavarro";
    private static final String UNKNOWN = "nobody";

    private static final String CREATED = "ORDER_CREATED";
    private static final String DENIED = "ORDER_ACCESS_DENIED";
    private static final String ORDERS = "orders";
    private static final String PATIENT = "P-0001";

    private static final String PATIENT_REQUIRED = "The patient is required";
    private static final String TEST_TYPE_REQUIRED = "The test type is required";
    private static final String PRIORITY_REQUIRED = "The priority is required";
    private static final String INDICATION_REQUIRED = "The clinical indication is required";
    private static final String JUSTIFICATION_REQUIRED = "The justification is required";

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

    private static DiagnosticOrder validOrder() {
        return order(PATIENT, "TT-01", "URGENT");
    }

    private static DiagnosticOrder order(String patientId, String testTypeId, String priorityId) {
        return new DiagnosticOrder(null, patientId, testTypeId, PHYSICIAN, "Chest pain", "Rule out pneumonia", null,
                priorityId);
    }

    private List<String> errorsOf(Executable call) {
        return assertThrows(ValidationException.class, call).getErrors();
    }

    private void assertDenied(String requester, Executable call, String resource) {
        AccessDeniedException denied = assertThrows(AccessDeniedException.class, call);
        assertEquals(requester, denied.getRequester());
        assertEquals(List.of(new AuditEntry(requester, DENIED, resource)), audit.entries());
    }

    @Nested
    @DisplayName("1. Permission")
    class Permission {

        @Test
        @DisplayName("TC06S-01 the physician creates an order and it is audited")
        void physicianCreatesTheOrder() throws AccessDeniedException, ValidationException {
            DiagnosticOrder created = service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            assertAll(
                    () -> assertEquals("ORD-0001", created.getOrderId()),
                    () -> assertEquals(PHYSICIAN, created.getRequester()),
                    () -> assertEquals(TODAY, created.getCreationDate()),
                    () -> assertSame(created, orders.findById("ORD-0001")),
                    () -> assertEquals(List.of(new AuditEntry(PHYSICIAN, CREATED, "ORDER:ORD-0001")),
                            audit.entries()));
        }

        @ParameterizedTest
        @ValueSource(strings = { ADMISSIONS, ADMIN, PATIENT_USER })
        @DisplayName("TC06S-02 an active user without ORDER_PROCEDURES is denied")
        void activeUserWithoutPermissionIsDenied(String requester) {
            assertDenied(requester, () -> service.orderDiagnosticProcedure(requester, validOrder()), ORDERS);
            assertAll(
                    () -> assertEquals(0, patients.getPatientCalls()),
                    () -> assertEquals(0, parameters.calls()),
                    () -> assertNull(orders.findById("ORD-0001")),
                    () -> assertTrue(needs.findPending().isEmpty()));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { INACTIVE, UNKNOWN })
        @DisplayName("TC06S-03 an inactive, unknown or null requester is denied")
        void inactiveUnknownOrNullRequesterIsDenied(String requester) {
            assertDenied(requester, () -> service.orderDiagnosticProcedure(requester, validOrder()), ORDERS);
            assertNull(orders.findById("ORD-0001"));
        }

        @Test
        @DisplayName("TC06S-22 the physician reads the pending scheduling needs")
        void physicianReadsThePendingNeeds() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            assertEquals(1, service.getPendingSchedulingNeeds(PHYSICIAN).size());
        }

        @Test
        @DisplayName("TC06S-23 the admissions staff reads the pending scheduling needs")
        void admissionsStaffReadsThePendingNeeds() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            assertEquals(1, service.getPendingSchedulingNeeds(ADMISSIONS).size());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { ADMIN, PATIENT_USER, INACTIVE, UNKNOWN })
        @DisplayName("TC06S-24 a requester without ORDER_PROCEDURES or MANAGE_APPOINTMENTS is denied in C5 operations")
        void userWithoutSchedulingPermissionIsDenied(String requester) {
            assertDenied(requester, () -> service.getPendingSchedulingNeeds(requester), ORDERS);
            audit.clear();
            assertDenied(requester, () -> service.getOrder(requester, "ORD-0001"), "ORDER:ORD-0001");
            audit.clear();
            assertDenied(requester, () -> service.markSchedulingNeedBooked(requester, "NEED-0001"),
                    "SCHEDULING_NEED:NEED-0001");
            audit.clear();
            assertDenied(requester, () -> service.markSchedulingNeedPending(requester, "NEED-0001"),
                    "SCHEDULING_NEED:NEED-0001");
        }
    }

    @Nested
    @DisplayName("2. A valid order")
    class ValidOrder {

        @Test
        @DisplayName("TC06S-05 the order gets its id, its requester and the date of the clock")
        void orderGetsItsOwnData() throws AccessDeniedException, ValidationException {
            DiagnosticOrder created = service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            assertAll(
                    () -> assertEquals("ORD-0001", created.getOrderId()),
                    () -> assertEquals(PATIENT, created.getPatientId()),
                    () -> assertEquals("TT-01", created.getTestTypeId()),
                    () -> assertEquals("URGENT", created.getPriorityId()),
                    () -> assertEquals("Chest pain", created.getIndication()),
                    () -> assertEquals("Rule out pneumonia", created.getJustification()),
                    () -> assertEquals(PHYSICIAN, created.getRequester()),
                    () -> assertEquals(TODAY, created.getCreationDate()));
        }

        @Test
        @DisplayName("TC06S-06 the id, the requester and the date sent by the client are ignored")
        void clientDataIsIgnored() throws AccessDeniedException, ValidationException {
            DiagnosticOrder sent = new DiagnosticOrder("ORD-0099", PATIENT, "TT-01", ADMIN, "Chest pain",
                    "Rule out pneumonia", LocalDate.of(2000, 1, 1), "URGENT");
            DiagnosticOrder created = service.orderDiagnosticProcedure(PHYSICIAN, sent);
            assertAll(
                    () -> assertEquals("ORD-0001", created.getOrderId()),
                    () -> assertEquals(PHYSICIAN, created.getRequester()),
                    () -> assertEquals(TODAY, created.getCreationDate()));
        }

        @Test
        @DisplayName("TC06S-07 the second order gets the next id")
        void secondOrderGetsTheNextId() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            assertEquals("ORD-0002", service.orderDiagnosticProcedure(PHYSICIAN, validOrder()).getOrderId());
        }

        @Test
        @DisplayName("TC06S-08 the order creates one pending scheduling need due today")
        void orderCreatesOnePendingNeed() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            List<SchedulingNeed> pending = needs.findPending();
            assertAll(
                    () -> assertEquals(1, pending.size()),
                    () -> assertEquals("NEED-0001", pending.get(0).getNeedId()),
                    () -> assertEquals("ORD-0001", pending.get(0).getOrderId()),
                    () -> assertEquals(TODAY, pending.get(0).getDueDate()),
                    () -> assertEquals(SchedulingNeedStatus.PENDING, pending.get(0).getStatus()));
        }

        @Test
        @DisplayName("TC06S-09 the ids skip the ones that the repositories already have")
        void idsSkipTheExistingOnes() throws AccessDeniedException, ValidationException {
            orders.save(new DiagnosticOrder("ORD-0001", PATIENT, "TT-01", PHYSICIAN, "Old", "Old", TODAY, "ROUTINE"));
            needs.save(new SchedulingNeed("NEED-0001", "ORD-0001", TODAY, SchedulingNeedStatus.BOOKED));
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            assertAll(
                    () -> assertEquals("ORD-0002", orders.findById("ORD-0002").getOrderId()),
                    () -> assertEquals("NEED-0002", needs.findById("NEED-0002").getNeedId()));
        }

        @ParameterizedTest
        @ValueSource(strings = { "URGENT", "PREFERENTIAL", "ROUTINE" })
        @DisplayName("TC06S-10 every active priority is accepted")
        void everyActivePriorityIsAccepted(String priorityId) throws AccessDeniedException, ValidationException {
            assertEquals(priorityId,
                    service.orderDiagnosticProcedure(PHYSICIAN, order(PATIENT, "TT-01", priorityId)).getPriorityId());
        }

        @Test
        @DisplayName("TC06S-11 the texts with spaces at the ends are accepted as they are sent")
        void textsWithSpacesAreAccepted() throws AccessDeniedException, ValidationException {
            DiagnosticOrder sent = new DiagnosticOrder(null, PATIENT, "TT-01", PHYSICIAN, "  Chest pain  ",
                    "  Rule out pneumonia  ", null, "URGENT");
            DiagnosticOrder created = service.orderDiagnosticProcedure(PHYSICIAN, sent);
            assertAll(
                    () -> assertEquals("  Chest pain  ", created.getIndication()),
                    () -> assertEquals("  Rule out pneumonia  ", created.getJustification()));
        }

        @Test
        @DisplayName("TC06S-12 C4 registers only ORDER_CREATED: it does not repeat the access to the patient")
        void createdOrderIsTheOnlyAuditEntry() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            assertAll(
                    () -> assertEquals(List.of(new AuditEntry(PHYSICIAN, CREATED, "ORDER:ORD-0001")),
                            audit.entries()),
                    () -> assertEquals(1, patients.getPatientCalls()));
        }
    }

    @Nested
    @DisplayName("3. A rejected order")
    class RejectedOrder {

        @Test
        @DisplayName("TC06S-04 a null order gives only its message")
        void nullOrder() {
            assertEquals(List.of("The order is required"),
                    errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, null)));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("TC06S-13 a blank patient is rejected")
        void blankPatient(String patientId) {
            assertEquals(List.of(PATIENT_REQUIRED),
                    errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, order(patientId, "TT-01", "URGENT"))));
        }

        @Test
        @DisplayName("TC06S-14 an unknown patient gives the message of C3")
        void unknownPatient() {
            assertEquals(List.of("There is no patient with the id P-9999"),
                    errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, order("P-9999", "TT-01", "URGENT"))));
        }

        @Test
        @DisplayName("TC06S-15 an inactive patient is rejected")
        void inactivePatient() {
            assertEquals(List.of("The patient P-0004 is not active"),
                    errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, order("P-0004", "TT-01", "URGENT"))));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("TC06S-16 a blank test type is rejected")
        void blankTestType(String testTypeId) {
            assertEquals(List.of(TEST_TYPE_REQUIRED),
                    errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, order(PATIENT, testTypeId, "URGENT"))));
        }

        @Test
        @DisplayName("TC06S-17 an unknown test type gives the message of C2")
        void unknownTestType() {
            assertEquals(List.of("There is no test type with the id TT-99"),
                    errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, order(PATIENT, "TT-99", "URGENT"))));
        }

        @Test
        @DisplayName("TC06S-18 a deactivated test type is rejected")
        void deactivatedTestType() {
            assertEquals(List.of("The test type TT-03 is not active"),
                    errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, order(PATIENT, "TT-03", "URGENT"))));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("TC06S-19 a blank priority is rejected")
        void blankPriority(String priorityId) {
            assertEquals(List.of(PRIORITY_REQUIRED),
                    errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, order(PATIENT, "TT-01", priorityId))));
        }

        @ParameterizedTest
        @ValueSource(strings = { "VIP", "urgent" })
        @DisplayName("TC06S-20 a priority that is not configured is rejected")
        void unknownPriority(String priorityId) {
            assertEquals(List.of("The priority " + priorityId + " is not one of the configured priorities"),
                    errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, order(PATIENT, "TT-01", priorityId))));
        }

        @Test
        @DisplayName("TC06S-21 a deactivated priority is rejected")
        void deactivatedPriority() {
            assertEquals(List.of("The priority OLD is not active"),
                    errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, order(PATIENT, "TT-01", "OLD"))));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("TC06S-25 a blank indication or justification is rejected")
        void blankTexts(String text) {
            DiagnosticOrder noIndication = new DiagnosticOrder(null, PATIENT, "TT-01", PHYSICIAN, text,
                    "Rule out pneumonia", null, "URGENT");
            DiagnosticOrder noJustification = new DiagnosticOrder(null, PATIENT, "TT-01", PHYSICIAN, "Chest pain",
                    text, null, "URGENT");
            assertAll(
                    () -> assertEquals(List.of(INDICATION_REQUIRED),
                            errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, noIndication))),
                    () -> assertEquals(List.of(JUSTIFICATION_REQUIRED),
                            errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, noJustification))));
        }

        @Test
        @DisplayName("TC06S-26 every error comes together and in the order of the plan")
        void everyErrorTogether() {
            DiagnosticOrder sent = new DiagnosticOrder(null, null, "", PHYSICIAN, "", "  ", null, "VIP");
            assertEquals(List.of(PATIENT_REQUIRED, TEST_TYPE_REQUIRED,
                    "The priority VIP is not one of the configured priorities", INDICATION_REQUIRED,
                    JUSTIFICATION_REQUIRED),
                    errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, sent)));
        }

        @Test
        @DisplayName("TC06S-27 a rejected order saves nothing and does not use the id")
        void rejectedOrderSavesNothing() throws AccessDeniedException, ValidationException {
            errorsOf(() -> service.orderDiagnosticProcedure(PHYSICIAN, order("P-9999", "TT-01", "URGENT")));
            assertAll(
                    () -> assertTrue(orders.findByPatient(PATIENT).isEmpty()),
                    () -> assertTrue(needs.findPending().isEmpty()),
                    () -> assertTrue(audit.entries().isEmpty()));
            assertEquals("ORD-0001", service.orderDiagnosticProcedure(PHYSICIAN, validOrder()).getOrderId());
        }
    }

    @Nested
    @DisplayName("4. Reading the orders")
    class ReadingOrders {

        @Test
        @DisplayName("TC06S-28 the orders of a patient come in the order of creation")
        void ordersOfAPatient() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            service.orderDiagnosticProcedure(PHYSICIAN, order(PATIENT, "TT-02", "ROUTINE"));
            List<ClinicalOrder> found = service.getOrdersOfPatient(PHYSICIAN, PATIENT);
            assertAll(
                    () -> assertEquals(2, found.size()),
                    () -> assertEquals("ORD-0001", found.get(0).getOrderId()),
                    () -> assertEquals("ORD-0002", found.get(1).getOrderId()));
        }

        @Test
        @DisplayName("TC06S-29 a patient without orders gives an empty list and C3 is not called")
        void patientWithoutOrders() throws AccessDeniedException, ValidationException {
            assertTrue(service.getOrdersOfPatient(PHYSICIAN, "P-0002").isEmpty());
            assertEquals(0, patients.getPatientCalls());
        }

        @Test
        @DisplayName("TC06S-30 an unknown patient gives an empty list and C3 is not called")
        void unknownPatientGivesNoOrders() throws AccessDeniedException, ValidationException {
            assertTrue(service.getOrdersOfPatient(PHYSICIAN, "P-9999").isEmpty());
            assertEquals(0, patients.getPatientCalls());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("TC06S-31 a blank patient id is rejected")
        void blankPatientId(String patientId) {
            assertEquals(List.of(PATIENT_REQUIRED),
                    errorsOf(() -> service.getOrdersOfPatient(PHYSICIAN, patientId)));
        }

        @Test
        @DisplayName("TC06S-32 one order is returned with its priority")
        void oneOrder() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            ClinicalOrder found = service.getOrder(PHYSICIAN, "ORD-0001");
            assertAll(
                    () -> assertInstanceOf(DiagnosticOrder.class, found),
                    () -> assertEquals("URGENT", ((DiagnosticOrder) found).getPriorityId()));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = { "ORD-9999" })
        @DisplayName("TC06S-33 an unknown order id is rejected")
        void unknownOrder(String orderId) {
            assertEquals(List.of("There is no order with the id " + orderId),
                    errorsOf(() -> service.getOrder(PHYSICIAN, orderId)));
        }
    }

    @Nested
    @DisplayName("5. Scheduling needs")
    class SchedulingNeeds {

        @Test
        @DisplayName("TC06S-34 without orders there are no pending needs")
        void noPendingNeeds() throws AccessDeniedException {
            assertTrue(service.getPendingSchedulingNeeds(PHYSICIAN).isEmpty());
        }

        @Test
        @DisplayName("TC06S-35 the pending needs come in the order of creation")
        void pendingNeedsInOrder() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            List<SchedulingNeed> pending = service.getPendingSchedulingNeeds(ADMISSIONS);
            assertAll(
                    () -> assertEquals(2, pending.size()),
                    () -> assertEquals("NEED-0001", pending.get(0).getNeedId()),
                    () -> assertEquals("NEED-0002", pending.get(1).getNeedId()));
        }

        @Test
        @DisplayName("TC06S-36 booking a need removes it from the pending ones")
        void bookingANeed() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            service.markSchedulingNeedBooked(ADMISSIONS, "NEED-0001");
            assertAll(
                    () -> assertEquals(SchedulingNeedStatus.BOOKED, needs.findById("NEED-0001").getStatus()),
                    () -> assertTrue(needs.findPending().isEmpty()));
        }

        @Test
        @DisplayName("TC06S-37 a need made pending again keeps its data and its first place")
        void needMadePendingAgain() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            service.markSchedulingNeedBooked(ADMISSIONS, "NEED-0001");
            service.markSchedulingNeedPending(ADMISSIONS, "NEED-0001");
            SchedulingNeed again = needs.findById("NEED-0001");
            assertAll(
                    () -> assertEquals(SchedulingNeedStatus.PENDING, again.getStatus()),
                    () -> assertEquals("ORD-0001", again.getOrderId()),
                    () -> assertEquals(TODAY, again.getDueDate()),
                    () -> assertEquals("NEED-0001", needs.findPending().get(0).getNeedId()));
        }

        @Test
        @DisplayName("TC06S-38 a need cannot be booked or made pending twice")
        void repeatedStatusChange() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            service.markSchedulingNeedBooked(ADMISSIONS, "NEED-0001");
            assertEquals(List.of("The scheduling need NEED-0001 is already booked"),
                    errorsOf(() -> service.markSchedulingNeedBooked(ADMISSIONS, "NEED-0001")));
            service.markSchedulingNeedPending(ADMISSIONS, "NEED-0001");
            assertEquals(List.of("The scheduling need NEED-0001 is already pending"),
                    errorsOf(() -> service.markSchedulingNeedPending(ADMISSIONS, "NEED-0001")));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = { "NEED-9999" })
        @DisplayName("TC06S-39 an unknown need id is rejected")
        void unknownNeed(String needId) {
            assertEquals(List.of("There is no scheduling need with the id " + needId),
                    errorsOf(() -> service.markSchedulingNeedBooked(ADMISSIONS, needId)));
        }

        @Test
        @DisplayName("TC06S-40 the operations of C5 are not audited")
        void c5OperationsAreNotAudited() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            audit.clear();
            service.getPendingSchedulingNeeds(PHYSICIAN);
            service.getOrder(PHYSICIAN, "ORD-0001");
            service.markSchedulingNeedBooked(ADMISSIONS, "NEED-0001");
            service.markSchedulingNeedPending(ADMISSIONS, "NEED-0001");
            assertTrue(audit.entries().isEmpty());
        }
    }

    @Nested
    @DisplayName("6. Permission: each requester class and the order of the checks")
    class PermissionOfEveryOperation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { ADMISSIONS, ADMIN, PATIENT_USER, INACTIVE, UNKNOWN })
        @DisplayName("TC06S-45 a requester without ORDER_PROCEDURES cannot read the orders of a patient")
        void readingOrdersWithoutPermissionIsDenied(String requester)
                throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            audit.clear();
            assertDenied(requester, () -> service.getOrdersOfPatient(requester, PATIENT), ORDERS);
        }

        @Test
        @DisplayName("TC06S-46 the permission is checked before the order is validated")
        void permissionBeforeValidation() {
            DiagnosticOrder unknownPatient = order("P-9999", "TT-01", "URGENT");
            DiagnosticOrder blankIndication = new DiagnosticOrder(null, PATIENT, "TT-01", PHYSICIAN, " ",
                    "Rule out pneumonia", null, "URGENT");
            assertDenied(ADMIN, () -> service.orderDiagnosticProcedure(ADMIN, null), ORDERS);
            audit.clear();
            assertDenied(ADMIN, () -> service.orderDiagnosticProcedure(ADMIN, unknownPatient), ORDERS);
            audit.clear();
            assertDenied(ADMIN, () -> service.orderDiagnosticProcedure(ADMIN, blankIndication), ORDERS);
            assertAll(
                    () -> assertEquals(0, patients.getPatientCalls()),
                    () -> assertEquals(0, parameters.calls()),
                    () -> assertNull(orders.findById("ORD-0001")));
        }

        @ParameterizedTest
        @ValueSource(strings = { PHYSICIAN, ADMISSIONS })
        @DisplayName("TC06S-47 ORDER_PROCEDURES or MANAGE_APPOINTMENTS is enough to read an order and change its need")
        void eitherPermissionIsAcceptedInC5Operations(String requester)
                throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            audit.clear();
            ClinicalOrder found = service.getOrder(requester, "ORD-0001");
            service.markSchedulingNeedBooked(requester, "NEED-0001");
            SchedulingNeedStatus booked = needs.findById("NEED-0001").getStatus();
            service.markSchedulingNeedPending(requester, "NEED-0001");
            assertAll(
                    () -> assertEquals("ORD-0001", found.getOrderId()),
                    () -> assertEquals(SchedulingNeedStatus.BOOKED, booked),
                    () -> assertEquals(SchedulingNeedStatus.PENDING, needs.findById("NEED-0001").getStatus()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }
    }

    @Nested
    @DisplayName("7. Reads, unknown needs, boundaries and orders that are never edited")
    class MoreCases {

        @Test
        @DisplayName("TC06S-48 the orders of another patient are not returned")
        void ordersOfAnotherPatientAreExcluded() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            service.orderDiagnosticProcedure(PHYSICIAN, order("P-0002", "TT-02", "ROUTINE"));
            service.orderDiagnosticProcedure(PHYSICIAN, order(PATIENT, "TT-02", "ROUTINE"));
            List<ClinicalOrder> found = service.getOrdersOfPatient(PHYSICIAN, PATIENT);
            assertEquals(List.of("ORD-0001", "ORD-0003"), found.stream().map(ClinicalOrder::getOrderId).toList());
        }

        @Test
        @DisplayName("TC06S-49 reading the orders of a patient is not audited")
        void readingOrdersIsNotAudited() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            audit.clear();
            service.getOrdersOfPatient(PHYSICIAN, PATIENT);
            assertTrue(audit.entries().isEmpty());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = { "NEED-9999" })
        @DisplayName("TC06S-50 an unknown need id cannot be made pending")
        void unknownNeedCannotBeMadePending(String needId) throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            assertEquals(List.of("There is no scheduling need with the id " + needId),
                    errorsOf(() -> service.markSchedulingNeedPending(ADMISSIONS, needId)));
        }

        @Test
        @DisplayName("TC06S-51 an empty need id cannot be booked")
        void emptyNeedIdCannotBeBooked() throws AccessDeniedException, ValidationException {
            service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            assertAll(
                    () -> assertEquals(List.of("There is no scheduling need with the id "),
                            errorsOf(() -> service.markSchedulingNeedBooked(ADMISSIONS, ""))),
                    () -> assertEquals(SchedulingNeedStatus.PENDING, needs.findById("NEED-0001").getStatus()));
        }

        @Test
        @DisplayName("TC06S-52 an indication and a justification of one character are accepted")
        void oneCharacterTextsAreAccepted() throws AccessDeniedException, ValidationException {
            DiagnosticOrder sent = new DiagnosticOrder(null, PATIENT, "TT-01", PHYSICIAN, "a", "b", null, "URGENT");
            DiagnosticOrder created = service.orderDiagnosticProcedure(PHYSICIAN, sent);
            assertAll(
                    () -> assertEquals("a", created.getIndication()),
                    () -> assertEquals("b", created.getJustification()));
        }

        @Test
        @DisplayName("TC06S-53 the contract has no operation to update, edit, modify, cancel, delete or remove")
        void contractHasNoOperationToChangeAnOrder() {
            List<String> forbidden = List.of("update", "edit", "modify", "cancel", "delete", "remove");
            List<String> names = Arrays.stream(IClinicalOrderService.class.getMethods()).map(Method::getName).toList();
            assertAll(
                    () -> assertEquals(7, names.size(),
                            "UC07 added orderFollowUp: C4 has seven operations and none of them changes an order"),
                    () -> assertFalse(names.stream().anyMatch(name -> forbidden.stream()
                            .anyMatch(prefix -> name.toLowerCase(Locale.ROOT).startsWith(prefix)))));
        }

        @Test
        @DisplayName("TC06S-54 sending again a created order creates a new one and leaves the first unchanged")
        void resentOrderCreatesANewOne() throws AccessDeniedException, ValidationException {
            DiagnosticOrder first = service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            DiagnosticOrder second = service.orderDiagnosticProcedure(PHYSICIAN, first);
            assertAll(
                    () -> assertEquals("ORD-0002", second.getOrderId()),
                    () -> assertSame(first, orders.findById("ORD-0001")),
                    () -> assertEquals("ORD-0001", first.getOrderId()),
                    () -> assertEquals(2, orders.findByPatient(PATIENT).size()));
        }

        @Test
        @DisplayName("TC06S-55 the service with the system clock dates the order and its need today")
        void systemClockDatesTheOrderToday() throws AccessDeniedException, ValidationException {
            ClinicalOrderService withSystemClock = new ClinicalOrderService(roles, audit, parameters, patients);
            LocalDate before = LocalDate.now();
            DiagnosticOrder created = withSystemClock.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            LocalDate after = LocalDate.now();
            List<SchedulingNeed> pending = withSystemClock.getPendingSchedulingNeeds(ADMISSIONS);
            assertAll(
                    () -> assertEquals("ORD-0001", created.getOrderId()),
                    () -> assertTrue(List.of(before, after).contains(created.getCreationDate())),
                    () -> assertEquals(1, pending.size()),
                    () -> assertEquals("NEED-0001", pending.get(0).getNeedId()),
                    () -> assertEquals("ORD-0001", pending.get(0).getOrderId()),
                    () -> assertEquals(created.getCreationDate(), pending.get(0).getDueDate()),
                    () -> assertEquals(SchedulingNeedStatus.PENDING, pending.get(0).getStatus()));
        }
    }

    @Nested
    @DisplayName("8. Seed data (D3, D4)")
    class SeedData {

        @Test
        @DisplayName("loadSeedData populates four orders and needs dated by the service clock without auditing (D3)")
        void seedDataUsesServiceClockAndIsNotAudited() throws AccessDeniedException {
            service.loadSeedData();
            List<SchedulingNeed> pending = service.getPendingSchedulingNeeds(ADMISSIONS);
            assertAll(
                    () -> assertEquals(TODAY, orders.findById("ORD-0001").getCreationDate()),
                    () -> assertEquals(TODAY, orders.findById("ORD-0002").getCreationDate()),
                    () -> assertEquals(TODAY, orders.findById("ORD-0003").getCreationDate()),
                    () -> assertEquals(TODAY, orders.findById("ORD-0004").getCreationDate()),
                    () -> assertEquals(SchedulingNeedStatus.BOOKED, needs.findById("NEED-0001").getStatus()),
                    () -> assertEquals(TODAY, needs.findById("NEED-0001").getDueDate()),
                    () -> assertEquals(TODAY, needs.findById("NEED-0002").getDueDate()),
                    () -> assertEquals(TODAY, needs.findById("NEED-0003").getDueDate()),
                    () -> assertEquals(TODAY, needs.findById("NEED-0004").getDueDate()),
                    () -> assertEquals(2, pending.size()),
                    () -> assertEquals(List.of("NEED-0002", "NEED-0004"),
                            pending.stream().map(SchedulingNeed::getNeedId).toList()),
                    () -> assertTrue(audit.entries().isEmpty()));
        }

        @Test
        @DisplayName("loadSeedData is idempotent when called twice and next ids continue at ORD-0005 and NEED-0005")
        void seedDataIsIdempotentAndIdsContinue() throws AccessDeniedException, ValidationException {
            service.loadSeedData();
            service.loadSeedData();
            DiagnosticOrder created = service.orderDiagnosticProcedure(PHYSICIAN, validOrder());
            List<SchedulingNeed> pending = service.getPendingSchedulingNeeds(ADMISSIONS);
            assertAll(
                    () -> assertEquals("ORD-0005", created.getOrderId()),
                    () -> assertEquals("NEED-0005", pending.get(pending.size() - 1).getNeedId()),
                    () -> assertEquals(3, pending.size()));
        }

        @ParameterizedTest
        @ValueSource(strings = { "ORD-0001", "ORD-0002", "ORD-0003", "ORD-0004" })
        @DisplayName("loadSeedData skips loading if any seed order id already exists (D4)")
        void seedSkippedWhenOrderIdExists(String existingOrderId) {
            orders.save(new DiagnosticOrder(existingOrderId, PATIENT, "TT-01", PHYSICIAN, "Existing", "Keep",
                    TODAY.minusDays(1), "ROUTINE"));
            service.loadSeedData();
            for (String id : List.of("ORD-0001", "ORD-0002", "ORD-0003", "ORD-0004")) {
                if (!id.equals(existingOrderId)) {
                    assertNull(orders.findById(id));
                }
            }
            assertNull(needs.findById("NEED-0001"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "NEED-0001", "NEED-0002", "NEED-0003", "NEED-0004" })
        @DisplayName("loadSeedData skips loading if any seed need id already exists (D4)")
        void seedSkippedWhenNeedIdExists(String existingNeedId) {
            needs.save(new SchedulingNeed(existingNeedId, "ORD-9999", TODAY.minusDays(1),
                    SchedulingNeedStatus.PENDING));
            service.loadSeedData();
            for (String id : List.of("NEED-0001", "NEED-0002", "NEED-0003", "NEED-0004")) {
                if (!id.equals(existingNeedId)) {
                    assertNull(needs.findById(id));
                }
            }
            assertNull(orders.findById("ORD-0001"));
        }
    }
}
