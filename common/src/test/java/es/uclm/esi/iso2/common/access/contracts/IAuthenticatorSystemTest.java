package es.uclm.esi.iso2.common.access.contracts;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of the default method {@link IAuthenticatorSystem#ping} (C1 patch 1.2.2, #112). */
@DisplayName("IAuthenticatorSystem")
class IAuthenticatorSystemTest {

    @Test
    @DisplayName("TC-P-08: an SSO written as a lambda answers the ping by default")
    void lambdaAnswersThePingByDefault() {
        // Arrange: only validateCredentials is written, so ping() is the default method
        IAuthenticatorSystem sso = (username, password) -> false;

        // Act and assert
        assertTrue(sso.ping());
    }

    @Test
    @DisplayName("TC-P-09: the default ping does not depend on the credentials")
    void pingDoesNotDependOnTheCredentials() {
        // Arrange
        IAuthenticatorSystem rejectsAll = (username, password) -> false;
        IAuthenticatorSystem acceptsAll = (username, password) -> true;

        // Assert
        assertAll(
                () -> assertTrue(rejectsAll.ping()),
                () -> assertTrue(acceptsAll.ping()),
                () -> assertFalse(rejectsAll.validateCredentials("admin", "Admin.2026")));
    }
}
