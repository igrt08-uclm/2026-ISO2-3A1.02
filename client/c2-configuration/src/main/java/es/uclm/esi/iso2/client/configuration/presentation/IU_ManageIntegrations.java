package es.uclm.esi.iso2.client.configuration.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.configuration.contracts.IIntegrationManagementService;
import es.uclm.esi.iso2.common.configuration.domain.ConnectionTestResult;
import es.uclm.esi.iso2.common.configuration.domain.HospitalSystemType;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationConfiguration;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationMode;
import es.uclm.esi.iso2.common.configuration.domain.IntegrationStatus;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Screen of UC20: the administrator sees the four integrations with the external hospital systems (SSO, electronic
 * health record, admissions and RIS/PACS), with their mode, whether they are enabled and their status, and edits the
 * one that is selected in the form below the table. Every operation goes to the server with the username of the
 * requester, and the server checks the permission {@code CONFIGURE_INTEGRATIONS}.
 */
public class IU_ManageIntegrations extends JPanel {
    private static final Color ERROR_COLOR = new Color(0xB00020);
    private static final String ERROR_ICON = "⚠ ";
    private static final Color SUCCESS_COLOR = new Color(0x1B5E20);
    private static final String SUCCESS_ICON = "✔ ";
    /** Dark amber, readable on white: the colour of the word SIMULATED (it is always a word, not only a colour). */
    private static final Color SIMULATED_COLOR = new Color(0x8A5A00);
    private static final String SIMULATED_LINE = "SIMULATED: no real system is contacted";
    private static final String UNSAVED_LINE = "Unsaved changes: save or revert to test the connection";
    private static final int MODE_COLUMN = 1;

    private final transient IIntegrationManagementService service;
    private final String requester;
    private final transient Predicate<String> confirm;

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[] {"System", "Mode", "Enabled", "Status"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtEndpoint = new JTextField(24);
    private final JTextArea txtSettings = new JTextArea(5, 24);
    private final JCheckBox chkEnabled = new JCheckBox("Enabled");
    private final JRadioButton rbSimulated = new JRadioButton("Simulated");
    private final JRadioButton rbReal = new JRadioButton("Real");
    private final JLabel lblSimulated = new JLabel(" ");
    private final JLabel lblUnsaved = new JLabel(" ");
    private final JLabel lblStatus = new JLabel(" ");
    private final JButton btnRevert = new JButton("Revert");
    private final JButton btnTest = new JButton("Test connection");
    private final JButton btnSave = new JButton("Save");
    private final JButton btnBack = new JButton("Back");
    private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));

    private transient List<IntegrationConfiguration> entries = new ArrayList<>();
    private boolean denied;
    private boolean loading;
    private boolean restoring;
    private int shownRow = -1;

    /**
     * Creates the screen and reads the integrations from the server.
     *
     * @param service the service of the integrations
     * @param requester the username of the logged user
     * @param confirm asks a question and returns {@code true} if the user accepts it; the tests pass an answer, so
     *        that they do not open a window
     * @param onBack is run when the user presses <em>Back</em>
     */
    public IU_ManageIntegrations(IIntegrationManagementService service, String requester,
            Predicate<String> confirm, Runnable onBack) {
        super(new BorderLayout(0, 12));
        this.service = service;
        this.requester = requester;
        this.confirm = confirm;
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel lblTitle = new JLabel("Integrations with other hospital systems");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 18f));
        lblSimulated.setFont(lblSimulated.getFont().deriveFont(Font.BOLD));
        lblSimulated.setForeground(SIMULATED_COLOR);
        btnRevert.setMnemonic('R');
        btnRevert.addActionListener(event -> {
            lblStatus.setText(" ");
            showSelected();
        });
        btnTest.setMnemonic('T');
        btnTest.addActionListener(event -> onTestConnection());
        btnSave.setMnemonic('S');
        btnSave.addActionListener(event -> onSaveConfiguration());
        btnBack.setMnemonic('B');
        btnBack.addActionListener(event -> onBack.run());

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new SimulatedRenderer());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !loading && !restoring && !denied) {
                changeRow();
            }
        });
        ButtonGroup modes = new ButtonGroup();
        modes.add(rbSimulated);
        modes.add(rbReal);
        rbSimulated.addActionListener(event -> formChanged());
        rbReal.addActionListener(event -> formChanged());
        chkEnabled.addActionListener(event -> updateControls());
        txtEndpoint.getDocument().addDocumentListener(onChange(this::updateControls));
        txtSettings.getDocument().addDocumentListener(onChange(this::updateControls));
        chkEnabled.setMnemonic('N');
        txtSettings.setLineWrap(false);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(620, 110));
        center.add(tableScroll, BorderLayout.NORTH);
        center.add(buildForm(), BorderLayout.CENTER);
        add(lblTitle, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        load(null);
    }

    /** Shows the integrations in the table, in the order they come. */
    public void displayIntegrations(List<IntegrationConfiguration> integrations) {
        entries = new ArrayList<>(integrations);
        tableModel.setRowCount(0);
        for (IntegrationConfiguration integration : entries) {
            tableModel.addRow(new Object[] {systemName(integration.getSystem()), integration.getMode().name(),
                    integration.isEnabled() ? "Yes" : "No", statusName(integration.getStatus())});
        }
    }

    /** Shows the message of a change that was made, in green. */
    public void showOperationResultSuccess(String message) {
        lblStatus.setForeground(SUCCESS_COLOR);
        lblStatus.setText(SUCCESS_ICON + message);
    }

    /** Shows the message of a change that was rejected, or of a connection that failed, in red. */
    public void showOperationResultError(String message) {
        lblStatus.setForeground(ERROR_COLOR);
        lblStatus.setText(ERROR_ICON + message);
    }

    /** Reads the integrations again from the server and selects the one of that system, or the first one. */
    private void load(HospitalSystemType toSelect) {
        List<IntegrationConfiguration> integrations;
        try {
            integrations = service.getIntegrations(requester);
        } catch (AccessDeniedException exception) {
            accessDenied();
            return;
        }
        loading = true;
        try {
            displayIntegrations(integrations);
            int row = indexOf(toSelect);
            if (row < 0 && !entries.isEmpty()) {
                row = 0;
            }
            if (row >= 0) {
                table.setRowSelectionInterval(row, row);
            } else {
                table.clearSelection();
            }
        } finally {
            loading = false;
        }
        shownRow = table.getSelectedRow();
        showSelected();
    }

    /**
     * The user pressed another row. If the form has unsaved changes, it asks whether to discard them; if not, the
     * selection goes back to the row it had, with its changes.
     */
    private void changeRow() {
        int newRow = table.getSelectedRow();
        if (newRow != shownRow && shownRow >= 0 && shownRow < entries.size()
                && isDirty(entries.get(shownRow)) && !confirm.test("Discard the unsaved changes?")) {
            restoring = true;
            try {
                table.setRowSelectionInterval(shownRow, shownRow);
            } finally {
                restoring = false;
            }
            return;
        }
        shownRow = newRow;
        showSelected();
    }

    /** Tells whether the form differs from the saved configuration of the integration. */
    private boolean isDirty(IntegrationConfiguration saved) {
        return !txtEndpoint.getText().strip().equals(saved.getEndpoint() == null ? "" : saved.getEndpoint())
                || !txtSettings.getText().strip().equals(formatSettings(saved.getSettings()).strip())
                || chkEnabled.isSelected() != saved.isEnabled()
                || rbReal.isSelected() != (saved.getMode() == IntegrationMode.REAL);
    }

    private void formChanged() {
        updateSimulatedLine();
        updateControls();
    }

    private static DocumentListener onChange(Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                action.run();
            }
        };
    }

    /** Tests the connection of the selected integration, as it is saved, and shows the result. */
    private void onTestConnection() {
        IntegrationConfiguration selected = selectedEntry();
        if (selected == null) {
            return;
        }
        try {
            showTestResult(service.testConnection(requester, selected.getSystem()));
        } catch (AccessDeniedException exception) {
            accessDenied();
        }
    }

    /** Shows the result of the connection test of the selected integration: green if it answered, red if not. */
    public void showTestResult(ConnectionTestResult result) {
        IntegrationConfiguration selected = selectedEntry();
        String prefix = "Connection test of " + (selected == null ? "the system" : systemName(selected.getSystem()))
                + ": ";
        if (result.isSuccess()) {
            showOperationResultSuccess(prefix + result.getMessage() + " (" + result.getResponseTimeMs() + " ms).");
        } else {
            showOperationResultError(prefix + result.getMessage() + ".");
        }
    }

    /** The server says that the user does not have the permission: nothing can be done but go back. */
    private void accessDenied() {
        denied = true;
        showOperationResultError("You do not have permission to configure the integrations.");
        updateControls();
    }

    private void showSelected() {
        IntegrationConfiguration selected = selectedEntry();
        txtEndpoint.setText(selected == null ? "" : selected.getEndpoint());
        txtSettings.setText(selected == null ? "" : formatSettings(selected.getSettings()));
        chkEnabled.setSelected(selected != null && selected.isEnabled());
        boolean real = selected != null && selected.getMode() == IntegrationMode.REAL;
        rbReal.setSelected(real);
        rbSimulated.setSelected(!real);
        updateSimulatedLine();
        updateControls();
    }

    /** The line above the form says it clearly when the mode is simulated, so that nobody mistakes it for a real one. */
    private void updateSimulatedLine() {
        lblSimulated.setText(selectedEntry() != null && rbSimulated.isSelected() ? SIMULATED_LINE : " ");
    }

    private void updateControls() {
        boolean editable = !denied && selectedEntry() != null;
        table.setEnabled(!denied);
        txtEndpoint.setEnabled(editable);
        txtSettings.setEnabled(editable);
        chkEnabled.setEnabled(editable);
        rbSimulated.setEnabled(editable);
        rbReal.setEnabled(editable);
        btnSave.setEnabled(editable);
        boolean changed = editable && isDirty(selectedEntry());
        btnTest.setEnabled(editable && !changed);
        btnRevert.setEnabled(changed);
        lblUnsaved.setText(changed ? UNSAVED_LINE : " ");
    }

    /**
     * Checks the form with the same rules as the server, asks to confirm a delicate change and saves the configuration of
     * the selected integration. If anything is wrong, every error is shown together and nothing is sent.
     */
    private void onSaveConfiguration() {
        IntegrationConfiguration selected = selectedEntry();
        if (selected == null) {
            return;
        }
        String endpoint = txtEndpoint.getText().strip();
        IntegrationMode mode = rbReal.isSelected() ? IntegrationMode.REAL : IntegrationMode.SIMULATED;
        List<String> errors = new ArrayList<>();
        if (endpoint.isEmpty() && mode == IntegrationMode.REAL) {
            errors.add("The endpoint is required in real mode");
        } else if (!endpoint.isEmpty() && !endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            errors.add("The endpoint must start with http:// or https://");
        }
        Map<String, String> settings = readSettings(errors);
        if (!errors.isEmpty()) {
            showOperationResultError(joined(errors));
            return;
        }
        String question = confirmationQuestion(selected, mode, chkEnabled.isSelected());
        if (question != null && !confirm.test(question)) {
            lblStatus.setText(" ");
            return;
        }
        try {
            service.configureIntegration(requester, new IntegrationConfiguration(selected.getSystem(), endpoint,
                    settings, chkEnabled.isSelected(), mode, selected.getStatus()));
            showOperationResultSuccess("Integration " + systemName(selected.getSystem()) + " saved.");
            load(selected.getSystem());
        } catch (AccessDeniedException exception) {
            accessDenied();
        } catch (ValidationException exception) {
            showOperationResultError(joined(exception.getErrors()));
        }
    }

    /**
     * Reads the text of the settings: one {@code key=value} per line, blank lines are ignored, and the key and the
     * value lose their spaces. A line without {@code =}, with an empty key or with an empty value, and a key that is
     * repeated, are added to {@code errors}. The order of the lines is kept.
     */
    private Map<String, String> readSettings(List<String> errors) {
        Map<String, String> settings = new LinkedHashMap<>();
        String[] lines = txtSettings.getText().split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].strip();
            if (line.isEmpty()) {
                continue;
            }
            int separator = line.indexOf('=');
            String key = separator < 0 ? "" : line.substring(0, separator).strip();
            String value = separator < 0 ? "" : line.substring(separator + 1).strip();
            if (key.isEmpty() || value.isEmpty()) {
                errors.add("Line " + (index + 1) + ": use key=value");
            } else if (settings.containsKey(key)) {
                errors.add("The setting " + key + " is repeated");
            } else {
                settings.put(key, value);
            }
        }
        return settings;
    }

    /** The question to accept before disabling an integration or switching it to real mode, or {@code null}. */
    private static String confirmationQuestion(IntegrationConfiguration saved, IntegrationMode newMode,
            boolean newEnabled) {
        String name = systemName(saved.getSystem());
        boolean disabling = saved.isEnabled() && !newEnabled;
        boolean toReal = saved.getMode() != IntegrationMode.REAL && newMode == IntegrationMode.REAL;
        if (disabling && toReal) {
            return "Disable the integration with " + name + " and switch it to real mode?";
        }
        if (disabling) {
            return "Disable the integration with " + name + "?";
        }
        return toReal ? "Switch " + name + " to real mode?" : null;
    }

    private static String joined(List<String> errors) {
        return errors.stream().map(error -> error + ".").collect(Collectors.joining(" "));
    }

    private IntegrationConfiguration selectedEntry() {
        int row = table.getSelectedRow();
        return row < 0 || row >= entries.size() ? null : entries.get(row);
    }

    private int indexOf(HospitalSystemType system) {
        for (int index = 0; system != null && index < entries.size(); index++) {
            if (entries.get(index).getSystem() == system) {
                return index;
            }
        }
        return -1;
    }

    /** One {@code key=value} per line, in the order of the map. */
    private static String formatSettings(Map<String, String> settings) {
        return settings == null ? "" : settings.entrySet().stream()
                .map(setting -> setting.getKey() + "=" + setting.getValue()).collect(Collectors.joining("\n"));
    }

    private static String systemName(HospitalSystemType system) {
        return switch (system) {
            case SSO -> "Single sign-on (SSO)";
            case EHR -> "Electronic health record (EHR)";
            case ADMISSIONS -> "Admissions";
            case RIS_PACS -> "RIS/PACS";
        };
    }

    private static String statusName(IntegrationStatus status) {
        return switch (status) {
            case ACTIVE -> "Active";
            case INACTIVE -> "Inactive";
            case ERROR -> "Error";
        };
    }

    /** The mode, the settings and whether it is enabled, with each label right above its control. */
    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        addAt(panel, lblSimulated, 0, 0, 2, 0);
        addLabel(panel, "Endpoint (required in real mode)", txtEndpoint, 'E', 0, 1);
        addAt(panel, txtEndpoint, 0, 2, 1, 4);
        JLabel lblMode = new JLabel("Mode");
        lblMode.setDisplayedMnemonic('M');
        lblMode.setLabelFor(rbSimulated);
        addAt(panel, lblMode, 0, 3, 1, 12);
        JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        modes.add(rbSimulated);
        modes.add(rbReal);
        addAt(panel, modes, 0, 4, 1, 4);
        addAt(panel, chkEnabled, 0, 5, 1, 12);
        addLabel(panel, "Settings (key=value, one per line)", txtSettings, 'G', 1, 1);
        addAt(panel, new JScrollPane(txtSettings), 1, 2, 1, 4, 3);
        buttons.add(btnRevert);
        buttons.add(btnTest);
        buttons.add(btnSave);
        addAt(panel, lblUnsaved, 0, 6, 1, 12);
        addAt(panel, buttons, 1, 5, 1, 12);
        return panel;
    }

    private static void addLabel(JPanel panel, String text, Component field, char mnemonic, int column, int row) {
        JLabel label = new JLabel(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(field);
        addAt(panel, label, column, row, 1, row == 1 ? 12 : 0);
    }

    private static void addAt(JPanel panel, Component component, int column, int row, int width, int topSpace) {
        addAt(panel, component, column, row, width, topSpace, 1);
    }

    private static void addAt(JPanel panel, Component component, int column, int row, int width, int topSpace,
            int height) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.gridwidth = width;
        constraints.gridheight = height;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.insets = new Insets(topSpace, column == 0 ? 0 : 24, 0, 0);
        panel.add(component, constraints);
    }

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(lblStatus, BorderLayout.WEST);
        panel.add(btnBack, BorderLayout.EAST);
        return panel;
    }

    /** Shows the word SIMULATED in bold and amber in the column of the mode, so that it is not mistaken for REAL. */
    private static class SimulatedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            boolean simulated = column == MODE_COLUMN && IntegrationMode.SIMULATED.name().equals(value);
            cell.setFont(cell.getFont().deriveFont(simulated ? Font.BOLD : Font.PLAIN));
            if (!selected) {
                cell.setForeground(simulated ? SIMULATED_COLOR : table.getForeground());
            }
            return cell;
        }
    }
}
