package es.uclm.esi.iso2.server.orders.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.util.List;
import java.util.Map;

/**
 * Test double of {@link IRoleManagementService} with the users of the test design of UC06: {@code mgarcia} (active
 * physician, with ORDER_PROCEDURES), {@code lmartin} (active Admissions staff, with MANAGE_APPOINTMENTS),
 * {@code pnavarro} (inactive physician, with ORDER_PROCEDURES), {@code admin} (active, with the permissions of C2 and
 * C1 but not of C4) and {@code jfernandez} (active patient, no permission); {@code nobody} does not exist. Only the
 * two reads that {@code hasPermission} uses are implemented.
 */
class FakeRoleManagementService implements IRoleManagementService {
    private static final Permission MANAGE_ROLES = new Permission("MANAGE_ROLES", "Manage roles");
    private static final Permission CONFIGURE_SYSTEM = new Permission("CONFIGURE_SYSTEM", "Configure the system");
    private static final Permission CONFIGURE_INTEGRATIONS =
            new Permission("CONFIGURE_INTEGRATIONS", "Configure the integrations");
    private static final Permission ORDER_PROCEDURES =
            new Permission("ORDER_PROCEDURES", "Order diagnostic and periodic follow-up procedures");
    private static final Permission MANAGE_APPOINTMENTS =
            new Permission("MANAGE_APPOINTMENTS", "Manage appointments, waitlists and daily schedules");

    private static final List<UserAccount> USERS = List.of(
            new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true, List.of()),
            new UserAccount("mgarcia", "Miguel García López", "miguel.garcia@sescam.test", true, List.of()),
            new UserAccount("lmartin", "Lucía Martín Pérez", "lucia.martin@sescam.test", true, List.of()),
            new UserAccount("jfernandez", "Juan Fernández Gómez", "juan.fernandez@sescam.test", true, List.of()),
            new UserAccount("pnavarro", "Pedro Navarro Díaz", "pedro.navarro@sescam.test", false, List.of()));

    private static final Map<String, List<Permission>> PERMISSIONS = Map.of(
            "admin", List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONFIGURE_INTEGRATIONS),
            "mgarcia", List.of(ORDER_PROCEDURES),
            "lmartin", List.of(MANAGE_APPOINTMENTS),
            "jfernandez", List.of(),
            "pnavarro", List.of(ORDER_PROCEDURES));

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
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public List<Permission> getAvailablePermissions() {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public List<Role> getAvailableRoles() {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public void assignRole(String requester, Role role, UserAccount user) throws AccessDeniedException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public void revokeRole(String requester, Role role, UserAccount user) throws AccessDeniedException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public void assignPermission(String requester, Role role, Permission permission) throws AccessDeniedException {
        throw new UnsupportedOperationException("Not used by C4");
    }

    @Override
    public void revokePermission(String requester, Role role, Permission permission) throws AccessDeniedException {
        throw new UnsupportedOperationException("Not used by C4");
    }
}
