package es.uclm.esi.iso2.common.orders.domain;

import java.time.LocalDate;

/**
 * An order of a diagnostic procedure (UC06): a clinical order with one of the priorities configured in C2
 * (urgent, preferential or routine). When it is created it generates one pending scheduling need for C5.
 */
public class DiagnosticOrder extends ClinicalOrder {
    private final String priorityId;

    /**
     * Creates the order.
     *
     * @param orderId the id given by the server ({@code ORD-0001}); {@code null} before the order is created
     * @param patientId the internal id of the patient (C3)
     * @param testTypeId the id of the test type of the catalogue (C2)
     * @param requester the username of the physician who asked for the order; the server sets it
     * @param indication the clinical indication
     * @param justification the justification of the order
     * @param creationDate the date when the order was created; the server sets it
     * @param priorityId the id of the priority (C2)
     */
    public DiagnosticOrder(String orderId, String patientId, String testTypeId, String requester,
            String indication, String justification, LocalDate creationDate, String priorityId) {
        super(orderId, patientId, testTypeId, requester, indication, justification, creationDate);
        this.priorityId = priorityId;
    }

    /** Returns the id of the priority. */
    public String getPriorityId() {
        return priorityId;
    }
}
