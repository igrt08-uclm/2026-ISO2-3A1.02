package es.uclm.esi.iso2.common.configuration.domain;

/** The result of a connection test with an external hospital system (UC20). A test never changes the configuration. */
public class ConnectionTestResult {
    private final boolean success;
    private final long responseTimeMs;
    private final String message;

    /**
     * Creates the result.
     *
     * @param success whether the system answered
     * @param responseTimeMs how long the answer took, in milliseconds; 0 if there was no answer
     * @param message the reason, readable by the user, for example {@code Connection OK}
     */
    public ConnectionTestResult(boolean success, long responseTimeMs, String message) {
        this.success = success;
        this.responseTimeMs = responseTimeMs;
        this.message = message;
    }

    /** Tells whether the system answered. */
    public boolean isSuccess() {
        return success;
    }

    /** Returns how long the answer took, in milliseconds. */
    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    /** Returns the reason, readable by the user. */
    public String getMessage() {
        return message;
    }
}
