package es.uclm.esi.iso2.server.appointments.persistence;

import es.uclm.esi.iso2.common.appointments.domain.WaitlistEntry;
import java.util.List;

/** Keeps the entries of the waitlist (UC15, D42). The service depends on this interface, not on the way the entries
 * are kept (D-030). */
public interface IWaitlistRepository {
    /**
     * Returns the entry with that id.
     *
     * @param entryId the id of the entry
     * @return the entry, or {@code null} if there is no entry with that id
     */
    WaitlistEntry findById(String entryId);

    /**
     * Returns every entry, whatever its status.
     *
     * @return the entries, in the order in which they were saved
     */
    List<WaitlistEntry> findAll();

    /**
     * Returns the entries of a test type that are still waiting.
     *
     * @param testTypeId the id of the test type
     * @return the entries with the status {@code WAITING}, in the order in which they were saved
     */
    List<WaitlistEntry> findWaitingByTestType(String testTypeId);

    /**
     * Returns the entry that waits for a pending scheduling need.
     *
     * @param needId the id of the scheduling need
     * @return the entry of that need with the status {@code WAITING}, or {@code null} if there is none
     */
    WaitlistEntry findWaitingByNeed(String needId);

    /**
     * Saves a new entry or replaces the entry with the same id.
     *
     * @param entry the entry to save
     */
    void save(WaitlistEntry entry);
}
