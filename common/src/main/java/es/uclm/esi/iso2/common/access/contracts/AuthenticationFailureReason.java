package es.uclm.esi.iso2.common.access.contracts;

/**
 * Reasons why an authentication attempt can fail (UC22).
 */
public enum AuthenticationFailureReason {
    /** The username is not recognized. */
    UNKNOWN_USER,
    /** The password does not match. */
    WRONG_PASSWORD,
    /** The user account is inactive. */
    INACTIVE_USER
}
