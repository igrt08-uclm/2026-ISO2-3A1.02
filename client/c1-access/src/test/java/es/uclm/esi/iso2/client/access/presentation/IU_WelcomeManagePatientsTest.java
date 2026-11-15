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
 * Tests of the button <em>Manage patients</em> of {@link IU_Welcome} (UC12, Q9 of the plan). It is only shown to a
 * user with the permission {@code MANAGE_PATIENTS} when the action is given, after <em>Configure integrations</em> and
 * before <em>Log out</em>. The panel is only built, never shown, so the tests also run without a display; its buttons
 * are read through the public {@code getComponents()} method.
 */
@DisplayName("IU_Welcome Manage patients")
class IU_WelcomeManagePatientsTest {
    private static final Permission MANAGE_PATIENTS =
            new Permission("MANAGE_PATIENTS", "Search, register and modify patients");
    private static final Permission CONFIGURE_INTEGRATIONS =
            new Permission("CONFIGURE_INTEGRATIONS", "Configure the integrations with other hospital systems");
    private static final Permission CONFIGURE_SYSTEM =
            new Permission("CONFIGURE_SYSTEM", "Configure the parameters of the system");
    private static final Permission MANAGE_ROLES =
            new Permission("MANAGE_ROLES", "Assign roles to users and permissions to roles");
    private static final List<Permission> ALL =
            List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONFIGURE_INTEGRATIONS, MANAGE_PATIENTS);
    private static final Runnable NOTHING = () -> { };

    private UserAccount admissions;
    private UserAccount admin;

    @BeforeEach
    void setUp() {
        admissions = new UserAccount("lmartin", "Lucía Martín Pérez", "lucia.martin@sescam.test", true,
                List.of(new Role("ADMISSIONS_STAFF", "Admissions staff", "Registers patients and manages appointments")));
        admin = new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true,
                List.of(new Role("SYSTEM_ADMINISTRATOR", "System administrator", "Configures and manages the system")));
    }

    @Test
    @DisplayName("TC12C-33 A user with MANAGE_PATIENTS sees Manage patients (Alt+P), which runs its action")
    void userWithManagePatientsSeesTheButton() {
        // Arrange
        int[] opened = {0};
        IU_Welcome welcome = new IU_Welcome(admissions, NOTHING, NOTHING, List.of(MANAGE_PATIENTS), NOTHING, NOTHING,
                NOTHING, () -> opened[0]++);

        // Act
        JButton button = buttonWithText(welcome, "Manage patients");
        button.doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Manage patients", "Log out"), buttonTexts(welcome)),
                () -> assertEquals(KeyEvent.VK_P, button.getMnemonic()),
                () -> assertEquals(1, opened[0]));
    }

    @Test
    @DisplayName("TC12C-34 With every permission and action, Manage patients goes after Configure integrations")
    void orderWithTheOtherButtons() {
        // Act
        IU_Welcome welcome = new IU_Welcome(admin, NOTHING, NOTHING, ALL, NOTHING, NOTHING, NOTHING, NOTHING);

        // Assert
        assertEquals(List.of("View audit log", "Manage roles", "Configure system", "Configure integrations",
                "Manage patients", "Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("TC12C-34 Manage patients runs only its own action, and the others do not run it")
    void managePatientsRunsOnlyItsOwnAction() {
        // Arrange
        int[] others = {0};
        int[] patients = {0};
        IU_Welcome welcome = new IU_Welcome(admin, () -> others[0]++, () -> others[0]++, ALL, () -> others[0]++,
                () -> others[0]++, () -> others[0]++, () -> patients[0]++);

        // Act
        buttonWithText(welcome, "Manage patients").doClick();
        int afterPatients = others[0];
        buttonWithText(welcome, "Configure integrations").doClick();
        buttonWithText(welcome, "Log out").doClick();

        // Assert
        assertAll(
                () -> assertEquals(1, patients[0]),
                () -> assertEquals(0, afterPatients),
                () -> assertEquals(2, others[0]));
    }

    @Test
    @DisplayName("TC12C-35 A user without MANAGE_PATIENTS (the administrator) does not see Manage patients")
    void userWithoutManagePatientsDoesNotSeeTheButton() {
        // Act
        IU_Welcome administrator = new IU_Welcome(admin, NOTHING, NOTHING,
                List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONFIGURE_INTEGRATIONS), NOTHING, NOTHING, NOTHING, NOTHING);
        IU_Welcome withoutPermissions =
                new IU_Welcome(admissions, NOTHING, NOTHING, List.of(), NOTHING, NOTHING, NOTHING, NOTHING);

        // Assert
        assertAll(
                () -> assertEquals(List.of("View audit log", "Manage roles", "Configure system",
                        "Configure integrations", "Log out"), buttonTexts(administrator)),
                () -> assertEquals(List.of("Log out"), buttonTexts(withoutPermissions)));
    }

    @Test
    @DisplayName("TC12C-35 Without the action, not even a user with MANAGE_PATIENTS sees the button")
    void withoutTheActionNobodySeesTheButton() {
        // Act
        IU_Welcome welcome =
                new IU_Welcome(admissions, NOTHING, NOTHING, List.of(MANAGE_PATIENTS), NOTHING, NOTHING, NOTHING, null);

        // Assert
        assertEquals(List.of("Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("TC12C-35 The older constructors do not show Manage patients")
    void olderConstructorsDoNotShowTheButton() {
        // Act
        IU_Welcome ofUc20 = new IU_Welcome(admin, NOTHING, NOTHING, ALL, NOTHING, NOTHING, NOTHING);
        IU_Welcome ofUc17 = new IU_Welcome(admin, NOTHING, NOTHING, ALL, NOTHING, NOTHING);
        IU_Welcome ofUc18 = new IU_Welcome(admin, NOTHING, NOTHING, ALL, NOTHING);
        IU_Welcome withAudit = new IU_Welcome(admissions, NOTHING, NOTHING);
        IU_Welcome withLogoutOnly = new IU_Welcome(admissions, NOTHING);

        // Assert
        assertAll(
                () -> assertEquals(List.of("View audit log", "Manage roles", "Configure system",
                        "Configure integrations", "Log out"), buttonTexts(ofUc20)),
                () -> assertEquals(List.of("View audit log", "Manage roles", "Configure system", "Log out"),
                        buttonTexts(ofUc17)),
                () -> assertEquals(List.of("View audit log", "Manage roles", "Log out"), buttonTexts(ofUc18)),
                () -> assertEquals(List.of("Log out"), buttonTexts(withAudit)),
                () -> assertEquals(List.of("Log out"), buttonTexts(withLogoutOnly)));
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
