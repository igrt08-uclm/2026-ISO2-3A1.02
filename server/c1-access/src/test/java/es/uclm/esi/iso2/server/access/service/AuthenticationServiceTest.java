package es.uclm.esi.iso2.server.access.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import es.uclm.esi.iso2.common.access.contracts.AuthenticationException;
import es.uclm.esi.iso2.common.access.contracts.AuthenticationFailureReason;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import es.uclm.esi.iso2.server.access.service.RecordingAuditRecorder.AuditEntry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/**
 * Unit tests of {@link AuthenticationService}: the order of the checks, the failure reasons and the audit entries.
 * The SSO, the repository and the audit are hand-written test doubles, so no real test data is used.
 */
@DisplayName("AuthenticationService (unit)")
class AuthenticationServiceTest {
    private UserAccount ana;
    private UserAccount bob;
    private FakeUserAccountRepository repository;
    private RecordingAuditRecorder recorder;

    @BeforeEach
    void setUp() {
        ana = new UserAccount("ana", "Ana Test", "ana@test.test", true,
                List.of(new Role("PHYSICIAN", "Physician", "Test physician role")));
        bob = new UserAccount("bob", "Bob Test", "bob@test.test", false,
                List.of(new Role("PATIENT", "Patient", "Test patient role")));
        repository = new FakeUserAccountRepository(ana, bob);
        recorder = new RecordingAuditRecorder();
    }

    private AuthenticationService serviceWith(SpyAuthenticatorSystem sso) {
        return new AuthenticationService(sso, repository, recorder);
    }

    @Test
    @DisplayName("A valid active user gets their account and a LOGIN_OK entry")
    void loginWithValidActiveUserReturnsAccountAndAuditsLoginOk() throws AuthenticationException {
        // Arrange
        AuthenticationService service = serviceWith(new SpyAuthenticatorSystem((u, p) -> true));

        // Act
        UserAccount result = service.login("ana", "any");

        // Assert
        assertAll(
                () -> assertSame(ana, result),
                () -> assertEquals(List.of(new AuditEntry("ana", "LOGIN_OK", "authentication")),
                        recorder.entries()),
                () -> assertEquals(0, repository.saveCalls()));
    }

    // The SSO would accept anything, so UNKNOWN_USER can only come from the repository check, which runs first.
    @Test
    @DisplayName("An unknown user gives UNKNOWN_USER and never reaches the SSO")
    void loginWithUnknownUserThrowsUnknownUserWithoutCallingSso() {
        // Arrange
        SpyAuthenticatorSystem sso = new SpyAuthenticatorSystem((u, p) -> true);
        AuthenticationService service = serviceWith(sso);

        // Act
        AuthenticationException exception =
                assertThrows(AuthenticationException.class, () -> service.login("ghost", "any"));

        // Assert
        assertAll(
                () -> assertEquals(AuthenticationFailureReason.UNKNOWN_USER, exception.getReason()),
                () -> assertEquals(0, sso.calls()),
                () -> assertEquals(
                        List.of(new AuditEntry("ghost", "LOGIN_FAILED", "authentication:UNKNOWN_USER")),
                        recorder.entries()));
    }

    @Test
    @DisplayName("A password rejected by the SSO gives WRONG_PASSWORD")
    void loginWithWrongPasswordThrowsWrongPassword() {
        // Arrange
        AuthenticationService service = serviceWith(new SpyAuthenticatorSystem((u, p) -> false));

        // Act
        AuthenticationException exception =
                assertThrows(AuthenticationException.class, () -> service.login("ana", "bad"));

        // Assert
        assertAll(
                () -> assertEquals(AuthenticationFailureReason.WRONG_PASSWORD, exception.getReason()),
                () -> assertEquals(
                        List.of(new AuditEntry("ana", "LOGIN_FAILED", "authentication:WRONG_PASSWORD")),
                        recorder.entries()));
    }

    // D-022: an inactive account is rejected even when the SSO accepts the password.
    @Test
    @DisplayName("An inactive user with a valid password gives INACTIVE_USER")
    void loginWithInactiveUserAndValidPasswordThrowsInactiveUser() {
        // Arrange
        SpyAuthenticatorSystem sso = new SpyAuthenticatorSystem((u, p) -> true);
        AuthenticationService service = serviceWith(sso);

        // Act
        AuthenticationException exception =
                assertThrows(AuthenticationException.class, () -> service.login("bob", "any"));

        // Assert
        assertAll(
                () -> assertEquals(AuthenticationFailureReason.INACTIVE_USER, exception.getReason()),
                () -> assertEquals(1, sso.calls()),
                () -> assertEquals(
                        List.of(new AuditEntry("bob", "LOGIN_FAILED", "authentication:INACTIVE_USER")),
                        recorder.entries()));
    }

    // The SSO check comes before the active check, so a wrong password wins over an inactive account.
    @Test
    @DisplayName("An inactive user with a wrong password gives WRONG_PASSWORD")
    void loginWithInactiveUserAndWrongPasswordThrowsWrongPassword() {
        // Arrange
        AuthenticationService service = serviceWith(new SpyAuthenticatorSystem((u, p) -> false));

        // Act
        AuthenticationException exception =
                assertThrows(AuthenticationException.class, () -> service.login("bob", "bad"));

        // Assert
        assertEquals(AuthenticationFailureReason.WRONG_PASSWORD, exception.getReason());
    }

    @ParameterizedTest(name = "{0} with SSO answer {1} gives {2}")
    @CsvSource({
        "ghost, true,  UNKNOWN_USER",
        "ana,   false, WRONG_PASSWORD",
        "bob,   true,  INACTIVE_USER"
    })
    @DisplayName("Each failed login registers exactly one LOGIN_FAILED entry")
    void failedLoginRegistersExactlyOneLoginFailedEntry(String username, boolean ssoAnswer,
            AuthenticationFailureReason expectedReason) {
        // Arrange
        AuthenticationService service = serviceWith(new SpyAuthenticatorSystem((u, p) -> ssoAnswer));

        // Act
        AuthenticationException exception =
                assertThrows(AuthenticationException.class, () -> service.login(username, "any"));

        // Assert
        assertAll(
                () -> assertEquals(expectedReason, exception.getReason()),
                () -> assertEquals(
                        List.of(new AuditEntry(username, "LOGIN_FAILED", "authentication:" + expectedReason)),
                        recorder.entries()));
    }

    // Agreed rule: a null or empty username is not a special case, it is just an unknown user.
    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("A null or empty username gives UNKNOWN_USER and is audited as it is")
    void loginWithNullOrEmptyUsernameThrowsUnknownUser(String username) {
        // Arrange
        SpyAuthenticatorSystem sso = new SpyAuthenticatorSystem((u, p) -> true);
        AuthenticationService service = serviceWith(sso);

        // Act
        AuthenticationException exception =
                assertThrows(AuthenticationException.class, () -> service.login(username, "any"));

        // Assert
        assertAll(
                () -> assertEquals(AuthenticationFailureReason.UNKNOWN_USER, exception.getReason()),
                () -> assertEquals(0, sso.calls()),
                () -> assertEquals(
                        List.of(new AuditEntry(username, "LOGIN_FAILED", "authentication:UNKNOWN_USER")),
                        recorder.entries()));
    }

    @Test
    @DisplayName("The credentials reach the SSO unchanged and only once")
    void loginPassesCredentialsUnchangedToAuthenticatorSystem() throws AuthenticationException {
        // Arrange
        SpyAuthenticatorSystem sso = new SpyAuthenticatorSystem((u, p) -> true);
        AuthenticationService service = serviceWith(sso);

        // Act
        service.login("ana", "S3cret!");

        // Assert
        assertAll(
                () -> assertEquals("ana", sso.lastUsername()),
                () -> assertEquals("S3cret!", sso.lastPassword()),
                () -> assertEquals(1, sso.calls()));
    }
}
