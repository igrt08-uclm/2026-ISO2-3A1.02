package es.uclm.esi.iso2.common.access.contracts;

import es.uclm.esi.iso2.common.access.domain.AuditLog;
import java.util.List;

public interface IAuditService {
    /**
     * Returns the entries of the audit log, the newest first. Only an active system administrator can consult them.
     *
     * @param requester the username of who asks
     * @param filters   optional {@code key=value} pairs separated by {@code ;}, with the keys {@code author},
     *                  {@code action}, {@code target}, {@code from} and {@code to} (dates as {@code yyyy-MM-dd}).
     *                  A null or empty value returns every entry
     */
    List<AuditLog> getActivityLog(String requester, String filters) throws AccessDeniedException;
}
