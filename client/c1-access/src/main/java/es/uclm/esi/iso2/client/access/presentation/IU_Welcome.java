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

/**
 * Displays the welcome screen with the options available for the logged user.
 */
public class IU_Welcome extends JPanel {
    // Temporary: with UC18 this check uses the permissions of the role.
    private static final String ADMINISTRATOR_ROLE = "SYSTEM_ADMINISTRATOR";
    private static final String MANAGE_ROLES = "MANAGE_ROLES";
    private static final String CONFIGURE_SYSTEM = "CONFIGURE_SYSTEM";
    private static final String CONFIGURE_INTEGRATIONS = "CONFIGURE_INTEGRATIONS";
    private static final String MANAGE_PATIENTS = "MANAGE_PATIENTS";
    private static final int COLUMNS = 6;

    /**
     * Creates the welcome screen with the button <em>Log out</em>.
     *
     * @param user the logged user
     * @param onLogout is run when the user presses <em>Log out</em>
     */
    public IU_Welcome(UserAccount user, Runnable onLogout) {
        this(user, onLogout, null);
    }

    /**
     * Creates the welcome screen with the button <em>View audit log</em> of UC19.
     *
     * @param user the logged user
     * @param onLogout is run when the user presses <em>Log out</em>
     * @param onViewAudit is run when the user presses <em>View audit log</em>; the button is only shown to an
     *                    administrator, and not at all if this is {@code null}
     */
    public IU_Welcome(UserAccount user, Runnable onLogout, Runnable onViewAudit) {
        this(user, onLogout, onViewAudit, List.of(), null);
    }

    /**
     * Creates the welcome screen with the button <em>Manage roles</em> of UC18.
     *
     * @param user the logged user
     * @param onLogout is run when the user presses <em>Log out</em>
     * @param onViewAudit is run when the user presses <em>View audit log</em> (see the other constructor)
     * @param permissions the permissions of the user, read from the server, because the roles of the logged user do not
     *                    carry them
     * @param onManageRoles is run when the user presses <em>Manage roles</em>; the button is only shown to a user
     *                      with the permission {@code MANAGE_ROLES}, and not at all if this is {@code null}
     */
    public IU_Welcome(UserAccount user, Runnable onLogout, Runnable onViewAudit, List<Permission> permissions,
            Runnable onManageRoles) {
        this(user, onLogout, onViewAudit, permissions, onManageRoles, null);
    }

    /**
     * Creates the welcome screen with the button <em>Configure system</em> of UC17.
     *
     * @param user the logged user
     * @param onLogout is run when the user presses <em>Log out</em>
     * @param onViewAudit is run when the user presses <em>View audit log</em> (see the other constructor)
     * @param permissions the permissions of the user, read from the server, because the roles of the logged user do not
     *                    carry them
     * @param onManageRoles is run when the user presses <em>Manage roles</em> (see the other constructor)
     * @param onConfigureSystem is run when the user presses <em>Configure system</em>; the button is only shown to a
     *                          user with the permission {@code CONFIGURE_SYSTEM}, and not at all if this is
     *                          {@code null}
     */
    public IU_Welcome(UserAccount user, Runnable onLogout, Runnable onViewAudit, List<Permission> permissions,
            Runnable onManageRoles, Runnable onConfigureSystem) {
        this(user, onLogout, onViewAudit, permissions, onManageRoles, onConfigureSystem, null);
    }

    /**
     * Creates the welcome screen with the button <em>Configure integrations</em> of UC20.
     *
     * @param user the logged user
     * @param onLogout is run when the user presses <em>Log out</em>
     * @param onViewAudit is run when the user presses <em>View audit log</em> (see the other constructor)
     * @param permissions the permissions of the user, read from the server, because the roles of the logged user do not
     *                    carry them
     * @param onManageRoles is run when the user presses <em>Manage roles</em> (see the other constructor)
     * @param onConfigureSystem is run when the user presses <em>Configure system</em> (see the other constructor)
     * @param onConfigureIntegrations is run when the user presses <em>Configure integrations</em>; the button is only
     *                                shown to a user with the permission {@code CONFIGURE_INTEGRATIONS}, and not at
     *                                all if this is {@code null}
     */
    public IU_Welcome(UserAccount user, Runnable onLogout, Runnable onViewAudit, List<Permission> permissions,
            Runnable onManageRoles, Runnable onConfigureSystem, Runnable onConfigureIntegrations) {
        this(user, onLogout, onViewAudit, permissions, onManageRoles, onConfigureSystem, onConfigureIntegrations, null);
    }

    /**
     * Creates the welcome screen with the button <em>Manage patients</em> of UC12.
     *
     * @param user the logged user
     * @param onLogout is run when the user presses <em>Log out</em>
     * @param onViewAudit is run when the user presses <em>View audit log</em> (see the other constructor)
     * @param permissions the permissions of the user, read from the server, because the roles of the logged user do not
     *                    carry them
     * @param onManageRoles is run when the user presses <em>Manage roles</em> (see the other constructor)
     * @param onConfigureSystem is run when the user presses <em>Configure system</em> (see the other constructors)
     * @param onConfigureIntegrations is run when the user presses <em>Configure integrations</em> (see the other
     *                                constructors)
     * @param onManagePatients is run when the user presses <em>Manage patients</em>; the button is only shown to a user
     *                         with the permission {@code MANAGE_PATIENTS}, and not at all if this is {@code null}
     */
    public IU_Welcome(UserAccount user, Runnable onLogout, Runnable onViewAudit, List<Permission> permissions,
            Runnable onManageRoles, Runnable onConfigureSystem, Runnable onConfigureIntegrations,
            Runnable onManagePatients) {
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
        if (onConfigureSystem != null && hasPermission(permissions, CONFIGURE_SYSTEM)) {
            JButton btnConfigureSystem = new JButton("Configure system");
            btnConfigureSystem.setMnemonic('C');
            btnConfigureSystem.addActionListener(event -> onConfigureSystem.run());
            addButton(btnConfigureSystem, 2);
        }
        if (onConfigureIntegrations != null && hasPermission(permissions, CONFIGURE_INTEGRATIONS)) {
            JButton btnConfigureIntegrations = new JButton("Configure integrations");
            btnConfigureIntegrations.setMnemonic('I');
            btnConfigureIntegrations.addActionListener(event -> onConfigureIntegrations.run());
            addButton(btnConfigureIntegrations, 3);
        }
        if (onManagePatients != null && hasPermission(permissions, MANAGE_PATIENTS)) {
            JButton btnManagePatients = new JButton("Manage patients");
            btnManagePatients.setMnemonic('P');
            btnManagePatients.addActionListener(event -> onManagePatients.run());
            addButton(btnManagePatients, 4);
        }
        addButton(btnLogout, 5);
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
