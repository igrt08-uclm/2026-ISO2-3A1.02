package es.uclm.esi.iso2.common.configuration.domain;

/** The state of an integration, which the server sets from the configuration (UC20). */
public enum IntegrationStatus {
    /** The integration is enabled. */
    ACTIVE,
    /** The integration is disabled. */
    INACTIVE,
    /** The last connection failed. It is never stored in this version, because the connection test changes nothing. */
    ERROR
}
