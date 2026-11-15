package es.uclm.esi.iso2.server.access.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests of {@link AuthenticatorSystemSimulator}, the simulated SSO, with the test users agreed in #13. */
@DisplayName("AuthenticatorSystemSimulator")
class AuthenticatorSystemSimulatorTest {
    private AuthenticatorSystemSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new AuthenticatorSystemSimulator();
    }

    // pnavarro is accepted too: the SSO does not know which accounts are inactive.
    @Test
    @DisplayName("Accepts the password of each test user")
    void acceptsThePasswordOfEachTestUser() {
        assertAll(
                () -> assertTrue(simulator.validateCredentials("admin", "Admin.2026")),
                () -> assertTrue(simulator.validateCredentials("mgarcia", "Physician.2026")),
                () -> assertTrue(simulator.validateCredentials("lmartin", "Admissions.2026")),
                () -> assertTrue(simulator.validateCredentials("jfernandez", "Patient.2026")),
                () -> assertTrue(simulator.validateCredentials("pnavarro", "Inactive.2026")));
    }

    @ParameterizedTest(name = "{0} / {1} is rejected")
    @CsvSource({
        "admin, admin.2026",
        "admin, Physician.2026",
        "ghost, Admin.2026"
    })
    @DisplayName("Rejects a wrong-case password, another user's password and an unknown user")
    void rejectsInvalidCredentials(String username, String password) {
        // Act
        boolean accepted = simulator.validateCredentials(username, password);

        // Assert
        assertFalse(accepted);
    }

    @Test
    @DisplayName("Rejects a null username or a null password without throwing")
    void rejectsNullUsernameOrNullPassword() {
        assertAll(
                () -> assertFalse(simulator.validateCredentials(null, "Admin.2026")),
                () -> assertFalse(simulator.validateCredentials("admin", null)));
    }
}
