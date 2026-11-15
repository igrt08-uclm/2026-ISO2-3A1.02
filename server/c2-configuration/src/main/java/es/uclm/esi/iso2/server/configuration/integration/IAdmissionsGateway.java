package es.uclm.esi.iso2.server.configuration.integration;

/** Gateway to the admissions system (UC20). In this version it only tells whether the system answers. */
public interface IAdmissionsGateway {
    /**
     * Tells whether the admissions system answers. It never changes anything.
     *
     * @return {@code true} if the system answers
     */
    boolean ping();
}
