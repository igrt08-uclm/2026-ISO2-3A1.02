package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.Role;
import java.util.ArrayList;
import java.util.List;

/** Keeps the roles in memory, preloaded with the four roles of the system. */
public class InMemoryRoleRepository implements IRoleRepository {
    private final List<Role> roles = new ArrayList<>(List.of(
            new Role("SYSTEM_ADMINISTRATOR", "System administrator", "Configures and manages the system",
                    List.of(InMemoryPermissionRepository.MANAGE_ROLES)),
            new Role("PHYSICIAN", "Physician", "Requests and reviews tests and reports"),
            new Role("ADMISSIONS_STAFF", "Admissions staff", "Registers patients and manages appointments"),
            new Role("PATIENT", "Patient", "Accesses their own appointments and results")));

    @Override
    public Role findById(String id) {
        for (Role role : roles) {
            if (role.getId().equals(id)) {
                return role;
            }
        }
        return null;
    }

    @Override
    public List<Role> findAll() {
        return List.copyOf(roles);
    }

    @Override
    public void save(Role role) {
        roles.removeIf(existing -> existing.getId().equals(role.getId()));
        roles.add(role);
    }
}
