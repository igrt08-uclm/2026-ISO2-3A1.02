package es.uclm.esi.iso2.client.access.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IAuditService;
import es.uclm.esi.iso2.common.access.domain.AuditLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test double of {@link IAuditService}. It returns fixed entries, or fails if a test asks it to, and remembers the
 * calls so tests can check what the screen sent.
 */
class FakeAuditService implements IAuditService {
    private final List<AuditLog> entries;
    private final List<String> receivedFilters = new ArrayList<>();
    private String lastRequester;
    private boolean rejectFilters;
    private boolean denyAccess;

    FakeAuditService(AuditLog... entries) {
        this.entries = new ArrayList<>(List.of(entries));
    }

    /** From now on, every search with filters fails like a wrong date in the real server. */
    void rejectFilters(boolean reject) {
        rejectFilters = reject;
    }

    /** From now on, every request is rejected, as if the user were not an administrator. */
    void denyAccess() {
        denyAccess = true;
    }

    @Override
    public List<AuditLog> getActivityLog(String requester, String filters) throws AccessDeniedException {
        lastRequester = requester;
        receivedFilters.add(filters);
        if (denyAccess) {
            throw new AccessDeniedException(requester);
        }
        if (rejectFilters && !filters.isEmpty()) {
            throw new IllegalArgumentException("Wrong date");
        }
        return List.copyOf(entries);
    }

    String lastRequester() {
        return lastRequester;
    }

    String lastFilters() {
        return receivedFilters.get(receivedFilters.size() - 1);
    }

    List<String> receivedFilters() {
        return Collections.unmodifiableList(receivedFilters);
    }
}
