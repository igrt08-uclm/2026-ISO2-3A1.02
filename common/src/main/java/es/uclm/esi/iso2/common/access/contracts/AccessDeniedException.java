package es.uclm.esi.iso2.common.access.contracts;

public class AccessDeniedException extends Exception {
    private final String requester;

    public AccessDeniedException(String requester) {
        super("Access denied to " + requester);
        this.requester = requester;
    }

    public String getRequester() {
        return requester;
    }
}
