package es.uclm.esi.iso2.server.access.service;

import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.server.access.persistence.IRoleRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Test double of {@link IRoleRepository} backed by a map that keeps the order. It counts the calls to {@code save}. */
class FakeRoleRepository implements IRoleRepository {
    private final Map<String, Role> roles = new LinkedHashMap<>();
    private int saveCalls;

    FakeRoleRepository(Role... initialRoles) {
        for (Role role : initialRoles) {
            add(role);
        }
    }

    /** Changes the data without counting it as a call to {@code save}. */
    void add(Role role) {
        roles.put(role.getId(), role);
    }

    @Override
    public Role findById(String id) {
        return roles.get(id);
    }

    @Override
    public List<Role> findAll() {
        return List.copyOf(roles.values());
    }

    @Override
    public void save(Role role) {
        saveCalls++;
        roles.put(role.getId(), role);
    }

    int saveCalls() {
        return saveCalls;
    }
}
