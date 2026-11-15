package es.uclm.esi.iso2.server.patients.persistence;

import es.uclm.esi.iso2.common.patients.domain.Patient;
import java.util.List;

/** Where the patients are kept (UC12). C3 is the only component that writes them. */
public interface IPatientRepository {
    /**
     * Finds a patient by its internal id.
     *
     * @param patientId the internal id, for example {@code P-0001}
     * @return the patient, or {@code null} if there is none
     */
    Patient findById(String patientId);

    /**
     * Finds a patient by its identifier (the health card number), ignoring case and the spaces at the ends.
     *
     * @param identifier the identifier
     * @return the patient, or {@code null} if there is none
     */
    Patient findByIdentifier(String identifier);

    /**
     * Searches the patients whose identifier or full name contains the text, ignoring case.
     *
     * @param filters the text to look for; an empty or {@code null} text matches every patient
     * @return the patients found, in the order in which they were saved
     */
    List<Patient> search(String filters);

    /**
     * Saves a patient: it is added, or it replaces the one with the same {@code patientId}.
     *
     * @param patient the patient to save
     */
    void save(Patient patient);
}
