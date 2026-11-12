package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.Permission;
import java.util.List;

/** Keeps the permissions in memory. C1 only uses this one: each use case adds its own. */
public class InMemoryPermissionRepository implements IPermissionRepository {
    public static final Permission MANAGE_ROLES =
            new Permission("MANAGE_ROLES", "Assign roles to users and permissions to roles");

    private final List<Permission> permissions = List.of(MANAGE_ROLES);

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
