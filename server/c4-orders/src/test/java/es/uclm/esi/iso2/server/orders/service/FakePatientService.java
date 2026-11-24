package es.uclm.esi.iso2.server.orders.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.patients.contracts.IPatientService;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.patients.domain.Sex;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Test double of {@link IPatientService} with the patients of C3: {@code P-0001}, {@code P-0002} and {@code P-0003}
 * are active and {@code P-0004} is inactive (the real C3 has no discharge yet, D-024). It counts the calls to
 * {@code getPatient}, so the tests can check when C3 is not reached.
 */
class FakePatientService implements IPatientService {
    private final List<Patient> patients = new ArrayList<>();
    private int getPatientCalls;

    FakePatientService() {
        patients.add(new Patient("P-0001", "HC-0000001", null, "Lucía Ejemplo Prueba", LocalDate.of(1980, 1, 1),
                Sex.FEMALE, null, null, null, true));
        patients.add(new Patient("P-0002", "HC-0000002", null, "Mario Demo Simulado", LocalDate.of(1975, 6, 15),
                Sex.MALE, null, null, null, true));
        patients.add(new Patient("P-0003", "HC-0000003", null, "Elena Muestra Ficticia", LocalDate.of(1990, 3, 20),
                Sex.FEMALE, null, null, null, true));
        patients.add(new Patient("P-0004", "HC-0000004", null, "Paciente Dado De Alta", LocalDate.of(1960, 9, 9),
                Sex.UNSPECIFIED, null, null, null, false));
    }

    @Override
    public Patient getPatient(String requester, String patientId) throws ValidationException {
        getPatientCalls++;
        return patients.stream()
                .filter(patient -> patient.getPatientId().equals(patientId))
                .findFirst()
                .orElseThrow(() -> new ValidationException(List.of("There is no patient with the id " + patientId)));
    }

    @Override
    public List<Patient> searchPatients(String requester, String filters) throws AccessDeniedException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public Patient registerPatient(String requester, Patient patient)
            throws AccessDeniedException, ValidationException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public Patient modifyPatient(String requester, String patientId, Patient patient)
            throws AccessDeniedException, ValidationException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    /** How many times the service asked C3 for a patient. */
    int getPatientCalls() {
        return getPatientCalls;
    }
}
