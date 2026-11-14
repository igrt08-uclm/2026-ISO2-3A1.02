package es.uclm.esi.iso2.common.shared;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TC17S-33: tests of {@link ValidationException}, which carries every validation error at once. */
@DisplayName("ValidationException")
class ValidationExceptionTest {

    @Test
    @DisplayName("It keeps every error, and the message joins them with '; '")
    void keepsEveryError() {
        // Act
        ValidationException exception = new ValidationException(List.of("The id is required", "The name is required"));

        // Assert
        assertAll(
                () -> assertEquals(List.of("The id is required", "The name is required"), exception.getErrors()),
                () -> assertEquals("The id is required; The name is required", exception.getMessage()));
    }

    @Test
    @DisplayName("It keeps its own unmodifiable copy of the errors")
    void keepsItsOwnCopy() {
        // Arrange
        List<String> errors = new ArrayList<>(List.of("The id is required"));
        ValidationException exception = new ValidationException(errors);

        // Act
        errors.add("Added later");

        // Assert
        assertAll(
                () -> assertEquals(List.of("The id is required"), exception.getErrors()),
                () -> assertThrows(UnsupportedOperationException.class, () -> exception.getErrors().add("x")));
    }
}
