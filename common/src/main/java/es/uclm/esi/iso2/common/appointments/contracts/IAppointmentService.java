package es.uclm.esi.iso2.common.appointments.contracts;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.PendingNeedView;
import es.uclm.esi.iso2.common.appointments.domain.TimeSlot;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.time.LocalDate;
import java.util.List;

/**
 * Books, moves and cancels the appointments of the patients (UC13, C5 1.0.0). Every operation needs the permission
 * {@code MANAGE_APPOINTMENTS} of the admissions staff (D37); a denied attempt and every booking, move or cancellation is
 * registered in the audit log (UC19). An appointment is never deleted: it is cancelled and its slot is free again
 * (D-022).
 */
public interface IAppointmentService {
    /**
     * Returns the pending scheduling needs of C4 with the data of the patient, the test type and the priority, so that the
     * admissions staff can choose the one to book.
     *
     * @param requester the username of the user who asks
     * @return the pending needs, in the order in which C4 created them
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if one of the data of the needs cannot be read
     */
    List<PendingNeedView> getPendingSchedulingNeeds(String requester) throws AccessDeniedException, ValidationException;

    /**
     * Returns the free slots of the room of a test type between two dates, built from the schedule of the room, the
     * duration of the test type and the appointments that are not cancelled.
     *
     * @param requester the username of the user who asks
     * @param testTypeId the id of the test type (C2)
     * @param fromDate the first day of the range, included
     * @param toDate the last day of the range, included
     * @return the free slots, in chronological order
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if the range is not valid, the test type does not exist or is not active
     */
    List<TimeSlot> getFreeSlots(String requester, String testTypeId, LocalDate fromDate, LocalDate toDate)
            throws AccessDeniedException, ValidationException;

    /**
     * Books a free slot for a pending scheduling need and marks the need as booked in C4. The physician of the appointment
     * is the one who ordered the test.
     *
     * @param requester the username of the admissions staff who books
     * @param needId the id of the pending scheduling need
     * @param slot the slot to book
     * @return the created appointment
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException with every reason if the booking is not valid; nothing is saved
     */
    Appointment bookAppointment(String requester, String needId, TimeSlot slot)
            throws AccessDeniedException, ValidationException;

    /**
     * Moves an appointment to a new free slot; the old slot is free again.
     *
     * @param requester the username of the admissions staff who moves it
     * @param appointmentId the id of the appointment
     * @param newSlot the new slot
     * @return the appointment, with its new slot and the status {@code RESCHEDULED}
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException with every reason if the move is not valid; nothing is saved
     */
    Appointment rescheduleAppointment(String requester, String appointmentId, TimeSlot newSlot)
            throws AccessDeniedException, ValidationException;

    /**
     * Cancels an appointment: its slot is free again and its scheduling need is pending in C4 again.
     *
     * @param requester the username of the admissions staff who cancels it
     * @param appointmentId the id of the appointment
     * @return the appointment, with the status {@code CANCELLED}
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if the appointment does not exist or is already cancelled
     */
    Appointment cancelAppointment(String requester, String appointmentId)
            throws AccessDeniedException, ValidationException;

    /**
     * Returns every appointment, cancelled ones included.
     *
     * @param requester the username of the user who asks
     * @return the appointments, in the order in which they were booked
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     */
    List<Appointment> getAppointments(String requester) throws AccessDeniedException;

    /**
     * Returns the appointments of a patient, cancelled ones included.
     *
     * @param requester the username of the user who asks
     * @param patientId the internal id of the patient
     * @return the appointments of the patient; empty if there are none
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if the patient id is not given
     */
    List<Appointment> getAppointmentsOfPatient(String requester, String patientId)
            throws AccessDeniedException, ValidationException;
}
