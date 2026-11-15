package es.uclm.esi.iso2.common.shared;

import java.util.List;

/** The data of a request is not valid: it carries every error found, so that all of them are shown together. */
public class ValidationException extends Exception {
    private final List<String> errors;

    /**
     * Creates the exception.
     *
     * @param errors the readable messages of every error found
     */
    public ValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    /** Returns the readable messages of every error found. */
    public List<String> getErrors() {
        return errors;
    }
}
