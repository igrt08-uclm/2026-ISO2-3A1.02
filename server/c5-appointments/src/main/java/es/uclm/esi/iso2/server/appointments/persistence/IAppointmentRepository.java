package es.uclm.esi.iso2.server.appointments.persistence;

import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import java.time.LocalDate;
import java.util.List;

/** Where the appointments are kept (UC13). C5 is the only component that writes them (D-023). */
public interface IAppointmentRepository {
    /**
     * Finds an appointment by its id.
     *
     * @param appointmentId the id, for example {@code APT-0001}
     * @return the appointment, or {@code null} if there is none
     */
    Appointment findById(String appointmentId);

    /**
     * Returns every appointment, cancelled ones included.
     *
     * @return the appointments, in the order in which they were saved
     */
    List<Appointment> findAll();

    /**
     * Returns the appointments of a patient.
     *
     * @param patientId the internal id of the patient
     * @return the appointments of that patient, cancelled ones included
     */
    List<Appointment> findByPatient(String patientId);

    /**
     * Returns the appointments of a room between two dates, both included.
     *
     * @param room the room
     * @param fromDate the first day of the range
     * @param toDate the last day of the range
     * @return the appointments of the room in the range
     */
    List<Appointment> findByRoomAndDateRange(String room, LocalDate fromDate, LocalDate toDate);

    /**
     * Returns the appointments of a day.
     *
     * @param date the day
     * @return the appointments of that day
     */
    List<Appointment> findByDate(LocalDate date);

    /**
     * Returns the appointments of a day in a room.
     *
     * @param date the day
     * @param room the room
     * @return the appointments of that day in that room
     */
    List<Appointment> findByDateAndRoom(LocalDate date, String room);

    /**
     * Returns the appointments of a day of a physician.
     *
     * @param date the day
     * @param physician the username of the physician
     * @return the appointments of that day of that physician
     */
    List<Appointment> findByDateAndPhysician(LocalDate date, String physician);

    /**
     * Saves an appointment: it is added, or it replaces the one with the same {@code appointmentId}.
     *
     * @param appointment the appointment to save
     */
    void save(Appointment appointment);
}
