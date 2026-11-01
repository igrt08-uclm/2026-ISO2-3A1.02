package es.uclm.esi.iso2.server.access.service;

import es.uclm.esi.iso2.server.access.integration.IAuthenticatorSystem;

/**
 * Test double of {@link IAuthenticatorSystem} that wraps another one. It counts the calls and remembers the last
 * credentials, and returns whatever the wrapped system returns.
 */
class SpyAuthenticatorSystem implements IAuthenticatorSystem {
    private final IAuthenticatorSystem delegate;
    private int calls;
    private String lastUsername;
    private String lastPassword;

    SpyAuthenticatorSystem(IAuthenticatorSystem delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean validateCredentials(String username, String password) {
        calls++;
        lastUsername = username;
        lastPassword = password;
        return delegate.validateCredentials(username, password);
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
