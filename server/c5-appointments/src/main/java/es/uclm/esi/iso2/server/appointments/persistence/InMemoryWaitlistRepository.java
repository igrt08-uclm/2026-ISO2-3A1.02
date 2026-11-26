package es.uclm.esi.iso2.server.appointments.persistence;

import es.uclm.esi.iso2.common.appointments.domain.WaitlistEntry;
import es.uclm.esi.iso2.common.appointments.domain.WaitlistStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Keeps the entries of the waitlist in memory (D-030), so they are lost when the program closes. */
public class InMemoryWaitlistRepository implements IWaitlistRepository {
    private final Map<String, WaitlistEntry> entries = new LinkedHashMap<>();

    @Override
    public WaitlistEntry findById(String entryId) {
        return entries.get(entryId);
    }

    @Override
    public List<WaitlistEntry> findAll() {
        return List.copyOf(entries.values());
    }

    @Override
    public List<WaitlistEntry> findWaitingByTestType(String testTypeId) {
        return entries.values().stream()
                .filter(entry -> entry.getStatus() == WaitlistStatus.WAITING)
                .filter(entry -> Objects.equals(entry.getTestTypeId(), testTypeId))
                .toList();
    }

    @Override
    public WaitlistEntry findWaitingByNeed(String needId) {
        return entries.values().stream()
                .filter(entry -> entry.getStatus() == WaitlistStatus.WAITING)
                .filter(entry -> Objects.equals(entry.getNeedId(), needId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void save(WaitlistEntry entry) {
        entries.put(entry.getEntryId(), entry);
    }
}
