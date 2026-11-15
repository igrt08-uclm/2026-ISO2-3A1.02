package es.uclm.esi.iso2.server.configuration.integration;

/** Simulates the electronic health record (A5): there is no real system, so it always answers. */
public class EhrGatewaySimulator implements IEhrGateway {
    /** The simulated system always answers. */
    @Override
    public boolean ping() {
        return true;
    }
}
