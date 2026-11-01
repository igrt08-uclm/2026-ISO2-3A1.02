package es.uclm.esi.iso2.client.access.presentation;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
