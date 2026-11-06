package es.uclm.esi.iso2.server.access.service;

import es.uclm.esi.iso2.common.access.domain.AuditLog;
import es.uclm.esi.iso2.server.access.persistence.IAuditLogRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Test double of {@link IAuditLogRepository} that ignores the filters and remembers the ones it received. */
class FakeAuditLogRepository implements IAuditLogRepository {
    private final List<AuditLog> entries = new ArrayList<>();
    private final List<String> receivedFilters = new ArrayList<>();

    @Override
    public void save(AuditLog entry) {
        entries.add(entry);
    }

    @Override
    public List<AuditLog> find(String filters) {
        receivedFilters.add(filters);
        return List.copyOf(entries);
    }

    List<AuditLog> entries() {
        return Collections.unmodifiableList(entries);
    }

    List<String> receivedFilters() {
        return Collections.unmodifiableList(receivedFilters);
    }
}
