package es.uclm.esi.iso2.server.patients.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.patients.domain.Sex;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TC12S-59 to TC12S-63 and TC12S-71: tests of the in-memory repository of the patients of C3 (Q9, D-030). */
@DisplayName("InMemoryPatientRepository")
class InMemoryPatientRepositoryTest {
    private InMemoryPatientRepository repository;
    private Patient lucia;
    private Patient mario;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPatientRepository();
        lucia = patient("P-0001", "HC-0000001", "Lucía Ejemplo Prueba");
        mario = patient("P-0002", "HC-0000002", "Mario Demo Simulado");
        repository.save(lucia);
        repository.save(mario);
    }

    private static Patient patient(String patientId, String identifier, String fullName) {
        return new Patient(patientId, identifier, "", fullName, LocalDate.of(1980, 3, 14), Sex.FEMALE, "", "", "",
                true);
    }

    @Test
    @DisplayName("TC12S-59: a patient is found by its id; an unknown or null id gives null")
    void findById() {
        assertAll(
                () -> assertSame(lucia, repository.findById("P-0001")),
                () -> assertNull(repository.findById("P-9999")),
                () -> assertNull(repository.findById(null)));
    }

    @Test
    @DisplayName("TC12S-60: a patient is found by its identifier ignoring case and the spaces at the ends")
    void findByIdentifier() {
        assertAll(
                () -> assertSame(lucia, repository.findByIdentifier("HC-0000001")),
                () -> assertSame(mario, repository.findByIdentifier("  hc-0000002 ")),
                () -> assertNull(repository.findByIdentifier("HC-0000003")),
                () -> assertNull(repository.findByIdentifier("HC 0000001")),
                () -> assertNull(repository.findByIdentifier(null)));
    }

    @Test
    @DisplayName("TC12S-61: the search looks in the identifier and the full name, ignoring case, in order of saving")
    void search() {
        assertAll(
                () -> assertEquals(List.of(mario), repository.search("0002")),
                () -> assertEquals(List.of(lucia), repository.search("EJEMPLO")),
                () -> assertEquals(List.of(lucia, mario), repository.search(" hc-0 ")),
                () -> assertEquals(List.of(lucia, mario), repository.search("")),
                () -> assertEquals(List.of(lucia, mario), repository.search(null)),
                () -> assertTrue(repository.search("zzz").isEmpty()));
    }

    @Test
    @DisplayName("TC12S-62: saving a patient with an id that exists replaces it and keeps its place")
    void saveReplaces() {
        // Arrange
        Patient changed = patient("P-0001", "HC-0000001", "Lucía Ejemplo Cambiada");

        // Act
        repository.save(changed);

        // Assert
        assertAll(
                () -> assertSame(changed, repository.findById("P-0001")),
                () -> assertEquals(List.of(changed, mario), repository.search("")));
    }

    @Test
    @DisplayName("TC12S-63: a stored patient with null identifier and name does not break the reads")
    void nullFieldsDoNotBreakTheReads() {
        // Arrange
        repository.save(patient("P-0003", null, null));

        // Assert
        assertAll(
                () -> assertTrue(repository.search("x").isEmpty()),
                () -> assertNull(repository.findByIdentifier("x")),
                () -> assertEquals(3, repository.search("").size()));
    }

    @Test
    @DisplayName("TC12S-71: the search ignores accents as well as case, in the text and in the stored name")
    void searchIgnoresAccents() {
        // Arrange
        Patient withoutAccent = patient("P-0003", "HC-0000003", "Lucia Sin Tilde");
        repository.save(withoutAccent);

        // Act and Assert
        assertAll(
                () -> assertEquals(List.of(lucia, withoutAccent), repository.search("lucia")),
                () -> assertEquals(List.of(lucia, withoutAccent), repository.search("LUCIA")),
                () -> assertEquals(List.of(lucia, withoutAccent), repository.search("Lucía")),
                () -> assertEquals(List.of(lucia, withoutAccent), repository.search(" LUCÍA ")),
                () -> assertEquals(List.of(lucia), repository.search("lucia ejemplo")),
                () -> assertEquals(List.of(lucia), repository.search("ejémplo")),
                () -> assertTrue(repository.search("lucio").isEmpty()));
    }
}
