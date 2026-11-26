package es.uclm.esi.iso2.server.appointments.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import es.uclm.esi.iso2.common.appointments.domain.WaitlistEntry;
import es.uclm.esi.iso2.common.appointments.domain.WaitlistStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link InMemoryWaitlistRepository} (UC15 server, #134), case TC15S-34 of the test design. */
@DisplayName("InMemoryWaitlistRepository")
class InMemoryWaitlistRepositoryTest {

    @Test
    @DisplayName("TC15S-34 saves, replaces and finds the entries")
    void saveReplaceAndFind() {
        // Arrange
        InMemoryWaitlistRepository repository = new InMemoryWaitlistRepository();
        WaitlistEntry first = entry("WL-0001", "NEED-0001", WaitlistStatus.WAITING);
        WaitlistEntry second = entry("WL-0002", "NEED-0002", WaitlistStatus.WAITING);
        WaitlistEntry removed = entry("WL-0001", "NEED-0001", WaitlistStatus.REMOVED);

        // Act
        repository.save(first);
        repository.save(second);
        repository.save(removed);

        // Assert
        assertAll(
                () -> assertEquals(List.of(removed, second), repository.findAll()),
                () -> assertEquals(removed, repository.findById("WL-0001")),
                () -> assertNull(repository.findById("WL-0009")),
                () -> assertEquals(List.of(second), repository.findWaitingByTestType("TT-01")),
                () -> assertEquals(List.of(), repository.findWaitingByTestType("TT-02")),
                () -> assertEquals(second, repository.findWaitingByNeed("NEED-0002")),
                () -> assertNull(repository.findWaitingByNeed("NEED-0001")));
    }

    private static WaitlistEntry entry(String entryId, String needId, WaitlistStatus status) {
        return new WaitlistEntry(entryId, needId, "P-0001", "TT-01", "URGENT",
                LocalDateTime.of(2026, 11, 23, 8, 0), status);
    }
}
