package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.Permission;
import java.util.List;

public interface IPermissionRepository {
    /** Returns the permission with this code, or {@code null} if it does not exist. */
    Permission findByCode(String code);

    List<Permission> findAll();
}
