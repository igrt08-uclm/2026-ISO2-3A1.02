package es.uclm.esi.iso2.common.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the enums of UC20 (#43, Q5 and Q12). The order of {@link HospitalSystemType} is the order in which the
 * server lists the integrations, so it is part of the behaviour.
 */
@DisplayName("Enums of the integrations")
class IntegrationEnumsTest {

    @Test
    @DisplayName("TC20S-57: the systems are SSO, EHR, admissions and RIS/PACS, in that order")
    void hospitalSystemTypes() {
        assertArrayEquals(new HospitalSystemType[] {HospitalSystemType.SSO, HospitalSystemType.EHR,
            HospitalSystemType.ADMISSIONS, HospitalSystemType.RIS_PACS}, HospitalSystemType.values());
    }

    @Test
    @DisplayName("TC20S-58: the modes are SIMULATED and REAL, and the statuses ACTIVE, INACTIVE and ERROR")
    void modesAndStatuses() {
        assertAll(
                () -> assertArrayEquals(new IntegrationMode[] {IntegrationMode.SIMULATED, IntegrationMode.REAL},
                        IntegrationMode.values()),
                () -> assertArrayEquals(new IntegrationStatus[] {IntegrationStatus.ACTIVE,
                    IntegrationStatus.INACTIVE, IntegrationStatus.ERROR}, IntegrationStatus.values()));
    }
}
