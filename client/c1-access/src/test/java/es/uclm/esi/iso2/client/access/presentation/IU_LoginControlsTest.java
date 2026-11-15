package es.uclm.esi.iso2.client.access.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.access.contracts.AuthenticationFailureReason;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.awt.Component;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests of what the user sees and types in {@link IU_Login}. The controls are private, so they are found through the
 * public {@code getComponents()} method, as in IU_WelcomeTest. The panel is never shown, so no display is needed.
 */
@DisplayName("IU_Login controls")
class IU_LoginControlsTest {
    private static final String ERROR_ICON = "⚠ ";

    private UserAccount mgarcia;

    @BeforeEach
    void setUp() {
        mgarcia = new UserAccount("mgarcia", "Miguel García López", "miguel.garcia@sescam.test", true,
                List.of(new Role("PHYSICIAN", "Physician", "Requests and reviews tests and reports")));
    }

    // Error prevention: empty fields never reach the server.
    @Test
    @DisplayName("Log in is enabled only when both fields have text")
    void logInIsEnabledOnlyWhenBothFieldsHaveText() {
        IU_Login login = new IU_Login(FakeAuthenticationService.succeeding(mgarcia), user -> { });
        boolean atStart = logInButton(login).isEnabled();
        usernameField(login).setText("mgarcia");
        boolean withUsernameOnly = logInButton(login).isEnabled();
        passwordField(login).setText("Physician.2026");
        boolean withBoth = logInButton(login).isEnabled();

        assertAll(
                () -> assertFalse(atStart),
                () -> assertFalse(withUsernameOnly),
                () -> assertTrue(withBoth));
    }

    @Test
    @DisplayName("Clicking Log in sends the typed credentials")
    void clickingLogInSendsTheTypedCredentials() {
        FakeAuthenticationService service = FakeAuthenticationService.succeeding(mgarcia);
        IU_Login login = new IU_Login(service, user -> { });
        usernameField(login).setText("mgarcia");
        passwordField(login).setText("Physician.2026");

        logInButton(login).doClick();

        assertAll(
                () -> assertEquals(1, service.calls()),
                () -> assertEquals("mgarcia", service.lastUsername()),
                () -> assertEquals("Physician.2026", service.lastPassword()));
    }

    // Enter submits from the password field, but only when Log in is enabled.
    @Test
    @DisplayName("Enter logs in only when both fields have text")
    void enterLogsInOnlyWhenBothFieldsHaveText() {
        FakeAuthenticationService service = FakeAuthenticationService.succeeding(mgarcia);
        IU_Login login = new IU_Login(service, user -> { });
        usernameField(login).setText("mgarcia");

        passwordField(login).postActionEvent();
        int callsWithEmptyPassword = service.calls();
        passwordField(login).setText("Physician.2026");
        passwordField(login).postActionEvent();

        assertAll(
                () -> assertEquals(0, callsWithEmptyPassword),
                () -> assertEquals(1, service.calls()));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "UNKNOWN_USER,   This username does not exist. Check it and try again.",
        "WRONG_PASSWORD, The password is not correct. Try again.",
        "INACTIVE_USER,  This account is inactive. Contact the system administrator."
    })
    @DisplayName("Each failure reason shows its own message with the error icon")
    void eachReasonShowsItsOwnMessage(AuthenticationFailureReason reason, String expectedMessage) {
        IU_Login login = new IU_Login(FakeAuthenticationService.failing(reason), user -> { });

        login.enterCredentials("mgarcia", "Wrong.2026");

        assertEquals(ERROR_ICON + expectedMessage, errorText(login));
    }

    // Recognise and recover from errors: the username stays, the password is emptied, so Log in is disabled again.
    @Test
    @DisplayName("After a wrong password the username stays and the password is emptied")
    void afterWrongPasswordUsernameStaysAndPasswordIsEmptied() {
        IU_Login login = new IU_Login(FakeAuthenticationService.failing(AuthenticationFailureReason.WRONG_PASSWORD),
                user -> { });
        usernameField(login).setText("mgarcia");
        passwordField(login).setText("Wrong.2026");

        logInButton(login).doClick();

        assertAll(
                () -> assertEquals("mgarcia", usernameField(login).getText()),
                () -> assertEquals(0, passwordField(login).getPassword().length),
                () -> assertFalse(logInButton(login).isEnabled()),
                () -> assertTrue(errorText(login).startsWith(ERROR_ICON)));
    }

    // Focus cannot be checked without a display; the selected username is the visible part of the rule.
    @Test
    @DisplayName("After an unknown user the username is selected to be retyped")
    void afterUnknownUserTheUsernameIsSelected() {
        IU_Login login = new IU_Login(FakeAuthenticationService.failing(AuthenticationFailureReason.UNKNOWN_USER),
                user -> { });
        usernameField(login).setText("ghost");
        passwordField(login).setText("Admin.2026");

        logInButton(login).doClick();

        assertAll(
                () -> assertEquals("ghost", usernameField(login).getSelectedText()),
                () -> assertEquals(0, passwordField(login).getPassword().length));
    }

    @Test
    @DisplayName("Typing again hides the error message")
    void typingAgainHidesTheErrorMessage() {
        IU_Login login = new IU_Login(FakeAuthenticationService.failing(AuthenticationFailureReason.WRONG_PASSWORD),
                user -> { });
        login.enterCredentials("mgarcia", "Wrong.2026");

        passwordField(login).setText("P");

        assertEquals(" ", errorText(login));
    }

    private static JTextField usernameField(IU_Login login) {
        for (Component component : login.getComponents()) {
            // JPasswordField is a JTextField too, so it is skipped here.
            if (component instanceof JTextField field && !(field instanceof JPasswordField)) {
                return field;
            }
        }
        throw new AssertionError("No username field found");
    }

    private static JPasswordField passwordField(IU_Login login) {
        for (Component component : login.getComponents()) {
            if (component instanceof JPasswordField field) {
                return field;
            }
        }
        throw new AssertionError("No password field found");
    }

    private static JButton logInButton(IU_Login login) {
        for (Component component : login.getComponents()) {
            if (component instanceof JButton button && "Log in".equals(button.getText())) {
                return button;
            }
        }
        throw new AssertionError("No \"Log in\" button found");
    }

    // The error label is the one placed just before the Log in button (row 6 of the layout).
    private static String errorText(IU_Login login) {
        Component[] components = login.getComponents();
        for (int i = 1; i < components.length; i++) {
            if (components[i] instanceof JButton && components[i - 1] instanceof JLabel label) {
                return label.getText();
            }
        }
        throw new AssertionError("No error label found");
    }
}
