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
 * Tests of the button <em>View audit log</em> of {@link IU_Welcome} (UC19, patch 1.2.3, #172). It is only shown when
 * the action is given and the list of permissions passed to the screen contains {@code CONSULT_AUDIT}; the name of the
 * role of the user does not count. The panel is only built, never shown, so the tests also run without a display; its
 * buttons are read through the public {@code getComponents()} method.
 */
@DisplayName("IU_Welcome View audit log")
class IU_WelcomeViewAuditTest {
    private static final Permission CONSULT_AUDIT =
            new Permission("CONSULT_AUDIT", "Consult the audit log of the system activity");
    private static final Permission MANAGE_PATIENTS =
            new Permission("MANAGE_PATIENTS", "Search, register and modify patients");
    private static final Permission CONFIGURE_INTEGRATIONS =
            new Permission("CONFIGURE_INTEGRATIONS", "Configure the integrations with other hospital systems");
    private static final Permission CONFIGURE_SYSTEM =
            new Permission("CONFIGURE_SYSTEM", "Configure the parameters of the system");
    private static final Permission MANAGE_ROLES =
            new Permission("MANAGE_ROLES", "Assign roles to users and permissions to roles");
    private static final Runnable NOTHING = () -> { };

    private UserAccount admin;
    private UserAccount physician;

    @BeforeEach
    void setUp() {
        admin = new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true,
                List.of(new Role("SYSTEM_ADMINISTRATOR", "System administrator", "Configures and manages the system")));
        physician = new UserAccount("mgarcia", "Miguel García López", "miguel.garcia@sescam.test", true,
                List.of(new Role("PHYSICIAN", "Physician", "Requests and reviews tests and reports")));
    }

    @Test
    @DisplayName("TC19C-01 A user with CONSULT_AUDIT sees View audit log (Alt+V), which runs only its own action")
    void userWithConsultAuditSeesTheButton() {
        // Arrange
        int[] views = {0};
        int[] logouts = {0};
        IU_Welcome welcome = new IU_Welcome(admin, () -> logouts[0]++, () -> views[0]++, List.of(CONSULT_AUDIT),
                null);

        // Act
        JButton button = buttonWithText(welcome, "View audit log");
        button.doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("View audit log", "Log out"), buttonTexts(welcome)),
                () -> assertEquals(KeyEvent.VK_V, button.getMnemonic()),
                () -> assertEquals(1, views[0]),
                () -> assertEquals(0, logouts[0]));
    }

    @Test
    @DisplayName("TC19C-01 A user who is not an administrator but has CONSULT_AUDIT sees View audit log")
    void userWithoutAdministratorRoleButWithConsultAuditSeesTheButton() {
        // Act
        IU_Welcome welcome = new IU_Welcome(physician, NOTHING, NOTHING, List.of(CONSULT_AUDIT), null);

        // Assert
        assertEquals(List.of("View audit log", "Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("TC19C-02 A user with the role SYSTEM_ADMINISTRATOR but without CONSULT_AUDIT does not see it")
    void administratorWithoutConsultAuditDoesNotSeeTheButton() {
        // Act
        IU_Welcome withoutPermissions = new IU_Welcome(admin, NOTHING, NOTHING, List.of(), null);
        IU_Welcome withOtherPermissions = new IU_Welcome(admin, NOTHING, NOTHING,
                List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONFIGURE_INTEGRATIONS, MANAGE_PATIENTS), NOTHING, NOTHING,
                NOTHING, NOTHING);

        // Assert
        assertAll(
                () -> assertEquals(List.of("Log out"), buttonTexts(withoutPermissions)),
                () -> assertEquals(List.of("Manage roles", "Configure system", "Configure integrations",
                        "Manage patients", "Log out"), buttonTexts(withOtherPermissions)));
    }

    @Test
    @DisplayName("TC19C-02 A user with another permission only does not see View audit log")
    void userWithAnotherPermissionOnlyDoesNotSeeTheButton() {
        // Act
        IU_Welcome welcome = new IU_Welcome(physician, NOTHING, NOTHING, List.of(MANAGE_PATIENTS), null);

        // Assert
        assertEquals(List.of("Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("TC19C-03 Without the action, not even a user with CONSULT_AUDIT sees View audit log")
    void withoutTheActionNobodySeesTheButton() {
        // Act
        IU_Welcome welcome = new IU_Welcome(admin, NOTHING, null, List.of(CONSULT_AUDIT), null);

        // Assert
        assertEquals(List.of("Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("TC19C-04 The 2- and 3-argument constructors never show View audit log, not even to an administrator")
    void theTwoAndThreeArgumentConstructorsNeverShowTheButton() {
        // Act
        IU_Welcome withLogoutOnly = new IU_Welcome(admin, NOTHING);
        IU_Welcome withAudit = new IU_Welcome(admin, NOTHING, NOTHING);

        // Assert
        assertAll(
                () -> assertEquals(List.of("Log out"), buttonTexts(withLogoutOnly)),
                () -> assertEquals(List.of("Log out"), buttonTexts(withAudit)));
    }

    @Test
    @DisplayName("TC19C-05 With every permission and action, the order of the buttons does not change")
    void orderOfTheButtonsDoesNotChange() {
        // Arrange
        List<Permission> all = List.of(CONSULT_AUDIT, MANAGE_PATIENTS, CONFIGURE_INTEGRATIONS, CONFIGURE_SYSTEM,
                MANAGE_ROLES);

        // Act
        IU_Welcome welcome = new IU_Welcome(admin, NOTHING, NOTHING, all, NOTHING, NOTHING, NOTHING, NOTHING);

        // Assert
        assertEquals(List.of("View audit log", "Manage roles", "Configure system", "Configure integrations",
                "Manage patients", "Log out"), buttonTexts(welcome));
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
