package es.uclm.esi.iso2.common.access.contracts;

/**
 * Signals that an authentication attempt failed (UC22).
 */
public class AuthenticationException extends Exception {
    private final AuthenticationFailureReason reason;

    /**
     * Creates an authentication exception.
     *
     * @param reason the reason why authentication failed
     */
    public AuthenticationException(AuthenticationFailureReason reason) {
        super(reason.name());
        this.reason = reason;
    }

    /**
     * Returns the reason why authentication failed.
     *
     * @return the failure reason
     */
    public AuthenticationFailureReason getReason() {
        return reason;
    }
}
