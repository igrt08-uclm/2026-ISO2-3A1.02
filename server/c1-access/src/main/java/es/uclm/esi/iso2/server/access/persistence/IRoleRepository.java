package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.Role;
import java.util.List;

public interface IRoleRepository {
    /** Returns the role with this id, or {@code null} if it does not exist. */
    Role findById(String id);

    List<Role> findAll();

    void save(Role role);
}
