package es.uclm.esi.iso2.common.appointments.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link ScheduleEntry} (UC14, C5 1.2.0, #137): case TC14S-16 of the test design. */
@DisplayName("ScheduleEntry")
class ScheduleEntryTest {
    @Test
    @DisplayName("TC14S-16 a line returns what it was given")
    void getters() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2026, 11, 24, 9, 0);
        Appointment appointment = new Appointment("APT-0001", "NEED-0001", "P-0001", "TT-01", "Room 1", "mgarcia",
                start, start.plusMinutes(15), AppointmentStatus.BOOKED);

        // Act
        ScheduleEntry entry = new ScheduleEntry(appointment, "Lucía Ejemplo Prueba", "Chest X-ray",
                "Remove metal objects");

        // Assert
        assertAll(
                () -> assertSame(appointment, entry.getAppointment()),
                () -> assertEquals("Lucía Ejemplo Prueba", entry.getPatientName()),
                () -> assertEquals("Chest X-ray", entry.getTestTypeName()),
                () -> assertEquals("Remove metal objects", entry.getPreparation()));
    }
}
