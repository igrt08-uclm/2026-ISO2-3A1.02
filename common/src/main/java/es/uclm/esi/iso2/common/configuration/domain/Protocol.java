package es.uclm.esi.iso2.common.configuration.domain;

/** How an examination is performed (UC17). It is deactivated, never deleted. */
public class Protocol {
    private final String id;
    private final String name;
    private final String description;
    private final boolean active;

    /**
     * Creates the protocol.
     *
     * @param id the identifier, given by who creates it
     * @param name the name shown to the users
     * @param description how the examination is performed
     * @param active whether it can be used
     */
    public Protocol(String id, String name, String description, boolean active) {
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

    /** Returns a copy of this protocol that is not active. */
    public Protocol deactivated() {
        return new Protocol(id, name, description, false);
    }
}
