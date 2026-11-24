package es.uclm.esi.iso2.common.orders.domain;

import java.time.LocalDate;

/**
 * An order of a periodic follow-up procedure (UC07): a clinical order with the number of months between one procedure
 * and the next. When it is created it generates the pending scheduling needs of the next 12 months for C5 (D-040).
 */
public class FollowUpOrder extends ClinicalOrder {
    private final int recurrenceIntervalMonths;

    /**
     * Creates the order.
     *
     * @param orderId the id given by the server ({@code ORD-0001}); {@code null} before the order is created
     * @param patientId the internal id of the patient (C3)
     * @param testTypeId the id of the test type of the catalogue, eligible for follow-up (C2)
     * @param requester the username of the physician who asked for the order; the server sets it
     * @param indication the clinical indication
     * @param justification the justification of the order
     * @param creationDate the date when the order was created; the server sets it
     * @param recurrenceIntervalMonths the number of months between one procedure and the next; positive
     */
    public FollowUpOrder(String orderId, String patientId, String testTypeId, String requester, String indication,
            String justification, LocalDate creationDate, int recurrenceIntervalMonths) {
        super(orderId, patientId, testTypeId, requester, indication, justification, creationDate);
        this.recurrenceIntervalMonths = recurrenceIntervalMonths;
    }

    /** Returns the number of months between one procedure and the next. */
    public int getRecurrenceIntervalMonths() {
        return recurrenceIntervalMonths;
    }
}
