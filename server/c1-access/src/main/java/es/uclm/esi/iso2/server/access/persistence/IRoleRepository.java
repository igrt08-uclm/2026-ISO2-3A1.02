package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.Role;
import java.util.List;

/**
 * Stores and retrieves user roles (UC18).
 */
public interface IRoleRepository {
    /**
     * Returns the role with this id, or {@code null} if it does not exist.
     *
     * @param id the identifier of the role
     * @return the role with this id, or {@code null} if it does not exist
     */
    Role findById(String id);

    /**
     * Returns all roles in the repository.
     *
     * @return the list of all roles
     */
    List<Role> findAll();

    /**
     * Saves a role in the repository.
     *
     * @param role the role to save
     */
    void save(Role role);
}
