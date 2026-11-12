package es.uclm.esi.iso2.server.access.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests of {@link InMemoryRoleRepository}: the four preloaded roles and the {@code save} method. */
@DisplayName("InMemoryRoleRepository")
class InMemoryRoleRepositoryTest {
    private InMemoryRoleRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRoleRepository();
    }

    @Test
    @DisplayName("findAll returns the four roles of the system")
    void findAllReturnsTheFourRoles() {
        // Act
        List<String> ids = repository.findAll().stream().map(Role::getId).toList();

        // Assert
        assertEquals(List.of("SYSTEM_ADMINISTRATOR", "PHYSICIAN", "ADMISSIONS_STAFF", "PATIENT"), ids);
    }

    @ParameterizedTest(name = "{0} has the permissions [{1}]")
    @CsvSource({
        "SYSTEM_ADMINISTRATOR, MANAGE_ROLES",
        "PHYSICIAN,            ''",
        "ADMISSIONS_STAFF,     ''",
        "PATIENT,              ''"
    })
    @DisplayName("Only the System administrator has MANAGE_ROLES")
    void onlyTheSystemAdministratorHasManageRoles(String id, String expectedPermissions) {
        // Act
        Role role = repository.findById(id);

        // Assert
        assertNotNull(role);
        List<String> codes = role.getPermissions().stream().map(Permission::getCode).toList();
        assertEquals(expectedPermissions.isEmpty() ? List.of() : List.of(expectedPermissions), codes);
    }

    // "physician" shows that the lookup is case sensitive.
    @ParameterizedTest(name = "\"{0}\" is not found")
    @NullAndEmptySource
    @ValueSource(strings = {"GHOST", "physician"})
    @DisplayName("findById returns null for an unknown, null or empty id")
    void findByIdReturnsNullForUnknownId(String id) {
        // Act
        Role role = repository.findById(id);

        // Assert
        assertNull(role);
    }

    @Test
    @DisplayName("Saving a role with an existing id replaces it")
    void saveExistingIdReplacesTheRole() {
        // Arrange
        Role physician = new Role("PHYSICIAN", "Physician", "Test physician role",
                List.of(InMemoryPermissionRepository.MANAGE_ROLES));

        // Act
        repository.save(physician);

        // Assert
        assertAll(
                () -> assertSame(physician, repository.findById("PHYSICIAN")),
                () -> assertEquals(4, repository.findAll().size()));
    }

    @Test
    @DisplayName("Saving a role with a new id adds it and keeps the others")
    void saveNewIdAddsTheRole() {
        // Arrange
        Role technician = new Role("TECHNICIAN", "Technician", "Test technician role");

        // Act
        repository.save(technician);

        // Assert
        assertAll(
                () -> assertSame(technician, repository.findById("TECHNICIAN")),
                () -> assertEquals(5, repository.findAll().size()),
                () -> assertNotNull(repository.findById("SYSTEM_ADMINISTRATOR")));
    }

    @Test
    @DisplayName("The list returned by findAll cannot change the repository")
    void findAllListCannotChangeTheRepository() {
        // Arrange
        List<Role> roles = repository.findAll();

        // Act and Assert
        assertThrows(UnsupportedOperationException.class, roles::clear);
        assertEquals(4, repository.findAll().size());
    }
}
