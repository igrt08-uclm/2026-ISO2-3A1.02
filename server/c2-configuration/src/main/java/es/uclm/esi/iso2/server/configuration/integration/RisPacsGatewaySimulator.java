package es.uclm.esi.iso2.server.configuration.integration;

/** Simulates the RIS/PACS (A5): there is no real system, so it always answers. */
public class RisPacsGatewaySimulator implements IRisPacsGateway {
    /** The simulated system always answers. */
    @Override
    public boolean ping() {
        return true;
    }
}
