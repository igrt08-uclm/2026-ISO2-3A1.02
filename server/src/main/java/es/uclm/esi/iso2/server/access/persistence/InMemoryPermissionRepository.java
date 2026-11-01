package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.Permission;
import java.util.List;

/** Keeps the permissions in memory. C1 only has these two: each use case adds its own. */
public class InMemoryPermissionRepository implements IPermissionRepository {
    public static final Permission MANAGE_ROLES =
            new Permission("MANAGE_ROLES", "Assign roles to users and permissions to roles");
    public static final Permission VIEW_AUDIT_LOG =
            new Permission("VIEW_AUDIT_LOG", "Consult the audit log");

    private final List<Permission> permissions = List.of(MANAGE_ROLES, VIEW_AUDIT_LOG);

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
