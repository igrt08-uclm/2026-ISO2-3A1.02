package es.uclm.esi.iso2.server.access.persistence;

import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Keeps the accounts in memory, preloaded with the test users agreed in #13. */
public class InMemoryUserAccountRepository implements IUserAccountRepository {
    private static final Role SYSTEM_ADMINISTRATOR =
            new Role("SYSTEM_ADMINISTRATOR", "System administrator", "Configures and manages the system");
    private static final Role PHYSICIAN =
            new Role("PHYSICIAN", "Physician", "Requests and reviews tests and reports");
    private static final Role ADMISSIONS_STAFF =
            new Role("ADMISSIONS_STAFF", "Admissions staff", "Registers patients and manages appointments");
    private static final Role PATIENT =
            new Role("PATIENT", "Patient", "Accesses their own appointments and results");

    private final List<UserAccount> users = new ArrayList<>(List.of(
            new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true,
                    List.of(SYSTEM_ADMINISTRATOR)),
            new UserAccount("mgarcia", "Miguel García López", "miguel.garcia@sescam.test", true,
                    List.of(PHYSICIAN, PATIENT)),
            new UserAccount("lmartin", "Lucía Martín Pérez", "lucia.martin@sescam.test", true,
                    List.of(ADMISSIONS_STAFF)),
            new UserAccount("jfernandez", "Juan Fernández Gómez", "juan.fernandez@sescam.test", true,
                    List.of(PATIENT)),
            new UserAccount("pnavarro", "Pedro Navarro Díaz", "pedro.navarro@sescam.test", false,
                    List.of(PHYSICIAN))));

    @Override
    public UserAccount findByUsername(String username) {
        for (UserAccount user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public List<UserAccount> findAll() {
        return List.copyOf(users);
    }

    @Override
    public void save(UserAccount user) {
        Objects.requireNonNull(user.getUsername(), "username");
        users.removeIf(existing -> existing.getUsername().equals(user.getUsername()));
        users.add(user);
    }
}
