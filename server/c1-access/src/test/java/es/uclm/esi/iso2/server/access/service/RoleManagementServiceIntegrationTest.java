package es.uclm.esi.iso2.server.access.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.AuthenticationException;
import es.uclm.esi.iso2.common.access.domain.AuditLog;
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
 * {@link AuditService}, as {@code Main} creates them, using the test users agreed in #13.
 */
@DisplayName("RoleManagementService with the real login, audit and repositories")
class RoleManagementServiceIntegrationTest {
    private InMemoryUserAccountRepository users;
    private AuditService auditService;
    private AuthenticationService authenticationService;
    private RoleManagementService roleService;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserAccountRepository();
        auditService = new AuditService(new InMemoryAuditLogRepository(), users);
        authenticationService = new AuthenticationService(new AuthenticatorSystemSimulator(), users, auditService);
        roleService = new RoleManagementService(new InMemoryRoleRepository(), new InMemoryPermissionRepository(), users,
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
}
