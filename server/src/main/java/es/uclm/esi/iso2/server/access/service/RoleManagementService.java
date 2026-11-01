package es.uclm.esi.iso2.server.access.service;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import es.uclm.esi.iso2.server.access.persistence.IPermissionRepository;
import es.uclm.esi.iso2.server.access.persistence.IRoleRepository;
import es.uclm.esi.iso2.server.access.persistence.IUserAccountRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RoleManagementService implements IRoleManagementService {
    private static final String MANAGE_ROLES = "MANAGE_ROLES";
    private static final String ROLE_ASSIGNED = "ROLE_ASSIGNED";
    private static final String ROLE_REVOKED = "ROLE_REVOKED";
    private static final String PERMISSION_ASSIGNED = "PERMISSION_ASSIGNED";
    private static final String PERMISSION_REVOKED = "PERMISSION_REVOKED";
    private static final String ROLE_CHANGE_DENIED = "ROLE_CHANGE_DENIED";
    private static final String ROLES_RESOURCE = "roles";

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

    @Override
    public List<Role> getRoles(String username) {
        return currentRoles(existingUser(username));
    }

    @Override
    public List<Permission> getPermissions(String username) {
        Map<String, Permission> permissions = new LinkedHashMap<>();
        for (Role role : getRoles(username)) {
            for (Permission permission : role.getPermissions()) {
                permissions.put(permission.getCode(), permission);
            }
        }
        return List.copyOf(permissions.values());
    }

    @Override
    public List<Permission> getAvailablePermissions() {
        return permissionRepository.findAll();
    }

    @Override
    public void assignRole(String requester, Role role, UserAccount user) throws AccessDeniedException {
        checkCanManageRoles(requester);
        UserAccount account = existingUser(user);
        Role assigned = existingRole(role);
        if (!hasRole(account, assigned)) {
            List<Role> roles = new ArrayList<>(account.getRoles());
            roles.add(assigned);
            userAccountRepository.save(withRoles(account, roles));
            auditRecorder.register(requester, ROLE_ASSIGNED, userAndRole(account, assigned));
        }
    }

    @Override
    public void revokeRole(String requester, Role role, UserAccount user) throws AccessDeniedException {
        checkCanManageRoles(requester);
        UserAccount account = existingUser(user);
        Role revoked = existingRole(role);
        if (hasRole(account, revoked)) {
            List<Role> roles = new ArrayList<>(account.getRoles());
            roles.removeIf(existing -> existing.getId().equals(revoked.getId()));
            UserAccount changed = withRoles(account, roles);
            requireAnAdministratorAfter(changed, null);
            userAccountRepository.save(changed);
            auditRecorder.register(requester, ROLE_REVOKED, userAndRole(account, revoked));
        }
    }

    @Override
    public void assignPermission(String requester, Role role, Permission permission) throws AccessDeniedException {
        checkCanManageRoles(requester);
        Role current = existingRole(role);
        Permission assigned = existingPermission(permission);
        if (!hasPermission(current, assigned.getCode())) {
            List<Permission> permissions = new ArrayList<>(current.getPermissions());
            permissions.add(assigned);
            roleRepository.save(withPermissions(current, permissions));
            auditRecorder.register(requester, PERMISSION_ASSIGNED, roleAndPermission(current, assigned));
        }
    }

    @Override
    public void revokePermission(String requester, Role role, Permission permission) throws AccessDeniedException {
        checkCanManageRoles(requester);
        Role current = existingRole(role);
        Permission revoked = existingPermission(permission);
        if (hasPermission(current, revoked.getCode())) {
            List<Permission> permissions = new ArrayList<>(current.getPermissions());
            permissions.removeIf(existing -> existing.getCode().equals(revoked.getCode()));
            Role changed = withPermissions(current, permissions);
            requireAnAdministratorAfter(null, changed);
            roleRepository.save(changed);
            auditRecorder.register(requester, PERMISSION_REVOKED, roleAndPermission(current, revoked));
        }
    }

    private void checkCanManageRoles(String requester) throws AccessDeniedException {
        UserAccount user = userAccountRepository.findByUsername(requester);
        if (user == null || !canManageRoles(user, null)) {
            auditRecorder.register(requester, ROLE_CHANGE_DENIED, ROLES_RESOURCE);
            throw new AccessDeniedException(requester);
        }
    }

    /** {@code changedRole}, if any, is used instead of the saved role with the same id. */
    private boolean canManageRoles(UserAccount user, Role changedRole) {
        return user.isActive() && currentRoles(user).stream()
                .map(role -> changedRole != null && role.getId().equals(changedRole.getId()) ? changedRole : role)
                .anyMatch(role -> hasPermission(role, MANAGE_ROLES));
    }

    /** Rejects a change that would leave nobody able to manage the roles. */
    private void requireAnAdministratorAfter(UserAccount changedUser, Role changedRole) {
        boolean remains = userAccountRepository.findAll().stream()
                .map(user -> changedUser != null && user.getUsername().equals(changedUser.getUsername())
                        ? changedUser : user)
                .anyMatch(user -> canManageRoles(user, changedRole));
        if (!remains) {
            throw new IllegalStateException("The system needs an active administrator");
        }
    }

    /** The account keeps a copy of its roles, so a change of permissions is read from the repository of roles. */
    private List<Role> currentRoles(UserAccount user) {
        return user.getRoles().stream().map(role -> roleRepository.findById(role.getId()))
                .filter(Objects::nonNull).toList();
    }

    private UserAccount existingUser(UserAccount user) {
        if (user == null) {
            throw new IllegalArgumentException("Missing user");
        }
        return existingUser(user.getUsername());
    }

    private UserAccount existingUser(String username) {
        UserAccount user = userAccountRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user: " + username);
        }
        return user;
    }

    private Role existingRole(Role role) {
        Role existing = role == null ? null : roleRepository.findById(role.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Unknown role");
        }
        return existing;
    }

    private Permission existingPermission(Permission permission) {
        Permission existing = permission == null ? null : permissionRepository.findByCode(permission.getCode());
        if (existing == null) {
            throw new IllegalArgumentException("Unknown permission");
        }
        return existing;
    }

    private static boolean hasRole(UserAccount user, Role role) {
        return user.getRoles().stream().anyMatch(existing -> existing.getId().equals(role.getId()));
    }

    private static boolean hasPermission(Role role, String code) {
        return role.getPermissions().stream().anyMatch(permission -> permission.getCode().equals(code));
    }

    private static UserAccount withRoles(UserAccount user, List<Role> roles) {
        return new UserAccount(user.getUsername(), user.getFullName(), user.getEmail(), user.isActive(), roles);
    }

    private static Role withPermissions(Role role, List<Permission> permissions) {
        return new Role(role.getId(), role.getName(), role.getDescription(), permissions);
    }

    private static String userAndRole(UserAccount user, Role role) {
        return "user:" + user.getUsername() + "/role:" + role.getId();
    }

    private static String roleAndPermission(Role role, Permission permission) {
        return "role:" + role.getId() + "/permission:" + permission.getCode();
    }
}
