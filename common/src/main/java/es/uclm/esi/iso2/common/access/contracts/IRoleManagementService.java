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
    List<Role> getRoles(String username);

    List<Permission> getPermissions(String username);

    List<Permission> getAvailablePermissions();

    /** Every user account, ordered by username, so that the client can show them in a list. */
    List<UserAccount> getUsers();

    /** Every role, ordered by name, so that the client can show them in a list. */
    List<Role> getAvailableRoles();

    void assignRole(String requester, Role role, UserAccount user) throws AccessDeniedException;

    void revokeRole(String requester, Role role, UserAccount user) throws AccessDeniedException;

    void assignPermission(String requester, Role role, Permission permission) throws AccessDeniedException;

    void revokePermission(String requester, Role role, Permission permission) throws AccessDeniedException;
}
