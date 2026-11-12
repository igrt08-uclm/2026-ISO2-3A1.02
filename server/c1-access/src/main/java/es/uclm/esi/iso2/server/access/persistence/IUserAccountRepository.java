package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.util.List;

public interface IUserAccountRepository {
    /** Returns the account with this username, or {@code null} if it does not exist. */
    UserAccount findByUsername(String username);

    List<UserAccount> findAll();

    void save(UserAccount user);
}
