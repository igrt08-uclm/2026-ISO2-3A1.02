package es.uclm.esi.iso2.server.access.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests of {@link InMemoryUserAccountRepository}: the preloaded test users of #13 and the {@code save} method. */
@DisplayName("InMemoryUserAccountRepository")
class InMemoryUserAccountRepositoryTest {
    private InMemoryUserAccountRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserAccountRepository();
    }

    @ParameterizedTest(name = "{0} is preloaded, active = {1}, roles {2}")
    @CsvSource({
        "admin,      true,  SYSTEM_ADMINISTRATOR",
        "mgarcia,    true,  PHYSICIAN;PATIENT",
        "lmartin,    true,  ADMISSIONS_STAFF",
        "jfernandez, true,  PATIENT",
        "pnavarro,   false, PHYSICIAN"
    })
    @DisplayName("Finds each preloaded user with their state and roles")
    void findsEachPreloadedUserWithStateAndRoles(String username, boolean expectedActive, String expectedRoles) {
        // Act
        UserAccount user = repository.findByUsername(username);

        // Assert
        assertNotNull(user);
        List<String> roleIds = user.getRoles().stream().map(Role::getId).toList();
        assertAll(
                () -> assertEquals(username, user.getUsername()),
                () -> assertEquals(expectedActive, user.isActive()),
                () -> assertEquals(Arrays.asList(expectedRoles.split(";")), roleIds));
    }

    // "ADMIN" shows that the lookup is case sensitive.
    @ParameterizedTest(name = "\"{0}\" is not found")
    @NullAndEmptySource
    @ValueSource(strings = "ADMIN")
    @DisplayName("Returns null for a missing, null or empty username")
    void findByUsernameReturnsNullForMissingUser(String username) {
        // Act
        UserAccount user = repository.findByUsername(username);

        // Assert
        assertNull(user);
    }

    @Test
    @DisplayName("Saving a new account makes it findable and keeps the others")
    void saveNewAccountMakesItFindable() {
        // Arrange
        UserAccount nlopez = new UserAccount("nlopez", "Nuria López", "nuria.lopez@sescam.test", true,
                List.of(new Role("PATIENT", "Patient", "Test patient role")));

        // Act
        repository.save(nlopez);

        // Assert
        assertAll(
                () -> assertSame(nlopez, repository.findByUsername("nlopez")),
                () -> assertNotNull(repository.findByUsername("admin")));
    }

    @Test
    @DisplayName("Saving an existing username replaces the account")
    void saveExistingUsernameReplacesTheAccount() {
        // Arrange
        UserAccount activePnavarro = new UserAccount("pnavarro", "Pedro Navarro Díaz", "pedro.navarro@sescam.test",
                true, List.of(new Role("PHYSICIAN", "Physician", "Test physician role")));

        // Act
        repository.save(activePnavarro);

        // Assert
        UserAccount found = repository.findByUsername("pnavarro");
        assertAll(
                () -> assertSame(activePnavarro, found),
                () -> assertTrue(found.isActive()));
    }
}
