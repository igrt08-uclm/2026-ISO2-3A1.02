package es.uclm.esi.iso2.common.configuration.domain;

/** A type of examination of the catalogue (UC17). It is deactivated, never deleted, so that old orders keep it. */
public class TestType {
    private final String id;
    private final String name;
    private final String modality;
    private final String room;
    private final int durationMinutes;
    private final String preparation;
    private final boolean requiresContrast;
    private final boolean requiresConsent;
    private final String safetyInfo;
    private final String priority;
    private final boolean followUpEligible;
    private final boolean active;

    /**
     * Creates a test type.
     *
     * @param id the identifier, given by who creates it, for example {@code CT-HEAD}
     * @param name the name shown to the users
     * @param modality the kind of equipment, for example CT
     * @param room the room where it is done
     * @param durationMinutes how long it lasts, in minutes
     * @param preparation what the patient must do before it
     * @param requiresContrast whether it needs a contrast agent
     * @param requiresConsent whether the patient must sign a consent
     * @param safetyInfo the safety information
     * @param priority the id of the {@link Priority} that it has by default
     * @param followUpEligible whether it can be ordered as a periodic follow-up
     * @param active whether it can be used for new orders
     */
    public TestType(String id, String name, String modality, String room, int durationMinutes, String preparation,
            boolean requiresContrast, boolean requiresConsent, String safetyInfo, String priority,
            boolean followUpEligible, boolean active) {
        this.id = id;
        this.name = name;
        this.modality = modality;
        this.room = room;
        this.durationMinutes = durationMinutes;
        this.preparation = preparation;
        this.requiresContrast = requiresContrast;
        this.requiresConsent = requiresConsent;
        this.safetyInfo = safetyInfo;
        this.priority = priority;
        this.followUpEligible = followUpEligible;
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

    /** Returns the modality. */
    public String getModality() {
        return modality;
    }

    /** Returns the room. */
    public String getRoom() {
        return room;
    }

    /** Returns the duration in minutes. */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    /** Returns the preparation. */
    public String getPreparation() {
        return preparation;
    }

    /** Returns whether it needs a contrast agent. */
    public boolean isRequiresContrast() {
        return requiresContrast;
    }

    /** Returns whether it needs a signed consent. */
    public boolean isRequiresConsent() {
        return requiresConsent;
    }

    /** Returns the safety information. */
    public String getSafetyInfo() {
        return safetyInfo;
    }

    /** Returns the id of its default priority. */
    public String getPriority() {
        return priority;
    }

    /** Returns whether it can be a periodic follow-up. */
    public boolean isFollowUpEligible() {
        return followUpEligible;
    }

    /** Returns whether it can be used for new orders. */
    public boolean isActive() {
        return active;
    }

    /** Returns a copy of this test type that is not active. */
    public TestType deactivated() {
        return new TestType(id, name, modality, room, durationMinutes, preparation, requiresContrast,
                requiresConsent, safetyInfo, priority, followUpEligible, false);
    }
}
