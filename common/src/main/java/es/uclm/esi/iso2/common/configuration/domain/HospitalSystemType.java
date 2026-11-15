package es.uclm.esi.iso2.common.configuration.domain;

/** The external hospital systems whose integration is configured in UC20, in the order in which they are listed. */
public enum HospitalSystemType {
    /** The corporate single sign-on, used to log in (UC22). */
    SSO,
    /** The electronic health record. */
    EHR,
    /** The admissions system. */
    ADMISSIONS,
    /** The radiology information system and the image archive (RIS/PACS). */
    RIS_PACS
}
