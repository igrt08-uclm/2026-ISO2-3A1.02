package es.uclm.esi.iso2.client.orders.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.orders.contracts.IClinicalOrderService;
import es.uclm.esi.iso2.common.orders.domain.ClinicalOrder;
import es.uclm.esi.iso2.common.orders.domain.DiagnosticOrder;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test double of {@link IClinicalOrderService} for the screen of UC06. It keeps the orders of each patient in memory,
 * in the order in which they are added, and gives the ids {@code ORD-0003}, {@code ORD-0004}… to the new ones. It
 * does not validate, so that a test can see that the screen sends nothing when its own validation fails: a test asks
 * for a rejection of the server with {@link #rejectNextOrder}, for a failure with {@link #failNextOrder} or
 * {@link #failReads}, and for the denied access with {@link #denyAll}. The operations of other use cases are not used.
 */
class FakeClinicalOrderService implements IClinicalOrderService {
    private final Map<String, List<ClinicalOrder>> orders = new LinkedHashMap<>();
    private final List<DiagnosticOrder> sent = new ArrayList<>();
    private final List<String> requesters = new ArrayList<>();
    private final List<String> reads = new ArrayList<>();

    private boolean denyAll;
    private List<String> nextErrors;
    private RuntimeException nextFailure;
    private Exception readFailure;
    private int nextNumber = 3;

    /** The usual data: P-0001 has two orders (a routine chest X-ray and an urgent knee MRI), the others none. */
    static FakeClinicalOrderService withTwoOrdersOfLucia() {
        FakeClinicalOrderService service = new FakeClinicalOrderService();
        service.add(new DiagnosticOrder("ORD-0001", "P-0001", "XRAY-CHEST", "mgarcia", "Persistent cough",
                "Rule out pneumonia", LocalDate.of(2026, 11, 20), "ROUTINE"));
        service.add(new DiagnosticOrder("ORD-0002", "P-0001", "MRI-KNEE", "mgarcia", "Knee pain after a fall",
                "Rule out a torn ligament", LocalDate.of(2026, 11, 21), "URGENT"));
        return service;
    }

    void add(ClinicalOrder order) {
        orders.computeIfAbsent(order.getPatientId(), key -> new ArrayList<>()).add(order);
    }

    /** From now on every call is denied. */
    void denyAll() {
        denyAll = true;
    }

    /** The next order is rejected with these errors. */
    void rejectNextOrder(String... errors) {
        nextErrors = List.of(errors);
    }

    /** The next order fails with this exception, as when the server cannot be reached. */
    void failNextOrder(RuntimeException failure) {
        nextFailure = failure;
    }

    /** Every reading of the orders of a patient fails with this exception (a {@link ValidationException} or a
     *  {@link RuntimeException}). */
    void failReads(Exception failure) {
        readFailure = failure;
    }

    /** Every order sent to be created, even if it was rejected. */
    List<DiagnosticOrder> sent() {
        return sent;
    }

    /** The requester of every call, in order. */
    List<String> requesters() {
        return requesters;
    }

    /** The patient id of every reading of the orders, in order. */
    List<String> reads() {
        return reads;
    }

    private void check(String requester) throws AccessDeniedException {
        requesters.add(requester);
        if (denyAll) {
            throw new AccessDeniedException(requester);
        }
    }

    @Override
    public DiagnosticOrder orderDiagnosticProcedure(String requester, DiagnosticOrder order)
            throws AccessDeniedException, ValidationException {
        check(requester);
        sent.add(order);
        if (nextErrors != null) {
            List<String> errors = nextErrors;
            nextErrors = null;
            throw new ValidationException(errors);
        }
        if (nextFailure != null) {
            RuntimeException failure = nextFailure;
            nextFailure = null;
            throw failure;
        }
        DiagnosticOrder created = new DiagnosticOrder(String.format("ORD-%04d", nextNumber++), order.getPatientId(),
                order.getTestTypeId(), requester, order.getIndication(), order.getJustification(), LocalDate.now(),
                order.getPriorityId());
        add(created);
        return created;
    }

    @Override
    public List<ClinicalOrder> getOrdersOfPatient(String requester, String patientId)
            throws AccessDeniedException, ValidationException {
        check(requester);
        reads.add(patientId);
        if (readFailure instanceof ValidationException validation) {
            throw validation;
        }
        if (readFailure instanceof RuntimeException runtime) {
            throw runtime;
        }
        return List.copyOf(orders.getOrDefault(patientId, List.of()));
    }

    @Override
    public List<SchedulingNeed> getPendingSchedulingNeeds(String requester) {
        throw new UnsupportedOperationException("Not used by UC06");
    }

    @Override
    public void markSchedulingNeedBooked(String requester, String needId) {
        throw new UnsupportedOperationException("Not used by UC06");
    }

    @Override
    public ClinicalOrder getOrder(String requester, String orderId) {
        throw new UnsupportedOperationException("Not used by UC06");
    }

    @Override
    public void markSchedulingNeedPending(String requester, String needId) {
        throw new UnsupportedOperationException("Not used by UC06");
    }
}
