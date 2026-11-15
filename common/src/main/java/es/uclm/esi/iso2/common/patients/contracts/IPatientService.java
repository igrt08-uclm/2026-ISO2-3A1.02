package es.uclm.esi.iso2.common.patients.contracts;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.util.List;

/**
 * Manages the demographic data of the patients (UC12): search, read, register and modify. Every operation needs the
 * permission {@code MANAGE_PATIENTS} of the requester, and every access and change is registered in the audit log. A
 * patient is never deleted.
 */
public interface IPatientService {
    /**
     * Searches the patients whose identifier or full name contains the text, ignoring case. An empty or {@code null}
     * text returns every patient. The access is registered once.
     *
     * @param requester the username of the user who asks
     * @param filters the text to look for
     * @throws AccessDeniedException if the requester does not have the permission
     */
    List<Patient> searchPatients(String requester, String filters) throws AccessDeniedException;

    /**
     * Returns one patient. The access is registered.
     *
     * @param requester the username of the user who asks
     * @param patientId the internal id of the patient
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if there is no patient with that id
     */
    Patient getPatient(String requester, String patientId) throws AccessDeniedException, ValidationException;

    /**
     * Registers a new patient, to whom the server gives the {@code patientId}.
     *
     * @param requester the username of the user who asks
     * @param patient the data of the patient; its {@code patientId} is ignored
     * @return the registered patient, with its {@code patientId}
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if a field is missing or invalid; every error comes together and nothing is saved
     * @throws DuplicatePatientException if a patient with the same identifier already exists; nothing is saved and
     *         the exception carries the existing patient
     */
    Patient registerPatient(String requester, Patient patient)
            throws AccessDeniedException, ValidationException, DuplicatePatientException;

    /**
     * Changes the data of a patient. The identifier cannot be changed.
     *
     * @param requester the username of the user who asks
     * @param patientId the internal id of the patient to change
     * @param patient the new data
     * @return the saved patient
     * @throws AccessDeniedException if the requester does not have the permission
     * @throws ValidationException if the patient does not exist, a field is invalid or the identifier changed
     */
    Patient modifyPatient(String requester, String patientId, Patient patient)
            throws AccessDeniedException, ValidationException;
}
