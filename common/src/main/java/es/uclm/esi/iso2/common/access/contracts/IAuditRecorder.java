package es.uclm.esi.iso2.common.access.contracts;

/**
 * Leaves a trace of an action in the audit log (UC19). Every component that changes data uses it, so that no
 * component depends on the module of C1 to register its actions.
 */
public interface IAuditRecorder {
    /**
     * Registers one action.
     *
     * @param author the username of the user who did the action
     * @param action the code of the action, for example {@code ROLE_ASSIGNED}
     * @param targetResource what the action was done on, for example {@code roles}
     */
    void register(String author, String action, String targetResource);
}
