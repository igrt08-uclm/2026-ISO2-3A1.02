package es.uclm.esi.iso2.server.patients.service;

import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.util.List;
import java.util.Map;

/**
 * Test double of {@link IRoleManagementService} with the users of the test design: {@code lmartin} (active, Admissions
 * staff, with MANAGE_PATIENTS), {@code admin} (active, without MANAGE_PATIENTS), {@code mgarcia} (active physician,
 * no permission), {@code jfernandez} (active patient, no permission), {@code pnavarro} (inactive, with MANAGE_PATIENTS)
 * and no {@code nobody}. Only the two reads that {@code hasPermission} uses are implemented.
 */
class FakeRoleManagementService implements IRoleManagementService {
    private static final Permission MANAGE_ROLES = new Permission("MANAGE_ROLES", "Manage roles");
    private static final Permission CONFIGURE_SYSTEM = new Permission("CONFIGURE_SYSTEM", "Configure the system");
    private static final Permission CONFIGURE_INTEGRATIONS =
            new Permission("CONFIGURE_INTEGRATIONS", "Configure the integrations");
    private static final Permission MANAGE_PATIENTS =
            new Permission("MANAGE_PATIENTS", "Search, register and modify patients");

    private static final List<UserAccount> USERS = List.of(
            new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true, List.of()),
            new UserAccount("mgarcia", "Miguel García López", "miguel.garcia@sescam.test", true, List.of()),
            new UserAccount("lmartin", "Lucía Martín Pérez", "lucia.martin@sescam.test", true, List.of()),
            new UserAccount("jfernandez", "Juan Fernández Gómez", "juan.fernandez@sescam.test", true, List.of()),
            new UserAccount("pnavarro", "Pedro Navarro Díaz", "pedro.navarro@sescam.test", false, List.of()));

    private static final Map<String, List<Permission>> PERMISSIONS = Map.of(
            "admin", List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONFIGURE_INTEGRATIONS),
            "mgarcia", List.of(),
            "lmartin", List.of(MANAGE_PATIENTS),
            "jfernandez", List.of(),
            "pnavarro", List.of(MANAGE_PATIENTS));

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
        throw new UnsupportedOperationException("Not used by C3");
    }

    @Override
    public List<Permission> getAvailablePermissions() {
        throw new UnsupportedOperationException("Not used by C3");
    }

    @Override
    public List<Role> getAvailableRoles() {
        throw new UnsupportedOperationException("Not used by C3");
    }

    @Override
    public void assignRole(String requester, Role role, UserAccount user) {
        throw new UnsupportedOperationException("Not used by C3");
    }

    @Override
    public void revokeRole(String requester, Role role, UserAccount user) {
        throw new UnsupportedOperationException("Not used by C3");
    }

    @Override
    public void assignPermission(String requester, Role role, Permission permission) {
        throw new UnsupportedOperationException("Not used by C3");
    }

    @Override
    public void revokePermission(String requester, Role role, Permission permission) {
        throw new UnsupportedOperationException("Not used by C3");
    }
}
