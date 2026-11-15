package es.uclm.esi.iso2.common.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests of {@link ConnectionTestResult} (UC20 server, #43, Q5 and Q7): it keeps the result of a connection test. */
@DisplayName("ConnectionTestResult")
class ConnectionTestResultTest {

    @Test
    @DisplayName("TC20S-55: a successful result keeps its response time and message")
    void successfulResult() {
        // Act
        ConnectionTestResult result = new ConnectionTestResult(true, 12, "Connection OK");

        // Assert
        assertAll(
                () -> assertTrue(result.isSuccess()),
                () -> assertEquals(12, result.getResponseTimeMs()),
                () -> assertEquals("Connection OK", result.getMessage()));
    }

    @Test
    @DisplayName("TC20S-56: a failed result keeps a response time of 0 and its reason")
    void failedResult() {
        // Act
        ConnectionTestResult result = new ConnectionTestResult(false, 0, "integration disabled");

        // Assert
        assertAll(
                () -> assertFalse(result.isSuccess()),
                () -> assertEquals(0, result.getResponseTimeMs()),
                () -> assertEquals("integration disabled", result.getMessage()));
    }
}
