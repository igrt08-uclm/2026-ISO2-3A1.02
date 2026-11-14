package es.uclm.esi.iso2.common.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TC17S-31: tests of the three entries of C2 that only have id, name, description and active: {@link Protocol},
 * {@link AppointmentPolicy} and {@link Priority}.
 */
@DisplayName("Protocol, AppointmentPolicy and Priority")
class SimpleConfigurationEntriesTest {

    @Test
    @DisplayName("A protocol keeps its data, and deactivated() returns an inactive copy")
    void protocol() {
        // Arrange
        Protocol original = new Protocol("P1", "Head CT", "Steps", true);

        // Act
        Protocol copy = original.deactivated();

        // Assert
        assertAll(
                () -> assertEquals("P1", copy.getId()),
                () -> assertEquals("Head CT", copy.getName()),
                () -> assertEquals("Steps", copy.getDescription()),
                () -> assertFalse(copy.isActive()),
                () -> assertTrue(original.isActive()));
    }

    @Test
    @DisplayName("An appointment policy keeps its data, and deactivated() returns an inactive copy")
    void appointmentPolicy() {
        // Arrange
        AppointmentPolicy original = new AppointmentPolicy("A1", "Cancellation notice", "24 hours", true);

        // Act
        AppointmentPolicy copy = original.deactivated();

        // Assert
        assertAll(
                () -> assertEquals("A1", copy.getId()),
                () -> assertEquals("Cancellation notice", copy.getName()),
                () -> assertEquals("24 hours", copy.getDescription()),
                () -> assertFalse(copy.isActive()),
                () -> assertTrue(original.isActive()));
    }

    @Test
    @DisplayName("A priority keeps its data, and deactivated() returns an inactive copy")
    void priority() {
        // Arrange
        Priority original = new Priority("URGENT", "Urgent", "Within hours", true);

        // Act
        Priority copy = original.deactivated();

        // Assert
        assertAll(
                () -> assertEquals("URGENT", copy.getId()),
                () -> assertEquals("Urgent", copy.getName()),
                () -> assertEquals("Within hours", copy.getDescription()),
                () -> assertFalse(copy.isActive()),
                () -> assertTrue(original.isActive()));
    }
}
