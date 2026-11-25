package es.uclm.esi.iso2.server.access.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IAuditRecorder;
import es.uclm.esi.iso2.common.access.contracts.IAuditService;
import es.uclm.esi.iso2.common.access.domain.AuditLog;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import es.uclm.esi.iso2.server.access.persistence.IAuditLogRepository;
import es.uclm.esi.iso2.server.access.persistence.IRoleRepository;
import es.uclm.esi.iso2.server.access.persistence.IUserAccountRepository;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Records and consults audit log entries (UC19). Only an active user with the permission
 * {@code CONSULT_AUDIT} in one of their roles can consult them; the permissions are read from the repository of
 * roles, so a change made with UC18 applies at once.
 */
public class AuditService implements IAuditService, IAuditRecorder {
    private static final String AUDIT_CONSULTED = "AUDIT_CONSULTED";
    private static final String AUDIT_ACCESS_DENIED = "AUDIT_ACCESS_DENIED";
    private static final String AUDIT_RESOURCE = "audit";
    private static final String CONSULT_AUDIT = "CONSULT_AUDIT";

    private final IAuditLogRepository auditLogRepository;
    private final IUserAccountRepository userAccountRepository;
    private final IRoleRepository roleRepository;

    /**
     * Creates the audit service with the given repositories.
     *
     * @param auditLogRepository the repository for audit log entries
     * @param userAccountRepository the repository for user accounts
     * @param roleRepository the repository for roles, which holds the current permissions of each role
     */
    public AuditService(IAuditLogRepository auditLogRepository, IUserAccountRepository userAccountRepository,
            IRoleRepository roleRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void register(String author, String action, String targetResource) {
        auditLogRepository.save(new AuditLog(nextId(), new Date(), author, action, targetResource));
    }

    @Override
    public List<AuditLog> getActivityLog(String requester, String filters) throws AccessDeniedException {
        if (!canConsultAudit(requester)) {
            register(requester, AUDIT_ACCESS_DENIED, AUDIT_RESOURCE);
            throw new AccessDeniedException(requester);
        }
        List<AuditLog> entries = auditLogRepository.find(filters);
        register(requester, AUDIT_CONSULTED, AUDIT_RESOURCE);
        return entries;
    }

    private boolean canConsultAudit(String username) {
        UserAccount user = userAccountRepository.findByUsername(username);
        return user != null && user.isActive() && user.getRoles().stream()
                .map(role -> roleRepository.findById(role.getId()))
                .filter(Objects::nonNull)
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(permission -> CONSULT_AUDIT.equals(permission.getCode()));
    }

    private Long nextId() {
        return auditLogRepository.find(null).stream().mapToLong(AuditLog::getId).max().orElse(0) + 1;
    }
}
