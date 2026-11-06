package es.uclm.esi.iso2.client.access.presentation;

import es.uclm.esi.iso2.common.access.contracts.IRoleManagementService;
import es.uclm.esi.iso2.common.access.domain.Permission;
import es.uclm.esi.iso2.common.access.domain.Role;
import es.uclm.esi.iso2.common.access.domain.UserAccount;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

public class IU_ManageRoles extends JPanel {
    private final transient IRoleManagementService roleService;
    private final String requester;

    private final JTabbedPane tabs = new JTabbedPane();

    // Tab "User roles"
    private final JComboBox<UserAccount> cmbUser = new JComboBox<>();
    private final JComboBox<Role> cmbRoleToAssign = new JComboBox<>();
    private final DefaultListModel<Role> userRolesModel = new DefaultListModel<>();
    private final JList<Role> lstUserRoles = new JList<>(userRolesModel);
    private final JButton btnAssignRole = new JButton("Assign role");
    private final JButton btnRevokeRole = new JButton("Revoke selected");

    // Tab "Role permissions"
    private final JComboBox<Role> cmbRole = new JComboBox<>();
    private final JComboBox<Permission> cmbPermissionToAssign = new JComboBox<>();
    private final DefaultListModel<Permission> rolePermissionsModel = new DefaultListModel<>();
    private final JList<Permission> lstRolePermissions = new JList<>(rolePermissionsModel);
    private final JButton btnAssignPermission = new JButton("Assign permission");
    private final JButton btnRevokePermission = new JButton("Revoke selected");

    private final JLabel lblStatus = new JLabel(" ");
    private final JButton btnBack = new JButton("Back");

    public IU_ManageRoles(IRoleManagementService roleService, String requester, Runnable onBack) {
        super(new BorderLayout(0, 12));
        this.roleService = roleService;
        this.requester = requester;
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        Function<UserAccount, String> userText = user -> user.getUsername() + " - " + user.getFullName();
        Function<Role, String> roleText = Role::getName;
        Function<Permission, String> permissionText =
                permission -> permission.getCode() + " - " + permission.getDescription();
        cmbUser.setRenderer(showing(userText));
        cmbRoleToAssign.setRenderer(showing(roleText));
        lstUserRoles.setCellRenderer(showing(roleText));
        cmbRole.setRenderer(showing(roleText));
        cmbPermissionToAssign.setRenderer(showing(permissionText));
        lstRolePermissions.setCellRenderer(showing(permissionText));
        btnRevokeRole.setEnabled(false);
        btnRevokePermission.setEnabled(false);
        btnBack.addActionListener(event -> onBack.run());

        JLabel lblTitle = new JLabel("Manage roles and permissions");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 18f));
        tabs.addTab("User roles", buildTab("User", cmbUser, "Role to assign", cmbRoleToAssign, btnAssignRole,
                "Roles of the user", lstUserRoles, btnRevokeRole));
        tabs.addTab("Role permissions", buildTab("Role", cmbRole, "Permission to assign", cmbPermissionToAssign,
                btnAssignPermission, "Permissions of the role", lstRolePermissions, btnRevokePermission));

        add(lblTitle, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    /** The selection on the left and the current state on the right, each label right above its control. */
    private static JPanel buildTab(String subjectText, Component subject, String toAssignText, Component toAssign,
            JButton assign, String currentText, JList<?> current, JButton revoke) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        current.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        current.setVisibleRowCount(6);
        JScrollPane currentScroll = new JScrollPane(current);
        currentScroll.setPreferredSize(new Dimension(280, 140));

        add(panel, new JLabel(subjectText), 0, 0, 1, 0, GridBagConstraints.HORIZONTAL);
        add(panel, subject, 0, 1, 1, 4, GridBagConstraints.HORIZONTAL);
        add(panel, new JLabel(toAssignText), 0, 2, 1, 16, GridBagConstraints.HORIZONTAL);
        add(panel, toAssign, 0, 3, 1, 4, GridBagConstraints.HORIZONTAL);
        add(panel, leftAligned(assign), 0, 4, 1, 12, GridBagConstraints.HORIZONTAL);

        add(panel, new JLabel(currentText), 1, 0, 1, 0, GridBagConstraints.HORIZONTAL);
        add(panel, currentScroll, 1, 1, 4, 4, GridBagConstraints.BOTH);
        add(panel, rightAligned(revoke), 1, 5, 1, 12, GridBagConstraints.HORIZONTAL);
        return panel;
    }

    private static void add(JPanel panel, Component component, int column, int row, int height, int topSpace,
            int fill) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.gridheight = height;
        constraints.weightx = 1;
        constraints.weighty = fill == GridBagConstraints.BOTH ? 1 : 0;
        constraints.fill = fill;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.insets = new Insets(topSpace, 0, 0, column == 0 ? 24 : 0);
        panel.add(component, constraints);
    }

    private static JPanel leftAligned(JButton button) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        panel.add(button);
        return panel;
    }

    private static JPanel rightAligned(JButton button) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        panel.add(button);
        return panel;
    }

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(lblStatus, BorderLayout.WEST);
        panel.add(btnBack, BorderLayout.EAST);
        return panel;
    }

    /** Shows the names of users, roles and permissions in the lists, not their ids. */
    private static <T> DefaultListCellRenderer showing(Function<T, String> text) {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                    boolean focused) {
                @SuppressWarnings("unchecked")
                T item = (T) value;
                return super.getListCellRendererComponent(list, item == null ? " " : text.apply(item), index,
                        selected, focused);
            }
        };
    }
}
