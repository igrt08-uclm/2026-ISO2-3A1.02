package es.uclm.esi.iso2.common.access.contracts;

/**
 * Signals that an operation was denied because the requester lacks permissions.
 */
public class AccessDeniedException extends Exception {
    private final String requester;

    /**
     * Creates an access denied exception.
     *
     * @param requester the username of the user whose access was denied
     */
    public AccessDeniedException(String requester) {
        super("Access denied to " + requester);
        this.requester = requester;
    }

    /**
     * Returns the username of the requester.
     *
     * @return the requester
     */
    public String getRequester() {
        return requester;
    }
}
