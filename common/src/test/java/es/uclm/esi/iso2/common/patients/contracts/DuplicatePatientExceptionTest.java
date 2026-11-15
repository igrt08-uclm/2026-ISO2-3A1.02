package es.uclm.esi.iso2.common.patients.contracts;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.patients.domain.Sex;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TC12S-68: tests of {@link DuplicatePatientException}, which carries the existing patient (Q4, D-037). */
@DisplayName("DuplicatePatientException")
class DuplicatePatientExceptionTest {

    @Test
    @DisplayName("TC12S-68: it carries the existing patient, names its identifier and is a checked exception")
    void carriesTheExistingPatient() {
        // Arrange
        Patient existing = new Patient("P-0001", "HC-0000001", "", "Lucía Ejemplo Prueba",
                LocalDate.of(1980, 3, 14), Sex.FEMALE, "", "", "", true);

        // Act
        DuplicatePatientException exception = new DuplicatePatientException(existing);

        // Assert
        assertAll(
                () -> assertSame(existing, exception.getExistingPatient()),
                () -> assertEquals("A patient with the identifier HC-0000001 already exists", exception.getMessage()),
                () -> assertFalse(RuntimeException.class.isInstance(exception)));
    }
}
