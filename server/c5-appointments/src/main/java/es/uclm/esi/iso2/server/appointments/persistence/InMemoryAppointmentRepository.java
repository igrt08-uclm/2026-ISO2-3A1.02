package es.uclm.esi.iso2.server.appointments.persistence;

import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Keeps the appointments in memory (D-030), so they are lost when the program closes. */
public class InMemoryAppointmentRepository implements IAppointmentRepository {
    private final Map<String, Appointment> appointments = new LinkedHashMap<>();

    @Override
    public Appointment findById(String appointmentId) {
        return appointments.get(appointmentId);
    }

    @Override
    public List<Appointment> findAll() {
        return List.copyOf(appointments.values());
    }

    @Override
    public List<Appointment> findByPatient(String patientId) {
        return appointments.values().stream()
                .filter(appointment -> Objects.equals(appointment.getPatientId(), patientId))
                .toList();
    }

    @Override
    public List<Appointment> findByRoomAndDateRange(String room, LocalDate fromDate, LocalDate toDate) {
        return appointments.values().stream()
                .filter(appointment -> Objects.equals(appointment.getRoom(), room))
                .filter(appointment -> isInRange(appointment, fromDate, toDate))
                .toList();
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        return appointments.values().stream()
                .filter(appointment -> Objects.equals(appointment.getStart().toLocalDate(), date))
                .toList();
    }

    @Override
    public List<Appointment> findByDateAndRoom(LocalDate date, String room) {
        return appointments.values().stream()
                .filter(appointment -> Objects.equals(appointment.getStart().toLocalDate(), date))
                .filter(appointment -> Objects.equals(appointment.getRoom(), room))
                .toList();
    }

    @Override
    public List<Appointment> findByDateAndPhysician(LocalDate date, String physician) {
        return appointments.values().stream()
                .filter(appointment -> Objects.equals(appointment.getStart().toLocalDate(), date))
                .filter(appointment -> Objects.equals(appointment.getPhysician(), physician))
                .toList();
    }

    @Override
    public void save(Appointment appointment) {
        appointments.put(appointment.getAppointmentId(), appointment);
    }

    private static boolean isInRange(Appointment appointment, LocalDate fromDate, LocalDate toDate) {
        LocalDate day = appointment.getStart().toLocalDate();
        return !day.isBefore(fromDate) && !day.isAfter(toDate);
    }
}
