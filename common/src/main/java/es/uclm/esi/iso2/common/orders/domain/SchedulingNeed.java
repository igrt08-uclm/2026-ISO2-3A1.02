package es.uclm.esi.iso2.common.orders.domain;

import java.time.LocalDate;

/**
 * Something that has to be booked in C5 because of an order (UC06, D33): C4 creates it as pending, and C5 marks it
 * as booked when it books the appointment, or as pending again when the appointment is cancelled. It is never
 * deleted (D-022).
 */
public class SchedulingNeed {
    private final String needId;
    private final String orderId;
    private final LocalDate dueDate;
    private final SchedulingNeedStatus status;

    /**
     * Creates the scheduling need.
     *
     * @param needId the id given by the server ({@code NEED-0001})
     * @param orderId the id of the order that generated it
     * @param dueDate the date from which it has to be booked
     * @param status whether it is pending or booked
     */
    public SchedulingNeed(String needId, String orderId, LocalDate dueDate, SchedulingNeedStatus status) {
        this.needId = needId;
        this.orderId = orderId;
        this.dueDate = dueDate;
        this.status = status;
    }

    /** Returns the id given by the server. */
    public String getNeedId() {
        return needId;
    }

    /** Returns the id of the order that generated it. */
    public String getOrderId() {
        return orderId;
    }

    /** Returns the date from which it has to be booked. */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /** Returns whether it is pending or booked. */
    public SchedulingNeedStatus getStatus() {
        return status;
    }
}
