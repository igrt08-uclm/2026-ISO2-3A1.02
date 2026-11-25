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
 * Displays the welcome screen with the options available for the logged user. Each button is only shown to a user
 * with its permission; for example, View audit log only to a user with {@code CONSULT_AUDIT}.
 */
public class IU_Welcome extends JPanel {
    private static final String CONSULT_AUDIT = "CONSULT_AUDIT";
    private static final String MANAGE_ROLES = "MANAGE_ROLES";
    private static final String CONFIGURE_SYSTEM = "CONFIGURE_SYSTEM";
    private static final String CONFIGURE_INTEGRATIONS = "CONFIGURE_INTEGRATIONS";
    private static final String MANAGE_PATIENTS = "MANAGE_PATIENTS";
    private static final String ORDER_PROCEDURES = "ORDER_PROCEDURES";
    private static final String MANAGE_APPOINTMENTS = "MANAGE_APPOINTMENTS";
    private static final int COLUMNS = 10;

    /**
     * Creates the welcome screen with the button <em>Log out</em>. It passes no action and an empty list of
     * permissions, so it shows no other button.
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
     * @param onViewAudit is run when the user presses <em>View audit log</em>; the button is only shown to a user
     *                    with the permission {@code CONSULT_AUDIT}, and not at all if this is {@code null}. This
     *                    constructor passes an empty list of permissions, so it never shows the button.
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
        this(user, onLogout, onViewAudit, permissions, onManageRoles, onConfigureSystem, onConfigureIntegrations,
                onManagePatients, null);
    }

    /**
     * Creates the welcome screen with the button <em>Order procedure</em> of UC06.
     *
     * @param user the logged user
     * @param onLogout is run when the user presses <em>Log out</em>
     * @param onViewAudit is run when the user presses <em>View audit log</em> (see the other constructors)
     * @param permissions the permissions of the user, read from the server, because the roles of the logged user do not
     *                    carry them
     * @param onManageRoles is run when the user presses <em>Manage roles</em> (see the other constructors)
     * @param onConfigureSystem is run when the user presses <em>Configure system</em> (see the other constructors)
     * @param onConfigureIntegrations is run when the user presses <em>Configure integrations</em> (see the other
     *                                constructors)
     * @param onManagePatients is run when the user presses <em>Manage patients</em> (see the other constructors)
     * @param onOrderProcedure is run when the user presses <em>Order procedure</em>; the button is only shown to a
     *                         user with the permission {@code ORDER_PROCEDURES}, and not at all if this is
     *                         {@code null}
     */
    public IU_Welcome(UserAccount user, Runnable onLogout, Runnable onViewAudit, List<Permission> permissions,
            Runnable onManageRoles, Runnable onConfigureSystem, Runnable onConfigureIntegrations,
            Runnable onManagePatients, Runnable onOrderProcedure) {
        this(user, onLogout, onViewAudit, permissions, onManageRoles, onConfigureSystem, onConfigureIntegrations,
                onManagePatients, onOrderProcedure, null);
    }

    /**
     * Creates the welcome screen with the button <em>Order follow-up</em> of UC07.
     *
     * @param user the logged user
     * @param onLogout is run when the user presses <em>Log out</em>
     * @param onViewAudit is run when the user presses <em>View audit log</em> (see the other constructors)
     * @param permissions the permissions of the user, read from the server, because the roles of the logged user do not
     *                    carry them
     * @param onManageRoles is run when the user presses <em>Manage roles</em> (see the other constructors)
     * @param onConfigureSystem is run when the user presses <em>Configure system</em> (see the other constructors)
     * @param onConfigureIntegrations is run when the user presses <em>Configure integrations</em> (see the other
     *                                constructors)
     * @param onManagePatients is run when the user presses <em>Manage patients</em> (see the other constructors)
     * @param onOrderProcedure is run when the user presses <em>Order procedure</em> (see the other constructor)
     * @param onOrderFollowUp is run when the user presses <em>Order follow-up</em>; the button is only shown to a user
     *                        with the permission {@code ORDER_PROCEDURES}, and not at all if this is {@code null}
     */
    public IU_Welcome(UserAccount user, Runnable onLogout, Runnable onViewAudit, List<Permission> permissions,
            Runnable onManageRoles, Runnable onConfigureSystem, Runnable onConfigureIntegrations,
            Runnable onManagePatients, Runnable onOrderProcedure, Runnable onOrderFollowUp) {
        this(user, onLogout, onViewAudit, permissions, onManageRoles, onConfigureSystem, onConfigureIntegrations,
                onManagePatients, onOrderProcedure, onOrderFollowUp, null);
    }

    /**
     * Creates the welcome screen with the button <em>Manage appointments</em> of UC13 (see the other constructor).
     *
     * @param user the logged user
     * @param onLogout is run when the user presses <em>Log out</em>
     * @param onViewAudit is run when the user presses <em>View audit log</em> (see the other constructors)
     * @param permissions the permissions of the user, read from the server
     * @param onManageRoles is run when the user presses <em>Manage roles</em> (see the other constructors)
     * @param onConfigureSystem is run when the user presses <em>Configure system</em> (see the other constructors)
     * @param onConfigureIntegrations is run when the user presses <em>Configure integrations</em> (see the others)
     * @param onManagePatients is run when the user presses <em>Manage patients</em> (see the other constructors)
     * @param onOrderProcedure is run when the user presses <em>Order procedure</em> (see the other constructors)
     * @param onOrderFollowUp is run when the user presses <em>Order follow-up</em> (see the other constructor)
     * @param onManageAppointments is run when the user presses <em>Manage appointments</em> (see the other
     *                             constructor)
     */
    public IU_Welcome(UserAccount user, Runnable onLogout, Runnable onViewAudit, List<Permission> permissions,
            Runnable onManageRoles, Runnable onConfigureSystem, Runnable onConfigureIntegrations,
            Runnable onManagePatients, Runnable onOrderProcedure, Runnable onOrderFollowUp,
            Runnable onManageAppointments) {
        this(user, onLogout, onViewAudit, permissions, onManageRoles, onConfigureSystem, onConfigureIntegrations,
                onManagePatients, onOrderProcedure, onOrderFollowUp, onManageAppointments, null);
    }

    /**
     * Creates the welcome screen with the button <em>Manage appointments</em> of UC13 and <em>Daily schedule</em> of UC14.
     *
     * @param user the logged user
     * @param onLogout is run when the user presses <em>Log out</em>
     * @param onViewAudit is run when the user presses <em>View audit log</em> (see the other constructors)
     * @param permissions the permissions of the user, read from the server, because the roles of the logged user do not
     *                    carry them
     * @param onManageRoles is run when the user presses <em>Manage roles</em> (see the other constructors)
     * @param onConfigureSystem is run when the user presses <em>Configure system</em> (see the other constructors)
     * @param onConfigureIntegrations is run when the user presses <em>Configure integrations</em> (see the other
     *                                constructors)
     * @param onManagePatients is run when the user presses <em>Manage patients</em> (see the other constructors)
     * @param onOrderProcedure is run when the user presses <em>Order procedure</em> (see the other constructors)
     * @param onOrderFollowUp is run when the user presses <em>Order follow-up</em> (see the other constructor)
     * @param onManageAppointments is run when the user presses <em>Manage appointments</em>; the button is only shown
     *                             to a user with the permission {@code MANAGE_APPOINTMENTS}, and not at all if this is
     *                             {@code null}
     * @param onDailySchedule is run when the user presses <em>Daily schedule</em>; the button is only
     *                        shown to a user with the permission {@code MANAGE_APPOINTMENTS}, and not at
     *                        all if this is {@code null}
     */
    public IU_Welcome(UserAccount user, Runnable onLogout, Runnable onViewAudit, List<Permission> permissions,
            Runnable onManageRoles, Runnable onConfigureSystem, Runnable onConfigureIntegrations,
            Runnable onManagePatients, Runnable onOrderProcedure, Runnable onOrderFollowUp,
            Runnable onManageAppointments, Runnable onDailySchedule) {
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
        if (onViewAudit != null && hasPermission(permissions, CONSULT_AUDIT)) {
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
        if (onOrderProcedure != null && hasPermission(permissions, ORDER_PROCEDURES)) {
            JButton btnOrderProcedure = new JButton("Order procedure");
            btnOrderProcedure.setMnemonic('O');
            btnOrderProcedure.addActionListener(event -> onOrderProcedure.run());
            addButton(btnOrderProcedure, 5);
        }
        if (onOrderFollowUp != null && hasPermission(permissions, ORDER_PROCEDURES)) {
            JButton btnOrderFollowUp = new JButton("Order follow-up");
            btnOrderFollowUp.setMnemonic('F');
            btnOrderFollowUp.addActionListener(event -> onOrderFollowUp.run());
            addButton(btnOrderFollowUp, 6);
        }
        if (onManageAppointments != null && hasPermission(permissions, MANAGE_APPOINTMENTS)) {
            JButton btnManageAppointments = new JButton("Manage appointments");
            btnManageAppointments.setMnemonic('A');
            btnManageAppointments.addActionListener(event -> onManageAppointments.run());
            addButton(btnManageAppointments, 7);
        }
        if (onDailySchedule != null && hasPermission(permissions, MANAGE_APPOINTMENTS)) {
            JButton btnDailySchedule = new JButton("Daily schedule");
            btnDailySchedule.setMnemonic('D');
            btnDailySchedule.addActionListener(event -> onDailySchedule.run());
            addButton(btnDailySchedule, 8);
        }
        addButton(btnLogout, 9);
    }

    private static boolean hasPermission(List<Permission> permissions, String code) {
        return permissions.stream().anyMatch(permission -> code.equals(permission.getCode()));
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
