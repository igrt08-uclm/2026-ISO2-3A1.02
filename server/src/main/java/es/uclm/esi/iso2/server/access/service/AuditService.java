package es.uclm.esi.iso2.server.access.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IAuditService;
import es.uclm.esi.iso2.common.access.domain.AuditLog;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import es.uclm.esi.iso2.server.access.persistence.IAuditLogRepository;
import es.uclm.esi.iso2.server.access.persistence.IUserAccountRepository;
import java.util.Date;
import java.util.List;

public class AuditService implements IAuditService, IAuditRecorder {
    private static final String AUDIT_CONSULTED = "AUDIT_CONSULTED";
    private static final String AUDIT_ACCESS_DENIED = "AUDIT_ACCESS_DENIED";
    private static final String AUDIT_RESOURCE = "audit";
    // Temporary: with UC18 this check uses the permissions of the role.
    private static final String ADMINISTRATOR_ROLE = "SYSTEM_ADMINISTRATOR";

    private final IAuditLogRepository auditLogRepository;
    private final IUserAccountRepository userAccountRepository;

    public AuditService(IAuditLogRepository auditLogRepository, IUserAccountRepository userAccountRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public void register(String author, String action, String targetResource) {
        auditLogRepository.save(new AuditLog(nextId(), new Date(), author, action, targetResource));
    }

    @Override
    public List<AuditLog> getActivityLog(String requester, String filters) throws AccessDeniedException {
        if (!isActiveAdministrator(requester)) {
            register(requester, AUDIT_ACCESS_DENIED, AUDIT_RESOURCE);
            throw new AccessDeniedException(requester);
        }
        List<AuditLog> entries = auditLogRepository.find(filters);
        register(requester, AUDIT_CONSULTED, AUDIT_RESOURCE);
        return entries;
    }

    private boolean isActiveAdministrator(String username) {
        UserAccount user = userAccountRepository.findByUsername(username);
        return user != null && user.isActive()
                && user.getRoles().stream().anyMatch(role -> ADMINISTRATOR_ROLE.equals(role.getId()));
    }

    private Long nextId() {
        return auditLogRepository.find(null).stream().mapToLong(AuditLog::getId).max().orElse(0) + 1;
    }
}
