package es.uclm.esi.iso2.server.configuration.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the simulators of the electronic health record, the admissions system and the RIS/PACS (UC20 server, #43,
 * Q6). There is no real system in this version (A5), so every simulator always answers the ping.
 */
@DisplayName("Gateway simulators")
class GatewaySimulatorsTest {

    @Test
    @DisplayName("TC20S-46: the EHR, admissions and RIS/PACS simulators always answer the ping")
    void simulatorsAlwaysAnswer() {
        // Arrange
        IEhrGateway ehr = new EhrGatewaySimulator();
        IAdmissionsGateway admissions = new AdmissionsGatewaySimulator();
        IRisPacsGateway risPacs = new RisPacsGatewaySimulator();

        // Act and Assert
        assertAll(
                () -> assertTrue(ehr.ping()),
                () -> assertTrue(admissions.ping()),
                () -> assertTrue(risPacs.ping()));
    }

    @Test
    @DisplayName("TC20S-47: a simulator answers every ping, not only the first one")
    void simulatorsAnswerEveryTime() {
        // Arrange
        IEhrGateway ehr = new EhrGatewaySimulator();
        IAdmissionsGateway admissions = new AdmissionsGatewaySimulator();
        IRisPacsGateway risPacs = new RisPacsGatewaySimulator();

        // Act
        boolean answered = true;
        for (int i = 0; i < 3; i++) {
            answered &= ehr.ping() && admissions.ping() && risPacs.ping();
        }

        // Assert
        assertTrue(answered);
    }
}
