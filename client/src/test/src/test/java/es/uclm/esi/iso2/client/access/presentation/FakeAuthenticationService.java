package es.uclm.esi.iso2.client.access.presentation;

import es.uclm.esi.iso2.common.access.contracts.AuthenticationException;
import es.uclm.esi.iso2.common.access.contracts.AuthenticationFailureReason;
import es.uclm.esi.iso2.common.access.contracts.IAuthenticationService;
import es.uclm.esi.iso2.common.access.domain.UserAccount;

/**
 * Test double of {@link IAuthenticationService}. It either returns a fixed user or throws a fixed reason, and it
 * remembers the calls so tests can check what the screen sent.
 */
class FakeAuthenticationService implements IAuthenticationService {
    private final UserAccount user;
    private AuthenticationFailureReason nextFailure;
    private boolean failAlways;
    private int calls;
    private String lastUsername;
    private String lastPassword;

    private FakeAuthenticationService(UserAccount user, AuthenticationFailureReason failure, boolean failAlways) {
        this.user = user;
        this.nextFailure = failure;
        this.failAlways = failAlways;
    }

    /** A service that always returns {@code user}. */
    static FakeAuthenticationService succeeding(UserAccount user) {
        return new FakeAuthenticationService(user, null, false);
    }

    /** A service that always throws an {@link AuthenticationException} with {@code reason}. */
    static FakeAuthenticationService failing(AuthenticationFailureReason reason) {
        return new FakeAuthenticationService(null, reason, true);
    }

    /** Makes only the next call fail with {@code reason}; the calls after it succeed again. */
    FakeAuthenticationService failNextWith(AuthenticationFailureReason reason) {
        nextFailure = reason;
        failAlways = false;
        return this;
    }

    @Override
    public UserAccount login(String username, String password) throws AuthenticationException {
        calls++;
        lastUsername = username;
        lastPassword = password;
        if (nextFailure != null) {
            AuthenticationFailureReason reason = nextFailure;
            if (!failAlways) {
                nextFailure = null;
            }
            throw new AuthenticationException(reason);
        }
        return user;
    }

    int calls() {
        return calls;
    }

    String lastUsername() {
        return lastUsername;
    }

    String lastPassword() {
        return lastPassword;
    }
}
