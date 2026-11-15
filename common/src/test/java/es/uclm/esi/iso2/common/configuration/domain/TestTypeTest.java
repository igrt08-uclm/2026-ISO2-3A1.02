package es.uclm.esi.iso2.common.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TC17S-31: tests of {@link TestType}, an entry of the examination catalogue with the fields of FR17. */
@DisplayName("TestType")
class TestTypeTest {

    private static TestType headCt() {
        return new TestType("CT-HEAD", "Head CT", "CT", "Room CT-1", 20, "Fast for 4 hours", true, false,
                "Ask about allergies", "URGENT", true, true);
    }

    @Test
    @DisplayName("A test type keeps every field of FR17")
    void keepsEveryField() {
        // Act
        TestType testType = headCt();

        // Assert
        assertAll(
                () -> assertEquals("CT-HEAD", testType.getId()),
                () -> assertEquals("Head CT", testType.getName()),
                () -> assertEquals("CT", testType.getModality()),
                () -> assertEquals("Room CT-1", testType.getRoom()),
                () -> assertEquals(20, testType.getDurationMinutes()),
                () -> assertEquals("Fast for 4 hours", testType.getPreparation()),
                () -> assertTrue(testType.isRequiresContrast()),
                () -> assertFalse(testType.isRequiresConsent()),
                () -> assertEquals("Ask about allergies", testType.getSafetyInfo()),
                () -> assertEquals("URGENT", testType.getPriority()),
                () -> assertTrue(testType.isFollowUpEligible()),
                () -> assertTrue(testType.isActive()));
    }

    @Test
    @DisplayName("deactivated() returns an inactive copy with the same data, and the original does not change")
    void deactivatedReturnsAnInactiveCopy() {
        // Arrange
        TestType original = headCt();

        // Act
        TestType copy = original.deactivated();

        // Assert
        assertAll(
                () -> assertFalse(copy.isActive()),
                () -> assertTrue(original.isActive()),
                () -> assertEquals(original.getId(), copy.getId()),
                () -> assertEquals(original.getName(), copy.getName()),
                () -> assertEquals(original.getModality(), copy.getModality()),
                () -> assertEquals(original.getRoom(), copy.getRoom()),
                () -> assertEquals(original.getDurationMinutes(), copy.getDurationMinutes()),
                () -> assertEquals(original.getPreparation(), copy.getPreparation()),
                () -> assertEquals(original.isRequiresContrast(), copy.isRequiresContrast()),
                () -> assertEquals(original.isRequiresConsent(), copy.isRequiresConsent()),
                () -> assertEquals(original.getSafetyInfo(), copy.getSafetyInfo()),
                () -> assertEquals(original.getPriority(), copy.getPriority()),
                () -> assertEquals(original.isFollowUpEligible(), copy.isFollowUpEligible()));
    }
}
