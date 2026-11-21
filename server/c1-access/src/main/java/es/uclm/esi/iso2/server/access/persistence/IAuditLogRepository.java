package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.AuditLog;
import java.util.List;

/**
 * Stores and retrieves audit log entries (UC19).
 */
public interface IAuditLogRepository {
    /**
     * Adds an entry. Entries are never modified or deleted (D11 of the analysis).
     *
     * @param entry the audit log entry to add
     */
    void save(AuditLog entry);

    /**
     * Returns the entries that meet the filters, the newest first. See {@code IAuditService} for their format.
     *
     * @param filters the filters to apply
     * @return the list of audit log entries matching the filters, the newest first
     */
    List<AuditLog> find(String filters);
}
