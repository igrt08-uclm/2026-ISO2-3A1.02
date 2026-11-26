package es.uclm.esi.iso2.common.appointments.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of the data classes of the waitlist in {@code common} (UC15, #134), case TC15S-34 of the test design. */
@DisplayName("Waitlist data classes")
class WaitlistModelTest {

    @Test
    @DisplayName("TC15S-34 an entry keeps its data and its status")
    void waitlistEntry() {
        LocalDateTime addedAt = LocalDateTime.of(2026, 11, 23, 8, 0);
        WaitlistEntry entry = new WaitlistEntry("WL-0001", "NEED-0001", "P-0001", "TT-01", "URGENT", addedAt,
                WaitlistStatus.WAITING);
        assertAll(
                () -> assertEquals("WL-0001", entry.getEntryId()),
                () -> assertEquals("NEED-0001", entry.getNeedId()),
                () -> assertEquals("P-0001", entry.getPatientId()),
                () -> assertEquals("TT-01", entry.getTestTypeId()),
                () -> assertEquals("URGENT", entry.getPriorityId()),
                () -> assertEquals(addedAt, entry.getAddedAt()),
                () -> assertEquals(WaitlistStatus.WAITING, entry.getStatus()),
                () -> assertEquals(3, WaitlistStatus.values().length));
    }
}
