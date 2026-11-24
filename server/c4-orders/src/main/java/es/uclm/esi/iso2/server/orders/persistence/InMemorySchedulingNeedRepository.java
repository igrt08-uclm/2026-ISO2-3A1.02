package es.uclm.esi.iso2.server.orders.persistence;

import es.uclm.esi.iso2.common.orders.domain.SchedulingNeed;
import es.uclm.esi.iso2.common.orders.domain.SchedulingNeedStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps the scheduling needs in memory (D-030), so they are lost when the program closes. */
public class InMemorySchedulingNeedRepository implements ISchedulingNeedRepository {
    private final Map<String, SchedulingNeed> needs = new LinkedHashMap<>();

    @Override
    public SchedulingNeed findById(String needId) {
        return needs.get(needId);
    }

    @Override
    public List<SchedulingNeed> findPending() {
        return needs.values().stream()
                .filter(need -> need.getStatus() == SchedulingNeedStatus.PENDING)
                .toList();
    }

    @Override
    public void save(SchedulingNeed need) {
        needs.put(need.getNeedId(), need);
    }
}
