package es.uclm.esi.iso2.common.orders.domain;

import java.time.LocalDate;

/**
 * What every clinical order has (UC06, D31): the patient, the test type, the physician who asked for it, the
 * clinical indication, the justification and the date when it was created. The server gives the {@code orderId},
 * the {@code requester} and the {@code creationDate}. An order is never edited, cancelled or deleted (D-039,
 * D-022).
 */
public abstract class ClinicalOrder {
    private final String orderId;
    private final String patientId;
    private final String testTypeId;
    private final String requester;
    private final String indication;
    private final String justification;
    private final LocalDate creationDate;

    /**
     * Creates the data that every order has.
     *
     * @param orderId the id given by the server ({@code ORD-0001}); {@code null} before the order is created
     * @param patientId the internal id of the patient (C3)
     * @param testTypeId the id of the test type of the catalogue (C2)
     * @param requester the username of the physician who asked for the order; the server sets it
     * @param indication the clinical indication
     * @param justification the justification of the order
     * @param creationDate the date when the order was created; the server sets it
     */
    protected ClinicalOrder(String orderId, String patientId, String testTypeId, String requester,
            String indication, String justification, LocalDate creationDate) {
        this.orderId = orderId;
        this.patientId = patientId;
        this.testTypeId = testTypeId;
        this.requester = requester;
        this.indication = indication;
        this.justification = justification;
        this.creationDate = creationDate;
    }

    /** Returns the id given by the server. */
    public String getOrderId() {
        return orderId;
    }

    /** Returns the internal id of the patient. */
    public String getPatientId() {
        return patientId;
    }

    /** Returns the id of the test type. */
    public String getTestTypeId() {
        return testTypeId;
    }

    /** Returns the username of the physician who asked for the order. */
    public String getRequester() {
        return requester;
    }

    /** Returns the clinical indication. */
    public String getIndication() {
        return indication;
    }

    /** Returns the justification. */
    public String getJustification() {
        return justification;
    }

    /** Returns the date when the order was created. */
    public LocalDate getCreationDate() {
        return creationDate;
    }
}
