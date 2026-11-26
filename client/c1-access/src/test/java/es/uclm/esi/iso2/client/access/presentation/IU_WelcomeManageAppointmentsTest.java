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
 * Tests of the button <em>Manage appointments</em> of {@link IU_Welcome} (UC13, #130). It is only shown to a user with
 * the permission {@code MANAGE_APPOINTMENTS} when the action is given, after <em>Manage patients</em> and before
 * <em>Log out</em>. The panel is only built, never shown, so the tests also run without a display; its buttons are
 * read through the public {@code getComponents()} method. The cases are TC13C-33 to TC13C-37 of
 * {@code doc/testing/test-cases/UC13-client-test-design.md}.
 */
@DisplayName("IU_Welcome Manage appointments")
class IU_WelcomeManageAppointmentsTest {
    private static final Permission MANAGE_APPOINTMENTS =
            new Permission("MANAGE_APPOINTMENTS", "Manage appointments, waitlists and daily schedules");
    private static final Permission MANAGE_PATIENTS =
            new Permission("MANAGE_PATIENTS", "Search, register and modify patients");
    private static final Permission CONFIGURE_INTEGRATIONS =
            new Permission("CONFIGURE_INTEGRATIONS", "Configure the integrations with other hospital systems");
    private static final Permission CONFIGURE_SYSTEM =
            new Permission("CONFIGURE_SYSTEM", "Configure the parameters of the system");
    private static final Permission MANAGE_ROLES =
            new Permission("MANAGE_ROLES", "Assign roles to users and permissions to roles");
    private static final Permission CONSULT_AUDIT =
            new Permission("CONSULT_AUDIT", "Consult the audit log of the system activity");
    private static final List<Permission> ALL = List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONFIGURE_INTEGRATIONS,
            MANAGE_PATIENTS, MANAGE_APPOINTMENTS, CONSULT_AUDIT);
    private static final Runnable NOTHING = () -> { };

    private UserAccount admissions;
    private UserAccount admin;

    @BeforeEach
    void setUp() {
        admissions = new UserAccount("lmartin", "Lucía Martín Pérez", "lucia.martin@sescam.test", true,
                List.of(new Role("ADMISSIONS_STAFF", "Admissions staff",
                        "Registers patients and manages appointments")));
        admin = new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true,
                List.of(new Role("SYSTEM_ADMINISTRATOR", "System administrator", "Configures and manages the system")));
    }

    @Test
    @DisplayName("TC13C-33 Admissions staff with MANAGE_APPOINTMENTS sees Manage appointments (Alt+A), which runs")
    void tc13c33UserWithManageAppointmentsSeesTheButton() {
        // Arrange
        int[] opened = {0};
        IU_Welcome welcome = new IU_Welcome(admissions, NOTHING, NOTHING, List.of(MANAGE_PATIENTS, MANAGE_APPOINTMENTS),
                NOTHING, NOTHING, NOTHING, NOTHING, NOTHING, NOTHING, () -> opened[0]++);

        // Act
        JButton button = buttonWithText(welcome, "Manage appointments");
        button.doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Manage patients", "Manage appointments", "Log out"), buttonTexts(welcome)),
                () -> assertEquals(KeyEvent.VK_A, button.getMnemonic()),
                () -> assertEquals(1, opened[0]));
    }

    @Test
    @DisplayName("TC13C-34 With every permission and action, Manage appointments goes after Manage patients")
    void tc13c34OrderWithTheOtherButtons() {
        // Act
        IU_Welcome welcome = new IU_Welcome(admin, NOTHING, NOTHING, ALL, NOTHING, NOTHING, NOTHING, NOTHING,
                NOTHING, NOTHING, NOTHING);

        // Assert
        assertEquals(List.of("View audit log", "Manage roles", "Configure system", "Configure integrations",
                "Manage patients", "Manage appointments", "Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("TC13C-34 Manage appointments runs only its own action, and the others do not run it")
    void tc13c34ManageAppointmentsRunsOnlyItsOwnAction() {
        // Arrange
        int[] others = {0};
        int[] appointments = {0};
        IU_Welcome welcome = new IU_Welcome(admin, () -> others[0]++, () -> others[0]++, ALL, () -> others[0]++,
                () -> others[0]++, () -> others[0]++, () -> others[0]++, NOTHING, NOTHING,
                () -> appointments[0]++);

        // Act
        buttonWithText(welcome, "Manage appointments").doClick();
        int afterAppointments = others[0];
        buttonWithText(welcome, "Manage patients").doClick();
        buttonWithText(welcome, "Log out").doClick();

        // Assert
        assertAll(
                () -> assertEquals(1, appointments[0]),
                () -> assertEquals(0, afterAppointments),
                () -> assertEquals(2, others[0]));
    }

    @Test
    @DisplayName("TC13C-35 A user without MANAGE_APPOINTMENTS (the administrator, no permission) does not see it")
    void tc13c35UserWithoutManageAppointmentsDoesNotSeeTheButton() {
        // Act
        IU_Welcome administrator = new IU_Welcome(admin, NOTHING, NOTHING,
                List.of(MANAGE_ROLES, CONFIGURE_SYSTEM, CONFIGURE_INTEGRATIONS, CONSULT_AUDIT), NOTHING, NOTHING,
                NOTHING, NOTHING, NOTHING, NOTHING, NOTHING);
        IU_Welcome withoutPermissions = new IU_Welcome(admissions, NOTHING, NOTHING, List.of(), NOTHING, NOTHING,
                NOTHING, NOTHING, NOTHING, NOTHING, NOTHING);

        // Assert
        assertAll(
                () -> assertEquals(List.of("View audit log", "Manage roles", "Configure system",
                        "Configure integrations", "Log out"), buttonTexts(administrator)),
                () -> assertEquals(List.of("Log out"), buttonTexts(withoutPermissions)));
    }

    @Test
    @DisplayName("TC13C-36 Without the action, not even a user with MANAGE_APPOINTMENTS sees the button")
    void tc13c36WithoutTheActionNobodySeesTheButton() {
        // Act
        IU_Welcome welcome = new IU_Welcome(admissions, NOTHING, NOTHING, List.of(MANAGE_APPOINTMENTS), NOTHING,
                NOTHING, NOTHING, NOTHING, NOTHING, NOTHING, null);

        // Assert
        assertEquals(List.of("Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("TC13C-37 The constructor of UC07 does not show Manage appointments")
    void tc13c37OlderConstructorDoesNotShowTheButton() {
        // Act
        IU_Welcome ofUc07 = new IU_Welcome(admin, NOTHING, NOTHING, ALL, NOTHING, NOTHING, NOTHING, NOTHING, NOTHING,
                NOTHING);

        // Assert
        assertEquals(List.of("View audit log", "Manage roles", "Configure system", "Configure integrations",
                "Manage patients", "Log out"), buttonTexts(ofUc07));
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
