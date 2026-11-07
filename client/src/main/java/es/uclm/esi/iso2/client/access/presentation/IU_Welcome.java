package es.uclm.esi.iso2.client.access.presentation;

import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class IU_Welcome extends JPanel {
    // Temporary: with UC18 this check uses the permissions of the role.
    private static final String ADMINISTRATOR_ROLE = "SYSTEM_ADMINISTRATOR";
    private static final String MANAGE_ROLES = "MANAGE_ROLES";
    private static final int COLUMNS = 3;

    public IU_Welcome(UserAccount user, Runnable onLogout) {
        this(user, onLogout, null);
    }

    public IU_Welcome(UserAccount user, Runnable onLogout, Runnable onViewAudit) {
        this(user, onLogout, onViewAudit, List.of(), null);
    }

    /**
     * @param permissions the permissions of the user, read from the server, because the roles of the logged user do not
     *                    carry them
     */
    public IU_Welcome(UserAccount user, Runnable onLogout, Runnable onViewAudit, List<Permission> permissions,
            Runnable onManageRoles) {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel lblWelcome = new JLabel("Welcome, " + user.getFullName());
        lblWelcome.setFont(lblWelcome.getFont().deriveFont(Font.BOLD, 18f));
        String roles = user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", "));
        JButton btnLogout = new JButton("Log out");
        btnLogout.setMnemonic('L');
        btnLogout.addActionListener(event -> onLogout.run());

        addRow(lblWelcome, 0, 0, GridBagConstraints.HORIZONTAL);
        addRow(new JLabel("You are logged in as: " + roles), 1, 12, GridBagConstraints.HORIZONTAL);
        addRow(new JLabel("The functions of your role will appear here."), 2, 12, GridBagConstraints.HORIZONTAL);
        if (onViewAudit != null && isAdministrator(user)) {
            JButton btnViewAudit = new JButton("View audit log");
            btnViewAudit.setMnemonic('V');
            btnViewAudit.addActionListener(event -> onViewAudit.run());
            addButton(btnViewAudit, 0);
        }
        if (onManageRoles != null && hasPermission(permissions, MANAGE_ROLES)) {
            JButton btnManageRoles = new JButton("Manage roles");
            btnManageRoles.setMnemonic('M');
            btnManageRoles.addActionListener(event -> onManageRoles.run());
            addButton(btnManageRoles, 1);
        }
        addButton(btnLogout, 2);
    }

    private static boolean hasPermission(List<Permission> permissions, String code) {
        return permissions.stream().anyMatch(permission -> code.equals(permission.getCode()));
    }

    private static boolean isAdministrator(UserAccount user) {
        return user.getRoles().stream().anyMatch(role -> ADMINISTRATOR_ROLE.equals(role.getId()));
    }

    private void addButton(JButton button, int column) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = 3;
        constraints.weightx = column == 0 ? 1 : 0;
        constraints.anchor = GridBagConstraints.LINE_END;
        constraints.insets = new Insets(24, 0, 0, column < COLUMNS - 1 ? 8 : 0);
        add(button, constraints);
    }

    private void addRow(Component component, int row, int topSpace, int fill) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.gridwidth = COLUMNS;
        constraints.weightx = 1;
        constraints.fill = fill;
        constraints.anchor = GridBagConstraints.LINE_END;
        constraints.insets = new Insets(topSpace, 0, 0, 0);
        add(component, constraints);
    }
}
