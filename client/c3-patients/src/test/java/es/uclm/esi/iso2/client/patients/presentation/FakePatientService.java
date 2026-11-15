package es.uclm.esi.iso2.client.patients.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.patients.contracts.DuplicatePatientException;
import es.uclm.esi.iso2.common.patients.contracts.IPatientService;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.patients.domain.Sex;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Test double of {@link IPatientService} for the screen of UC12. It keeps the patients in memory, in the order in
 * which they are added, gives the ids {@code P-0004}, {@code P-0005}… to the new ones and does not validate them, so
 * that a test can see that the screen sends nothing when its own validation fails: a test asks for a rejection of
 * the server with {@link #rejectNextChange}. As the server, it finds the duplicates by the identifier ignoring case
 * and the spaces at the ends, and it can deny every call, as the server does when the user has no permission.
 */
class FakePatientService implements IPatientService {
    private final List<Patient> patients = new ArrayList<>();
    private final List<String> searches = new ArrayList<>();
    private final List<Patient> registrations = new ArrayList<>();
    private final List<String> modifiedIds = new ArrayList<>();
    private final List<Patient> modifications = new ArrayList<>();
    private final List<String> requesters = new ArrayList<>();

    private boolean denyAll;
    private List<String> nextErrors;
    private int nextNumber = 4;

    /** The usual data: the three invented patients of the seed data of the server, P-0001 to P-0003. */
    static FakePatientService withThreePatients() {
        FakePatientService service = new FakePatientService();
        service.add(new Patient("P-0001", "HC-0000001", "", "Lucía Ejemplo Prueba", LocalDate.of(1980, 3, 14),
                Sex.FEMALE, "Calle Inventada 1, 13001 Ciudad Real", "600000001", "lucia.ejemplo@example.test", true));
        service.add(new Patient("P-0002", "HC-0000002", "12345678Z", "Mario Demo Simulado",
                LocalDate.of(1975, 11, 2), Sex.MALE, "Avenida de Muestra 22, 45001 Toledo", "600000002",
                "mario.demo@example.test", true));
        service.add(new Patient("P-0003", "HC-0000003", "", "Elena Muestra Ficticia", LocalDate.of(2001, 7, 25),
                Sex.UNSPECIFIED, "Plaza Ficticia 3, 02001 Albacete", "600000003", "elena.muestra@example.test",
                true));
        return service;
    }

    void add(Patient patient) {
        patients.add(patient);
    }

    /** From now on every call is denied. */
    void denyAll() {
        denyAll = true;
    }

    /** The next registration or modification is rejected with these errors. */
    void rejectNextChange(String... errors) {
        nextErrors = List.of(errors);
    }

    /** The texts of every search, in order. */
    List<String> searches() {
        return searches;
    }

    /** Every patient sent to be registered, even if it was rejected. */
    List<Patient> registrations() {
        return registrations;
    }

    /** The ids of every modification, even if it was rejected. */
    List<String> modifiedIds() {
        return modifiedIds;
    }

    /** Every patient sent to be modified, even if it was rejected. */
    List<Patient> modifications() {
        return modifications;
    }

    /** How many registrations and modifications reached the service. */
    int changes() {
        return registrations.size() + modifications.size();
    }

    /** The requester of every call, in order. */
    List<String> requesters() {
        return requesters;
    }

    /** The patients that are stored. */
    List<Patient> stored() {
        return List.copyOf(patients);
    }

    private void check(String requester) throws AccessDeniedException {
        requesters.add(requester);
        if (denyAll) {
            throw new AccessDeniedException(requester);
        }
    }

    private void rejectIfAsked() throws ValidationException {
        if (nextErrors != null) {
            List<String> errors = nextErrors;
            nextErrors = null;
            throw new ValidationException(errors);
        }
    }

    private static String key(String text) {
        return text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public List<Patient> searchPatients(String requester, String filters) throws AccessDeniedException {
        check(requester);
        searches.add(filters);
        String text = key(filters);
        return patients.stream().filter(patient -> key(patient.getIdentifier()).contains(text)
                || key(patient.getFullName()).contains(text)).toList();
    }

    @Override
    public Patient getPatient(String requester, String patientId) throws AccessDeniedException, ValidationException {
        check(requester);
        return patients.stream().filter(patient -> patient.getPatientId().equals(patientId)).findFirst()
                .orElseThrow(() -> new ValidationException(List.of("The patient does not exist")));
    }

    @Override
    public Patient registerPatient(String requester, Patient patient)
            throws AccessDeniedException, ValidationException, DuplicatePatientException {
        check(requester);
        registrations.add(patient);
        rejectIfAsked();
        for (Patient existing : patients) {
            if (key(existing.getIdentifier()).equals(key(patient.getIdentifier()))) {
                throw new DuplicatePatientException(existing);
            }
        }
        Patient saved = copy(String.format("P-%04d", nextNumber++), patient.getIdentifier(), patient);
        patients.add(saved);
        return saved;
    }

    @Override
    public Patient modifyPatient(String requester, String patientId, Patient patient)
            throws AccessDeniedException, ValidationException {
        check(requester);
        modifiedIds.add(patientId);
        modifications.add(patient);
        rejectIfAsked();
        for (int index = 0; index < patients.size(); index++) {
            Patient existing = patients.get(index);
            if (existing.getPatientId().equals(patientId)) {
                Patient saved = copy(patientId, existing.getIdentifier(), patient);
                patients.set(index, saved);
                return saved;
            }
        }
        throw new ValidationException(List.of("The patient does not exist"));
    }

    private static Patient copy(String patientId, String identifier, Patient data) {
        return new Patient(patientId, identifier, data.getNationalId(), data.getFullName(), data.getDateOfBirth(),
                data.getSex(), data.getAddress(), data.getPhone(), data.getEmail(), true);
    }
}
