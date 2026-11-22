package es.uclm.esi.iso2.server.configuration.service;

import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.util.List;
import java.util.Map;

/**
 * Test double of {@link IRoleManagementService} with the users of the test design: {@code admin} (active, with
 * CONFIGURE_SYSTEM and CONFIGURE_INTEGRATIONS), {@code mgarcia} (active, no permission), {@code lmartin} (active, with
 * CONFIGURE_SYSTEM only), {@code pnavarro} (inactive, with CONFIGURE_SYSTEM and CONFIGURE_INTEGRATIONS), the users of
 * the patch C2 1.1.2 (#174) {@code druiz} (active, with ORDER_PROCEDURES only), {@code aortega} (active, with
 * MANAGE_APPOINTMENTS only), {@code rmoreno} (inactive, with ORDER_PROCEDURES) and {@code cvega} (inactive, with
 * MANAGE_APPOINTMENTS), and no {@code nobody}. Only the two reads that C2 uses are implemented; {@code hasPermission}
 * is the default method.
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
            new UserAccount("lmartin", "Lucía Martín Gómez", "lucia.martin@sescam.test", true, List.of()),
            new UserAccount("mgarcia", "Miguel García López", "miguel.garcia@sescam.test", true, List.of()),
            new UserAccount("pnavarro", "Pedro Navarro Díaz", "pedro.navarro@sescam.test", false, List.of()),
            new UserAccount("druiz", "Daniel Ruiz Torres", "daniel.ruiz@sescam.test", true, List.of()),
            new UserAccount("aortega", "Ana Ortega Vidal", "ana.ortega@sescam.test", true, List.of()),
            new UserAccount("rmoreno", "Raúl Moreno Gil", "raul.moreno@sescam.test", false, List.of()),
            new UserAccount("cvega", "Carmen Vega Ramos", "carmen.vega@sescam.test", false, List.of()));

    private static final Map<String, List<Permission>> PERMISSIONS = Map.of(
            "admin", List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONFIGURE_INTEGRATIONS),
            "lmartin", List.of(CONFIGURE_SYSTEM),
            "mgarcia", List.of(),
            "pnavarro", List.of(CONFIGURE_SYSTEM, CONFIGURE_INTEGRATIONS),
            "druiz", List.of(ORDER_PROCEDURES),
            "aortega", List.of(MANAGE_APPOINTMENTS),
            "rmoreno", List.of(ORDER_PROCEDURES),
            "cvega", List.of(MANAGE_APPOINTMENTS));

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
        throw new UnsupportedOperationException("Not used by C2");
    }

    @Override
    public List<Permission> getAvailablePermissions() {
        throw new UnsupportedOperationException("Not used by C2");
    }

    @Override
    public List<Role> getAvailableRoles() {
        throw new UnsupportedOperationException("Not used by C2");
    }

    @Override
    public void assignRole(String requester, Role role, UserAccount user) {
        throw new UnsupportedOperationException("Not used by C2");
    }

    @Override
    public void revokeRole(String requester, Role role, UserAccount user) {
        throw new UnsupportedOperationException("Not used by C2");
    }

    @Override
    public void assignPermission(String requester, Role role, Permission permission) {
        throw new UnsupportedOperationException("Not used by C2");
    }

    @Override
    public void revokePermission(String requester, Role role, Permission permission) {
        throw new UnsupportedOperationException("Not used by C2");
    }
}
