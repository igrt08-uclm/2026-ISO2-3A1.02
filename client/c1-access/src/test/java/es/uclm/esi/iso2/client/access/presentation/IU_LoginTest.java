package es.uclm.esi.iso2.client.access.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.access.contracts.AuthenticationFailureReason;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests of the {@link IU_Login} screen with a fake authentication service: what reaches the service and when
 * {@code onSuccess} is called. The panel is only built, never shown, so the tests also run without a display. What
 * the user sees on the screen is tested in {@link IU_LoginControlsTest}.
 */
@DisplayName("IU_Login")
class IU_LoginTest {
    private UserAccount mgarcia;
    private List<UserAccount> received;

    @BeforeEach
    void setUp() {
        mgarcia = new UserAccount("mgarcia", "Miguel García López", "miguel.garcia@sescam.test", true, List.of(
                new Role("PHYSICIAN", "Physician", "Requests and reviews tests and reports"),
                new Role("PATIENT", "Patient", "Accesses their own appointments and results")));
        received = new ArrayList<>();
    }

    @Test
    @DisplayName("Valid credentials call onSuccess with the user")
    void enterCredentialsWithValidCredentialsCallsOnSuccessWithTheUser() {
        // Arrange
        FakeAuthenticationService service = FakeAuthenticationService.succeeding(mgarcia);
        IU_Login login = new IU_Login(service, received::add);

        // Act
        login.enterCredentials("mgarcia", "Physician.2026");

        // Assert
        assertAll(
                () -> assertEquals(1, received.size()),
                () -> assertSame(mgarcia, received.get(0)),
                () -> assertEquals("mgarcia", service.lastUsername()),
                () -> assertEquals("Physician.2026", service.lastPassword()));
    }

    // The checked AuthenticationException is handled inside the panel and must not reach the caller.
    @ParameterizedTest(name = "{0}")
    @EnumSource(AuthenticationFailureReason.class)
    @DisplayName("A failed login does not call onSuccess")
    void enterCredentialsWithFailureDoesNotCallOnSuccess(AuthenticationFailureReason reason) {
        // Arrange
        FakeAuthenticationService service = FakeAuthenticationService.failing(reason);
        IU_Login login = new IU_Login(service, received::add);

        // Act
        assertDoesNotThrow(() -> login.enterCredentials("x", "y"));

        // Assert
        assertAll(
                () -> assertTrue(received.isEmpty()),
                () -> assertEquals(1, service.calls()));
    }

    @Test
    @DisplayName("After a failed attempt, a valid attempt calls onSuccess once")
    void failedAttemptThenValidAttemptCallsOnSuccessOnce() {
        // Arrange
        FakeAuthenticationService service = FakeAuthenticationService.succeeding(mgarcia)
                .failNextWith(AuthenticationFailureReason.WRONG_PASSWORD);
        IU_Login login = new IU_Login(service, received::add);

        // Act
        login.enterCredentials("mgarcia", "Wrong.2026");
        login.enterCredentials("mgarcia", "Physician.2026");

        // Assert
        assertAll(
                () -> assertEquals(1, received.size()),
                () -> assertSame(mgarcia, received.get(0)),
                () -> assertEquals(2, service.calls()));
    }
}
