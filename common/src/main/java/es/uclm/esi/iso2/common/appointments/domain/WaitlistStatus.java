package es.uclm.esi.iso2.common.appointments.domain;

/** The state of an entry of the waitlist (UC15, D42): waiting, booked in an opening, or removed. It is never deleted
 * (D-022). */
public enum WaitlistStatus {
    /** The patient is waiting for an appointment of the test type. */
    WAITING,
    /** An opening was filled and the patient has an appointment booked through UC13. */
    BOOKED,
    /** The admissions staff removed the patient from the waitlist; its record is kept (D-022). */
    REMOVED
}
