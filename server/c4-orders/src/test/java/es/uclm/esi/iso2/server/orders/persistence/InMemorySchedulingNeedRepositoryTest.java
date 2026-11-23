package es.uclm.esi.iso2.server.orders.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeedStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link InMemorySchedulingNeedRepository} (TC06S-44 of the test design of UC06). */
@DisplayName("InMemorySchedulingNeedRepository")
class InMemorySchedulingNeedRepositoryTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    private InMemorySchedulingNeedRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemorySchedulingNeedRepository();
    }

    private static SchedulingNeed need(String needId, SchedulingNeedStatus status) {
        return new SchedulingNeed(needId, "ORD-0001", TODAY, status);
    }

    @Test
    @DisplayName("TC06S-44 an unknown id gives null")
    void unknownId() {
        assertNull(repository.findById("NEED-9999"));
    }

    @Test
    @DisplayName("TC06S-44 only the pending needs are returned, in the order of saving")
    void onlyPendingNeeds() {
        repository.save(need("NEED-0001", SchedulingNeedStatus.PENDING));
        repository.save(need("NEED-0002", SchedulingNeedStatus.BOOKED));
        repository.save(need("NEED-0003", SchedulingNeedStatus.PENDING));
        List<SchedulingNeed> pending = repository.findPending();
        assertAll(
                () -> assertEquals(2, pending.size()),
                () -> assertEquals("NEED-0001", pending.get(0).getNeedId()),
                () -> assertEquals("NEED-0003", pending.get(1).getNeedId()));
    }

    @Test
    @DisplayName("TC06S-44 saving the same id twice replaces the need and keeps its place")
    void saveReplacesAndKeepsThePlace() {
        repository.save(need("NEED-0001", SchedulingNeedStatus.PENDING));
        repository.save(need("NEED-0002", SchedulingNeedStatus.PENDING));
        repository.save(need("NEED-0001", SchedulingNeedStatus.BOOKED));
        repository.save(need("NEED-0001", SchedulingNeedStatus.PENDING));
        List<SchedulingNeed> pending = repository.findPending();
        assertAll(
                () -> assertEquals(2, pending.size()),
                () -> assertEquals("NEED-0001", pending.get(0).getNeedId()),
                () -> assertEquals(SchedulingNeedStatus.PENDING, repository.findById("NEED-0001").getStatus()));
    }
}
