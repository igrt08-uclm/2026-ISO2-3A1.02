package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.Permission;
import java.util.List;

/**
 * Stores and retrieves system permissions (UC18).
 */
public interface IPermissionRepository {
    /**
     * Returns the permission with this code, or {@code null} if it does not exist.
     *
     * @param code the code of the permission
     * @return the permission with this code, or {@code null} if it does not exist
     */
    Permission findByCode(String code);

    /**
     * Returns all permissions in the repository.
     *
     * @return the list of all permissions
     */
    List<Permission> findAll();
}
