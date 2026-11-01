package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.UserAccount;

public interface IUserAccountRepository {
    /** Returns the account with this username, or {@code null} if it does not exist. */
    UserAccount findByUsername(String username);

    void save(UserAccount user);
}
