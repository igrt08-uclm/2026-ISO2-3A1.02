package es.uclm.esi.iso2.common.access.contracts;

public class AuthenticationException extends Exception {
    private final AuthenticationFailureReason reason;

    public AuthenticationException(AuthenticationFailureReason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public AuthenticationFailureReason getReason() {
        return reason;
    }
}
