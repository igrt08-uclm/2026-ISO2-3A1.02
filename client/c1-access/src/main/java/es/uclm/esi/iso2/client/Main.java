package es.uclm.esi.iso2.client;

import es.uclm.esi.iso2.client.access.presentation.IU_Audit;
import es.uclm.esi.iso2.client.access.presentation.IU_Login;
import es.uclm.esi.iso2.client.access.presentation.IU_ManageRoles;
import es.uclm.esi.iso2.client.access.presentation.IU_Welcome;
import es.uclm.esi.iso2.common.access.contracts.IAuthenticationService;
import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import es.uclm.esi.iso2.server.access.integration.AuthenticatorSystemSimulator;
import es.uclm.esi.iso2.server.access.persistence.IUserAccountRepository;
import es.uclm.esi.iso2.server.access.persistence.InMemoryAuditLogRepository;
import es.uclm.esi.iso2.server.access.persistence.InMemoryPermissionRepository;
import es.uclm.esi.iso2.server.access.persistence.InMemoryRoleRepository;
import es.uclm.esi.iso2.server.access.persistence.InMemoryUserAccountRepository;
import es.uclm.esi.iso2.server.access.service.AuditService;
import es.uclm.esi.iso2.server.access.service.AuthenticationService;
import es.uclm.esi.iso2.server.access.service.RoleManagementService;
import java.awt.Container;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/** Starts the application: the client calls the server directly, in one program (D-016). */
public final class Main {
    private static final String APPLICATION_TITLE = "Radiodiagnostics System";

    private final JFrame window = new JFrame(APPLICATION_TITLE);
    private final IUserAccountRepository userAccountRepository = new InMemoryUserAccountRepository();
    private final AuditService auditService =
            new AuditService(new InMemoryAuditLogRepository(), userAccountRepository);
    private final IAuthenticationService authenticationService =
            new AuthenticationService(new AuthenticatorSystemSimulator(), userAccountRepository, auditService);
    private final IRoleManagementService roleManagementService = new RoleManagementService(new InMemoryRoleRepository(),
            new InMemoryPermissionRepository(), userAccountRepository, auditService);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().start());
    }

    private void start() {
        useNativeLookAndFeel();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        showLogin();
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
        show(new IU_Welcome(user, this::showLogin, () -> showAudit(user),
                roleManagementService.getPermissions(user.getUsername()), () -> showManageRoles(user)));
    }

    private void showManageRoles(UserAccount user) {
        window.setTitle(APPLICATION_TITLE + " - Manage roles");
        show(new IU_ManageRoles(roleManagementService, user.getUsername(), this::confirm,
                () -> showWelcome(userAccountRepository.findByUsername(user.getUsername()))));
    }

    private boolean confirm(String question) {
        return JOptionPane.showConfirmDialog(window, question, "Confirm", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void showAudit(UserAccount user) {
        window.setTitle(APPLICATION_TITLE + " - Audit log");
        show(new IU_Audit(auditService, user.getUsername(), () -> showWelcome(user)));
    }

    private void show(Container screen) {
        window.setContentPane(screen);
        window.pack();
        window.setLocationRelativeTo(null);
    }
}
