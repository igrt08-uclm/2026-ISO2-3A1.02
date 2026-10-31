package es.uclm.esi.iso2.client.access.presentation;

import es.uclm.esi.iso2.common.access.contracts.AuthenticationException;
import es.uclm.esi.iso2.common.access.contracts.AuthenticationFailureReason;
import es.uclm.esi.iso2.common.access.contracts.IAuthenticationService;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class IU_Login extends JPanel {
    private static final int FIELD_COLUMNS = 30;
    private static final Color ERROR_COLOR = new Color(0xB00020);
    private static final String ERROR_ICON = "⚠ ";

    private final transient IAuthenticationService authenticationService;
    private final transient Consumer<UserAccount> onSuccess;
    private transient UserAccount authenticatedUser;

    private final JTextField txtUsername = new JTextField(FIELD_COLUMNS);
    private final JPasswordField txtPassword = new JPasswordField(FIELD_COLUMNS);
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnLogin = new JButton("Log in");

    public IU_Login(IAuthenticationService authenticationService, Consumer<UserAccount> onSuccess) {
        super(new GridBagLayout());
        this.authenticationService = authenticationService;
        this.onSuccess = onSuccess;
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        buildLayout();
        configureControls();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(txtUsername::requestFocusInWindow);
    }

    public void enterCredentials(String username, String password) {
        try {
            authenticatedUser = authenticationService.login(username, password);
            showSuccess();
        } catch (AuthenticationException exception) {
            showLoginError(exception.getReason());
        }
    }

    public void showLoginError(AuthenticationFailureReason reason) {
        switch (reason) {
            case UNKNOWN_USER -> focusUsername();
            case WRONG_PASSWORD -> focusPassword();
            case INACTIVE_USER -> focusUsername();
        }
        lblError.setText(ERROR_ICON + messageFor(reason));
    }

    public void showSuccess() {
        onSuccess.accept(authenticatedUser);
    }

    private static String messageFor(AuthenticationFailureReason reason) {
        return switch (reason) {
            case UNKNOWN_USER -> "This username does not exist. Check it and try again.";
            case WRONG_PASSWORD -> "The password is not correct. Try again.";
            case INACTIVE_USER -> "This account is inactive. Contact the system administrator.";
        };
    }

    private void focusUsername() {
        txtPassword.setText("");
        txtUsername.selectAll();
        txtUsername.requestFocusInWindow();
    }

    private void focusPassword() {
        txtPassword.setText("");
        txtPassword.requestFocusInWindow();
    }

    private void buildLayout() {
        JLabel lblTitle = new JLabel(
                "<html><div style='text-align:center'>Radiodiagnostics and Medical Imaging<br>Management System</div></html>",
                SwingConstants.CENTER);
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 18f));
        JLabel lblHelp = new JLabel("Log in with your corporate username and password", SwingConstants.CENTER);

        JLabel lblUsername = new JLabel("Username");
        lblUsername.setDisplayedMnemonic('U');
        lblUsername.setLabelFor(txtUsername);
        JLabel lblPassword = new JLabel("Password");
        lblPassword.setDisplayedMnemonic('P');
        lblPassword.setLabelFor(txtPassword);

        lblError.setForeground(ERROR_COLOR);

        addRow(lblTitle, 0, 0, GridBagConstraints.HORIZONTAL);
        addRow(lblHelp, 1, 8, GridBagConstraints.HORIZONTAL);
        addRow(lblUsername, 2, 20, GridBagConstraints.HORIZONTAL);
        addRow(txtUsername, 3, 4, GridBagConstraints.HORIZONTAL);
        addRow(lblPassword, 4, 14, GridBagConstraints.HORIZONTAL);
        addRow(txtPassword, 5, 4, GridBagConstraints.HORIZONTAL);
        addRow(lblError, 6, 14, GridBagConstraints.HORIZONTAL);
        addRow(btnLogin, 7, 14, GridBagConstraints.NONE);
    }

    private void addRow(Component component, int row, int topSpace, int fill) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.weightx = 1;
        constraints.fill = fill;
        constraints.anchor = GridBagConstraints.LINE_END;
        constraints.insets = new Insets(topSpace, 0, 0, 0);
        add(component, constraints);
    }

    private void configureControls() {
        btnLogin.setMnemonic('L');
        btnLogin.setEnabled(false);
        DocumentListener inputListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                inputChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                inputChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                inputChanged();
            }
        };
        txtUsername.getDocument().addDocumentListener(inputListener);
        txtPassword.getDocument().addDocumentListener(inputListener);
        btnLogin.addActionListener(event -> submit());
        txtUsername.addActionListener(event -> submit());
        txtPassword.addActionListener(event -> submit());
    }

    private void submit() {
        if (btnLogin.isEnabled()) {
            enterCredentials(txtUsername.getText(), new String(txtPassword.getPassword()));
        }
    }

    private void inputChanged() {
        lblError.setText(" ");
        btnLogin.setEnabled(!txtUsername.getText().isEmpty() && txtPassword.getPassword().length > 0);
    }
}
