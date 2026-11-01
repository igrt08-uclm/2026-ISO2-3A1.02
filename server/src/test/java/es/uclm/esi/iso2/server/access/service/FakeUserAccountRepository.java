package es.uclm.esi.iso2.server.access.service;

import es.uclm.esi.iso2.common.access.domain.UserAccount;
import es.uclm.esi.iso2.server.access.persistence.IUserAccountRepository;
import java.util.HashMap;
import java.util.Map;

/** Test double of {@link IUserAccountRepository} backed by a map. It counts the calls to {@code save}. */
class FakeUserAccountRepository implements IUserAccountRepository {
    private final Map<String, UserAccount> accounts = new HashMap<>();
    private int saveCalls;

    FakeUserAccountRepository(UserAccount... initialAccounts) {
        for (UserAccount account : initialAccounts) {
            add(account);
        }
    }

    void add(UserAccount account) {
        accounts.put(account.getUsername(), account);
    }

    @Override
    public UserAccount findByUsername(String username) {
        return accounts.get(username);
    }

    @Override
    public void save(UserAccount user) {
        saveCalls++;
        accounts.put(user.getUsername(), user);
    }

    int saveCalls() {
        return saveCalls;
    }
}
