package es.uclm.esi.iso2.common.access.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link AuditLog}: an entry keeps its data and cannot be modified (D11 of the analysis). */
@DisplayName("AuditLog")
class AuditLogTest {
    private static final long A_MOMENT = 1_793_000_000_000L;

    @Test
    @DisplayName("Keeps its five attributes")
    void keepsItsFiveAttributes() {
        // Act
        AuditLog entry = new AuditLog(7L, new Date(A_MOMENT), "admin", "LOGIN_OK", "authentication");

        // Assert
        assertAll(
                () -> assertEquals(7L, entry.getId()),
                () -> assertEquals(new Date(A_MOMENT), entry.getTimestamp()),
                () -> assertEquals("admin", entry.getAuthor()),
                () -> assertEquals("LOGIN_OK", entry.getAction()),
                () -> assertEquals("authentication", entry.getTargetResource()));
    }

    // D11: Date can be modified, so the entry keeps its own copy.
    @Test
    @DisplayName("Changing the Date used to create it does not change the entry")
    void changingTheOriginalDateDoesNotChangeTheEntry() {
        // Arrange
        Date date = new Date(A_MOMENT);
        AuditLog entry = new AuditLog(1L, date, "admin", "LOGIN_OK", "authentication");

        // Act
        date.setTime(0);

        // Assert
        assertEquals(new Date(A_MOMENT), entry.getTimestamp());
    }

    @Test
    @DisplayName("Changing the returned Date does not change the entry")
    void changingTheReturnedDateDoesNotChangeTheEntry() {
        // Arrange
        AuditLog entry = new AuditLog(1L, new Date(A_MOMENT), "admin", "LOGIN_OK", "authentication");

        // Act
        entry.getTimestamp().setTime(0);

        // Assert
        assertEquals(new Date(A_MOMENT), entry.getTimestamp());
    }
}
