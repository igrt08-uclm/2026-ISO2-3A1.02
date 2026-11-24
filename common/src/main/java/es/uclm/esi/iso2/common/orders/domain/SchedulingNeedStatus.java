package es.uclm.esi.iso2.common.orders.domain;

/** The state of a scheduling need (UC06, D33): waiting to be booked in C5, or already booked. */
public enum SchedulingNeedStatus {
    /** Waiting for the admissions staff to book an appointment for it (UC13). */
    PENDING,
    /** An appointment was booked for it in C5 (UC13). */
    BOOKED
}
