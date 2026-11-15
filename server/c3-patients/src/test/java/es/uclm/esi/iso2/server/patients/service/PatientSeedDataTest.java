package es.uclm.esi.iso2.server.patients.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.patients.domain.Sex;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the seed data of C3: three invented patients, loaded only when the system starts with no patients (Q10),
 * numbered by the same counter as the registrations (Q8).
 */
@DisplayName("PatientService seed data")
class PatientSeedDataTest {
    private static final String ADMISSIONS = "lmartin";

    private RecordingAuditRecorder audit;
    private PatientService service;

    @BeforeEach
    void setUp() {
        audit = new RecordingAuditRecorder();
        service = new PatientService(new FakeRoleManagementService(), audit);
    }

    private static Patient newPatient(String identifier) {
        return new Patient(null, identifier, "", "Ana Prueba Inventada", LocalDate.of(1990, 5, 10), Sex.FEMALE, "",
                "", "", true);
    }

    @Test
    @DisplayName("TC12S-10: an empty system gets P-0001 to P-0003 with HC-0000001 to HC-0000003, and it is not audited")
    void seedDataIsLoadedOnAnEmptySystem() throws Exception {
        // Act
        service.loadSeedData();

        // Assert
        // The audit is checked before the search, which is audited itself
        assertTrue(audit.entries().isEmpty());
        List<Patient> patients = service.searchPatients(ADMISSIONS, "");
        assertAll(
                () -> assertEquals(List.of("P-0001", "P-0002", "P-0003"),
                        patients.stream().map(Patient::getPatientId).toList()),
                () -> assertEquals(List.of("HC-0000001", "HC-0000002", "HC-0000003"),
                        patients.stream().map(Patient::getIdentifier).toList()),
                () -> assertEquals(List.of("Lucía Ejemplo Prueba", "Mario Demo Simulado", "Elena Muestra Ficticia"),
                        patients.stream().map(Patient::getFullName).toList()),
                () -> assertTrue(patients.stream().allMatch(Patient::isActive)));
    }

    @Test
    @DisplayName("TC12S-11: loading the seed data twice does not duplicate it")
    void seedDataIsLoadedOnlyOnce() throws Exception {
        // Act
        service.loadSeedData();
        service.loadSeedData();

        // Assert
        assertEquals(3, service.searchPatients(ADMISSIONS, "").size());
    }

    @Test
    @DisplayName("TC12S-12: the first patient registered after the seed data gets P-0004")
    void counterContinuesAfterTheSeed() throws Exception {
        // Arrange
        service.loadSeedData();

        // Act
        Patient saved = service.registerPatient(ADMISSIONS, newPatient("HC-1234567"));

        // Assert
        assertEquals("P-0004", saved.getPatientId());
    }

    @Test
    @DisplayName("TC12S-13: if there is already a patient, the seed data is not loaded")
    void seedDataIsNotLoadedWhenThereIsData() throws Exception {
        // Arrange
        service.registerPatient(ADMISSIONS, newPatient("HC-1234567"));

        // Act
        service.loadSeedData();

        // Assert
        List<Patient> patients = service.searchPatients(ADMISSIONS, "");
        assertAll(
                () -> assertEquals(1, patients.size()),
                () -> assertEquals("P-0001", patients.get(0).getPatientId()),
                () -> assertEquals("HC-1234567", patients.get(0).getIdentifier()));
    }

    @Test
    @DisplayName("TC12S-14: every seed patient is complete, valid and clearly invented")
    void seedPatientsAreComplete() throws Exception {
        // Arrange
        service.loadSeedData();

        // Assert
        for (Patient patient : service.searchPatients(ADMISSIONS, "")) {
            assertAll(patient.getPatientId(),
                    () -> assertFalse(patient.getFullName().isBlank()),
                    () -> assertNotNull(patient.getSex()),
                    () -> assertTrue(patient.getDateOfBirth().isBefore(LocalDate.now())),
                    () -> assertTrue(patient.getPhone().matches("\\d{9,15}")),
                    () -> assertTrue(patient.getEmail().endsWith("@example.test")),
                    () -> assertFalse(patient.getAddress().isBlank()),
                    () -> assertEquals("", patient.getNationalId()));
        }
    }
}
