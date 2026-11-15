package es.uclm.esi.iso2.common.configuration.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The configuration of the integration with one external hospital system (UC20). */
public class IntegrationConfiguration {
    private final HospitalSystemType system;
    private final String endpoint;
    private final Map<String, String> settings;
    private final boolean enabled;
    private final IntegrationMode mode;
    private final IntegrationStatus status;

    /**
     * Creates the configuration.
     *
     * @param system the external system it belongs to
     * @param endpoint the address of the real system; it may be empty in simulated mode
     * @param settings the extra settings as key and value, in the order they were written; copied, and a
     *        {@code null} map is kept as {@code null}
     * @param enabled whether the integration is enabled
     * @param mode whether it works against a simulator or against the real system
     * @param status the state of the integration, which the server sets and ignores when it comes from the client
     */
    public IntegrationConfiguration(HospitalSystemType system, String endpoint, Map<String, String> settings,
            boolean enabled, IntegrationMode mode, IntegrationStatus status) {
        this.system = system;
        this.endpoint = endpoint;
        this.settings = settings == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(settings));
        this.enabled = enabled;
        this.mode = mode;
        this.status = status;
    }

    /** Returns the external system it belongs to. */
    public HospitalSystemType getSystem() {
        return system;
    }

    /** Returns the address of the real system. */
    public String getEndpoint() {
        return endpoint;
    }

    /** Returns the extra settings, in the order they were written. */
    public Map<String, String> getSettings() {
        return settings;
    }

    /** Tells whether the integration is enabled. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Returns the mode: simulated or real. */
    public IntegrationMode getMode() {
        return mode;
    }

    /** Returns the state of the integration. */
    public IntegrationStatus getStatus() {
        return status;
    }
}
