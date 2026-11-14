package es.uclm.esi.iso2.server.configuration.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import es.uclm.esi.iso2.common.configuration.domain.Priority;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TC17S-30: tests of the generic in-memory repository of C2, used here with priorities. */
@DisplayName("InMemoryConfigurationRepository")
class InMemoryConfigurationRepositoryTest {
    private InMemoryConfigurationRepository<Priority> repository;
    private Priority urgent;
    private Priority routine;

    @BeforeEach
    void setUp() {
        repository = new InMemoryConfigurationRepository<>(Priority::getId);
        urgent = new Priority("URGENT", "Urgent", "Hours", true);
        routine = new Priority("ROUTINE", "Routine", "Weeks", true);
    }

    @Test
    @DisplayName("An entry is found by its id, and an unknown id gives null")
    void findById() {
        // Act
        repository.save(urgent);

        // Assert
        assertAll(
                () -> assertSame(urgent, repository.findById("URGENT")),
                () -> assertNull(repository.findById("ROUTINE")));
    }

    @Test
    @DisplayName("findAll keeps the order in which the entries were saved")
    void findAllKeepsTheOrder() {
        // Act
        repository.save(urgent);
        repository.save(routine);

        // Assert
        assertEquals(List.of(urgent, routine), repository.findAll());
    }

    @Test
    @DisplayName("Saving an entry with an existing id replaces it")
    void saveSameIdReplaces() {
        // Arrange
        repository.save(urgent);
        Priority renamed = new Priority("URGENT", "Very urgent", "Minutes", true);

        // Act
        repository.save(renamed);

        // Assert
        assertEquals(List.of(renamed), repository.findAll());
    }

    @Test
    @DisplayName("The list returned by findAll cannot be changed from outside")
    void findAllCannotBeModified() {
        repository.save(urgent);
        assertThrows(UnsupportedOperationException.class, () -> repository.findAll().add(routine));
    }
}
