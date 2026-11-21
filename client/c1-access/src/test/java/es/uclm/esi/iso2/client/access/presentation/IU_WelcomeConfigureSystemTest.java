package es.uclm.esi.iso2.client.access.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the button <em>Configure system</em> of {@link IU_Welcome} (UC17). It is only shown to a user with the
 * permission {@code CONFIGURE_SYSTEM} when the action is given. The panel is only built, never shown, so the tests
 * also run without a display; its buttons are read through the public {@code getComponents()} method.
 */
@DisplayName("IU_Welcome Configure system")
class IU_WelcomeConfigureSystemTest {
    private static final Permission CONFIGURE_SYSTEM =
            new Permission("CONFIGURE_SYSTEM", "Configure the parameters of the system");
    private static final Permission CONSULT_AUDIT =
            new Permission("CONSULT_AUDIT", "Consult the audit log of the system activity");
    private static final Permission MANAGE_ROLES =
            new Permission("MANAGE_ROLES", "Assign roles to users and permissions to roles");

    private UserAccount admin;

    @BeforeEach
    void setUp() {
        admin = new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true,
                List.of(new Role("SYSTEM_ADMINISTRATOR", "System administrator", "Configures and manages the system")));
    }

    @Test
    @DisplayName("TC17C-02 A user with CONFIGURE_SYSTEM sees Configure system, which runs its action")
    void userWithConfigureSystemSeesTheButton() {
        // Arrange
        int[] opened = {0};
        IU_Welcome welcome = new IU_Welcome(admin, () -> { }, null, List.of(CONFIGURE_SYSTEM), null,
                () -> opened[0]++);

        // Act
        buttonWithText(welcome, "Configure system").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Configure system", "Log out"), buttonTexts(welcome)),
                () -> assertEquals(1, opened[0]));
    }

    @Test
    @DisplayName("TC17C-02 An administrator with every permission sees the four buttons in order")
    void administratorSeesTheFourButtonsInOrder() {
        // Act
        IU_Welcome welcome = new IU_Welcome(admin, () -> { }, () -> { },
                List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONSULT_AUDIT), () -> { }, () -> { });

        // Assert
        assertEquals(List.of("View audit log", "Manage roles", "Configure system", "Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("TC17C-02 Configure system runs only its own action")
    void configureSystemRunsOnlyItsOwnAction() {
        // Arrange
        int[] others = {0};
        int[] configured = {0};
        IU_Welcome welcome = new IU_Welcome(admin, () -> others[0]++, () -> others[0]++,
                List.of(MANAGE_ROLES, CONFIGURE_SYSTEM), () -> others[0]++, () -> configured[0]++);

        // Act
        buttonWithText(welcome, "Configure system").doClick();

        // Assert
        assertAll(
                () -> assertEquals(1, configured[0]),
                () -> assertEquals(0, others[0]));
    }

    @Test
    @DisplayName("TC17C-03 A user without CONFIGURE_SYSTEM does not see Configure system")
    void userWithoutConfigureSystemDoesNotSeeTheButton() {
        // Act
        IU_Welcome withOtherPermission = new IU_Welcome(admin, () -> { }, null, List.of(MANAGE_ROLES), null,
                () -> { });
        IU_Welcome withoutPermissions = new IU_Welcome(admin, () -> { }, null, List.of(), null, () -> { });

        // Assert
        assertAll(
                () -> assertEquals(List.of("Log out"), buttonTexts(withOtherPermission)),
                () -> assertEquals(List.of("Log out"), buttonTexts(withoutPermissions)));
    }

    @Test
    @DisplayName("TC17C-03 Without the action, not even a user with CONFIGURE_SYSTEM sees Configure system")
    void withoutTheActionNobodySeesTheButton() {
        // Act
        IU_Welcome welcome = new IU_Welcome(admin, () -> { }, null, List.of(CONFIGURE_SYSTEM), null, null);

        // Assert
        assertEquals(List.of("Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("TC17C-03 The constructor of UC18 does not show Configure system")
    void theConstructorOfUc18DoesNotShowTheButton() {
        // Act
        IU_Welcome welcome = new IU_Welcome(admin, () -> { }, () -> { },
                List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONSULT_AUDIT), () -> { });

        // Assert
        assertEquals(List.of("View audit log", "Manage roles", "Log out"), buttonTexts(welcome));
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
