package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.Permission;
import java.util.List;

/**
 * Keeps the permissions in memory: each use case adds its own (MANAGE_ROLES of UC18, CONFIGURE_SYSTEM of UC17,
 * CONFIGURE_INTEGRATIONS of UC20, MANAGE_PATIENTS of UC12 and CONSULT_AUDIT of UC19).
 */
public class InMemoryPermissionRepository implements IPermissionRepository {
    /** Permission of UC18 to assign roles to users and permissions to roles. */
    public static final Permission MANAGE_ROLES =
            new Permission("MANAGE_ROLES", "Assign roles to users and permissions to roles");

    /** Permission of UC17 to configure the examination catalogue and the other parameters of the system. */
    public static final Permission CONFIGURE_SYSTEM =
            new Permission("CONFIGURE_SYSTEM", "Configure the examination catalogue and the system parameters");

    /** Permission of UC20 to configure the integrations with the other hospital systems. */
    public static final Permission CONFIGURE_INTEGRATIONS = new Permission("CONFIGURE_INTEGRATIONS",
            "Configure the integrations with the other hospital systems");

    /** Permission of UC12 to search, register and modify patients. */
    public static final Permission MANAGE_PATIENTS =
            new Permission("MANAGE_PATIENTS", "Search, register and modify patients");

    /** Permission of UC19 to consult the audit log of the system activity. */
    public static final Permission CONSULT_AUDIT =
            new Permission("CONSULT_AUDIT", "Consult the audit log of the system activity");

    private final List<Permission> permissions = List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONFIGURE_INTEGRATIONS,
            MANAGE_PATIENTS, CONSULT_AUDIT);

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
