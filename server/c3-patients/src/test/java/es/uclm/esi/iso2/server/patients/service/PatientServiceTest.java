package es.uclm.esi.iso2.server.patients.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.patients.contracts.DuplicatePatientException;
import es.uclm.esi.iso2.common.patients.contracts.IPatientService;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.patients.domain.Sex;
import es.uclm.esi.iso2.common.shared.ValidationException;
import es.uclm.esi.iso2.server.patients.persistence.InMemoryPatientRepository;
import es.uclm.esi.iso2.server.patients.service.RecordingAuditRecorder.AuditEntry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of {@link PatientService} (UC12 server, #46). Each test names the case of the test design (TC12S-xx) that it
 * checks. The service uses an empty in-memory repository, a fake of the role management of C1 and a recording audit;
 * every test starts with the three seed patients ({@code P-0001} to {@code P-0003}) and an empty audit, so the next
 * id is always {@code P-0004}. The dates "today" and "tomorrow" come from {@link LocalDate#now()}, so the tests do not
 * depend on the day they are run.
 */
@DisplayName("PatientService")
class PatientServiceTest {
    private static final String ADMISSIONS = "lmartin";
    private static final String ADMIN = "admin";
    private static final String PHYSICIAN = "mgarcia";
    private static final String PATIENT_USER = "jfernandez";
    private static final String INACTIVE = "pnavarro";
    private static final String UNKNOWN = "nobody";

    private static final String ACCESSED = "PATIENT_ACCESSED";
    private static final String REGISTERED = "PATIENT_REGISTERED";
    private static final String MODIFIED = "PATIENT_MODIFIED";
    private static final String DENIED = "PATIENT_ACCESS_DENIED";
    private static final String PATIENTS = "patients";

    private static final String PATIENT_REQUIRED = "The patient is required";
    private static final String IDENTIFIER_REQUIRED = "The identifier is required";
    private static final String NAME_REQUIRED = "The full name is required";
    private static final String DATE_REQUIRED = "The date of birth is required";
    private static final String DATE_FUTURE = "The date of birth cannot be in the future";
    private static final String SEX_REQUIRED = "The sex is required";
    private static final String EMAIL_FORM = "The e-mail must have the form name@domain.ext";
    private static final String PHONE_DIGITS = "The phone must have 9 to 15 digits";
    private static final String IDENTIFIER_FIXED = "The identifier cannot be changed";

    private static final LocalDate BIRTH = LocalDate.of(1990, 5, 10);

    private RecordingAuditRecorder audit;
    private PatientService service;

    @BeforeEach
    void setUp() {
        audit = new RecordingAuditRecorder();
        service = new PatientService(new FakeRoleManagementService(), audit);
        service.loadSeedData();
    }

    /** The valid patient of the test design. */
    private static Patient ana() {
        return patient("HC-1234567", "Ana Prueba Inventada", BIRTH, Sex.FEMALE, "600111222", "ana.prueba@example.test");
    }

    private static Patient patient(String identifier, String fullName, LocalDate dateOfBirth, Sex sex, String phone,
            String email) {
        return new Patient(null, identifier, "00000000T", fullName, dateOfBirth, sex, "Calle Falsa 1", phone, email,
                true);
    }

    private static Patient withEmail(String email) {
        return patient("HC-1234567", "Ana Prueba Inventada", BIRTH, Sex.FEMALE, "600111222", email);
    }

    private static Patient withPhone(String phone) {
        return patient("HC-1234567", "Ana Prueba Inventada", BIRTH, Sex.FEMALE, phone, "ana.prueba@example.test");
    }

    /** New data for the seed patient P-0001 (Lucía), keeping its identifier. */
    private static Patient luciaChanged(String identifier) {
        return new Patient(null, identifier, "11111111H", "Lucía Ejemplo Cambiada", LocalDate.of(1981, 4, 15),
                Sex.UNSPECIFIED, "Calle Nueva 9, 13002 Ciudad Real", "+34 600 999 888", "lucia.nueva@example.test",
                true);
    }

    private List<Patient> allPatients() throws AccessDeniedException {
        return service.searchPatients(ADMISSIONS, "");
    }

    private List<String> idsOf(List<Patient> patients) {
        return patients.stream().map(Patient::getPatientId).toList();
    }

    /** Runs the act and checks the messages of the {@link ValidationException}. */
    private static void assertErrors(List<String> expected, Executable act) {
        ValidationException exception = assertThrows(ValidationException.class, act);
        assertEquals(expected, exception.getErrors());
    }

    /** Registers a patient that must fail validation and checks that nothing was saved or audited. */
    private void assertRegisterRejected(List<String> expected, Patient patient) throws Exception {
        assertErrors(expected, () -> service.registerPatient(ADMISSIONS, patient));
        assertTrue(audit.entries().isEmpty(), "nothing audited");
        assertEquals(3, allPatients().size(), "nothing saved");
    }

    private void assertAllFieldsEqual(Patient expected, Patient actual) {
        assertAll(
                () -> assertEquals(expected.getPatientId(), actual.getPatientId()),
                () -> assertEquals(expected.getIdentifier(), actual.getIdentifier()),
                () -> assertEquals(expected.getNationalId(), actual.getNationalId()),
                () -> assertEquals(expected.getFullName(), actual.getFullName()),
                () -> assertEquals(expected.getDateOfBirth(), actual.getDateOfBirth()),
                () -> assertEquals(expected.getSex(), actual.getSex()),
                () -> assertEquals(expected.getAddress(), actual.getAddress()),
                () -> assertEquals(expected.getPhone(), actual.getPhone()),
                () -> assertEquals(expected.getEmail(), actual.getEmail()),
                () -> assertEquals(expected.isActive(), actual.isActive()));
    }

    @Nested
    @DisplayName("Permission")
    class PermissionTests {

        @BeforeEach
        void clearAudit() {
            audit.clear();
        }

        /** Runs the four operations with the requester, checking that each one is denied with its resource. */
        private void assertDeniedInEveryOperation(String requester) throws Exception {
            // Act
            AccessDeniedException search = assertThrows(AccessDeniedException.class,
                    () -> service.searchPatients(requester, ""));
            AccessDeniedException get = assertThrows(AccessDeniedException.class,
                    () -> service.getPatient(requester, "P-0001"));
            AccessDeniedException register = assertThrows(AccessDeniedException.class,
                    () -> service.registerPatient(requester, ana()));
            AccessDeniedException modify = assertThrows(AccessDeniedException.class,
                    () -> service.modifyPatient(requester, "P-0001", luciaChanged("HC-0000001")));

            // Assert
            List<AuditEntry> denials = List.of(
                    new AuditEntry(requester, DENIED, PATIENTS),
                    new AuditEntry(requester, DENIED, "PATIENT:P-0001"),
                    new AuditEntry(requester, DENIED, PATIENTS),
                    new AuditEntry(requester, DENIED, "PATIENT:P-0001"));
            assertAll(
                    () -> assertEquals(requester, search.getRequester()),
                    () -> assertEquals(requester, get.getRequester()),
                    () -> assertEquals(requester, register.getRequester()),
                    () -> assertEquals(requester, modify.getRequester()),
                    () -> assertEquals(denials, audit.entries()));
            // Nothing was saved or changed
            assertAll(
                    () -> assertEquals(List.of("P-0001", "P-0002", "P-0003"), idsOf(allPatients())),
                    () -> assertEquals("Lucía Ejemplo Prueba",
                            service.getPatient(ADMISSIONS, "P-0001").getFullName()));
        }

        @Test
        @DisplayName("TC12S-01: the Admissions staff can use the four operations, and each one is audited")
        void admissionsStaffUsesEveryOperation() throws Exception {
            // Act
            List<Patient> found = service.searchPatients(ADMISSIONS, "");
            Patient read = service.getPatient(ADMISSIONS, "P-0001");
            Patient registered = service.registerPatient(ADMISSIONS, ana());
            Patient modified = service.modifyPatient(ADMISSIONS, "P-0001", luciaChanged("HC-0000001"));

            // Assert
            assertAll(
                    () -> assertEquals(3, found.size()),
                    () -> assertEquals("P-0001", read.getPatientId()),
                    () -> assertEquals("P-0004", registered.getPatientId()),
                    () -> assertEquals("Lucía Ejemplo Cambiada", modified.getFullName()),
                    () -> assertEquals(List.of(
                            new AuditEntry(ADMISSIONS, ACCESSED, PATIENTS),
                            new AuditEntry(ADMISSIONS, ACCESSED, "PATIENT:P-0001"),
                            new AuditEntry(ADMISSIONS, REGISTERED, "PATIENT:P-0004"),
                            new AuditEntry(ADMISSIONS, MODIFIED, "PATIENT:P-0001")), audit.entries()));
        }

        @Test
        @DisplayName("TC12S-02: the administrator does not have MANAGE_PATIENTS and is denied in every operation")
        void administratorIsDenied() throws Exception {
            assertDeniedInEveryOperation(ADMIN);
        }

        @Test
        @DisplayName("TC12S-03: a physician is denied in every operation")
        void physicianIsDenied() throws Exception {
            assertDeniedInEveryOperation(PHYSICIAN);
        }

        @Test
        @DisplayName("TC12S-04: a patient user is denied in every operation")
        void patientUserIsDenied() throws Exception {
            assertDeniedInEveryOperation(PATIENT_USER);
        }

        @Test
        @DisplayName("TC12S-05: an inactive user is denied even if he has MANAGE_PATIENTS")
        void inactiveUserIsDenied() throws Exception {
            assertDeniedInEveryOperation(INACTIVE);
        }

        @Test
        @DisplayName("TC12S-06: an unknown user is denied in every operation")
        void unknownUserIsDenied() throws Exception {
            assertDeniedInEveryOperation(UNKNOWN);
        }

        @Test
        @DisplayName("TC12S-07: a null requester is denied with AccessDeniedException, audited with a null author")
        void nullRequesterIsDenied() throws Exception {
            assertDeniedInEveryOperation(null);
        }

        @Test
        @DisplayName("TC12S-08: the permission is checked before the data, and the resource follows the id given")
        void permissionComesBeforeTheData() {
            // Act
            assertThrows(AccessDeniedException.class, () -> service.registerPatient(ADMIN, null));
            assertThrows(AccessDeniedException.class,
                    () -> service.registerPatient(ADMIN, patient("", "", null, null, "1", "x")));
            assertThrows(AccessDeniedException.class,
                    () -> service.modifyPatient(ADMIN, "P-9999", luciaChanged("HC-0000001")));
            assertThrows(AccessDeniedException.class, () -> service.getPatient(ADMIN, null));

            // Assert
            assertEquals(List.of(
                    new AuditEntry(ADMIN, DENIED, PATIENTS),
                    new AuditEntry(ADMIN, DENIED, PATIENTS),
                    new AuditEntry(ADMIN, DENIED, "PATIENT:P-9999"),
                    new AuditEntry(ADMIN, DENIED, PATIENTS)), audit.entries());
        }

        @Test
        @DisplayName("TC12S-09: after a denial, the same call by the Admissions staff works")
        void denialDoesNotBlockTheNextUser() throws Exception {
            // Act
            assertThrows(AccessDeniedException.class, () -> service.searchPatients(ADMIN, "mario"));
            List<Patient> found = service.searchPatients(ADMISSIONS, "mario");

            // Assert
            assertAll(
                    () -> assertEquals(List.of("P-0002"), idsOf(found)),
                    () -> assertEquals(List.of(
                            new AuditEntry(ADMIN, DENIED, PATIENTS),
                            new AuditEntry(ADMISSIONS, ACCESSED, PATIENTS)), audit.entries()));
        }
    }

    @Nested
    @DisplayName("Registering a patient: valid data")
    class RegisterValidTests {

        @BeforeEach
        void clearAudit() {
            audit.clear();
        }

        @Test
        @DisplayName("TC12S-15: a valid patient gets P-0004, keeps its data, is active and is audited")
        void validPatientIsRegistered() throws Exception {
            // Act
            Patient saved = service.registerPatient(ADMISSIONS, ana());

            // Assert
            assertAll(
                    () -> assertEquals("P-0004", saved.getPatientId()),
                    () -> assertEquals("HC-1234567", saved.getIdentifier()),
                    () -> assertEquals("00000000T", saved.getNationalId()),
                    () -> assertEquals("Ana Prueba Inventada", saved.getFullName()),
                    () -> assertEquals(BIRTH, saved.getDateOfBirth()),
                    () -> assertEquals(Sex.FEMALE, saved.getSex()),
                    () -> assertEquals("Calle Falsa 1", saved.getAddress()),
                    () -> assertEquals("600111222", saved.getPhone()),
                    () -> assertEquals("ana.prueba@example.test", saved.getEmail()),
                    () -> assertTrue(saved.isActive()),
                    () -> assertEquals(List.of(new AuditEntry(ADMISSIONS, REGISTERED, "PATIENT:P-0004")),
                            audit.entries()));
            assertAllFieldsEqual(saved, service.getPatient(ADMISSIONS, "P-0004"));
        }

        @Test
        @DisplayName("TC12S-16: the id and the inactive state sent by the client are ignored")
        void sentIdAndActiveAreIgnored() throws Exception {
            // Arrange
            Patient sent = new Patient("P-9999", "HC-1234567", "", "Ana Prueba Inventada", BIRTH, Sex.FEMALE, "", "",
                    "", false);

            // Act
            Patient saved = service.registerPatient(ADMISSIONS, sent);

            // Assert
            assertAll(
                    () -> assertEquals("P-0004", saved.getPatientId()),
                    () -> assertTrue(saved.isActive()),
                    () -> assertThrows(ValidationException.class, () -> service.getPatient(ADMISSIONS, "P-9999")));
        }

        @Test
        @DisplayName("TC12S-17: every text is saved without the spaces at the ends")
        void textsAreStripped() throws Exception {
            // Arrange
            Patient sent = new Patient(null, "  HC-1234567 ", " 00000000T ", "  Ana Prueba Inventada  ", BIRTH,
                    Sex.FEMALE, "  Calle Falsa 1 ", " 600111222  ", "  ana.prueba@example.test ", true);

            // Act
            Patient saved = service.registerPatient(ADMISSIONS, sent);

            // Assert
            assertAllFieldsEqual(new Patient("P-0004", "HC-1234567", "00000000T", "Ana Prueba Inventada", BIRTH,
                    Sex.FEMALE, "Calle Falsa 1", "600111222", "ana.prueba@example.test", true), saved);
            assertAllFieldsEqual(saved, service.getPatient(ADMISSIONS, "P-0004"));
        }

        @ParameterizedTest(name = "optional fields = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("TC12S-18: the national ID, address, phone and e-mail are optional and saved as empty")
        void optionalFieldsCanBeEmpty(String empty) throws Exception {
            // Arrange
            Patient sent = new Patient(null, "HC-1234567", empty, "Ana Prueba Inventada", BIRTH, Sex.FEMALE, empty,
                    empty, empty, true);

            // Act
            Patient saved = service.registerPatient(ADMISSIONS, sent);

            // Assert
            assertAll(
                    () -> assertEquals("", saved.getNationalId()),
                    () -> assertEquals("", saved.getAddress()),
                    () -> assertEquals("", saved.getPhone()),
                    () -> assertEquals("", saved.getEmail()));
        }

        @Test
        @DisplayName("TC12S-19: two registrations get consecutive ids")
        void idsAreConsecutive() throws Exception {
            // Act
            Patient first = service.registerPatient(ADMISSIONS, ana());
            Patient second = service.registerPatient(ADMISSIONS,
                    patient("HC-7654321", "Bruno Prueba Inventado", BIRTH, Sex.MALE, "", ""));

            // Assert
            assertAll(
                    () -> assertEquals("P-0004", first.getPatientId()),
                    () -> assertEquals("P-0005", second.getPatientId()),
                    () -> assertEquals(5, allPatients().size()));
        }

        @Test
        @DisplayName("TC12S-20: a date of birth of today is valid")
        void dateOfBirthTodayIsValid() throws Exception {
            // Arrange
            LocalDate today = LocalDate.now();

            // Act
            Patient saved = service.registerPatient(ADMISSIONS,
                    patient("HC-1234567", "Bebé Prueba Inventada", today, Sex.UNSPECIFIED, "", ""));

            // Assert
            assertEquals(today, saved.getDateOfBirth());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(Sex.class)
        @DisplayName("TC12S-21: every value of Sex is accepted")
        void everySexIsAccepted(Sex sex) throws Exception {
            // Act
            Patient saved = service.registerPatient(ADMISSIONS,
                    patient("HC-1234567", "Ana Prueba Inventada", BIRTH, sex, "", ""));

            // Assert
            assertEquals(sex, saved.getSex());
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"a@b.c", "name.surname@sub.example.test", "ana+urg@example.test"})
        @DisplayName("TC12S-22: e-mails of the form name@domain.ext are accepted, the shortest one too")
        void validEmailsAreAccepted(String email) throws Exception {
            // Act
            Patient saved = service.registerPatient(ADMISSIONS, withEmail(email));

            // Assert
            assertEquals(email, saved.getEmail());
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"600111222", "346001112223334", "600 111 222", "+34600111222", "+34 600 111 222"})
        @DisplayName("TC12S-23: phones of 9 to 15 digits, with spaces or a leading + sign, are accepted as given")
        void validPhonesAreAccepted(String phone) throws Exception {
            // Act
            Patient saved = service.registerPatient(ADMISSIONS, withPhone(phone));

            // Assert
            assertEquals(phone, saved.getPhone());
        }
    }

    @Nested
    @DisplayName("Registering a patient: invalid data")
    class RegisterInvalidTests {

        @BeforeEach
        void clearAudit() {
            audit.clear();
        }

        @Test
        @DisplayName("TC12S-24: a null patient is rejected")
        void nullPatientIsRejected() throws Exception {
            assertRegisterRejected(List.of(PATIENT_REQUIRED), null);
        }

        @ParameterizedTest(name = "identifier = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("TC12S-25: the identifier is required")
        void identifierIsRequired(String identifier) throws Exception {
            assertRegisterRejected(List.of(IDENTIFIER_REQUIRED),
                    patient(identifier, "Ana Prueba Inventada", BIRTH, Sex.FEMALE, "", ""));
        }

        @ParameterizedTest(name = "full name = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("TC12S-26: the full name is required")
        void fullNameIsRequired(String fullName) throws Exception {
            assertRegisterRejected(List.of(NAME_REQUIRED),
                    patient("HC-1234567", fullName, BIRTH, Sex.FEMALE, "", ""));
        }

        @Test
        @DisplayName("TC12S-27: the date of birth is required")
        void dateOfBirthIsRequired() throws Exception {
            assertRegisterRejected(List.of(DATE_REQUIRED),
                    patient("HC-1234567", "Ana Prueba Inventada", null, Sex.FEMALE, "", ""));
        }

        @ParameterizedTest(name = "today + {0} days")
        @ValueSource(ints = {1, 365})
        @DisplayName("TC12S-28: a date of birth in the future (tomorrow, in a year) is rejected")
        void futureDateOfBirthIsRejected(int days) throws Exception {
            assertRegisterRejected(List.of(DATE_FUTURE),
                    patient("HC-1234567", "Ana Prueba Inventada", LocalDate.now().plusDays(days), Sex.FEMALE, "", ""));
        }

        @Test
        @DisplayName("TC12S-29: the sex is required")
        void sexIsRequired() throws Exception {
            assertRegisterRejected(List.of(SEX_REQUIRED),
                    patient("HC-1234567", "Ana Prueba Inventada", BIRTH, null, "", ""));
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"ana", "ana@example", "ana.example.test", "ana@@example.test",
            "ana prueba@example.test", "@example.test", "ana@.test", "ana@example."})
        @DisplayName("TC12S-30: an e-mail without the form name@domain.ext is rejected")
        void invalidEmailIsRejected(String email) throws Exception {
            assertRegisterRejected(List.of(EMAIL_FORM), withEmail(email));
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"60011122", "3460011122233344", "60011122a", "+", "++34600111222", "600-111-222",
            "34+600111222"})
        @DisplayName("TC12S-31: a phone that does not have 9 to 15 digits is rejected")
        void invalidPhoneIsRejected(String phone) throws Exception {
            assertRegisterRejected(List.of(PHONE_DIGITS), withPhone(phone));
        }

        @Test
        @DisplayName("TC12S-32: every error comes together, in the order of the rules")
        void everyErrorComesTogether() throws Exception {
            assertRegisterRejected(
                    List.of(IDENTIFIER_REQUIRED, NAME_REQUIRED, DATE_REQUIRED, SEX_REQUIRED, EMAIL_FORM, PHONE_DIGITS),
                    patient(" ", "", null, null, "12", "ana"));
        }

        @Test
        @DisplayName("TC12S-33: with a future date, its message takes the place of the required date")
        void everyErrorWithAFutureDate() throws Exception {
            assertRegisterRejected(
                    List.of(IDENTIFIER_REQUIRED, NAME_REQUIRED, DATE_FUTURE, SEX_REQUIRED, EMAIL_FORM, PHONE_DIGITS),
                    patient(null, null, LocalDate.now().plusDays(1), null, "+", "ana@"));
        }

        @Test
        @DisplayName("TC12S-34: invalid data with an existing identifier is a validation error, not a duplicate")
        void validationComesBeforeTheDuplicate() throws Exception {
            assertRegisterRejected(List.of(SEX_REQUIRED),
                    patient("HC-0000001", "Lucía Ejemplo Prueba", BIRTH, null, "", ""));
        }
    }

    @Nested
    @DisplayName("Duplicate patient")
    class DuplicateTests {

        @BeforeEach
        void clearAudit() {
            audit.clear();
        }

        @ParameterizedTest(name = "identifier = \"{0}\"")
        @ValueSource(strings = {"HC-0000001", "  hc-0000001 "})
        @DisplayName("TC12S-35/36: an existing identifier, also in other case and with spaces, gives the existing "
                + "patient")
        void duplicateCarriesTheExistingPatient(String identifier) throws Exception {
            // Arrange
            Patient lucia = service.getPatient(ADMISSIONS, "P-0001");
            audit.clear();

            // Act
            DuplicatePatientException exception = assertThrows(DuplicatePatientException.class,
                    () -> service.registerPatient(ADMISSIONS,
                            patient(identifier, "Otra Persona Inventada", BIRTH, Sex.MALE, "", "")));

            // Assert
            assertAll(
                    () -> assertEquals("A patient with the identifier HC-0000001 already exists",
                            exception.getMessage()),
                    () -> assertEquals(List.of(new AuditEntry(ADMISSIONS, ACCESSED, "PATIENT:P-0001")),
                            audit.entries()));
            assertAllFieldsEqual(lucia, exception.getExistingPatient());
        }

        @Test
        @DisplayName("TC12S-37: after a duplicate nothing is saved and the next id is still P-0004")
        void duplicateSavesNothing() throws Exception {
            // Act
            assertThrows(DuplicatePatientException.class,
                    () -> service.registerPatient(ADMISSIONS,
                            patient("HC-0000002", "Otra Persona Inventada", BIRTH, Sex.MALE, "", "")));

            // Assert
            assertAll(
                    () -> assertEquals(List.of(new AuditEntry(ADMISSIONS, ACCESSED, "PATIENT:P-0002")),
                            audit.entries()),
                    () -> assertEquals(3, allPatients().size()),
                    () -> assertEquals("Mario Demo Simulado",
                            service.getPatient(ADMISSIONS, "P-0002").getFullName()),
                    () -> assertEquals("P-0004", service.registerPatient(ADMISSIONS, ana()).getPatientId()));
        }

        @Test
        @DisplayName("TC12S-38: an identifier with one more character is not a duplicate")
        void longerIdentifierIsNotADuplicate() throws Exception {
            // Act
            Patient saved = service.registerPatient(ADMISSIONS,
                    patient("HC-00000011", "Otra Persona Inventada", BIRTH, Sex.MALE, "", ""));

            // Assert
            assertEquals("P-0004", saved.getPatientId());
        }

        @Test
        @DisplayName("TC12S-39: a registered patient is found as duplicate of a later registration")
        void registeredPatientIsADuplicateLater() throws Exception {
            // Arrange
            service.registerPatient(ADMISSIONS, ana());

            // Act
            DuplicatePatientException exception = assertThrows(DuplicatePatientException.class,
                    () -> service.registerPatient(ADMISSIONS, ana()));

            // Assert
            assertAll(
                    () -> assertEquals("P-0004", exception.getExistingPatient().getPatientId()),
                    () -> assertEquals(4, allPatients().size()));
        }
    }

    @Nested
    @DisplayName("Modifying a patient")
    class ModifyTests {

        @BeforeEach
        void clearAudit() {
            audit.clear();
        }

        @Test
        @DisplayName("TC12S-40: a valid change is saved, keeps the id and the identifier, and is audited")
        void validChangeIsSaved() throws Exception {
            // Act
            Patient saved = service.modifyPatient(ADMISSIONS, "P-0001", luciaChanged("HC-0000001"));

            // Assert
            Patient expected = new Patient("P-0001", "HC-0000001", "11111111H", "Lucía Ejemplo Cambiada",
                    LocalDate.of(1981, 4, 15), Sex.UNSPECIFIED, "Calle Nueva 9, 13002 Ciudad Real",
                    "+34 600 999 888", "lucia.nueva@example.test", true);
            assertAllFieldsEqual(expected, saved);
            assertEquals(List.of(new AuditEntry(ADMISSIONS, MODIFIED, "PATIENT:P-0001")), audit.entries());
            assertAllFieldsEqual(expected, service.getPatient(ADMISSIONS, "P-0001"));
            assertEquals(List.of("P-0001", "P-0002", "P-0003"), idsOf(allPatients()));
        }

        @Test
        @DisplayName("TC12S-41: the same identifier in other case and with spaces is accepted and not changed")
        void sameIdentifierInOtherCaseIsAccepted() throws Exception {
            // Act
            Patient saved = service.modifyPatient(ADMISSIONS, "P-0001", luciaChanged(" hc-0000001 "));

            // Assert
            assertAll(
                    () -> assertEquals("HC-0000001", saved.getIdentifier()),
                    () -> assertEquals("HC-0000001", service.getPatient(ADMISSIONS, "P-0001").getIdentifier()));
        }

        @Test
        @DisplayName("TC12S-42: another identifier is rejected, and nothing changes or is audited")
        void identifierCannotBeChanged() throws Exception {
            // Act
            assertErrors(List.of(IDENTIFIER_FIXED),
                    () -> service.modifyPatient(ADMISSIONS, "P-0001", luciaChanged("HC-0000002")));

            // Assert
            assertTrue(audit.entries().isEmpty());
            assertEquals("Lucía Ejemplo Prueba", service.getPatient(ADMISSIONS, "P-0001").getFullName());
        }

        @Test
        @DisplayName("TC12S-43: the id and the active state in the data are ignored: only the patient of the id changes")
        void idAndActiveInTheDataAreIgnored() throws Exception {
            // Arrange
            Patient mario = service.getPatient(ADMISSIONS, "P-0002");
            Patient data = new Patient("P-0002", "HC-0000001", "", "Lucía Ejemplo Cambiada", BIRTH, Sex.FEMALE, "",
                    "", "", false);

            // Act
            Patient saved = service.modifyPatient(ADMISSIONS, "P-0001", data);

            // Assert
            assertAll(
                    () -> assertEquals("P-0001", saved.getPatientId()),
                    () -> assertTrue(saved.isActive()),
                    () -> assertTrue(service.getPatient(ADMISSIONS, "P-0001").isActive()));
            assertAllFieldsEqual(mario, service.getPatient(ADMISSIONS, "P-0002"));
        }

        @ParameterizedTest(name = "id = {0}")
        @ValueSource(strings = {"P-9999", "p-0001", ""})
        @DisplayName("TC12S-44: an unknown id is rejected and nothing is audited")
        void unknownIdIsRejected(String patientId) {
            // Act
            assertErrors(List.of("There is no patient with the id " + patientId),
                    () -> service.modifyPatient(ADMISSIONS, patientId, luciaChanged("HC-0000001")));

            // Assert
            assertTrue(audit.entries().isEmpty());
        }

        @Test
        @DisplayName("TC12S-44: a null id is rejected as an unknown id")
        void nullIdIsRejected() {
            // Act
            assertErrors(List.of("There is no patient with the id null"),
                    () -> service.modifyPatient(ADMISSIONS, null, luciaChanged("HC-0000001")));

            // Assert
            assertTrue(audit.entries().isEmpty());
        }

        @Test
        @DisplayName("TC12S-45: a null patient is rejected")
        void nullPatientIsRejected() throws Exception {
            // Act
            assertErrors(List.of(PATIENT_REQUIRED), () -> service.modifyPatient(ADMISSIONS, "P-0001", null));

            // Assert
            assertTrue(audit.entries().isEmpty());
            assertEquals("Lucía Ejemplo Prueba", service.getPatient(ADMISSIONS, "P-0001").getFullName());
        }

        @Test
        @DisplayName("TC12S-46: every error comes together, and the changed identifier comes last")
        void everyErrorComesTogether() throws Exception {
            // Act
            assertErrors(List.of(NAME_REQUIRED, DATE_FUTURE, SEX_REQUIRED, EMAIL_FORM, PHONE_DIGITS, IDENTIFIER_FIXED),
                    () -> service.modifyPatient(ADMISSIONS, "P-0001",
                            patient("HC-0000009", "  ", LocalDate.now().plusDays(1), null, "6001112223334445",
                                    "lucia@example")));

            // Assert
            assertTrue(audit.entries().isEmpty());
            assertAllFieldsEqual(new Patient("P-0001", "HC-0000001", "", "Lucía Ejemplo Prueba",
                    LocalDate.of(1980, 3, 14), Sex.FEMALE, "Calle Inventada 1, 13001 Ciudad Real", "600000001",
                    "lucia.ejemplo@example.test", true), service.getPatient(ADMISSIONS, "P-0001"));
        }

        @Test
        @DisplayName("TC12S-47: a blank identifier is required and also counts as a change")
        void blankIdentifierGivesBothMessages() {
            assertErrors(List.of(IDENTIFIER_REQUIRED, IDENTIFIER_FIXED),
                    () -> service.modifyPatient(ADMISSIONS, "P-0001", luciaChanged("   ")));
        }

        @Test
        @DisplayName("TC12S-48: the texts are stripped, the optional fields become empty and today is a valid date")
        void modificationIsCleaned() throws Exception {
            // Arrange
            LocalDate today = LocalDate.now();
            Patient data = new Patient(null, "HC-0000001", null, "  Lucía Ejemplo Prueba  ", today, Sex.FEMALE,
                    null, null, null, true);

            // Act
            Patient saved = service.modifyPatient(ADMISSIONS, "P-0001", data);

            // Assert
            assertAllFieldsEqual(new Patient("P-0001", "HC-0000001", "", "Lucía Ejemplo Prueba", today, Sex.FEMALE,
                    "", "", "", true), saved);
        }
    }

    @Nested
    @DisplayName("Reading a patient")
    class GetPatientTests {

        @BeforeEach
        void clearAudit() {
            audit.clear();
        }

        @Test
        @DisplayName("TC12S-49: an existing patient is returned with every field, and the access is audited once")
        void existingPatientIsReturned() throws Exception {
            // Act
            Patient mario = service.getPatient(ADMISSIONS, "P-0002");

            // Assert
            assertAllFieldsEqual(new Patient("P-0002", "HC-0000002", "", "Mario Demo Simulado",
                    LocalDate.of(1975, 11, 2), Sex.MALE, "Avenida de Muestra 22, 45001 Toledo", "600000002",
                    "mario.demo@example.test", true), mario);
            assertEquals(List.of(new AuditEntry(ADMISSIONS, ACCESSED, "PATIENT:P-0002")), audit.entries());
        }

        @Test
        @DisplayName("TC12S-50: an unknown or null id is rejected and nothing is audited")
        void unknownIdIsRejected() {
            // Act
            assertErrors(List.of("There is no patient with the id P-9999"),
                    () -> service.getPatient(ADMISSIONS, "P-9999"));
            assertErrors(List.of("There is no patient with the id null"),
                    () -> service.getPatient(ADMISSIONS, null));

            // Assert
            assertTrue(audit.entries().isEmpty());
        }

        @Test
        @DisplayName("TC12S-51: a patient just registered can be read")
        void registeredPatientCanBeRead() throws Exception {
            // Arrange
            Patient registered = service.registerPatient(ADMISSIONS, ana());

            // Act
            Patient read = service.getPatient(ADMISSIONS, registered.getPatientId());

            // Assert
            assertAllFieldsEqual(registered, read);
        }
    }

    @Nested
    @DisplayName("Searching patients")
    class SearchTests {

        @BeforeEach
        void clearAudit() {
            audit.clear();
        }

        @Test
        @DisplayName("TC12S-52: a part of the identifier finds the patient")
        void partOfTheIdentifier() throws Exception {
            assertEquals(List.of("P-0002"), idsOf(service.searchPatients(ADMISSIONS, "0002")));
        }

        @Test
        @DisplayName("TC12S-53: a part of the full name finds the patient, ignoring case")
        void partOfTheFullNameIgnoringCase() throws Exception {
            assertAll(
                    () -> assertEquals(List.of("P-0003"), idsOf(service.searchPatients(ADMISSIONS, "muestra"))),
                    () -> assertEquals(List.of("P-0001"), idsOf(service.searchPatients(ADMISSIONS, "EJEMPLO"))));
        }

        @Test
        @DisplayName("TC12S-54: the spaces at the ends of the text are ignored")
        void spacesAtTheEndsAreIgnored() throws Exception {
            assertEquals(List.of("P-0002"), idsOf(service.searchPatients(ADMISSIONS, "  mario  ")));
        }

        @ParameterizedTest(name = "filters = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("TC12S-55: an empty or null text returns every patient in the order of saving")
        void emptyTextReturnsEveryPatient(String filters) throws Exception {
            assertEquals(List.of("P-0001", "P-0002", "P-0003"), idsOf(service.searchPatients(ADMISSIONS, filters)));
        }

        @Test
        @DisplayName("TC12S-56: a text that matches nothing returns an empty list")
        void noMatchReturnsEmpty() throws Exception {
            assertTrue(service.searchPatients(ADMISSIONS, "zzz").isEmpty());
        }

        @Test
        @DisplayName("TC12S-57: the patients come in the order in which they were saved")
        void orderOfSaving() throws Exception {
            // Arrange
            service.registerPatient(ADMISSIONS, ana());

            // Act
            List<Patient> found = service.searchPatients(ADMISSIONS, "hc-");

            // Assert
            assertEquals(List.of("P-0001", "P-0002", "P-0003", "P-0004"), idsOf(found));
        }

        @Test
        @DisplayName("TC12S-58: every search is audited once, also when it finds nothing")
        void everySearchIsAuditedOnce() throws Exception {
            // Act
            service.searchPatients(ADMISSIONS, "zzz");
            service.searchPatients(ADMISSIONS, "");

            // Assert
            assertEquals(List.of(
                    new AuditEntry(ADMISSIONS, ACCESSED, PATIENTS),
                    new AuditEntry(ADMISSIONS, ACCESSED, PATIENTS)), audit.entries());
        }
    }

    @Nested
    @DisplayName("Construction")
    class ConstructionTests {

        @Test
        @DisplayName("TC12S-64: the service saves in the repository it is given, and it is an IPatientService")
        void serviceUsesTheGivenRepository() throws Exception {
            // Arrange
            InMemoryPatientRepository repository = new InMemoryPatientRepository();
            PatientService own = new PatientService(repository, new FakeRoleManagementService(), audit);

            // Act
            Patient saved = own.registerPatient(ADMISSIONS, ana());

            // Assert
            assertAll(
                    () -> assertInstanceOf(IPatientService.class, own),
                    () -> assertEquals("P-0001", saved.getPatientId()),
                    () -> assertSame(saved, repository.findById("P-0001")),
                    () -> assertNull(repository.findById("P-0002")),
                    () -> assertFalse(repository.search("").isEmpty()));
        }
    }
}
