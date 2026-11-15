package es.uclm.esi.iso2.server.configuration.service;

import es.uclm.esi.iso2.common.access.contracts.IAuthenticatorSystem;
import es.uclm.esi.iso2.server.configuration.integration.IAdmissionsGateway;
import es.uclm.esi.iso2.server.configuration.integration.IEhrGateway;
import es.uclm.esi.iso2.server.configuration.integration.IRisPacsGateway;

/**
 * Test double of any of the four external systems of UC20 (SSO, EHR, admissions and RIS/PACS). It answers the ping
 * with a fixed value, can wait some milliseconds before answering, and counts how many times it was asked, so the
 * tests can check that the connection test goes through the gateway of the right system.
 */
class FakeHospitalSystem implements IAuthenticatorSystem, IEhrGateway, IAdmissionsGateway, IRisPacsGateway {
    private final boolean answer;
    private final long delayMs;
    private int pings;

    /**
     * Creates a system that answers at once.
     *
     * @param answer what {@code ping()} returns
     */
    FakeHospitalSystem(boolean answer) {
        this(answer, 0);
    }

    /**
     * Creates a system that waits before answering.
     *
     * @param answer what {@code ping()} returns
     * @param delayMs how long {@code ping()} waits, in milliseconds
     */
    FakeHospitalSystem(boolean answer, long delayMs) {
        this.answer = answer;
        this.delayMs = delayMs;
    }

    @Override
    public boolean validateCredentials(String username, String password) {
        throw new UnsupportedOperationException("Not used by UC20");
    }

    @Override
    public boolean ping() {
        pings++;
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return answer;
    }

    /** Returns how many times {@code ping()} was called. */
    int pings() {
        return pings;
    }
}
