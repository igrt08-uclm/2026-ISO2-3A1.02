package es.uclm.esi.iso2.client;

import es.uclm.esi.iso2.client.access.presentation.IU_Login;
import es.uclm.esi.iso2.client.access.presentation.IU_Welcome;
import es.uclm.esi.iso2.common.access.contracts.IAuthenticationService;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import es.uclm.esi.iso2.server.access.integration.AuthenticatorSystemSimulator;
import es.uclm.esi.iso2.server.access.persistence.InMemoryUserAccountRepository;
import es.uclm.esi.iso2.server.access.service.AuditRecorderSimulator;
import es.uclm.esi.iso2.server.access.service.AuthenticationService;
import java.awt.Container;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/** Starts the application: the client calls the server directly, in one program (D-016). */
public final class Main {
    private static final String APPLICATION_TITLE = "Radiodiagnostics System";

    private final JFrame window = new JFrame(APPLICATION_TITLE);
    private final IAuthenticationService authenticationService = new AuthenticationService(
            new AuthenticatorSystemSimulator(), new InMemoryUserAccountRepository(), new AuditRecorderSimulator());

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().start());
    }

    private void start() {
        useNativeLookAndFeel();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        showLogin();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    private void useNativeLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException exception) {
            System.err.println("Native look and feel not available: " + exception.getMessage());
        }
    }

    private void showLogin() {
        window.setTitle(APPLICATION_TITLE + " - Log in");
        show(new IU_Login(authenticationService, this::showWelcome));
    }

    private void showWelcome(UserAccount user) {
        window.setTitle(APPLICATION_TITLE);
        show(new IU_Welcome(user, this::showLogin));
    }

    private void show(Container screen) {
        window.setContentPane(screen);
        window.pack();
    }
}
