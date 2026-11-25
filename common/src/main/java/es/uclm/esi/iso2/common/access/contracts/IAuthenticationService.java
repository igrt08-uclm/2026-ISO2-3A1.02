package es.uclm.esi.iso2.common.access.contracts;

import es.uclm.esi.iso2.common.access.domain.UserAccount;

/**
 * Authenticates users into the system (UC22).
 */
public interface IAuthenticationService {
    /**
     * Authenticates a user with credentials.
     *
     * @param username the username of the user
     * @param password the password of the user
     * @return the authenticated user account
     * @throws AuthenticationException if authentication fails
     */
    UserAccount login(String username, String password) throws AuthenticationException;
}
