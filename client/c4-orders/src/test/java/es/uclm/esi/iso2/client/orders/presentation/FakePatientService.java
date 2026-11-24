package es.uclm.esi.iso2.client.orders.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.patients.contracts.IPatientService;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.patients.domain.Sex;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Test double of {@link IPatientService} (C3) for the screen of UC06. It keeps the patients in memory, in the order
 * in which they are added, and finds them as the server does, by a part of the identifier or of the full name ignoring
 * case. It can deny every call and fail every search, and it records the text of every search. Registering and
 * modifying patients are not used by UC06.
 */
class FakePatientService implements IPatientService {
    private final List<Patient> patients = new ArrayList<>();
    private final List<String> searches = new ArrayList<>();

    private boolean denyAll;
    private RuntimeException failure;

    /**
     * The usual data: the active patients P-0001 Lucía and P-0002 Mario of the seed data of C3, and the invented
     * inactive patient P-0009 Inés, without a date of birth.
     */
    static FakePatientService withThreePatients() {
        FakePatientService service = new FakePatientService();
        service.patients.add(new Patient("P-0001", "HC-0000001", "", "Lucía Ejemplo Prueba",
                LocalDate.of(1980, 3, 14), Sex.FEMALE, "", "", "", true));
        service.patients.add(new Patient("P-0002", "HC-0000002", "12345678Z", "Mario Demo Simulado",
                LocalDate.of(1975, 11, 2), Sex.MALE, "", "", "", true));
        service.patients.add(new Patient("P-0009", "HC-0000009", "", "Inés Inactiva Inventada", null,
                Sex.UNSPECIFIED, "", "", "", false));
        return service;
    }

    /** From now on every call is denied. */
    void denyAll() {
        denyAll = true;
    }

    /** From now on every search fails with this exception, as when the server cannot be reached. */
    void failSearches(RuntimeException exception) {
        failure = exception;
    }

    /** The texts of every search, in order. */
    List<String> searches() {
        return searches;
    }

    @Override
    public List<Patient> searchPatients(String requester, String filters) throws AccessDeniedException {
        if (denyAll) {
            throw new AccessDeniedException(requester);
        }
        if (failure != null) {
            throw failure;
        }
        searches.add(filters);
        String text = filters.toLowerCase(Locale.ROOT);
        return patients.stream().filter(patient -> patient.getIdentifier().toLowerCase(Locale.ROOT).contains(text)
                || patient.getFullName().toLowerCase(Locale.ROOT).contains(text)).toList();
    }

    @Override
    public Patient getPatient(String requester, String patientId) {
        throw new UnsupportedOperationException("Not used by the screen of UC06");
    }

    @Override
    public Patient registerPatient(String requester, Patient patient) {
        throw new UnsupportedOperationException("Not used by UC06");
    }

    @Override
    public Patient modifyPatient(String requester, String patientId, Patient patient) {
        throw new UnsupportedOperationException("Not used by UC06");
    }
}
