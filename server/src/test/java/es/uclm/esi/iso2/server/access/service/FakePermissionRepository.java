package es.uclm.esi.iso2.server.access.service;

import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.server.access.persistence.IPermissionRepository;
import java.util.List;

/** Test double of {@link IPermissionRepository} with a fixed list of permissions. */
class FakePermissionRepository implements IPermissionRepository {
    private final List<Permission> permissions;

    FakePermissionRepository(Permission... permissions) {
        this.permissions = List.of(permissions);
    }

    @Override
    public Permission findByCode(String code) {
        return permissions.stream().filter(permission -> permission.getCode().equals(code)).findFirst().orElse(null);
    }

    @Override
    public List<Permission> findAll() {
        return permissions;
    }
}
