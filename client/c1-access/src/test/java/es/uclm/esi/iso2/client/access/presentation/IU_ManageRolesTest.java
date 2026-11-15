package es.uclm.esi.iso2.client.access.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests of {@link IU_ManageRoles}. The controls are private, so they are found through the public
 * {@code getComponents()} method, as in IU_WelcomeTest. The panel is never shown, so no display is needed, and the
 * confirmation is a function of the test, so no window is opened.
 */
@DisplayName("IU_ManageRoles")
class IU_ManageRolesTest {
    private static final int USER_ROLES_TAB = 0;
    private static final int ROLE_PERMISSIONS_TAB = 1;
    private static final Color SUCCESS_COLOR = new Color(0x1B5E20);
    private static final Color ERROR_COLOR = new Color(0xB00020);

    private static final Permission MANAGE_ROLES =
            new Permission("MANAGE_ROLES", "Assign roles to users and permissions to roles");
    private static final Role ADMINISTRATOR = new Role("SYSTEM_ADMINISTRATOR", "System administrator",
            "Configures and manages the system", List.of(MANAGE_ROLES));
    private static final Role PHYSICIAN = new Role("PHYSICIAN", "Physician", "Requests and reviews tests and reports");
    private static final Role PATIENT = new Role("PATIENT", "Patient", "Accesses their own appointments and results");

    private FakeRoleManagementService service;
    private List<String> questions;
    private boolean answer;
    private int backs;

    @BeforeEach
    void setUp() {
        service = new FakeRoleManagementService(
                List.of(new UserAccount("admin", "Laura Sánchez Ruiz", "laura.sanchez@sescam.test", true,
                                List.of(ADMINISTRATOR)),
                        new UserAccount("mgarcia", "Miguel García López", "miguel.garcia@sescam.test", true,
                                List.of(PHYSICIAN, PATIENT))),
                List.of(ADMINISTRATOR, PHYSICIAN, PATIENT),
                List.of(MANAGE_ROLES));
        questions = new ArrayList<>();
        answer = true;
        backs = 0;
    }

    private IU_ManageRoles openScreen() {
        return new IU_ManageRoles(service, "admin", question -> {
            questions.add(question);
            return answer;
        }, () -> backs++);
    }

    // --- Opening the screen and the lists ---

    @Test
    @DisplayName("Opening the screen loads the lists and shows the first user and the first role")
    void openingTheScreenLoadsTheLists() {
        // Act
        IU_ManageRoles screen = openScreen();

        // Assert
        assertAll(
                () -> assertEquals(List.of("admin", "mgarcia"), usernames(subject(screen, USER_ROLES_TAB))),
                () -> assertEquals(List.of("System administrator", "Physician", "Patient"),
                        roleNames(toAssign(screen, USER_ROLES_TAB))),
                () -> assertEquals(List.of("System administrator"), listItems(screen, USER_ROLES_TAB)),
                () -> assertEquals("System administrator",
                        ((Role) subject(screen, ROLE_PERMISSIONS_TAB).getSelectedItem()).getName()),
                () -> assertEquals(List.of("MANAGE_ROLES"), listItems(screen, ROLE_PERMISSIONS_TAB)),
                () -> assertEquals(" ", status(screen).getText()));
    }

    @Test
    @DisplayName("The lists show the names of the users, roles and permissions, not their ids")
    void theListsShowNamesNotIds() {
        // Arrange
        IU_ManageRoles screen = openScreen();

        // Act
        String user = shownText(subject(screen, USER_ROLES_TAB), 1);
        String role = shownText(toAssign(screen, USER_ROLES_TAB), 2);
        String permission = shownText(toAssign(screen, ROLE_PERMISSIONS_TAB), 0);

        // Assert
        assertAll(
                () -> assertEquals("mgarcia - Miguel García López", user),
                () -> assertEquals("Patient", role),
                () -> assertEquals("MANAGE_ROLES - Assign roles to users and permissions to roles", permission));
    }

    @Test
    @DisplayName("Selecting another user shows their roles, and another role shows its permissions")
    void selectingShowsTheCurrentState() {
        // Arrange
        IU_ManageRoles screen = openScreen();

        // Act
        subject(screen, USER_ROLES_TAB).setSelectedIndex(1);
        subject(screen, ROLE_PERMISSIONS_TAB).setSelectedIndex(1);

        // Assert
        assertAll(
                () -> assertEquals(List.of("Physician", "Patient"), listItems(screen, USER_ROLES_TAB)),
                () -> assertEquals(List.of(), listItems(screen, ROLE_PERMISSIONS_TAB)));
    }

    // Error prevention: nothing can be revoked until it is selected.
    @ParameterizedTest(name = "tab {0}")
    @ValueSource(ints = {USER_ROLES_TAB, ROLE_PERMISSIONS_TAB})
    @DisplayName("Revoke selected is only enabled when something is selected")
    void revokeIsOnlyEnabledWithASelection(int tab) {
        // Arrange
        IU_ManageRoles screen = openScreen();
        JButton revoke = button(screen, tab, "Revoke selected");
        boolean enabledBefore = revoke.isEnabled();

        // Act
        list(screen, tab).setSelectedIndex(0);

        // Assert
        assertAll(
                () -> assertFalse(enabledBefore),
                () -> assertTrue(revoke.isEnabled()));
    }

    // --- The four changes with OK ---

    @Test
    @DisplayName("Assigning a role with OK sends it, shows the new roles and a success message")
    void assigningARoleWithOk() {
        // Arrange
        IU_ManageRoles screen = openScreen();
        subject(screen, USER_ROLES_TAB).setSelectedIndex(1);
        toAssign(screen, USER_ROLES_TAB).setSelectedIndex(0);

        // Act
        button(screen, USER_ROLES_TAB, "Assign role").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("assignRole admin SYSTEM_ADMINISTRATOR mgarcia"), service.changes()),
                () -> assertEquals(List.of("Physician", "Patient", "System administrator"),
                        listItems(screen, USER_ROLES_TAB)),
                () -> assertEquals("mgarcia",
                        ((UserAccount) subject(screen, USER_ROLES_TAB).getSelectedItem()).getUsername()),
                () -> assertSuccess(screen, "✔ Role System administrator assigned to mgarcia."));
    }

    @Test
    @DisplayName("Revoking a role with OK sends it, shows the new roles and a success message")
    void revokingARoleWithOk() {
        // Arrange
        IU_ManageRoles screen = openScreen();
        subject(screen, USER_ROLES_TAB).setSelectedIndex(1);
        list(screen, USER_ROLES_TAB).setSelectedIndex(1);

        // Act
        button(screen, USER_ROLES_TAB, "Revoke selected").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("revokeRole admin PATIENT mgarcia"), service.changes()),
                () -> assertEquals(List.of("Physician"), listItems(screen, USER_ROLES_TAB)),
                () -> assertSuccess(screen, "✔ Role Patient revoked from mgarcia."));
    }

    @Test
    @DisplayName("Assigning a permission with OK sends it, shows the new permissions and a success message")
    void assigningAPermissionWithOk() {
        // Arrange
        IU_ManageRoles screen = openScreen();
        subject(screen, ROLE_PERMISSIONS_TAB).setSelectedIndex(1);

        // Act
        button(screen, ROLE_PERMISSIONS_TAB, "Assign permission").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("assignPermission admin PHYSICIAN MANAGE_ROLES"), service.changes()),
                () -> assertEquals(List.of("MANAGE_ROLES"), listItems(screen, ROLE_PERMISSIONS_TAB)),
                () -> assertEquals("Physician",
                        ((Role) subject(screen, ROLE_PERMISSIONS_TAB).getSelectedItem()).getName()),
                () -> assertSuccess(screen, "✔ Permission MANAGE_ROLES assigned to Physician."));
    }

    @Test
    @DisplayName("Revoking a permission with OK sends it, shows the new permissions and a success message")
    void revokingAPermissionWithOk() {
        // Arrange
        IU_ManageRoles screen = openScreen();
        list(screen, ROLE_PERMISSIONS_TAB).setSelectedIndex(0);

        // Act
        button(screen, ROLE_PERMISSIONS_TAB, "Revoke selected").doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("revokePermission admin SYSTEM_ADMINISTRATOR MANAGE_ROLES"),
                        service.changes()),
                () -> assertEquals(List.of(), listItems(screen, ROLE_PERMISSIONS_TAB)),
                () -> assertSuccess(screen, "✔ Permission MANAGE_ROLES revoked from System administrator."));
    }

    // --- Confirmation ---

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "assignRole       | Assign the role System administrator to mgarcia?",
        "revokeRole       | Revoke the role Patient from mgarcia?",
        "assignPermission | Assign the permission MANAGE_ROLES to Physician?",
        "revokePermission | Revoke the permission MANAGE_ROLES from System administrator?"
    })
    @DisplayName("Each change asks its own question")
    void eachChangeAsksItsQuestion(String operation, String expectedQuestion) {
        // Arrange
        IU_ManageRoles screen = openScreen();
        JButton button = prepare(screen, operation);

        // Act
        button.doClick();

        // Assert
        assertEquals(List.of(expectedQuestion), questions);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"assignRole", "revokeRole", "assignPermission", "revokePermission"})
    @DisplayName("Cancel does not send the change and shows no message")
    void cancelDoesNotSendTheChange(String operation) {
        // Arrange
        IU_ManageRoles screen = openScreen();
        JButton button = prepare(screen, operation);
        answer = false;

        // Act
        button.doClick();

        // Assert
        assertAll(
                () -> assertEquals(1, questions.size()),
                () -> assertTrue(service.changes().isEmpty()),
                () -> assertEquals(" ", status(screen).getText()));
    }

    // The list can be out of date if another user changed the data (the case of "revoke" below).
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "assignRole       | Nothing changed: mgarcia already has that role.",
        "revokeRole       | Nothing changed: mgarcia does not have that role.",
        "assignPermission | Nothing changed: System administrator already has that permission.",
        "revokePermission | Nothing changed: System administrator does not have that permission."
    })
    @DisplayName("A change that is already done asks nothing and sends nothing")
    void aChangeAlreadyDoneAsksNothing(String operation, String expectedMessage) {
        // Arrange
        IU_ManageRoles screen = openScreen();
        JButton button = switch (operation) {
            case "assignRole" -> {
                subject(screen, USER_ROLES_TAB).setSelectedIndex(1);
                toAssign(screen, USER_ROLES_TAB).setSelectedIndex(1);
                yield button(screen, USER_ROLES_TAB, "Assign role");
            }
            case "revokeRole" -> {
                subject(screen, USER_ROLES_TAB).setSelectedIndex(1);
                list(screen, USER_ROLES_TAB).setSelectedIndex(1);
                service.setUser(new UserAccount("mgarcia", "Miguel García López", "miguel.garcia@sescam.test", true,
                        List.of(PHYSICIAN)));
                yield button(screen, USER_ROLES_TAB, "Revoke selected");
            }
            case "assignPermission" -> button(screen, ROLE_PERMISSIONS_TAB, "Assign permission");
            default -> {
                list(screen, ROLE_PERMISSIONS_TAB).setSelectedIndex(0);
                service.setRole(new Role("SYSTEM_ADMINISTRATOR", "System administrator", "Test role"));
                yield button(screen, ROLE_PERMISSIONS_TAB, "Revoke selected");
            }
        };

        // Act
        button.doClick();

        // Assert
        assertAll(
                () -> assertTrue(questions.isEmpty()),
                () -> assertTrue(service.changes().isEmpty()),
                () -> assertEquals(expectedMessage, status(screen).getText()));
    }

    // --- Errors ---

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "AccessDeniedException    | ⚠ You do not have permission to manage roles.",
        "IllegalStateException    | ⚠ This change would leave the system without an active administrator.",
        "IllegalArgumentException | ⚠ That user, role or permission no longer exists. Reload the screen."
    })
    @DisplayName("A rejected change shows its message and the state of the server")
    void aRejectedChangeShowsItsMessage(String exception, String expectedMessage) {
        // Arrange
        IU_ManageRoles screen = openScreen();
        subject(screen, USER_ROLES_TAB).setSelectedIndex(1);
        toAssign(screen, USER_ROLES_TAB).setSelectedIndex(0);
        service.failNextChange(switch (exception) {
            case "AccessDeniedException" -> new AccessDeniedException("admin");
            case "IllegalStateException" -> new IllegalStateException("The system needs an active administrator");
            default -> new IllegalArgumentException("Unknown role");
        });

        // Act
        button(screen, USER_ROLES_TAB, "Assign role").doClick();

        // Assert
        assertAll(
                () -> assertEquals(expectedMessage, status(screen).getText()),
                () -> assertEquals(ERROR_COLOR, status(screen).getForeground()),
                () -> assertEquals(List.of("Physician", "Patient"), listItems(screen, USER_ROLES_TAB)));
    }

    @Test
    @DisplayName("After an error, a change that works replaces the message")
    void afterAnErrorASuccessReplacesTheMessage() {
        // Arrange
        IU_ManageRoles screen = openScreen();
        subject(screen, USER_ROLES_TAB).setSelectedIndex(1);
        toAssign(screen, USER_ROLES_TAB).setSelectedIndex(0);
        service.failNextChange(new AccessDeniedException("admin"));
        button(screen, USER_ROLES_TAB, "Assign role").doClick();

        // Act
        button(screen, USER_ROLES_TAB, "Assign role").doClick();

        // Assert
        assertSuccess(screen, "✔ Role System administrator assigned to mgarcia.");
    }

    // --- Back and shortcuts ---

    @Test
    @DisplayName("The Back button runs onBack")
    void backButtonRunsOnBack() {
        // Arrange
        IU_ManageRoles screen = openScreen();

        // Act
        buttonWithText(screen, "Back").doClick();

        // Assert
        assertEquals(1, backs);
    }

    @Test
    @DisplayName("The tabs and the buttons have their shortcuts")
    void tabsAndButtonsHaveShortcuts() {
        // Act
        IU_ManageRoles screen = openScreen();

        // Assert
        JTabbedPane tabs = tabs(screen);
        assertAll(
                () -> assertEquals(KeyEvent.VK_U, tabs.getMnemonicAt(USER_ROLES_TAB)),
                () -> assertEquals(KeyEvent.VK_P, tabs.getMnemonicAt(ROLE_PERMISSIONS_TAB)),
                () -> assertEquals(KeyEvent.VK_A, button(screen, USER_ROLES_TAB, "Assign role").getMnemonic()),
                () -> assertEquals(KeyEvent.VK_R, button(screen, USER_ROLES_TAB, "Revoke selected").getMnemonic()),
                () -> assertEquals(KeyEvent.VK_A,
                        button(screen, ROLE_PERMISSIONS_TAB, "Assign permission").getMnemonic()),
                () -> assertEquals(KeyEvent.VK_R,
                        button(screen, ROLE_PERMISSIONS_TAB, "Revoke selected").getMnemonic()),
                () -> assertEquals(KeyEvent.VK_B, buttonWithText(screen, "Back").getMnemonic()));
    }

    // --- Helpers ---

    /** Selects what each operation needs, so that it is a real change, and returns the button that runs it. */
    private JButton prepare(IU_ManageRoles screen, String operation) {
        return switch (operation) {
            case "assignRole" -> {
                subject(screen, USER_ROLES_TAB).setSelectedIndex(1);
                toAssign(screen, USER_ROLES_TAB).setSelectedIndex(0);
                yield button(screen, USER_ROLES_TAB, "Assign role");
            }
            case "revokeRole" -> {
                subject(screen, USER_ROLES_TAB).setSelectedIndex(1);
                list(screen, USER_ROLES_TAB).setSelectedIndex(1);
                yield button(screen, USER_ROLES_TAB, "Revoke selected");
            }
            case "assignPermission" -> {
                subject(screen, ROLE_PERMISSIONS_TAB).setSelectedIndex(1);
                yield button(screen, ROLE_PERMISSIONS_TAB, "Assign permission");
            }
            default -> {
                list(screen, ROLE_PERMISSIONS_TAB).setSelectedIndex(0);
                yield button(screen, ROLE_PERMISSIONS_TAB, "Revoke selected");
            }
        };
    }

    private static void assertSuccess(IU_ManageRoles screen, String expectedMessage) {
        assertAll(
                () -> assertEquals(expectedMessage, status(screen).getText()),
                () -> assertEquals(SUCCESS_COLOR, status(screen).getForeground()));
    }

    private static JTabbedPane tabs(IU_ManageRoles screen) {
        return find(screen, JTabbedPane.class).get(0);
    }

    private static Container tab(IU_ManageRoles screen, int index) {
        return (Container) tabs(screen).getComponentAt(index);
    }

    /** The user in the first tab, or the role in the second one. */
    @SuppressWarnings("unchecked")
    private static <T> JComboBox<T> subject(IU_ManageRoles screen, int tab) {
        return find(tab(screen, tab), JComboBox.class).get(0);
    }

    /** The role or the permission to assign. */
    @SuppressWarnings("unchecked")
    private static <T> JComboBox<T> toAssign(IU_ManageRoles screen, int tab) {
        return find(tab(screen, tab), JComboBox.class).get(1);
    }

    private static JList<?> list(IU_ManageRoles screen, int tab) {
        return find(tab(screen, tab), JList.class).get(0);
    }

    /** The names of the roles, or the codes of the permissions, shown in the list of a tab. */
    private static List<String> listItems(IU_ManageRoles screen, int tab) {
        ListModel<?> model = list(screen, tab).getModel();
        List<String> items = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            Object item = model.getElementAt(i);
            items.add(item instanceof Role role ? role.getName() : ((Permission) item).getCode());
        }
        return items;
    }

    private static List<String> usernames(JComboBox<UserAccount> combo) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < combo.getItemCount(); i++) {
            names.add(combo.getItemAt(i).getUsername());
        }
        return names;
    }

    private static List<String> roleNames(JComboBox<Role> combo) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < combo.getItemCount(); i++) {
            names.add(combo.getItemAt(i).getName());
        }
        return names;
    }

    /** The text that the list of the combo box shows for one item. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String shownText(JComboBox combo, int index) {
        Component shown = combo.getRenderer().getListCellRendererComponent(new JList<>(), combo.getItemAt(index),
                index, false, false);
        return ((JLabel) shown).getText();
    }

    private static JButton button(IU_ManageRoles screen, int tab, String text) {
        return buttonWithText(tab(screen, tab), text);
    }

    private static JButton buttonWithText(Container container, String text) {
        return find(container, JButton.class).stream().filter(button -> text.equals(button.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("No \"" + text + "\" button found"));
    }

    /** The message at the bottom of the screen, on the left of Back. */
    private static JLabel status(IU_ManageRoles screen) {
        JPanel footer = (JPanel) ((BorderLayout) screen.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
        return (JLabel) footer.getComponent(0);
    }

    /** Every component of this type inside the container, in order, without looking inside the combo boxes. */
    private static <T> List<T> find(Container container, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                found.add(type.cast(component));
            }
            if (component instanceof Container child && !(component instanceof JComboBox)) {
                found.addAll(find(child, type));
            }
        }
        return found;
    }
}
