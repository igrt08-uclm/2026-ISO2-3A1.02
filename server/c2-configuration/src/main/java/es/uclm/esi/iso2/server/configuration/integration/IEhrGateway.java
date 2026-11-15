package es.uclm.esi.iso2.server.configuration.integration;

/** Gateway to the electronic health record (UC20). In this version it only tells whether the system answers. */
public interface IEhrGateway {
    /**
     * Tells whether the electronic health record answers. It never changes anything.
     *
     * @return {@code true} if the system answers
     */
    boolean ping();
}
