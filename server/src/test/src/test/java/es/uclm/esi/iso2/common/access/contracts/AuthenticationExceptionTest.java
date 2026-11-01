package es.uclm.esi.iso2.common.access.contracts;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Tests of {@link AuthenticationException}: the client reads the reason, and the message helps in logs. */
@DisplayName("AuthenticationException")
class AuthenticationExceptionTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(AuthenticationFailureReason.class)
    @DisplayName("Keeps the reason and uses its name as the message")
    void keepsReasonAndUsesItsNameAsMessage(AuthenticationFailureReason reason) {
        // Act
        AuthenticationException exception = new AuthenticationException(reason);

        // Assert
        assertAll(
                () -> assertEquals(reason, exception.getReason()),
                () -> assertEquals(reason.name(), exception.getMessage()));
    }
}
