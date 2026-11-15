package es.uclm.esi.iso2.client.access.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the button <em>Configure integrations</em> of {@link IU_Welcome} (UC20, Q13 of the plan). It is only shown
 * to a user with the permission {@code CONFIGURE_INTEGRATIONS} when the action is given, after <em>Configure
 * system</em> and before <em>Log out</em>. The panel is only built, never shown, so the tests also run without a
 * display; its buttons are read through the public {@code getComponents()} method.
 */
@DisplayName("IU_Welcome Configure integrations")
class IU_WelcomeConfigureIntegrationsTest {
    private static final Permission CONFIGURE_INTEGRATIONS =
            new Permission("CONFIGURE_INTEGRATIONS", "Configure the integrations with other hospital systems");
    private static final Permission CONFIGURE_SYSTEM =
            new Permission("CONFIGURE_SYSTEM", "Configure the parameters of the system");
    private static final Permission MANAGE_ROLES =
            new Permission("MANAGE_ROLES", "Assign roles to users and permissions to roles");
    private static final Runnable NOTHING = () -> { };

    private UserAccount admin;

    @BeforeEach
    void setUp() {
        admin = new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true,
                List.of(new Role("SYSTEM_ADMINISTRATOR", "System administrator", "Configures and manages the system")));
    }

    @Test
    @DisplayName("TC20C-07 A user with CONFIGURE_INTEGRATIONS sees Configure integrations (Alt+I), which runs its action")
    void userWithConfigureIntegrationsSeesTheButton() {
        // Arrange
        int[] opened = {0};
        IU_Welcome welcome = new IU_Welcome(admin, NOTHING, null, List.of(CONFIGURE_INTEGRATIONS), null, null,
                () -> opened[0]++);

        // Act
        JButton button = buttonWithText(welcome, "Configure integrations");
        button.doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Configure integrations", "Log out"), buttonTexts(welcome)),
                () -> assertEquals(KeyEvent.VK_I, button.getMnemonic()),
                () -> assertEquals(1, opened[0]));
    }

    @Test
    @DisplayName("TC20C-08 An administrator with every permission sees the five buttons, integrations after system")
    void administratorSeesTheFiveButtonsInOrder() {
        // Act
        IU_Welcome welcome = new IU_Welcome(admin, NOTHING, NOTHING,
                List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONFIGURE_INTEGRATIONS), NOTHING, NOTHING, NOTHING);

        // Assert
        assertEquals(List.of("View audit log", "Manage roles", "Configure system", "Configure integrations",
                "Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("TC20C-08 Configure integrations runs only its own action, and the others do not run it")
    void configureIntegrationsRunsOnlyItsOwnAction() {
        // Arrange
        int[] others = {0};
        int[] integrations = {0};
        IU_Welcome welcome = new IU_Welcome(admin, () -> others[0]++, () -> others[0]++,
                List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONFIGURE_INTEGRATIONS), () -> others[0]++,
                () -> others[0]++, () -> integrations[0]++);

        // Act
        buttonWithText(welcome, "Configure integrations").doClick();
        int afterIntegrations = others[0];
        buttonWithText(welcome, "Configure system").doClick();

        // Assert
        assertAll(
                () -> assertEquals(1, integrations[0]),
                () -> assertEquals(0, afterIntegrations),
                () -> assertEquals(1, others[0]));
    }

    @Test
    @DisplayName("TC20C-09 A user without CONFIGURE_INTEGRATIONS does not see Configure integrations")
    void userWithoutConfigureIntegrationsDoesNotSeeTheButton() {
        // Act
        IU_Welcome withOtherPermissions = new IU_Welcome(admin, NOTHING, null, List.of(MANAGE_ROLES, CONFIGURE_SYSTEM),
                NOTHING, NOTHING, NOTHING);
        IU_Welcome withoutPermissions = new IU_Welcome(admin, NOTHING, null, List.of(), NOTHING, NOTHING, NOTHING);

        // Assert
        assertAll(
                () -> assertEquals(List.of("Manage roles", "Configure system", "Log out"),
                        buttonTexts(withOtherPermissions)),
                () -> assertEquals(List.of("Log out"), buttonTexts(withoutPermissions)));
    }

    @Test
    @DisplayName("TC20C-09 Without the action, not even a user with CONFIGURE_INTEGRATIONS sees the button")
    void withoutTheActionNobodySeesTheButton() {
        // Act
        IU_Welcome welcome = new IU_Welcome(admin, NOTHING, null, List.of(CONFIGURE_INTEGRATIONS), null, null, null);

        // Assert
        assertEquals(List.of("Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("TC20C-09 The older constructors do not show Configure integrations")
    void olderConstructorsDoNotShowTheButton() {
        // Arrange
        List<Permission> all = List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONFIGURE_INTEGRATIONS);

        // Act
        IU_Welcome ofUc17 = new IU_Welcome(admin, NOTHING, NOTHING, all, NOTHING, NOTHING);
        IU_Welcome ofUc18 = new IU_Welcome(admin, NOTHING, NOTHING, all, NOTHING);

        // Assert
        assertAll(
                () -> assertEquals(List.of("View audit log", "Manage roles", "Configure system", "Log out"),
                        buttonTexts(ofUc17)),
                () -> assertEquals(List.of("View audit log", "Manage roles", "Log out"), buttonTexts(ofUc18)));
    }

    private static List<String> buttonTexts(IU_Welcome welcome) {
        List<String> texts = new ArrayList<>();
        for (Component component : welcome.getComponents()) {
            if (component instanceof JButton button) {
                texts.add(button.getText());
            }
        }
        return texts;
    }

    private static JButton buttonWithText(IU_Welcome welcome, String text) {
        for (Component component : welcome.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
        }
        throw new AssertionError("No \"" + text + "\" button found");
    }
}
