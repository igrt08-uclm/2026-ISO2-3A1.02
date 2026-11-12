package es.uclm.esi.iso2.client.access.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the {@link IU_Welcome} screen. The panel is only built, never shown, so the tests also run without a
 * display. Its labels and button are read through the public {@code getComponents()} method.
 */
@DisplayName("IU_Welcome")
class IU_WelcomeTest {
    private static final Permission MANAGE_ROLES =
            new Permission("MANAGE_ROLES", "Assign roles to users and permissions to roles");

    private UserAccount mgarcia;

    @BeforeEach
    void setUp() {
        mgarcia = new UserAccount("mgarcia", "Miguel García López", "miguel.garcia@sescam.test", true, List.of(
                new Role("PHYSICIAN", "Physician", "Requests and reviews tests and reports"),
                new Role("PATIENT", "Patient", "Accesses their own appointments and results")));
    }

    @Test
    @DisplayName("Shows the full name and every role name, in order")
    void showsFullNameAndAllRoleNames() {
        // Arrange
        IU_Welcome welcome = new IU_Welcome(mgarcia, () -> { });

        // Act
        List<String> texts = labelTexts(welcome);

        // Assert
        assertAll(
                () -> assertTrue(texts.contains("Welcome, Miguel García López"), texts::toString),
                () -> assertTrue(texts.contains("You are logged in as: Physician, Patient"), texts::toString));
    }

    // doClick() fires the action listeners and works without a display.
    @Test
    @DisplayName("The Log out button runs onLogout")
    void logOutButtonRunsOnLogout() {
        // Arrange
        int[] logouts = {0};
        IU_Welcome welcome = new IU_Welcome(mgarcia, () -> logouts[0]++);

        // Act
        logOutButton(welcome).doClick();

        // Assert
        assertEquals(1, logouts[0]);
    }

    @Test
    @DisplayName("An administrator sees View audit log, which runs onViewAudit")
    void administratorSeesViewAuditLog() {
        // Arrange
        UserAccount admin = new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true,
                List.of(new Role("SYSTEM_ADMINISTRATOR", "System administrator", "Configures and manages the system")));
        int[] views = {0};
        IU_Welcome welcome = new IU_Welcome(admin, () -> { }, () -> views[0]++);

        // Act
        buttonWithText(welcome, "View audit log").doClick();

        // Assert
        assertEquals(1, views[0]);
    }

    @Test
    @DisplayName("A user who is not an administrator does not see View audit log")
    void otherUsersDoNotSeeViewAuditLog() {
        // Act
        IU_Welcome welcome = new IU_Welcome(mgarcia, () -> { }, () -> { });

        // Assert
        assertEquals(List.of("Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("Without the action, not even an administrator sees View audit log")
    void withoutTheActionNobodySeesViewAuditLog() {
        // Arrange
        UserAccount admin = new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true,
                List.of(new Role("SYSTEM_ADMINISTRATOR", "System administrator", "Configures and manages the system")));

        // Act
        IU_Welcome welcome = new IU_Welcome(admin, () -> { });

        // Assert
        assertEquals(List.of("Log out"), buttonTexts(welcome));
    }

    // The permissions come from the server, because the roles of the logged user do not carry them (UC18).
    @Test
    @DisplayName("A user with MANAGE_ROLES sees Manage roles, which runs onManageRoles")
    void userWithManageRolesSeesManageRoles() {
        // Arrange
        int[] opened = {0};
        IU_Welcome welcome = new IU_Welcome(mgarcia, () -> { }, () -> { }, List.of(MANAGE_ROLES), () -> opened[0]++);

        // Act
        buttonWithText(welcome, "Manage roles").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Manage roles", "Log out"), buttonTexts(welcome)),
                () -> assertEquals(1, opened[0]));
    }

    @Test
    @DisplayName("An administrator sees View audit log, Manage roles and Log out, in that order")
    void administratorSeesTheThreeButtonsInOrder() {
        // Act
        IU_Welcome welcome = new IU_Welcome(admin(), () -> { }, () -> { }, List.of(MANAGE_ROLES), () -> { });

        // Assert
        assertEquals(List.of("View audit log", "Manage roles", "Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("A user without MANAGE_ROLES does not see Manage roles")
    void userWithoutManageRolesDoesNotSeeIt() {
        // Act
        IU_Welcome welcome = new IU_Welcome(mgarcia, () -> { }, () -> { }, List.of(), () -> { });

        // Assert
        assertEquals(List.of("Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("Without the action, not even a user with MANAGE_ROLES sees Manage roles")
    void withoutTheActionNobodySeesManageRoles() {
        // Act
        IU_Welcome welcome = new IU_Welcome(mgarcia, () -> { }, () -> { }, List.of(MANAGE_ROLES), null);

        // Assert
        assertEquals(List.of("Log out"), buttonTexts(welcome));
    }

    // The constructors of UC22 and UC19 keep working as before.
    @Test
    @DisplayName("The constructors of UC22 and UC19 do not show Manage roles")
    void theOlderConstructorsDoNotShowManageRoles() {
        // Act
        IU_Welcome login = new IU_Welcome(admin(), () -> { });
        IU_Welcome audit = new IU_Welcome(admin(), () -> { }, () -> { });

        // Assert
        assertAll(
                () -> assertEquals(List.of("Log out"), buttonTexts(login)),
                () -> assertEquals(List.of("View audit log", "Log out"), buttonTexts(audit)));
    }

    private static UserAccount admin() {
        return new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true,
                List.of(new Role("SYSTEM_ADMINISTRATOR", "System administrator", "Configures and manages the system")));
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

    private static List<String> labelTexts(IU_Welcome welcome) {
        List<String> texts = new ArrayList<>();
        for (Component component : welcome.getComponents()) {
            if (component instanceof JLabel label) {
                texts.add(label.getText());
            }
        }
        return texts;
    }

    private static JButton logOutButton(IU_Welcome welcome) {
        for (Component component : welcome.getComponents()) {
            if (component instanceof JButton button && "Log out".equals(button.getText())) {
                return button;
            }
        }
        throw new AssertionError("No \"Log out\" button found");
    }
}
