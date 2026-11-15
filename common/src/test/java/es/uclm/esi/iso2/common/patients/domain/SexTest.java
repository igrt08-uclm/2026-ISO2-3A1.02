package es.uclm.esi.iso2.common.patients.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TC12S-67: tests of {@link Sex}, the values of the design of UC12 (Q3). */
@DisplayName("Sex")
class SexTest {

    @Test
    @DisplayName("TC12S-67: the values are FEMALE, MALE and UNSPECIFIED, in that order")
    void values() {
        assertArrayEquals(new Sex[] {Sex.FEMALE, Sex.MALE, Sex.UNSPECIFIED}, Sex.values());
    }

    @Test
    @DisplayName("TC12S-67: valueOf finds a value by its name")
    void valueOfFindsTheValue() {
        assertEquals(Sex.UNSPECIFIED, Sex.valueOf("UNSPECIFIED"));
    }
}
