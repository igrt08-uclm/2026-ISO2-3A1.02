package es.uclm.esi.iso2.server.access.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.AuthenticationException;
import es.uclm.esi.iso2.common.access.domain.AuditLog;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import es.uclm.esi.iso2.server.access.integration.AuthenticatorSystemSimulator;
import es.uclm.esi.iso2.server.access.persistence.InMemoryAuditLogRepository;
import es.uclm.esi.iso2.server.access.persistence.InMemoryPermissionRepository;
import es.uclm.esi.iso2.server.access.persistence.InMemoryRoleRepository;
import es.uclm.esi.iso2.server.access.persistence.InMemoryUserAccountRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests of {@link RoleManagementService} with the real repositories, {@link AuthenticationService} and
 * {@link AuditService}, as {@code Main} creates them (one repository of roles shared by both services), using the
 * test users agreed in #13.
 */
@DisplayName("RoleManagementService with the real login, audit and repositories")
class RoleManagementServiceIntegrationTest {
    private InMemoryUserAccountRepository users;
    private InMemoryAuditLogRepository auditLog;
    private InMemoryRoleRepository roles;
    private AuditService auditService;
    private AuthenticationService authenticationService;
    private RoleManagementService roleService;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserAccountRepository();
        auditLog = new InMemoryAuditLogRepository();
        roles = new InMemoryRoleRepository();
        auditService = new AuditService(auditLog, users, roles);
        authenticationService = new AuthenticationService(new AuthenticatorSystemSimulator(), users, auditService);
        roleService = new RoleManagementService(roles, new InMemoryPermissionRepository(), users,
                auditService);
    }

    private Role role(String id) {
        return roleService.getAvailableRoles().stream().filter(role -> role.getId().equals(id)).findFirst()
                .orElseThrow();
    }

    // A change of roles takes full effect at once (#17).
    @Test
    @DisplayName("A role assigned by admin is in the next login, and in the audit log")
    void anAssignedRoleIsInTheNextLoginAndInTheAuditLog() throws AccessDeniedException, AuthenticationException {
        // Act
        roleService.assignRole("admin", role("PHYSICIAN"), users.findByUsername("lmartin"));
        UserAccount lmartin = authenticationService.login("lmartin", "Admissions.2026");
        List<AuditLog> assigned = auditService.getActivityLog("admin", "action=ROLE_ASSIGNED");

        // Assert
        assertAll(
                () -> assertEquals(List.of("ADMISSIONS_STAFF", "PHYSICIAN"),
                        lmartin.getRoles().stream().map(Role::getId).toList()),
                () -> assertEquals(1, assigned.size()),
                () -> assertEquals("admin", assigned.get(0).getAuthor()),
                () -> assertEquals("user:lmartin/role:PHYSICIAN", assigned.get(0).getTargetResource()));
    }

    // mgarcia is active, but none of their roles has MANAGE_ROLES.
    @Test
    @DisplayName("mgarcia is rejected, and admin sees the attempt in the audit log")
    void mgarciaIsRejectedAndAdminSeesTheAttempt() throws AccessDeniedException {
        // Act
        assertThrows(AccessDeniedException.class,
                () -> roleService.assignRole("mgarcia", role("SYSTEM_ADMINISTRATOR"), users.findByUsername("mgarcia")));
        List<AuditLog> denied = auditService.getActivityLog("admin", "action=ROLE_CHANGE_DENIED");

        // Assert
        assertAll(
                () -> assertEquals(1, denied.size()),
                () -> assertEquals("mgarcia", denied.get(0).getAuthor()),
                () -> assertEquals(List.of("PHYSICIAN", "PATIENT"),
                        users.findByUsername("mgarcia").getRoles().stream().map(Role::getId).toList()));
    }

    private boolean hasConsultAudit(String roleId) {
        return roles.findById(roleId).getPermissions().stream().map(Permission::getCode)
                .anyMatch("CONSULT_AUDIT"::equals);
    }

    // mgarcia has the roles PHYSICIAN and PATIENT, and none of them has CONSULT_AUDIT before the change.
    @Test
    @DisplayName("TC19S-06: CONSULT_AUDIT granted to another role with UC18 lets its user consult the log at once")
    void tc19s06ConsultAuditGrantedToAnotherRoleAllowsAtOnce() throws AccessDeniedException {
        // Arrange
        assertThrows(AccessDeniedException.class, () -> auditService.getActivityLog("mgarcia", null));

        // Act
        roleService.assignPermission("admin", role("PHYSICIAN"), InMemoryPermissionRepository.CONSULT_AUDIT);
        List<AuditLog> denied = auditService.getActivityLog("mgarcia", "action=AUDIT_ACCESS_DENIED");

        // Assert
        assertAll(
                () -> assertEquals(1, denied.size()),
                () -> assertEquals("mgarcia", denied.get(0).getAuthor()));
    }

    @Test
    @DisplayName("TC19S-07: CONSULT_AUDIT revoked from the administrator with UC18 denies admin at once")
    void tc19s07ConsultAuditRevokedFromTheAdministratorDeniesAtOnce() throws AccessDeniedException {
        // Arrange
        auditService.getActivityLog("admin", null);

        // Act
        roleService.revokePermission("admin", role("SYSTEM_ADMINISTRATOR"), InMemoryPermissionRepository.CONSULT_AUDIT);
        AccessDeniedException exception =
                assertThrows(AccessDeniedException.class, () -> auditService.getActivityLog("admin", null));

        // Assert
        List<AuditLog> denied = auditLog.find("action=AUDIT_ACCESS_DENIED");
        assertAll(
                () -> assertEquals("admin", exception.getRequester()),
                () -> assertEquals(1, denied.size()),
                () -> assertEquals("admin", denied.get(0).getAuthor()),
                () -> assertEquals("audit", denied.get(0).getTargetResource()));
    }

    // Unlike MANAGE_ROLES, CONSULT_AUDIT is not protected: the system may be left with nobody able to consult the log.
    @Test
    @DisplayName("TC19S-08: CONSULT_AUDIT can be revoked from every role")
    void tc19s08ConsultAuditCanBeRevokedFromEveryRole() throws AccessDeniedException {
        // Arrange
        roleService.assignPermission("admin", role("PHYSICIAN"), InMemoryPermissionRepository.CONSULT_AUDIT);

        // Act
        assertDoesNotThrow(() -> roleService.revokePermission("admin", role("SYSTEM_ADMINISTRATOR"),
                InMemoryPermissionRepository.CONSULT_AUDIT));
        assertDoesNotThrow(() -> roleService.revokePermission("admin", role("PHYSICIAN"),
                InMemoryPermissionRepository.CONSULT_AUDIT));

        // Assert
        assertAll(
                () -> assertTrue(roles.findAll().stream().noneMatch(role -> hasConsultAudit(role.getId()))),
                () -> assertEquals(List.of("MANAGE_ROLES", "CONFIGURE_SYSTEM", "CONFIGURE_INTEGRATIONS"),
                        roles.findById("SYSTEM_ADMINISTRATOR").getPermissions().stream().map(Permission::getCode)
                                .toList()),
                () -> assertEquals(2, auditLog.find("action=PERMISSION_REVOKED").size()),
                () -> assertThrows(AccessDeniedException.class, () -> auditService.getActivityLog("admin", null)),
                () -> assertThrows(AccessDeniedException.class, () -> auditService.getActivityLog("mgarcia", null)));
    }
}
