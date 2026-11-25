package es.uclm.esi.iso2.common.access.contracts;

import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.util.List;

/**
 * Consults and changes roles and permissions. Any user can consult them. To change them, {@code requester} has to be an
 * active user with the permission {@code MANAGE_ROLES}.
 */
public interface IRoleManagementService {
    /**
     * Returns the roles assigned to a user.
     *
     * @param username the username of the user
     * @return the list of roles assigned to the user
     */
    List<Role> getRoles(String username);

    /**
     * Returns the permissions granted to a user.
     *
     * @param username the username of the user
     * @return the list of permissions granted to the user
     */
    List<Permission> getPermissions(String username);

    /**
     * Returns all permissions available in the system.
     *
     * @return the list of all available permissions
     */
    List<Permission> getAvailablePermissions();

    /**
     * Every user account, ordered by username, so that the client can show them in a list.
     *
     * @return the list of all user accounts ordered by username
     */
    List<UserAccount> getUsers();

    /**
     * Every role, ordered by name, so that the client can show them in a list.
     *
     * @return the list of all roles ordered by name
     */
    List<Role> getAvailableRoles();

    /**
     * Tells whether a user has a permission: the user exists, is active and has the permission in one of the roles. It
     * never throws: it answers {@code false} for a {@code null}, unknown or inactive username and for a user without
     * the permission, so that the caller decides what to do (for example register the denied access and throw an
     * {@link AccessDeniedException}). Components other than C1 use it instead of writing their own check.
     *
     * @param username the username to check; it may be {@code null}
     * @param permissionCode the code of the permission, for example {@code CONFIGURE_INTEGRATIONS}
     * @return {@code true} if the user is active and has the permission
     */
    default boolean hasPermission(String username, String permissionCode) {
        if (username == null) {
            return false;
        }
        boolean activeUser = getUsers().stream()
                .anyMatch(user -> username.equals(user.getUsername()) && user.isActive());
        return activeUser && getPermissions(username).stream()
                .anyMatch(permission -> permission.getCode().equals(permissionCode));
    }

    /**
     * Assigns a role to a user account.
     *
     * @param requester the username of the user requesting the assignment
     * @param role the role to assign
     * @param user the user account receiving the role
     * @throws AccessDeniedException if the requester does not have permission to manage roles
     */
    void assignRole(String requester, Role role, UserAccount user) throws AccessDeniedException;

    /**
     * Revokes a role from a user account.
     *
     * @param requester the username of the user requesting the revocation
     * @param role the role to revoke
     * @param user the user account losing the role
     * @throws AccessDeniedException if the requester does not have permission to manage roles
     */
    void revokeRole(String requester, Role role, UserAccount user) throws AccessDeniedException;

    /**
     * Assigns a permission to a role.
     *
     * @param requester the username of the user requesting the assignment
     * @param role the role receiving the permission
     * @param permission the permission to assign
     * @throws AccessDeniedException if the requester does not have permission to manage roles
     */
    void assignPermission(String requester, Role role, Permission permission) throws AccessDeniedException;

    /**
     * Revokes a permission from a role.
     *
     * @param requester the username of the user requesting the revocation
     * @param role the role losing the permission
     * @param permission the permission to revoke
     * @throws AccessDeniedException if the requester does not have permission to manage roles
     */
    void revokePermission(String requester, Role role, Permission permission) throws AccessDeniedException;
}
