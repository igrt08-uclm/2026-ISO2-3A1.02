package es.uclm.esi.iso2.server.patients.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IAuditRecorder;
import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.patients.contracts.DuplicatePatientException;
import es.uclm.esi.iso2.common.patients.contracts.IPatientService;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.patients.persistence.IPatientRepository;
import es.uclm.esi.iso2.server.patients.persistence.InMemoryPatientRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Manages the demographic data of the patients (UC12). Every operation checks that the requester is an active user
 * with the permission {@code MANAGE_PATIENTS}, validates the data, never deletes, and registers every access and
 * change in the audit log (D26). The server gives each patient its id ({@code P-0001}, {@code P-0002}...).
 */
public class PatientService implements IPatientService {
    private static final String MANAGE_PATIENTS = "MANAGE_PATIENTS";
    private static final String ACCESSED = "PATIENT_ACCESSED";
    private static final String REGISTERED = "PATIENT_REGISTERED";
    private static final String MODIFIED = "PATIENT_MODIFIED";
    private static final String ACCESS_DENIED = "PATIENT_ACCESS_DENIED";
    private static final String PATIENTS = "patients";
    private static final String PATIENT = "PATIENT:";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");
    private static final LocalDate EARLIEST_BIRTH = LocalDate.of(1900, 1, 1);
    private static final Pattern PHONE_DIGITS = Pattern.compile("\\d{9,15}");

    private final IPatientRepository patients;
    private final IRoleManagementService roleManagementService;
    private final IAuditRecorder auditRecorder;
    private int lastNumber;

    /**
     * Creates the service with the given repository.
     *
     * @param patients where the patients are kept
     * @param roleManagementService used to know the permissions of the requester (contract of C1)
     * @param auditRecorder where every access and change is registered (contract of C1)
     */
    public PatientService(IPatientRepository patients, IRoleManagementService roleManagementService,
            IAuditRecorder auditRecorder) {
        this.patients = patients;
        this.roleManagementService = roleManagementService;
        this.auditRecorder = auditRecorder;
    }

    /**
     * Creates the service with an empty in-memory repository (D-030).
     *
     * @param roleManagementService used to know the permissions of the requester (contract of C1)
     * @param auditRecorder where every access and change is registered (contract of C1)
     */
    public PatientService(IRoleManagementService roleManagementService, IAuditRecorder auditRecorder) {
        this(new InMemoryPatientRepository(), roleManagementService, auditRecorder);
    }

    /**
     * Loads three invented example patients, only if the system starts with no patients at all. It is called when the
     * program starts, not by a user, so it does not need a permission and it is not registered in the audit log.
     */
    public void loadSeedData() {
        if (patients.search("").isEmpty()) {
            PatientSeedData.patients().forEach(this::saveNew);
        }
    }

    @Override
    public List<Patient> searchPatients(String requester, String filters) throws AccessDeniedException {
        checkPermission(requester, PATIENTS);
        List<Patient> found = patients.search(filters);
        auditRecorder.register(requester, ACCESSED, PATIENTS);
        return found;
    }

    @Override
    public Patient getPatient(String requester, String patientId) throws AccessDeniedException, ValidationException {
        checkPermission(requester, resourceOf(patientId));
        Patient patient = existing(patientId);
        auditRecorder.register(requester, ACCESSED, resourceOf(patientId));
        return patient;
    }

    @Override
    public Patient registerPatient(String requester, Patient patient)
            throws AccessDeniedException, ValidationException, DuplicatePatientException {
        checkPermission(requester, PATIENTS);
        List<String> errors = validate(patient);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        Patient duplicate = findDuplicate(patient.getIdentifier());
        if (duplicate != null) {
            auditRecorder.register(requester, ACCESSED, resourceOf(duplicate.getPatientId()));
            throw new DuplicatePatientException(duplicate);
        }
        Patient saved = saveNew(patient);
        auditRecorder.register(requester, REGISTERED, resourceOf(saved.getPatientId()));
        return saved;
    }

    @Override
    public Patient modifyPatient(String requester, String patientId, Patient patient)
            throws AccessDeniedException, ValidationException {
        checkPermission(requester, resourceOf(patientId));
        Patient current = existing(patientId);
        List<String> errors = validate(patient);
        if (patient != null && !identifierKey(current.getIdentifier()).equals(identifierKey(patient.getIdentifier()))) {
            errors.add("The identifier cannot be changed");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        Patient saved = new Patient(current.getPatientId(), current.getIdentifier(), clean(patient.getNationalId()),
                clean(patient.getFullName()), patient.getDateOfBirth(), patient.getSex(), clean(patient.getAddress()),
                clean(patient.getPhone()), clean(patient.getEmail()), current.isActive());
        patients.save(saved);
        auditRecorder.register(requester, MODIFIED, resourceOf(saved.getPatientId()));
        return saved;
    }

    /** Gives the patient the next id, cleans its text and saves it. It is the way to add a patient, also for the seed. */
    Patient saveNew(Patient data) {
        lastNumber++;
        Patient saved = new Patient(String.format("P-%04d", lastNumber), clean(data.getIdentifier()),
                clean(data.getNationalId()), clean(data.getFullName()), data.getDateOfBirth(), data.getSex(),
                clean(data.getAddress()), clean(data.getPhone()), clean(data.getEmail()), true);
        patients.save(saved);
        return saved;
    }

    /** The patient with the same identifier, ignoring case, every space and {@code -} (R-02), or {@code null}. */
    private Patient findDuplicate(String identifier) {
        String wanted = identifierKey(identifier);
        for (Patient existing : patients.search("")) {
            if (identifierKey(existing.getIdentifier()).equals(wanted)) {
                return existing;
            }
        }
        return null;
    }

    /** The identifier as it is compared: without case, spaces or {@code -}, so {@code HC 1} and {@code hc-1} match. */
    private static String identifierKey(String identifier) {
        return clean(identifier).replaceAll("[\\s-]", "").toLowerCase(Locale.ROOT);
    }

    /** The requester must exist, be active and have MANAGE_PATIENTS; if not, it is registered and denied. */
    private void checkPermission(String requester, String resource) throws AccessDeniedException {
        if (!roleManagementService.hasPermission(requester, MANAGE_PATIENTS)) {
            auditRecorder.register(requester, ACCESS_DENIED, resource);
            throw new AccessDeniedException(requester);
        }
    }

    private Patient existing(String patientId) throws ValidationException {
        Patient patient = patientId == null ? null : patients.findById(patientId);
        if (patient == null) {
            throw new ValidationException(List.of("There is no patient with the id " + patientId));
        }
        return patient;
    }

    private static String resourceOf(String patientId) {
        return patientId == null ? PATIENTS : PATIENT + patientId;
    }

    private List<String> validate(Patient patient) {
        List<String> errors = new ArrayList<>();
        if (patient == null) {
            errors.add("The patient is required");
            return errors;
        }
        if (clean(patient.getIdentifier()).isEmpty()) {
            errors.add("The identifier is required");
        }
        if (clean(patient.getFullName()).isEmpty()) {
            errors.add("The full name is required");
        }
        if (patient.getDateOfBirth() == null) {
            errors.add("The date of birth is required");
        } else if (patient.getDateOfBirth().isAfter(LocalDate.now())) {
            errors.add("The date of birth cannot be in the future");
        } else if (patient.getDateOfBirth().isBefore(EARLIEST_BIRTH)) {
            errors.add("The date of birth cannot be before 01/01/1900");
        }
        if (patient.getSex() == null) {
            errors.add("The sex is required");
        }
        String email = clean(patient.getEmail());
        if (!email.isEmpty() && !EMAIL.matcher(email).matches()) {
            errors.add("The e-mail must have the form name@domain.ext");
        }
        String phone = clean(patient.getPhone());
        if (!phone.isEmpty() && !PHONE_DIGITS.matcher(digitsOf(phone)).matches()) {
            errors.add("The phone must have 9 to 15 digits");
        }
        return errors;
    }

    /** The phone without its spaces and without a leading plus sign. */
    private static String digitsOf(String phone) {
        String withoutSpaces = phone.replace(" ", "");
        return withoutSpaces.startsWith("+") ? withoutSpaces.substring(1) : withoutSpaces;
    }

    private static String clean(String text) {
        return text == null ? "" : text.strip();
    }
}
