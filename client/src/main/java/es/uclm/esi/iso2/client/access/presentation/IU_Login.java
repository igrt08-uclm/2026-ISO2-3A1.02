package es.uclm.esi.iso2.client.access.presentation;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class IU_Login extends JPanel {
    private static final int FIELD_COLUMNS = 30;
    private static final Color ERROR_COLOR = new Color(0xB00020);

    private final JTextField txtUsername = new JTextField(FIELD_COLUMNS);
    private final JPasswordField txtPassword = new JPasswordField(FIELD_COLUMNS);
    private final JLabel lblError = new JLabel(" ");
    private final JButton btnLogin = new JButton("Log in");

    public IU_Login() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        buildLayout();
        configureControls();
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
    }

    private void inputChanged() {
        btnLogin.setEnabled(!txtUsername.getText().isEmpty() && txtPassword.getPassword().length > 0);
    }
}
