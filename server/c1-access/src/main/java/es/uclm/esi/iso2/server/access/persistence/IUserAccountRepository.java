package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.util.List;

/**
 * Stores and retrieves user accounts.
 */
public interface IUserAccountRepository {
    /**
     * Returns the account with this username, or {@code null} if it does not exist.
     *
     * @param username the username of the account
     * @return the account with this username, or {@code null} if it does not exist
     */
    UserAccount findByUsername(String username);

    /**
     * Returns all user accounts in the repository.
     *
     * @return the list of all user accounts
     */
    List<UserAccount> findAll();

    /**
     * Saves a user account in the repository.
     *
     * @param user the user account to save
     */
    void save(UserAccount user);
}
