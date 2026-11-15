package es.uclm.esi.iso2.common.access.contracts;

/**
 * The corporate single sign-on (SSO) that checks the credentials. It is in {@code common} so that C1 uses it to log in
 * and C2 uses it to test the connection with the SSO, without one component depending on the module of the other.
 */
public interface IAuthenticatorSystem {
    /**
     * Checks a username and a password against the SSO.
     *
     * @param username the username
     * @param password the password
     * @return {@code true} if the SSO accepts the credentials
     */
    boolean validateCredentials(String username, String password);

    /**
     * Tells whether the SSO answers. It is used by the connection test of UC20 and never changes anything.
     * By default it answers {@code true}, which is right for the simulated SSO (A5); a real adapter must override it.
     *
     * @return {@code true} if the SSO answers
     */
    default boolean ping() {
        return true;
    }
}
