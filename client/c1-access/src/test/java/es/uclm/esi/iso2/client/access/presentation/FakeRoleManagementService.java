package es.uclm.esi.iso2.client.access.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test double of {@link IRoleManagementService}. It keeps the users and the roles in memory, in the order they were
 * given, remembers every change the screen asks for, and fails the next change if a test asks it to.
 */
class FakeRoleManagementService implements IRoleManagementService {
    private final Map<String, UserAccount> users = new LinkedHashMap<>();
    private final Map<String, Role> roles = new LinkedHashMap<>();
    private final List<Permission> permissions;
    private final List<String> changes = new ArrayList<>();
    private Exception nextFailure;

    FakeRoleManagementService(List<UserAccount> users, List<Role> roles, List<Permission> permissions) {
        users.forEach(this::setUser);
        roles.forEach(this::setRole);
        this.permissions = List.copyOf(permissions);
    }

    /** Changes the data without the screen, as if another user had done it. It is not remembered as a change. */
    void setUser(UserAccount user) {
        users.put(user.getUsername(), user);
    }

    /** Changes the data without the screen, as if another user had done it. It is not remembered as a change. */
    void setRole(Role role) {
        roles.put(role.getId(), role);
    }

    /** The next change fails with this exception, and changes nothing. */
    void failNextChange(Exception failure) {
        nextFailure = failure;
    }

    /** Each change as {@code operation requester role target}, for example {@code assignRole admin PATIENT mgarcia}. */
    List<String> changes() {
        return Collections.unmodifiableList(changes);
    }

    @Override
    public List<Role> getRoles(String username) {
        return users.get(username).getRoles().stream().map(role -> roles.get(role.getId())).toList();
    }

    @Override
    public List<Permission> getPermissions(String username) {
        return getRoles(username).stream().flatMap(role -> role.getPermissions().stream()).distinct().toList();
    }

    @Override
    public List<Permission> getAvailablePermissions() {
        return permissions;
    }

    @Override
    public List<UserAccount> getUsers() {
        return List.copyOf(users.values());
    }

    @Override
    public List<Role> getAvailableRoles() {
        return List.copyOf(roles.values());
    }

    @Override
    public void assignRole(String requester, Role role, UserAccount user) throws AccessDeniedException {
        change("assignRole", requester, role.getId(), user.getUsername());
        List<Role> userRoles = new ArrayList<>(users.get(user.getUsername()).getRoles());
        userRoles.add(role);
        setUser(withRoles(users.get(user.getUsername()), userRoles));
    }

    @Override
    public void revokeRole(String requester, Role role, UserAccount user) throws AccessDeniedException {
        change("revokeRole", requester, role.getId(), user.getUsername());
        List<Role> userRoles = new ArrayList<>(users.get(user.getUsername()).getRoles());
        userRoles.removeIf(existing -> existing.getId().equals(role.getId()));
        setUser(withRoles(users.get(user.getUsername()), userRoles));
    }

    @Override
    public void assignPermission(String requester, Role role, Permission permission) throws AccessDeniedException {
        change("assignPermission", requester, role.getId(), permission.getCode());
        List<Permission> rolePermissions = new ArrayList<>(roles.get(role.getId()).getPermissions());
        rolePermissions.add(permission);
        setRole(withPermissions(roles.get(role.getId()), rolePermissions));
    }

    @Override
    public void revokePermission(String requester, Role role, Permission permission) throws AccessDeniedException {
        change("revokePermission", requester, role.getId(), permission.getCode());
        List<Permission> rolePermissions = new ArrayList<>(roles.get(role.getId()).getPermissions());
        rolePermissions.removeIf(existing -> existing.getCode().equals(permission.getCode()));
        setRole(withPermissions(roles.get(role.getId()), rolePermissions));
    }

    private void change(String operation, String requester, String role, String target) throws AccessDeniedException {
        changes.add(String.join(" ", operation, requester, role, target));
        Exception failure = nextFailure;
        nextFailure = null;
        if (failure instanceof AccessDeniedException accessDenied) {
            throw accessDenied;
        }
        if (failure instanceof RuntimeException runtime) {
            throw runtime;
        }
    }

    private static UserAccount withRoles(UserAccount user, List<Role> userRoles) {
        return new UserAccount(user.getUsername(), user.getFullName(), user.getEmail(), user.isActive(), userRoles);
    }

    private static Role withPermissions(Role role, List<Permission> rolePermissions) {
        return new Role(role.getId(), role.getName(), role.getDescription(), rolePermissions);
    }
}
