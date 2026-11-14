package es.uclm.esi.iso2.client.configuration.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * The tab of the test types (the examination catalogue) of the configuration screen (UC17). It shows the test types in
 * a table, with the form of the selected one at the right, and creates, changes and deactivates them through the
 * server. The priority is chosen from the list of active priorities. The messages are not shown here: they go to the
 * screen that owns the panel.
 */
class TestTypePanel extends JPanel {
    private static final String ACTIVE = "active";
    private static final String INACTIVE = "inactive";

    private final transient ISystemParameterService service;
    private final String requester;
    private final transient Predicate<String> confirm;
    private final transient Consumer<String> onSuccess;
    private final transient Consumer<String> onError;
    private final transient Runnable onAccessDenied;

    private final DefaultTableModel tableModel = new DefaultTableModel(new String[] {"Id", "Name", "State"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtId = new JTextField(16);
    private final JTextField txtName = new JTextField(16);
    private final JTextField txtModality = new JTextField(16);
    private final JTextField txtRoom = new JTextField(16);
    private final JTextField txtDuration = new JTextField(16);
    private final JComboBox<Priority> cmbPriority = new JComboBox<>();
    private final JTextField txtPreparation = new JTextField(20);
    private final JTextField txtSafetyInfo = new JTextField(20);
    private final JCheckBox chkContrast = new JCheckBox("Requires contrast");
    private final JCheckBox chkConsent = new JCheckBox("Requires consent");
    private final JCheckBox chkFollowUp = new JCheckBox("Eligible for follow-up");
    private final JButton btnNew = new JButton("New");
    private final JButton btnSave = new JButton("Save");
    private final JButton btnDeactivate = new JButton("Deactivate");

    private transient List<TestType> entries = new ArrayList<>();
    private transient List<Priority> priorities = new ArrayList<>();
    private boolean denied;

    /**
     * Creates the panel and reads the test types and the priorities from the server.
     *
     * @param service the service of the parameters of the system
     * @param requester the username of the logged user
     * @param confirm asks the question of a deactivation and returns {@code true} if the user accepts it
     * @param onSuccess receives the message of a change that was made
     * @param onError receives the message of a change that was rejected
     * @param onAccessDenied is run when the server says that the user does not have the permission
     */
    TestTypePanel(ISystemParameterService service, String requester, Predicate<String> confirm,
            Consumer<String> onSuccess, Consumer<String> onError, Runnable onAccessDenied) {
        super(new BorderLayout(16, 0));
        this.service = service;
        this.requester = requester;
        this.confirm = confirm;
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.onAccessDenied = onAccessDenied;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new ParameterGroupPanel.InactiveGreyRenderer());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelected();
            }
        });
        cmbPriority.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                    boolean focused) {
                return super.getListCellRendererComponent(list, value == null ? " " : ((Priority) value).getName(),
                        index, selected, focused);
            }
        });
        btnNew.setMnemonic('N');
        btnSave.setMnemonic('S');
        btnDeactivate.setMnemonic('D');
        btnNew.addActionListener(event -> startNew());
        btnSave.addActionListener(event -> save());
        btnDeactivate.addActionListener(event -> deactivate());

        add(buildList(), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.EAST);
        readPriorities();
        load(null);
    }

    /** Reads the test types and the priorities again from the server and keeps the selected test type. */
    void reload() {
        readPriorities();
        load(selectedId());
    }

    /** Reads the priorities again, for example after a change in the tab of the priorities, and keeps the form. */
    void refreshPriorities() {
        Priority chosen = (Priority) cmbPriority.getSelectedItem();
        readPriorities();
        showPriorities(chosen == null ? null : chosen.getId());
    }

    /** Disables every control: the user does not have the permission any more. */
    void disableAll() {
        denied = true;
        updateControls();
    }

    private void readPriorities() {
        try {
            priorities = new ArrayList<>(service.getPriorities(requester));
        } catch (AccessDeniedException exception) {
            onAccessDenied.run();
        }
    }

    private void load(String idToSelect) {
        try {
            entries = new ArrayList<>(service.getTestTypes(requester));
        } catch (AccessDeniedException exception) {
            onAccessDenied.run();
            return;
        }
        tableModel.setRowCount(0);
        for (TestType entry : entries) {
            tableModel.addRow(new Object[] {entry.getId(), entry.getName(), entry.isActive() ? ACTIVE : INACTIVE});
        }
        int row = indexOf(idToSelect);
        if (row >= 0) {
            table.setRowSelectionInterval(row, row);
        } else {
            table.clearSelection();
        }
        showSelected();
    }

    private void showSelected() {
        TestType selected = selectedEntry();
        txtId.setText(selected == null ? "" : selected.getId());
        txtName.setText(selected == null ? "" : selected.getName());
        txtModality.setText(selected == null ? "" : selected.getModality());
        txtRoom.setText(selected == null ? "" : selected.getRoom());
        txtDuration.setText(selected == null ? "" : String.valueOf(selected.getDurationMinutes()));
        txtPreparation.setText(selected == null ? "" : selected.getPreparation());
        txtSafetyInfo.setText(selected == null ? "" : selected.getSafetyInfo());
        chkContrast.setSelected(selected != null && selected.isRequiresContrast());
        chkConsent.setSelected(selected != null && selected.isRequiresConsent());
        chkFollowUp.setSelected(selected != null && selected.isFollowUpEligible());
        showPriorities(selected == null ? null : selected.getPriority());
        updateControls();
    }

    /** Offers the active priorities, plus the one that the test type has even if it was deactivated. */
    private void showPriorities(String idToSelect) {
        List<Priority> items = new ArrayList<>(priorities.stream().filter(Priority::isActive).toList());
        if (idToSelect != null && !idToSelect.isBlank() && items.stream().noneMatch(p -> p.getId().equals(idToSelect))) {
            items.add(priorities.stream().filter(p -> p.getId().equals(idToSelect)).findFirst()
                    .orElse(new Priority(idToSelect, idToSelect, "", false)));
        }
        cmbPriority.setModel(new DefaultComboBoxModel<>(items.toArray(new Priority[0])));
        cmbPriority.setSelectedIndex(-1);
        items.stream().filter(p -> p.getId().equals(idToSelect)).findFirst().ifPresent(cmbPriority::setSelectedItem);
    }

    /** The id can only be written for a new test type, and a deactivated one can only be read. */
    private void updateControls() {
        TestType selected = selectedEntry();
        boolean editable = !denied && (selected == null || selected.isActive());
        txtId.setEnabled(!denied && selected == null);
        for (Component field : new Component[] {txtName, txtModality, txtRoom, txtDuration, cmbPriority,
                txtPreparation, txtSafetyInfo, chkContrast, chkConsent, chkFollowUp}) {
            field.setEnabled(editable);
        }
        btnNew.setEnabled(!denied);
        btnSave.setEnabled(editable);
        btnDeactivate.setEnabled(!denied && selected != null && selected.isActive());
    }

    private void startNew() {
        table.clearSelection();
        showSelected();
        txtId.requestFocusInWindow();
    }

    private void save() {
        int duration;
        try {
            duration = Integer.parseInt(txtDuration.getText().trim());
        } catch (NumberFormatException exception) {
            onError.accept("The duration must be a whole number of minutes.");
            txtDuration.requestFocusInWindow();
            return;
        }
        TestType selected = selectedEntry();
        String id = selected == null ? txtId.getText().trim() : selected.getId();
        Priority priority = (Priority) cmbPriority.getSelectedItem();
        TestType testType = new TestType(id, txtName.getText().trim(), txtModality.getText().trim(),
                txtRoom.getText().trim(), duration, txtPreparation.getText().trim(), chkContrast.isSelected(),
                chkConsent.isSelected(), txtSafetyInfo.getText().trim(), priority == null ? "" : priority.getId(),
                chkFollowUp.isSelected(), true);
        try {
            if (selected == null) {
                service.createTestType(requester, testType);
            } else {
                service.updateTestType(requester, id, testType);
            }
            onSuccess.accept("Test type " + id + " saved.");
            load(id);
        } catch (AccessDeniedException exception) {
            onAccessDenied.run();
        } catch (ValidationException exception) {
            onError.accept(joined(exception));
        }
    }

    private void deactivate() {
        TestType selected = selectedEntry();
        if (selected == null || !confirm.test("Deactivate the test type " + selected.getId() + "?")) {
            return;
        }
        try {
            service.deactivateTestType(requester, selected.getId());
            onSuccess.accept("Test type " + selected.getId() + " deactivated.");
            load(selected.getId());
        } catch (AccessDeniedException exception) {
            onAccessDenied.run();
        } catch (ValidationException exception) {
            onError.accept(joined(exception));
            load(null);
        }
    }

    private static String joined(ValidationException exception) {
        return exception.getErrors().stream().map(error -> error + ".").collect(Collectors.joining(" "));
    }

    private TestType selectedEntry() {
        int row = table.getSelectedRow();
        return row < 0 || row >= entries.size() ? null : entries.get(row);
    }

    private String selectedId() {
        TestType selected = selectedEntry();
        return selected == null ? null : selected.getId();
    }

    private int indexOf(String id) {
        for (int index = 0; id != null && index < entries.size(); index++) {
            if (entries.get(index).getId().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private JPanel buildList() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(300, 300));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        buttons.add(btnNew);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    /** Two columns, each label right above its field, and the buttons at the bottom right. */
    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        addField(panel, "Id", txtId, 0, 0, 'I');
        addField(panel, "Name", txtName, 0, 2, 'M');
        addField(panel, "Modality", txtModality, 0, 4, 'L');
        addField(panel, "Room", txtRoom, 0, 6, 'O');
        addField(panel, "Duration (minutes)", txtDuration, 0, 8, 'U');
        addField(panel, "Priority", cmbPriority, 0, 10, 'Y');
        addField(panel, "Preparation", txtPreparation, 1, 0, 'E');
        addField(panel, "Safety information", txtSafetyInfo, 1, 2, 'F');
        chkContrast.setMnemonic('C');
        chkConsent.setMnemonic('Q');
        chkFollowUp.setMnemonic('W');
        addCheck(panel, chkContrast, 4);
        addCheck(panel, chkConsent, 5);
        addCheck(panel, chkFollowUp, 6);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        buttons.add(btnSave);
        buttons.add(btnDeactivate);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 10;
        constraints.gridheight = 2;
        constraints.anchor = GridBagConstraints.LAST_LINE_END;
        panel.add(buttons, constraints);
        return panel;
    }

    private static void addField(JPanel panel, String text, Component field, int column, int row, char mnemonic) {
        JLabel label = new JLabel(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(field);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.insets = new Insets(row == 0 ? 0 : 12, column == 0 ? 0 : 24, 0, 0);
        panel.add(label, constraints);
        constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row + 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(4, column == 0 ? 0 : 24, 0, 0);
        panel.add(field, constraints);
    }

    private static void addCheck(JPanel panel, JCheckBox check, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.insets = new Insets(12, 24, 0, 0);
        panel.add(check, constraints);
    }
}
