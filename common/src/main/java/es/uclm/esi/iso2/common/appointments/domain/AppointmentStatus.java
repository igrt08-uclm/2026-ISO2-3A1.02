package es.uclm.esi.iso2.common.appointments.domain;

/** The state of an appointment (UC13, D40): booked, moved to another slot, or cancelled. It is never deleted
 * (D-022). */
public enum AppointmentStatus {
    /** The appointment was booked in a free slot. */
    BOOKED,
    /** The appointment was moved to another free slot. */
    RESCHEDULED,
    /** The appointment was cancelled and its slot is free again. */
    CANCELLED
}
