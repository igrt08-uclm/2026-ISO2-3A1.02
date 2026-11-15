package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.AuditLog;
import java.util.List;

public interface IAuditLogRepository {
    /** Adds an entry. Entries are never modified or deleted (D11 of the analysis). */
    void save(AuditLog entry);

    /** Returns the entries that meet the filters, the newest first. See {@code IAuditService} for their format. */
    List<AuditLog> find(String filters);
}
