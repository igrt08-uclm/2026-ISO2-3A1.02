package es.uclm.esi.iso2.common.appointments.contracts;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.TimeSlot;
import es.uclm.esi.iso2.common.appointments.domain.WaitlistEntry;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.time.LocalDate;
import java.util.List;

/**
 * Keeps the waitlist of the patients who wait for an appointment of a test type, ordered by priority tier and by the
 * date in which they were added, and fills the openings that appear when an appointment is cancelled (UC15,
 * C5 1.1.0). Every operation needs the permission {@code MANAGE_APPOINTMENTS} of the admissions staff (D37); a denied
 * attempt and every change are registered in the audit log (UC19). Filling an opening books the appointment through
 * UC13 (the include of D42). An entry is never deleted: its status changes (D-022).
 */
public interface IWaitlistService {
    /**
     * Returns the entries of the waitlist of a test type that are still waiting, ordered by the index of their
     * priority tier in the configured priorities and then by the moment they were added.
     *
     * @param requester the username of the user who asks
     * @param testTypeId the id of the test type (C2)
     * @return the entries that wait, in the order in which they must be offered
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if the test type is not given
     */
    List<WaitlistEntry> getWaitlist(String requester, String testTypeId) throws AccessDeniedException,
            ValidationException;

    /**
     * Adds a pending scheduling need of C4 to the waitlist of a test type, with the priority tier of its patient.
     *
     * @param requester the username of the admissions staff who adds it
     * @param needId the id of the pending scheduling need
     * @param priorityId the id of the priority tier (C2)
     * @return the created entry
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException with every reason if the need is not pending, the tier is not a configured
     *     priority or the need is already waiting; nothing is saved
     */
    WaitlistEntry addToWaitlist(String requester, String needId, String priorityId) throws AccessDeniedException,
            ValidationException;

    /**
     * Changes the priority tier of an entry that is still waiting; its position in the waitlist changes with it.
     *
     * @param requester the username of the admissions staff who changes it
     * @param entryId the id of the entry
     * @param priorityId the id of the new priority tier (C2)
     * @return the entry, with its new tier
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if the entry does not exist, is not waiting, or the tier is not a configured
     *     priority; nothing is saved
     */
    WaitlistEntry changeWaitlistPriority(String requester, String entryId, String priorityId)
            throws AccessDeniedException, ValidationException;

    /**
     * Removes an entry from the waitlist, keeping its record with the status {@code REMOVED} (D-022).
     *
     * @param requester the username of the admissions staff who removes it
     * @param entryId the id of the entry
     * @return the entry, with the status {@code REMOVED}
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if the entry does not exist or is not waiting
     */
    WaitlistEntry removeFromWaitlist(String requester, String entryId) throws AccessDeniedException,
            ValidationException;

    /**
     * Returns the openings of the calendar of a test type between two dates: the free slots of its room, the slots
     * that a cancellation frees included.
     *
     * @param requester the username of the user who asks
     * @param testTypeId the id of the test type (C2)
     * @param fromDate the first day of the range, included
     * @param toDate the last day of the range, included
     * @return the openings, in chronological order
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if the range is not valid, or the test type does not exist or is not active
     */
    List<TimeSlot> getCalendarOpenings(String requester, String testTypeId, LocalDate fromDate, LocalDate toDate)
            throws AccessDeniedException, ValidationException;

    /**
     * Returns the first patient of the waitlist of a test type who could fill an opening: the first entry, in the order
     * of {@link #getWaitlist(String, String)}, whose patient is active and whose scheduling need is still pending.
     *
     * @param requester the username of the user who asks
     * @param testTypeId the id of the test type (C2)
     * @param opening the opening to fill
     * @return the entry that is proposed to fill the opening, or {@code null} if no waiting entry can fill it
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if the test type or the opening is not given
     */
    WaitlistEntry proposePatientForOpening(String requester, String testTypeId, TimeSlot opening)
            throws AccessDeniedException, ValidationException;

    /**
     * Fills an opening with an entry of the waitlist: books the appointment through UC13 (the include of D42) and
     * marks the entry as booked.
     *
     * @param requester the username of the admissions staff who fills it
     * @param entryId the id of the entry of the waitlist
     * @param opening the opening to fill
     * @return the appointment that UC13 books
     * @throws AccessDeniedException if the requester does not have {@code MANAGE_APPOINTMENTS}
     * @throws ValidationException if the entry does not exist or is not waiting, or if UC13 rejects the booking
     *     because the opening is no longer free; nothing is saved
     */
    Appointment fillOpening(String requester, String entryId, TimeSlot opening) throws AccessDeniedException,
            ValidationException;
}
