package es.uclm.esi.iso2.server.access.service;

import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import es.uclm.esi.iso2.server.access.persistence.IPermissionRepository;
import es.uclm.esi.iso2.server.access.persistence.IRoleRepository;
import es.uclm.esi.iso2.server.access.persistence.IUserAccountRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RoleManagementService {
    private final IRoleRepository roleRepository;
    private final IPermissionRepository permissionRepository;
    private final IUserAccountRepository userAccountRepository;
    private final IAuditRecorder auditRecorder;

    public RoleManagementService(IRoleRepository roleRepository, IPermissionRepository permissionRepository,
            IUserAccountRepository userAccountRepository, IAuditRecorder auditRecorder) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditRecorder = auditRecorder;
    }

    public List<Role> getRoles(String username) {
        return currentRoles(existingUser(username));
    }

    public List<Permission> getPermissions(String username) {
        Map<String, Permission> permissions = new LinkedHashMap<>();
        for (Role role : getRoles(username)) {
            for (Permission permission : role.getPermissions()) {
                permissions.put(permission.getCode(), permission);
            }
        }
        return List.copyOf(permissions.values());
    }

    public List<Permission> getAvailablePermissions() {
        return permissionRepository.findAll();
    }

    /** The account keeps a copy of its roles, so a change of permissions is read from the repository of roles. */
    private List<Role> currentRoles(UserAccount user) {
        return user.getRoles().stream().map(role -> roleRepository.findById(role.getId()))
                .filter(Objects::nonNull).toList();
    }

    private UserAccount existingUser(String username) {
        UserAccount user = userAccountRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user: " + username);
        }
        return user;
    }
}
