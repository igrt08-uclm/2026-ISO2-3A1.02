package es.uclm.esi.iso2.common.appointments.contracts;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.appointments.domain.ScheduleEntry;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reads the daily schedule of the admissions staff and the worklists of a room or of a physician (UC14, C5 1.2.0).
 * Only the booked appointments of the day are shown, in time order, with the patient, the test type and its
 * preparation; a cancelled appointment is never shown (D43). Every access needs the permission
 * {@code MANAGE_APPOINTMENTS} (D37) and is registered in the audit log as {@code SCHEDULE_ACCESSED} or
 * {@code WORKLIST_ACCESSED} (UC19). It only reads: it changes no data.
 */
public interface IDailyScheduleService {
    /**
     * Returns the schedule of a day, grouped by room and in time order inside each room.
     *
     * @param requester the username of the admissions staff
     * @param date the day of the schedule
     * @return the booked appointments of that day, per room, in time order
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if the day is not given, or the data of an appointment cannot be read
     */
    Map<String, List<ScheduleEntry>> getDailySchedule(String requester, LocalDate date)
            throws AccessDeniedException, ValidationException;

    /**
     * Returns the worklist of a room for a day, in time order.
     *
     * @param requester the username of the admissions staff
     * @param date the day of the worklist
     * @param room the room
     * @return the booked appointments of that room that day; empty if there are none
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if the day or the room is not given, or the data of an appointment cannot be read
     */
    List<ScheduleEntry> getRoomWorklist(String requester, LocalDate date, String room)
            throws AccessDeniedException, ValidationException;

    /**
     * Returns the worklist of a physician for a day, in time order.
     *
     * @param requester the username of the admissions staff
     * @param date the day of the worklist
     * @param physician the username of the physician who ordered the tests
     * @return the booked appointments of that physician that day; empty if there are none
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if the day or the physician is not given, or the data of an appointment cannot be
     *     read
     */
    List<ScheduleEntry> getPhysicianWorklist(String requester, LocalDate date, String physician)
            throws AccessDeniedException, ValidationException;
}
