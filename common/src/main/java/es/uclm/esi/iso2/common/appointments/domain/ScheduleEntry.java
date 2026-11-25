package es.uclm.esi.iso2.common.appointments.domain;

/**
 * One line of the daily schedule or of a worklist (UC14, D43): a booked appointment of a day with the data that the
 * admissions staff needs to prepare it, which come from the patient (C3) and from the test type (C2). A cancelled
 * appointment is never shown.
 */
public class ScheduleEntry {
    private final Appointment appointment;
    private final String patientName;
    private final String testTypeName;
    private final String preparation;

    /**
     * Creates the line of the schedule.
     *
     * @param appointment the booked appointment
     * @param patientName the full name of the patient (C3)
     * @param testTypeName the name of the test type (C2)
     * @param preparation the preparation that the patient needs for that test type (C2)
     */
    public ScheduleEntry(Appointment appointment, String patientName, String testTypeName, String preparation) {
        this.appointment = appointment;
        this.patientName = patientName;
        this.testTypeName = testTypeName;
        this.preparation = preparation;
    }

    /** Returns the booked appointment. */
    public Appointment getAppointment() {
        return appointment;
    }

    /** Returns the full name of the patient. */
    public String getPatientName() {
        return patientName;
    }

    /** Returns the name of the test type. */
    public String getTestTypeName() {
        return testTypeName;
    }

    /** Returns the preparation that the patient needs. */
    public String getPreparation() {
        return preparation;
    }
}
