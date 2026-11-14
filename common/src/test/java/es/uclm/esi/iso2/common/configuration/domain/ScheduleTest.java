package es.uclm.esi.iso2.common.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TC17S-31 and TC17S-32: tests of {@link Schedule}, the working hours of a room. */
@DisplayName("Schedule")
class ScheduleTest {
    private static final LocalTime EIGHT = LocalTime.of(8, 0);
    private static final LocalTime THREE = LocalTime.of(15, 0);

    @Test
    @DisplayName("A schedule keeps its resource, days and times")
    void keepsItsData() {
        // Act
        Schedule schedule = new Schedule("Room CT-1", Set.of(DayOfWeek.MONDAY), EIGHT, THREE, true);

        // Assert
        assertAll(
                () -> assertEquals("Room CT-1", schedule.getResource()),
                () -> assertEquals(Set.of(DayOfWeek.MONDAY), schedule.getDaysOfWeek()),
                () -> assertEquals(EIGHT, schedule.getStartTime()),
                () -> assertEquals(THREE, schedule.getEndTime()),
                () -> assertTrue(schedule.isActive()));
    }

    @Test
    @DisplayName("TC17S-32: the schedule keeps its own copy of the days")
    void keepsItsOwnCopyOfTheDays() {
        // Arrange
        Set<DayOfWeek> days = EnumSet.of(DayOfWeek.MONDAY);
        Schedule schedule = new Schedule("Room CT-1", days, EIGHT, THREE, true);

        // Act
        days.add(DayOfWeek.SUNDAY);

        // Assert
        assertEquals(Set.of(DayOfWeek.MONDAY), schedule.getDaysOfWeek());
    }

    @Test
    @DisplayName("A schedule created without days keeps null, so the service can report it")
    void nullDaysStayNull() {
        assertNull(new Schedule("Room CT-1", null, EIGHT, THREE, true).getDaysOfWeek());
    }

    @Test
    @DisplayName("deactivated() returns an inactive copy with the same data")
    void deactivatedReturnsAnInactiveCopy() {
        // Arrange
        Schedule original = new Schedule("Room CT-1", Set.of(DayOfWeek.MONDAY), EIGHT, THREE, true);

        // Act
        Schedule copy = original.deactivated();

        // Assert
        assertAll(
                () -> assertFalse(copy.isActive()),
                () -> assertTrue(original.isActive()),
                () -> assertEquals(original.getResource(), copy.getResource()),
                () -> assertEquals(original.getDaysOfWeek(), copy.getDaysOfWeek()),
                () -> assertEquals(original.getStartTime(), copy.getStartTime()),
                () -> assertEquals(original.getEndTime(), copy.getEndTime()));
    }
}
