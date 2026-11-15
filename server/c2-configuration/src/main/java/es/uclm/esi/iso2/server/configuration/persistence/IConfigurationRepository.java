package es.uclm.esi.iso2.server.configuration.persistence;

import java.util.List;

/**
 * Keeps the entries of one kind of the configuration (test types, protocols, schedules, policies or priorities).
 * Nothing is deleted: an entry is replaced by a deactivated copy.
 *
 * @param <T> the kind of entry
 */
public interface IConfigurationRepository<T> {
    /**
     * Finds an entry.
     *
     * @param id the identifier of the entry
     * @return the entry, or {@code null} if there is none with that id
     */
    T findById(String id);

    /** Returns every entry, in the order in which they were first saved. */
    List<T> findAll();

    /**
     * Saves an entry, replacing the one with the same id if there is one.
     *
     * @param entry the entry to save
     */
    void save(T entry);
}
