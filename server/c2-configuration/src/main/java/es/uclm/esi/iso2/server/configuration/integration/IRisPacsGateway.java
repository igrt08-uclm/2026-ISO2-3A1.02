package es.uclm.esi.iso2.server.configuration.integration;

/** Gateway to the radiology information system and the image archive, RIS/PACS (UC20). It only tells if it answers. */
public interface IRisPacsGateway {
    /**
     * Tells whether the RIS/PACS answers. It never changes anything.
     *
     * @return {@code true} if the system answers
     */
    boolean ping();
}
