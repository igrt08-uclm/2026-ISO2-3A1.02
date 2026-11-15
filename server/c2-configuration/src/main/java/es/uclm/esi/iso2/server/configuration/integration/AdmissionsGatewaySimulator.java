package es.uclm.esi.iso2.server.configuration.integration;

/** Simulates the admissions system (A5): there is no real system, so it always answers. */
public class AdmissionsGatewaySimulator implements IAdmissionsGateway {
    /** The simulated system always answers. */
    @Override
    public boolean ping() {
        return true;
    }
}
