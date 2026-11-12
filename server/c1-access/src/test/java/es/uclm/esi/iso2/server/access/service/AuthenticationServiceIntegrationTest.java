package es.uclm.esi.iso2.server.access.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.access.contracts.AuthenticationException;
import es.uclm.esi.iso2.common.access.contracts.AuthenticationFailureReason;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import es.uclm.esi.iso2.server.access.integration.AuthenticatorSystemSimulator;
import es.uclm.esi.iso2.server.access.persistence.InMemoryUserAccountRepository;
import es.uclm.esi.iso2.server.access.service.RecordingAuditRecorder.AuditEntry;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Integration tests of {@link AuthenticationService} with the real SSO simulator and the real in-memory repository,
 * using the test users agreed in #13. Only the audit is replaced, by a recorder whose entries can be checked.
 */
@DisplayName("AuthenticationService with the real simulator and repository")
class AuthenticationServiceIntegrationTest {
    private SpyAuthenticatorSystem sso;
    private RecordingAuditRecorder recorder;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        sso = new SpyAuthenticatorSystem(new AuthenticatorSystemSimulator());
        recorder = new RecordingAuditRecorder();
        service = new AuthenticationService(sso, new InMemoryUserAccountRepository(), recorder);
    }

    @ParameterizedTest(name = "{0} logs in with roles {2}")
    @CsvSource({
        "admin,      Admin.2026,      SYSTEM_ADMINISTRATOR",
        "mgarcia,    Physician.2026,  PHYSICIAN;PATIENT",
        "lmartin,    Admissions.2026, ADMISSIONS_STAFF",
        "jfernandez, Patient.2026,    PATIENT"
    })
    @DisplayName("Each active test user logs in with their roles")
    void activeTestUsersLogInWithTheirRoles(String username, String password, String expectedRoles)
            throws AuthenticationException {
        // Act
        UserAccount user = service.login(username, password);

        // Assert
        List<String> roleIds = user.getRoles().stream().map(Role::getId).toList();
        assertAll(
                () -> assertEquals(username, user.getUsername()),
                () -> assertTrue(user.isActive()),
                () -> assertEquals(Arrays.asList(expectedRoles.split(";")), roleIds),
                () -> assertEquals(List.of(new AuditEntry(username, "LOGIN_OK", "authentication")),
                        recorder.entries()));
    }

    // The two pnavarro rows check the order of the checks: a wrong password wins over an inactive account.
    @ParameterizedTest(name = "\"{0}\" / \"{1}\" gives {2}")
    @CsvSource({
        "pnavarro,   Inactive.2026, INACTIVE_USER",
        "pnavarro,   Wrong.2026,    WRONG_PASSWORD",
        "mgarcia,    Admin.2026,    WRONG_PASSWORD",
        "jfernandez, '',            WRONG_PASSWORD",
        "ghost,      Admin.2026,    UNKNOWN_USER",
        "'',         Admin.2026,    UNKNOWN_USER"
    })
    @DisplayName("Each failed login gives its reason and one audit entry")
    void failedLoginsGiveTheirReasonAndOneAuditEntry(String username, String password,
            AuthenticationFailureReason expectedReason) {
        // Act
        AuthenticationException exception =
                assertThrows(AuthenticationException.class, () -> service.login(username, password));

        // Assert
        assertAll(
                () -> assertEquals(expectedReason, exception.getReason()),
                () -> assertEquals(
                        List.of(new AuditEntry(username, "LOGIN_FAILED", "authentication:" + expectedReason)),
                        recorder.entries()));
    }

    @Test
    @DisplayName("A null password gives WRONG_PASSWORD")
    void loginWithNullPasswordThrowsWrongPassword() {
        // Act
        AuthenticationException exception =
                assertThrows(AuthenticationException.class, () -> service.login("admin", null));

        // Assert
        assertAll(
                () -> assertEquals(AuthenticationFailureReason.WRONG_PASSWORD, exception.getReason()),
                () -> assertEquals(
                        List.of(new AuditEntry("admin", "LOGIN_FAILED", "authentication:WRONG_PASSWORD")),
                        recorder.entries()));
    }

    @Test
    @DisplayName("A null username gives UNKNOWN_USER and is audited with a null author")
    void loginWithNullUsernameThrowsUnknownUser() {
        // Act
        AuthenticationException exception =
                assertThrows(AuthenticationException.class, () -> service.login(null, "Admin.2026"));

        // Assert
        assertAll(
                () -> assertEquals(AuthenticationFailureReason.UNKNOWN_USER, exception.getReason()),
                () -> assertEquals(
                        List.of(new AuditEntry(null, "LOGIN_FAILED", "authentication:UNKNOWN_USER")),
                        recorder.entries()));
    }

    // Admin.2026 is a real password, so only the order of the checks keeps the SSO from being called.
    @Test
    @DisplayName("An unknown user never reaches the SSO")
    void unknownUserNeverReachesTheSso() {
        // Act
        AuthenticationException exception =
                assertThrows(AuthenticationException.class, () -> service.login("ghost", "Admin.2026"));

        // Assert
        assertAll(
                () -> assertEquals(AuthenticationFailureReason.UNKNOWN_USER, exception.getReason()),
                () -> assertEquals(0, sso.calls()));
    }

    // A failed attempt must not leave any state behind that changes the next attempt.
    @Test
    @DisplayName("Each attempt in a sequence is audited once and in order")
    void eachAttemptInASequenceIsAuditedOnce() throws AuthenticationException {
        // Act
        assertThrows(AuthenticationException.class, () -> service.login("admin", "Wrong"));
        service.login("admin", "Admin.2026");
        assertThrows(AuthenticationException.class, () -> service.login("pnavarro", "Inactive.2026"));

        // Assert
        assertAll(
                () -> assertEquals(List.of(
                        new AuditEntry("admin", "LOGIN_FAILED", "authentication:WRONG_PASSWORD"),
                        new AuditEntry("admin", "LOGIN_OK", "authentication"),
                        new AuditEntry("pnavarro", "LOGIN_FAILED", "authentication:INACTIVE_USER")),
                        recorder.entries()),
                () -> assertEquals(3, sso.calls()));
    }
}
