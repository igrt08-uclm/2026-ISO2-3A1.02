package es.uclm.esi.iso2.common.patients.contracts;

import es.uclm.esi.iso2.common.patients.domain.Patient;

/** A patient with the same identifier already exists, so nothing was registered: it carries the existing record. */
public class DuplicatePatientException extends Exception {
    private final transient Patient existingPatient;

    /**
     * Creates the exception.
     *
     * @param existingPatient the patient that already has that identifier
     */
    public DuplicatePatientException(Patient existingPatient) {
        super("A patient with the identifier " + existingPatient.getIdentifier() + " already exists");
        this.existingPatient = existingPatient;
    }

    /** Returns the patient that already has that identifier. */
    public Patient getExistingPatient() {
        return existingPatient;
    }
}
