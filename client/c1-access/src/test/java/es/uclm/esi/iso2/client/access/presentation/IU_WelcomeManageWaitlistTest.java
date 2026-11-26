package es.uclm.esi.iso2.client.access.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests of the button <em>Manage waitlist</em> of {@link IU_Welcome} (UC15, #133). It is only shown to a user with the
 * permission {@code MANAGE_APPOINTMENTS} when the action is given. The cases are TC15C-32 and TC15C-33 of
 * {@code doc/testing/test-cases/UC15-client-test-design.md}.
 */
@DisplayName("IU_Welcome Manage waitlist")
class IU_WelcomeManageWaitlistTest {
    private static final Permission MANAGE_APPOINTMENTS =
            new Permission("MANAGE_APPOINTMENTS", "Manage appointments, waitlists and daily schedules");
    private static final Runnable NOTHING = () -> { };
    private static final UserAccount ADMISSIONS =
            new UserAccount("lmartin", "Lucía Martín Pérez", "lucia.martin@sescam.test", true, List.of());

    @Test
    @DisplayName("TC15C-32 with the permission Manage waitlist (Alt+W) is shown and runs its action")
    void shownWithThePermission() {
        // Arrange
        int[] opened = {0};
        IU_Welcome welcome = welcome(List.of(MANAGE_APPOINTMENTS), () -> opened[0]++);

        // Act
        JButton button = buttons(welcome).stream().filter(b -> "Manage waitlist".equals(b.getText())).findFirst()
                .orElseThrow();
        button.doClick();

        // Assert
        assertAll(
                () -> assertEquals(List.of("Manage appointments", "Manage waitlist", "Log out"), texts(welcome)),
                () -> assertEquals(KeyEvent.VK_W, button.getMnemonic()),
                () -> assertEquals(1, opened[0]));
    }

    @Test
    @DisplayName("TC15C-33 without the permission or the action it is not shown")
    void notShown() {
        // Act
        IU_Welcome withoutPermission = welcome(List.of(), NOTHING);
        IU_Welcome withoutAction = welcome(List.of(MANAGE_APPOINTMENTS), null);

        // Assert
        assertAll(
                () -> assertEquals(List.of("Log out"), texts(withoutPermission)),
                () -> assertEquals(List.of("Manage appointments", "Log out"), texts(withoutAction)));
    }

    private static IU_Welcome welcome(List<Permission> permissions, Runnable onManageWaitlist) {
        return new IU_Welcome(ADMISSIONS, NOTHING, NOTHING, permissions, NOTHING, NOTHING, NOTHING, NOTHING, NOTHING,
                NOTHING, NOTHING, onManageWaitlist);
    }

    private static List<JButton> buttons(IU_Welcome welcome) {
        List<JButton> buttons = new ArrayList<>();
        for (Component component : welcome.getComponents()) {
            if (component instanceof JButton button) {
                buttons.add(button);
            }
        }
        return buttons;
    }

    private static List<String> texts(IU_Welcome welcome) {
        return buttons(welcome).stream().map(JButton::getText).toList();
    }
}
