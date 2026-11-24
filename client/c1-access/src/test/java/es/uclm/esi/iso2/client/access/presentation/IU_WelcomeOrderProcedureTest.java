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
 * Tests of the button <em>Order procedure</em> of {@link IU_Welcome} (UC06, Q9 of {@code doc/plans/UC06-client.md}).
 * It is only shown to a user with the permission {@code ORDER_PROCEDURES} when the action is given, after
 * <em>Manage patients</em> and before <em>Log out</em>, with the key Alt+O. The panel is only built, never shown, so
 * the tests also run without a display; its buttons are read through the public {@code getComponents()} method.
 */
@DisplayName("IU_Welcome Order procedure")
class IU_WelcomeOrderProcedureTest {
    private static final Permission ORDER_PROCEDURES =
            new Permission("ORDER_PROCEDURES", "Order diagnostic and periodic follow-up procedures");
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
            MANAGE_PATIENTS, CONSULT_AUDIT, ORDER_PROCEDURES);
    private static final Runnable NOTHING = () -> { };

    private UserAccount physician;
    private UserAccount admissions;

    @BeforeEach
    void setUp() {
        physician = new UserAccount("mgarcia", "Miguel García López", "miguel.garcia@sescam.test", true,
                List.of(new Role("PHYSICIAN", "Physician", "Requests and reviews tests and reports")));
        admissions = new UserAccount("lmartin", "Lucía Martín Pérez", "lucia.martin@sescam.test", true,
                List.of(new Role("ADMISSIONS_STAFF", "Admissions staff",
                        "Registers patients and manages appointments")));
    }

    @Test
    @DisplayName("TC06C-38 A user with ORDER_PROCEDURES sees Order procedure (Alt+O), which runs its action")
    void tc06c38UserWithOrderProceduresSeesTheButton() {
        // Arrange
        int[] opened = {0};
        IU_Welcome welcome = new IU_Welcome(physician, NOTHING, NOTHING, List.of(ORDER_PROCEDURES), NOTHING, NOTHING,
                NOTHING, NOTHING, () -> opened[0]++);

        // Act
        JButton button = buttonWithText(welcome, "Order procedure");
        button.doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Order procedure", "Log out"), buttonTexts(welcome)),
                () -> assertEquals(KeyEvent.VK_O, button.getMnemonic()),
                () -> assertEquals(1, opened[0]));
    }

    @Test
    @DisplayName("TC06C-39 With every permission and action, Order procedure goes after Manage patients")
    void tc06c39OrderWithTheOtherButtons() {
        // Act
        IU_Welcome welcome = new IU_Welcome(physician, NOTHING, NOTHING, ALL, NOTHING, NOTHING, NOTHING, NOTHING,
                NOTHING);

        // Assert
        assertEquals(List.of("View audit log", "Manage roles", "Configure system", "Configure integrations",
                "Manage patients", "Order procedure", "Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("TC06C-39 Order procedure runs only its own action, and the others do not run it")
    void tc06c39OrderProcedureRunsOnlyItsOwnAction() {
        // Arrange
        int[] others = {0};
        int[] orders = {0};
        Runnable other = () -> others[0]++;
        IU_Welcome welcome = new IU_Welcome(physician, other, other, ALL, other, other, other, other,
                () -> orders[0]++);

        // Act
        buttonWithText(welcome, "Order procedure").doClick();
        int afterOrder = others[0];
        buttonWithText(welcome, "Manage patients").doClick();
        buttonWithText(welcome, "Log out").doClick();

        // Assert
        assertAll(
                () -> assertEquals(1, orders[0]),
                () -> assertEquals(0, afterOrder),
                () -> assertEquals(2, others[0]));
    }

    @Test
    @DisplayName("TC06C-40 A user without ORDER_PROCEDURES (admissions staff, or none) does not see Order procedure")
    void tc06c40UserWithoutOrderProceduresDoesNotSeeTheButton() {
        // Act
        IU_Welcome staff = new IU_Welcome(admissions, NOTHING, NOTHING, List.of(MANAGE_PATIENTS), NOTHING, NOTHING,
                NOTHING, NOTHING, NOTHING);
        IU_Welcome withoutPermissions =
                new IU_Welcome(physician, NOTHING, NOTHING, List.of(), NOTHING, NOTHING, NOTHING, NOTHING, NOTHING);

        // Assert
        assertAll(
                () -> assertEquals(List.of("Manage patients", "Log out"), buttonTexts(staff)),
                () -> assertEquals(List.of("Log out"), buttonTexts(withoutPermissions)));
    }

    @Test
    @DisplayName("TC06C-41 Without the action, not even a user with ORDER_PROCEDURES sees the button")
    void tc06c41WithoutTheActionNobodySeesTheButton() {
        // Act
        IU_Welcome welcome = new IU_Welcome(physician, NOTHING, NOTHING, List.of(ORDER_PROCEDURES), NOTHING, NOTHING,
                NOTHING, NOTHING, null);

        // Assert
        assertEquals(List.of("Log out"), buttonTexts(welcome));
    }

    @Test
    @DisplayName("TC06C-42 The constructor of UC12 does not show Order procedure, even with ORDER_PROCEDURES")
    void tc06c42OlderConstructorDoesNotShowTheButton() {
        // Act
        IU_Welcome ofUc12 = new IU_Welcome(physician, NOTHING, NOTHING, ALL, NOTHING, NOTHING, NOTHING, NOTHING);

        // Assert
        assertEquals(List.of("View audit log", "Manage roles", "Configure system", "Configure integrations",
                "Manage patients", "Log out"), buttonTexts(ofUc12));
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
