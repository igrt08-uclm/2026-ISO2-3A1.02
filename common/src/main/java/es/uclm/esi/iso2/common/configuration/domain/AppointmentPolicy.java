package es.uclm.esi.iso2.common.configuration.domain;

/** A rule applied to the appointments, for example the notice needed to cancel one (UC17). It is never deleted. */
public class AppointmentPolicy {
    private final String id;
    private final String name;
    private final String description;
    private final boolean active;

    /**
     * Creates the policy.
     *
     * @param id the identifier, given by who creates it
     * @param name the name shown to the users
     * @param description the rule that it applies
     * @param active whether it is applied
     */
    public AppointmentPolicy(String id, String name, String description, boolean active) {
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

    /** Returns whether it is applied. */
    public boolean isActive() {
        return active;
    }

    /** Returns a copy of this policy that is not active. */
    public AppointmentPolicy deactivated() {
        return new AppointmentPolicy(id, name, description, false);
    }
}
