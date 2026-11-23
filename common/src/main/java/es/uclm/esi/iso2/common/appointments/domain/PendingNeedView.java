package es.uclm.esi.iso2.common.appointments.domain;

import java.time.LocalDate;

/**
 * A pending scheduling need of C4 with the data that the admissions staff needs to book it (UC13): the patient, the test
 * type, the priority and the date of the order. It is computed when it is asked for and it is never saved.
 */
public class PendingNeedView {
    private final String needId;
    private final String orderId;
    private final String patientId;
    private final String patientName;
    private final String testTypeId;
    private final String testTypeName;
    private final String priorityId;
    private final LocalDate orderDate;
    private final LocalDate dueDate;

    /**
     * Creates the view.
     *
     * @param needId the id of the scheduling need
     * @param orderId the id of the order that generated it
     * @param patientId the internal id of the patient
     * @param patientName the full name of the patient
     * @param testTypeId the id of the test type
     * @param testTypeName the name of the test type
     * @param priorityId the id of the priority of the order; empty for a follow-up order, which has none
     * @param orderDate the date when the order was created
     * @param dueDate the date from which the need has to be booked
     */
    public PendingNeedView(String needId, String orderId, String patientId, String patientName, String testTypeId,
            String testTypeName, String priorityId, LocalDate orderDate, LocalDate dueDate) {
        this.needId = needId;
        this.orderId = orderId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.testTypeId = testTypeId;
        this.testTypeName = testTypeName;
        this.priorityId = priorityId;
        this.orderDate = orderDate;
        this.dueDate = dueDate;
    }

    /** Returns the id of the scheduling need. */
    public String getNeedId() {
        return needId;
    }

    /** Returns the id of the order that generated it. */
    public String getOrderId() {
        return orderId;
    }

    /** Returns the internal id of the patient. */
    public String getPatientId() {
        return patientId;
    }

    /** Returns the full name of the patient. */
    public String getPatientName() {
        return patientName;
    }

    /** Returns the id of the test type. */
    public String getTestTypeId() {
        return testTypeId;
    }

    /** Returns the name of the test type. */
    public String getTestTypeName() {
        return testTypeName;
    }

    /** Returns the id of the priority, or an empty text for a follow-up order. */
    public String getPriorityId() {
        return priorityId;
    }

    /** Returns the date when the order was created. */
    public LocalDate getOrderDate() {
        return orderDate;
    }

    /** Returns the date from which the need has to be booked. */
    public LocalDate getDueDate() {
        return dueDate;
    }
}
