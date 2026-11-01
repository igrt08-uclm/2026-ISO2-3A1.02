package es.uclm.esi.iso2.common.access.contracts;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link AccessDeniedException}: it says who was rejected, and the message helps in logs. */
@DisplayName("AccessDeniedException")
class AccessDeniedExceptionTest {
    @Test
    @DisplayName("Keeps the requester and names it in the message")
    void keepsTheRequesterAndNamesItInTheMessage() {
        // Act
        AccessDeniedException exception = new AccessDeniedException("mgarcia");

        // Assert
        assertAll(
                () -> assertEquals("mgarcia", exception.getRequester()),
                () -> assertEquals("Access denied to mgarcia", exception.getMessage()));
    }

    @Test
    @DisplayName("Accepts a null requester")
    void acceptsNullRequester() {
        // Act
        AccessDeniedException exception = new AccessDeniedException(null);

        // Assert
        assertNull(exception.getRequester());
    }
}
