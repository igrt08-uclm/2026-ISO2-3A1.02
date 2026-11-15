package es.uclm.esi.iso2.common.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of {@link IntegrationConfiguration} (UC20 server, #43, Q5 and step 2 of the plan): it keeps its data and
 * copies the settings into an unmodifiable map that keeps their order and accepts empty values, so that the server can
 * report them as errors.
 */
@DisplayName("IntegrationConfiguration")
class IntegrationConfigurationTest {

    private static IntegrationConfiguration withSettings(Map<String, String> settings) {
        return new IntegrationConfiguration(HospitalSystemType.EHR, "", settings, true, IntegrationMode.SIMULATED,
                IntegrationStatus.ACTIVE);
    }

    @Test
    @DisplayName("TC20S-48: the configuration keeps every field it was created with")
    void keepsItsData() {
        // Act
        IntegrationConfiguration configuration = new IntegrationConfiguration(HospitalSystemType.RIS_PACS,
                "https://pacs.hospital.test", Map.of("aeTitle", "HGUCR"), false, IntegrationMode.REAL,
                IntegrationStatus.INACTIVE);

        // Assert
        assertAll(
                () -> assertEquals(HospitalSystemType.RIS_PACS, configuration.getSystem()),
                () -> assertEquals("https://pacs.hospital.test", configuration.getEndpoint()),
                () -> assertEquals(Map.of("aeTitle", "HGUCR"), configuration.getSettings()),
                () -> assertFalse(configuration.isEnabled()),
                () -> assertEquals(IntegrationMode.REAL, configuration.getMode()),
                () -> assertEquals(IntegrationStatus.INACTIVE, configuration.getStatus()));
    }

    @Test
    @DisplayName("TC20S-49: the settings are copied, so a later change of the original map does not reach them")
    void settingsAreCopied() {
        // Arrange
        Map<String, String> original = new HashMap<>();
        original.put("timeout", "30");
        IntegrationConfiguration configuration = withSettings(original);

        // Act
        original.put("timeout", "60");
        original.put("facility", "HGUCR");

        // Assert
        assertEquals(Map.of("timeout", "30"), configuration.getSettings());
    }

    @Test
    @DisplayName("TC20S-50: the settings cannot be changed from outside")
    void settingsAreUnmodifiable() {
        // Arrange
        IntegrationConfiguration configuration = withSettings(Map.of("timeout", "30"));

        // Act and Assert
        assertAll(
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> configuration.getSettings().put("facility", "HGUCR")),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> configuration.getSettings().remove("timeout")),
                () -> assertEquals(Map.of("timeout", "30"), configuration.getSettings()));
    }

    @Test
    @DisplayName("TC20S-51: the settings keep the order in which they were written")
    void settingsKeepTheirOrder() {
        // Arrange
        Map<String, String> original = new LinkedHashMap<>();
        original.put("zeta", "1");
        original.put("alpha", "2");
        original.put("mu", "3");

        // Act
        IntegrationConfiguration configuration = withSettings(original);

        // Assert
        assertEquals(List.of("zeta", "alpha", "mu"), List.copyOf(configuration.getSettings().keySet()));
    }

    @Test
    @DisplayName("TC20S-52: empty and null keys and values are kept, so the server can report them")
    void emptyValuesAreKept() {
        // Arrange
        Map<String, String> original = new LinkedHashMap<>();
        original.put("timeout", "");
        original.put("facility", null);
        original.put("", "x");
        original.put(null, "y");

        // Act
        IntegrationConfiguration configuration = withSettings(original);

        // Assert
        Map<String, String> settings = configuration.getSettings();
        assertAll(
                () -> assertEquals(4, settings.size()),
                () -> assertEquals("", settings.get("timeout")),
                () -> assertTrue(settings.containsKey("facility")),
                () -> assertNull(settings.get("facility")),
                () -> assertEquals("x", settings.get("")),
                () -> assertEquals("y", settings.get(null)));
    }

    @Test
    @DisplayName("TC20S-53: a null map of settings is kept as null, and an empty one stays empty")
    void nullAndEmptySettings() {
        assertAll(
                () -> assertNull(withSettings(null).getSettings()),
                () -> assertTrue(withSettings(Map.of()).getSettings().isEmpty()));
    }

    @Test
    @DisplayName("TC20S-54: null fields are kept as they come, so that the server can validate them")
    void nullFieldsAreKept() {
        // Act
        IntegrationConfiguration configuration = new IntegrationConfiguration(null, null, null, false, null, null);

        // Assert
        assertAll(
                () -> assertNull(configuration.getSystem()),
                () -> assertNull(configuration.getEndpoint()),
                () -> assertNull(configuration.getMode()),
                () -> assertNull(configuration.getStatus()));
    }
}
