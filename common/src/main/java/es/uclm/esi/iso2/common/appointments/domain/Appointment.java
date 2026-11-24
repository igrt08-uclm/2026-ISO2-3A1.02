package es.uclm.esi.iso2.common.appointments.domain;

import java.time.LocalDateTime;

/**
 * An appointment of a patient for a test type in a room (UC13, D40). It is created from a pending scheduling need
 * of C4, which is marked as booked, and it keeps the physician of the order (D44). It is never deleted: it is only
 * changed to {@link AppointmentStatus#RESCHEDULED} or {@link AppointmentStatus#CANCELLED} (D-022).
 */
public class Appointment {
    private final String appointmentId;
    private final String needId;
    private final String patientId;
    private final String testTypeId;
    private final String room;
    private final String physician;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final AppointmentStatus status;

    /**
     * Creates the appointment.
     *
     * @param appointmentId the id given by the server ({@code APT-0001})
     * @param needId the id of the scheduling need of C4 that is booked with it
     * @param patientId the internal id of the patient (C3)
     * @param testTypeId the id of the test type of the catalogue (C2)
     * @param room the room where the test type is done (C2)
     * @param physician the username of the physician who ordered it (C4, the requester of the order)
     * @param start the instant when the appointment starts
     * @param end the instant when the appointment ends
     * @param status the state of the appointment
     */
    public Appointment(String appointmentId, String needId, String patientId, String testTypeId, String room,
            String physician, LocalDateTime start, LocalDateTime end, AppointmentStatus status) {
        this.appointmentId = appointmentId;
        this.needId = needId;
        this.patientId = patientId;
        this.testTypeId = testTypeId;
        this.room = room;
        this.physician = physician;
        this.start = start;
        this.end = end;
        this.status = status;
    }

    /** Returns the id given by the server. */
    public String getAppointmentId() {
        return appointmentId;
    }

    /** Returns the id of the scheduling need that was booked. */
    public String getNeedId() {
        return needId;
    }

    /** Returns the internal id of the patient. */
    public String getPatientId() {
        return patientId;
    }

    /** Returns the id of the test type. */
    public String getTestTypeId() {
        return testTypeId;
    }

    /** Returns the room where the test type is done. */
    public String getRoom() {
        return room;
    }

    /** Returns the username of the physician who ordered the test. */
    public String getPhysician() {
        return physician;
    }

    /** Returns the instant when the appointment starts. */
    public LocalDateTime getStart() {
        return start;
    }

    /** Returns the instant when the appointment ends. */
    public LocalDateTime getEnd() {
        return end;
    }

    /** Returns the state of the appointment. */
    public AppointmentStatus getStatus() {
        return status;
    }
}
