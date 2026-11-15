package es.uclm.esi.iso2.server.access.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link AuthenticatorSystemSimulator#ping} (C1 patch 1.2.2, #112), used by the connection test of UC20. */
@DisplayName("AuthenticatorSystemSimulator.ping")
class AuthenticatorSystemSimulatorPingTest {

    @Test
    @DisplayName("TC-P-10: the simulated SSO always answers, and the ping does not change the logins")
    void simulatedSsoAlwaysAnswers() {
        // Arrange
        AuthenticatorSystemSimulator sso = new AuthenticatorSystemSimulator();

        // Act
        boolean first = sso.ping();
        boolean second = sso.ping();

        // Assert
        assertAll(
                () -> assertTrue(first),
                () -> assertTrue(second),
                () -> assertTrue(sso.validateCredentials("admin", "Admin.2026")));
    }
}
