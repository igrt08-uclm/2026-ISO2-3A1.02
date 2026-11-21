package es.uclm.esi.iso2.common.access.contracts;

import es.uclm.esi.iso2.common.access.domain.AuditLog;
import java.util.List;

/**
 * Consults the entries of the system audit log (UC19).
 */
public interface IAuditService {
    /**
     * Returns the entries of the audit log, the newest first. Only an active system administrator can consult them.
     *
     * @param requester the username of who asks
     * @param filters   optional {@code key=value} pairs separated by {@code ;}, with the keys {@code author},
     *                  {@code action}, {@code target}, {@code from} and {@code to} (dates as {@code yyyy-MM-dd}).
     *                  A null or empty value returns every entry
     * @return the list of audit log entries matching the filters
     * @throws AccessDeniedException if the requester is not an active system administrator
     */
    List<AuditLog> getActivityLog(String requester, String filters) throws AccessDeniedException;
}
