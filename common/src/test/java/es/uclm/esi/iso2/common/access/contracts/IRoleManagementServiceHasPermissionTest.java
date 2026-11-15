package es.uclm.esi.iso2.common.access.contracts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the default method {@link IRoleManagementService#hasPermission} (C1 patch 1.2.2, #112). A small fake gives
 * the users and their permissions, so only the logic of the default method is tested: the user must exist, be active
 * and have the permission in one of the roles, and the method never throws.
 */
@DisplayName("IRoleManagementService.hasPermission")
class IRoleManagementServiceHasPermissionTest {
    private static final String CONFIGURE_INTEGRATIONS = "CONFIGURE_INTEGRATIONS";

    private IRoleManagementService service;

    @BeforeEach
    void setUp() {
        service = new FakeRoleManagementService();
    }

    @Test
    @DisplayName("TC-P-01: an active user with the permission has it")
    void activeUserWithThePermission() {
        assertTrue(service.hasPermission("admin", CONFIGURE_INTEGRATIONS));
    }

    @Test
    @DisplayName("TC-P-02: an active user without the permission does not have it")
    void activeUserWithoutThePermission() {
        assertFalse(service.hasPermission("mgarcia", CONFIGURE_INTEGRATIONS));
    }

    @Test
    @DisplayName("TC-P-03: an inactive user does not have it, even if one of the roles gives it")
    void inactiveUserDoesNotHaveIt() {
        assertFalse(service.hasPermission("pnavarro", CONFIGURE_INTEGRATIONS));
    }

    @Test
    @DisplayName("TC-P-04: an unknown user does not have it")
    void unknownUserDoesNotHaveIt() {
        assertFalse(service.hasPermission("nobody", CONFIGURE_INTEGRATIONS));
    }

    @Test
    @DisplayName("TC-P-05: a null username gives false instead of an exception")
    void nullUsernameGivesFalse() {
        assertFalse(service.hasPermission(null, CONFIGURE_INTEGRATIONS));
    }

    @Test
    @DisplayName("TC-P-06: a null permission code gives false instead of an exception")
    void nullPermissionCodeGivesFalse() {
        assertFalse(service.hasPermission("admin", null));
    }

    @Test
    @DisplayName("TC-P-07: the code must be exactly the same, so a lower-case code is not the permission")
    void codeIsCaseSensitive() {
        assertFalse(service.hasPermission("admin", "configure_integrations"));
    }

    /**
     * Test double with three users: {@code admin} (active, CONFIGURE_INTEGRATIONS), {@code mgarcia} (active, no
     * permission) and {@code pnavarro} (inactive, CONFIGURE_INTEGRATIONS). Only the two reads used by the default
     * method are implemented.
     */
    private static final class FakeRoleManagementService implements IRoleManagementService {
        private static final Permission PERMISSION = new Permission(CONFIGURE_INTEGRATIONS, "Configure integrations");

        private static final List<UserAccount> USERS = List.of(
                new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true, List.of()),
                new UserAccount("mgarcia", "Miguel García López", "miguel.garcia@sescam.test", true, List.of()),
                new UserAccount("pnavarro", "Pedro Navarro Díaz", "pedro.navarro@sescam.test", false, List.of()));

        private static final Map<String, List<Permission>> PERMISSIONS = Map.of(
                "admin", List.of(PERMISSION),
                "mgarcia", List.of(),
                "pnavarro", List.of(PERMISSION));

        @Override
        public List<UserAccount> getUsers() {
            return USERS;
        }

        @Override
        public List<Permission> getPermissions(String username) {
            return PERMISSIONS.getOrDefault(username, List.of());
        }

        @Override
        public List<Role> getRoles(String username) {
            throw new UnsupportedOperationException("Not used by hasPermission");
        }

        @Override
        public List<Permission> getAvailablePermissions() {
            throw new UnsupportedOperationException("Not used by hasPermission");
        }

        @Override
        public List<Role> getAvailableRoles() {
            throw new UnsupportedOperationException("Not used by hasPermission");
        }

        @Override
        public void assignRole(String requester, Role role, UserAccount user) {
            throw new UnsupportedOperationException("Not used by hasPermission");
        }

        @Override
        public void revokeRole(String requester, Role role, UserAccount user) {
            throw new UnsupportedOperationException("Not used by hasPermission");
        }

        @Override
        public void assignPermission(String requester, Role role, Permission permission) {
            throw new UnsupportedOperationException("Not used by hasPermission");
        }

        @Override
        public void revokePermission(String requester, Role role, Permission permission) {
            throw new UnsupportedOperationException("Not used by hasPermission");
        }
    }
}
