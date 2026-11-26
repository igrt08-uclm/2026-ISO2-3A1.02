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
 * Tests of the button <em>Daily schedule</em> of {@link IU_Welcome} (UC14, #136). It is only shown to a user with the
 * permission {@code MANAGE_APPOINTMENTS} when the action is given. The cases are TC14C-17 to TC14C-20 of
 * {@code doc/testing/test-cases/UC14-client-test-design.md}.
 */
@DisplayName("IU_Welcome Daily schedule")
class IU_WelcomeDailyScheduleTest {
    private static final Permission MANAGE_APPOINTMENTS =
            new Permission("MANAGE_APPOINTMENTS", "Manage appointments, waitlists and daily schedules");
    private static final Runnable NOTHING = () -> { };
    private static final UserAccount ADMISSIONS =
            new UserAccount("lmartin", "Lucía Martín Pérez", "lucia.martin@sescam.test", true, List.of());

    @Test
    @DisplayName("TC14C-17 with the permission Daily schedule (Alt+D) is shown and runs its action")
    void shownWithThePermission() {
        // Arrange
        int[] opened = {0};
        IU_Welcome welcome = welcome(List.of(MANAGE_APPOINTMENTS), () -> opened[0]++);

        // Act
        JButton button = buttons(welcome).stream().filter(b -> "Daily schedule".equals(b.getText())).findFirst()
                .orElseThrow();
        button.doClick();

        // Assert
        assertAll(
                () -> assertEquals(KeyEvent.VK_D, button.getMnemonic()),
                () -> assertEquals(1, opened[0]));
    }

    @Test
    @DisplayName("TC14C-18 without the permission it is not shown")
    void notShownWithoutThePermission() {
        // Act
        IU_Welcome welcome = welcome(List.of(), NOTHING);

        // Assert
        assertEquals(List.of("Log out"), texts(welcome));
    }

    @Test
    @DisplayName("TC14C-19 with both actions Manage waitlist and Daily schedule are shown")
    void shownWithTheWaitlist() {
        // Act
        IU_Welcome welcome = welcome(List.of(MANAGE_APPOINTMENTS), NOTHING);

        // Assert
        assertEquals(List.of("Manage appointments", "Manage waitlist", "Daily schedule", "Log out"), texts(welcome));
    }

    @Test
    @DisplayName("TC14C-20 the constructor of UC15 shows Manage waitlist and no Daily schedule")
    void constructorWithoutTheAction() {
        // Act
        IU_Welcome welcome = new IU_Welcome(ADMISSIONS, NOTHING, NOTHING, List.of(MANAGE_APPOINTMENTS), NOTHING,
                NOTHING, NOTHING, NOTHING, NOTHING, NOTHING, NOTHING, NOTHING);

        // Assert
        assertEquals(List.of("Manage appointments", "Manage waitlist", "Log out"), texts(welcome));
    }

    private static IU_Welcome welcome(List<Permission> permissions, Runnable onDailySchedule) {
        return new IU_Welcome(ADMISSIONS, NOTHING, NOTHING, permissions, NOTHING, NOTHING, NOTHING, NOTHING, NOTHING,
                NOTHING, NOTHING, NOTHING, onDailySchedule);
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
