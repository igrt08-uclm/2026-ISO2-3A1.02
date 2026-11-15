package es.uclm.esi.iso2.common.configuration.domain;

/** How an integration works: against a simulator (the default) or against the real external system (UC20). */
public enum IntegrationMode {
    /** The system answers from a simulator, without contacting the real external system. */
    SIMULATED,
    /** The system contacts the real external system. There is no real adapter in this version. */
    REAL
}
