package es.uclm.esi.iso2.common.patients.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TC12S-65 and TC12S-66: tests of {@link Patient}, the demographic data of a patient (UC12, D-037). */
@DisplayName("Patient")
class PatientTest {

    @Test
    @DisplayName("TC12S-65: a patient keeps every field")
    void keepsEveryField() {
        // Act
        Patient patient = new Patient("P-0001", "HC-0000001", "00000000T", "Lucía Ejemplo Prueba",
                LocalDate.of(1980, 3, 14), Sex.FEMALE, "Calle Inventada 1", "600000001",
                "lucia.ejemplo@example.test", true);

        // Assert
        assertAll(
                () -> assertEquals("P-0001", patient.getPatientId()),
                () -> assertEquals("HC-0000001", patient.getIdentifier()),
                () -> assertEquals("00000000T", patient.getNationalId()),
                () -> assertEquals("Lucía Ejemplo Prueba", patient.getFullName()),
                () -> assertEquals(LocalDate.of(1980, 3, 14), patient.getDateOfBirth()),
                () -> assertEquals(Sex.FEMALE, patient.getSex()),
                () -> assertEquals("Calle Inventada 1", patient.getAddress()),
                () -> assertEquals("600000001", patient.getPhone()),
                () -> assertEquals("lucia.ejemplo@example.test", patient.getEmail()),
                () -> assertTrue(patient.isActive()));
    }

    @Test
    @DisplayName("TC12S-66: the nullable fields are kept as null, because the server validates them")
    void keepsNullFields() {
        // Act
        Patient patient = new Patient(null, null, null, null, null, null, null, null, null, false);

        // Assert
        assertAll(
                () -> assertNull(patient.getPatientId()),
                () -> assertNull(patient.getIdentifier()),
                () -> assertNull(patient.getNationalId()),
                () -> assertNull(patient.getFullName()),
                () -> assertNull(patient.getDateOfBirth()),
                () -> assertNull(patient.getSex()),
                () -> assertNull(patient.getAddress()),
                () -> assertNull(patient.getPhone()),
                () -> assertNull(patient.getEmail()),
                () -> assertFalse(patient.isActive()));
    }
}
