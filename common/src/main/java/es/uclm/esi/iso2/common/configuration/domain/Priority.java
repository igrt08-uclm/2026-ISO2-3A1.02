package es.uclm.esi.iso2.common.configuration.domain;

/** A tier used by the orders and the waitlist: urgent, preferential or routine (UC17). It is never deleted. */
public class Priority {
    private final String id;
    private final String name;
    private final String description;
    private final boolean active;

    /**
     * Creates the priority.
     *
     * @param id the identifier, given by who creates it
     * @param name the name shown to the users
     * @param description what the tier means
     * @param active whether it can be used
     */
    public Priority(String id, String name, String description, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    /** Returns the identifier. */
    public String getId() {
        return id;
    }

    /** Returns the name. */
    public String getName() {
        return name;
    }

    /** Returns the description. */
    public String getDescription() {
        return description;
    }

    /** Returns whether it can be used. */
    public boolean isActive() {
        return active;
    }

    /** Returns a copy of this priority that is not active. */
    public Priority deactivated() {
        return new Priority(id, name, description, false);
    }
}
