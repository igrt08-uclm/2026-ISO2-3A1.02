package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.Permission;
import java.util.List;

/** Keeps the permissions in memory: each use case adds its own (MANAGE_ROLES of UC18, CONFIGURE_SYSTEM of UC17). */
public class InMemoryPermissionRepository implements IPermissionRepository {
    public static final Permission MANAGE_ROLES =
            new Permission("MANAGE_ROLES", "Assign roles to users and permissions to roles");

    /** Permission of UC17 to configure the examination catalogue and the other parameters of the system. */
    public static final Permission CONFIGURE_SYSTEM =
            new Permission("CONFIGURE_SYSTEM", "Configure the examination catalogue and the system parameters");

    private final List<Permission> permissions = List.of(MANAGE_ROLES, CONFIGURE_SYSTEM);

    @Override
    public Permission findByCode(String code) {
        for (Permission permission : permissions) {
            if (permission.getCode().equals(code)) {
                return permission;
            }
        }
        return null;
    }

    @Override
    public List<Permission> findAll() {
        return permissions;
    }
}
