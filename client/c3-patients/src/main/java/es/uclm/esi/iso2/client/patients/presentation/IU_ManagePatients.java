package es.uclm.esi.iso2.client.patients.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.patients.contracts.IPatientService;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.patients.domain.Sex;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
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
 * Screen of UC12: the Admissions staff search for patients, and register a new patient or modify the demographic data
 * of the one that is selected. The search and the table are at the left and the form at the right. Every operation
 * goes to the server with the username of the requester, and the server checks the permission
 * {@code MANAGE_PATIENTS}.
 */
public class IU_ManagePatients extends JPanel {
    private static final Color ERROR_COLOR = new Color(0xB00020);
    private static final String ERROR_ICON = "⚠ ";
    private static final Color SUCCESS_COLOR = new Color(0x1B5E20);
    private static final String SUCCESS_ICON = "✔ ";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final transient IPatientService service;
    private final String requester;
    private final transient Predicate<String> confirm;

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[] {"Identifier", "Full name", "Date of birth"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtSearch = new JTextField(18);
    private final JButton btnSearch = new JButton("Search");
    private final JLabel lblFound = new JLabel(" ");
    private final JTextField txtIdentifier = new JTextField(20);
    private final JTextField txtNationalId = new JTextField(20);
    private final JTextField txtFullName = new JTextField(20);
    private final JTextField txtBirth = new JTextField(20);
    private final JComboBox<Sex> cmbSex = new JComboBox<>(Sex.values());
    private final JTextField txtAddress = new JTextField(20);
    private final JTextField txtPhone = new JTextField(20);
    private final JTextField txtEmail = new JTextField(20);
    private final JLabel lblStatus = new JLabel(" ");
    private final JButton btnBack = new JButton("Back");

    private transient List<Patient> patients = new ArrayList<>();
    private boolean denied;
    private boolean loading;

    /**
     * Creates the screen and shows all the patients.
     *
     * @param service the service of the patients
     * @param requester the username of the logged user
     * @param confirm asks a question and returns {@code true} if the user accepts it; the tests pass an answer, so
     *        that they do not open a window
     * @param onBack is run when the user presses <em>Back</em>
     */
    public IU_ManagePatients(IPatientService service, String requester, Predicate<String> confirm,
            Runnable onBack) {
        super(new BorderLayout(0, 12));
        this.service = service;
        this.requester = requester;
        this.confirm = confirm;
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel lblTitle = new JLabel("Patients");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 18f));
        btnBack.setMnemonic('B');
        btnBack.addActionListener(event -> onBack.run());
        btnSearch.setMnemonic('H');
        btnSearch.addActionListener(event -> onSearch(txtSearch.getText()));
        txtSearch.addActionListener(event -> onSearch(txtSearch.getText()));
        cmbSex.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                    boolean focused) {
                return super.getListCellRendererComponent(list, value == null ? " " : sexName((Sex) value), index,
                        selected, focused);
            }
        });
        cmbSex.setSelectedIndex(-1);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !loading) {
                showSelected();
            }
        });

        JPanel center = new JPanel(new BorderLayout(24, 0));
        center.add(buildList(), BorderLayout.CENTER);
        center.add(buildForm(), BorderLayout.EAST);
        add(lblTitle, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        onSearch("");
    }

    /** Asks the server for the patients whose identifier or full name contains the text, and shows them. */
    public void onSearch(String filters) {
        List<Patient> found;
        try {
            found = service.searchPatients(requester, filters);
        } catch (AccessDeniedException exception) {
            accessDenied();
            return;
        }
        displayPatients(found);
        lblFound.setText(found.size() == 1 ? "1 patient found." : found.size() + " patients found.");
    }

    /** Shows the patients in the table, in the order they come, with none selected. */
    public void displayPatients(List<Patient> found) {
        loading = true;
        try {
            patients = new ArrayList<>(found);
            tableModel.setRowCount(0);
            for (Patient patient : patients) {
                tableModel.addRow(new Object[] {patient.getIdentifier(), patient.getFullName(),
                        patient.getDateOfBirth().format(DATE_FORMAT)});
            }
            table.clearSelection();
        } finally {
            loading = false;
        }
        showSelected();
    }

    /** Shows the message of a change that was made, in green. */
    public void showOperationResultSuccess(String message) {
        lblStatus.setForeground(SUCCESS_COLOR);
        lblStatus.setText(SUCCESS_ICON + message);
    }

    /** Shows the message of a change that was rejected, in red. */
    public void showOperationResultError(String message) {
        lblStatus.setForeground(ERROR_COLOR);
        lblStatus.setText(ERROR_ICON + message);
    }

    /** The server says that the user does not have the permission: nothing can be done but go back. */
    private void accessDenied() {
        denied = true;
        showOperationResultError("You do not have permission to manage patients.");
        updateControls();
    }

    /** Loads the selected patient in the form, or leaves the form empty for a new patient. */
    private void showSelected() {
        Patient selected = selectedPatient();
        txtIdentifier.setText(selected == null ? "" : selected.getIdentifier());
        txtNationalId.setText(selected == null ? "" : selected.getNationalId());
        txtFullName.setText(selected == null ? "" : selected.getFullName());
        txtBirth.setText(selected == null ? "" : selected.getDateOfBirth().format(DATE_FORMAT));
        cmbSex.setModel(new DefaultComboBoxModel<>(Sex.values()));
        cmbSex.setSelectedItem(selected == null ? null : selected.getSex());
        txtAddress.setText(selected == null ? "" : selected.getAddress());
        txtPhone.setText(selected == null ? "" : selected.getPhone());
        txtEmail.setText(selected == null ? "" : selected.getEmail());
        updateControls();
    }

    /** The identifier can only be written for a new patient; everything stops if the permission is denied. */
    private void updateControls() {
        boolean enabled = !denied;
        txtSearch.setEnabled(enabled);
        btnSearch.setEnabled(enabled);
        table.setEnabled(enabled);
        txtIdentifier.setEnabled(enabled && selectedPatient() == null);
        for (Component field : new Component[] {txtNationalId, txtFullName, txtBirth, cmbSex, txtAddress, txtPhone,
                txtEmail}) {
            field.setEnabled(enabled);
        }
    }

    private Patient selectedPatient() {
        int row = table.getSelectedRow();
        return row < 0 || row >= patients.size() ? null : patients.get(row);
    }

    private static String sexName(Sex sex) {
        return switch (sex) {
            case FEMALE -> "Female";
            case MALE -> "Male";
            case UNSPECIFIED -> "Unspecified";
        };
    }

    /** The search and the table, with the number of patients found below. */
    private JPanel buildList() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        JLabel lblSearch = new JLabel("Search");
        lblSearch.setDisplayedMnemonic('H');
        lblSearch.setLabelFor(txtSearch);
        search.add(lblSearch);
        search.add(txtSearch);
        search.add(btnSearch);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(420, 260));
        JPanel below = new JPanel(new BorderLayout());
        below.add(lblFound, BorderLayout.WEST);
        panel.add(search, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(below, BorderLayout.SOUTH);
        return panel;
    }

    /** Each label right above its field. */
    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        addField(panel, "Identifier (health card number)", txtIdentifier, 0, 'I');
        addField(panel, "National ID (optional)", txtNationalId, 2, 'A');
        addField(panel, "Full name", txtFullName, 4, 'F');
        addField(panel, "Date of birth (dd/MM/yyyy)", txtBirth, 6, 'D');
        addField(panel, "Sex", cmbSex, 8, 'X');
        addField(panel, "Address", txtAddress, 10, 'R');
        addField(panel, "Phone", txtPhone, 12, 'O');
        addField(panel, "E-mail", txtEmail, 14, 'E');
        return panel;
    }

    private static void addField(JPanel panel, String text, Component field, int row, char mnemonic) {
        JLabel label = new JLabel(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(field);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.insets = new Insets(row == 0 ? 0 : 8, 0, 0, 0);
        panel.add(label, constraints);
        constraints = new GridBagConstraints();
        constraints.gridy = row + 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(4, 0, 0, 0);
        panel.add(field, constraints);
    }

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(lblStatus, BorderLayout.WEST);
        panel.add(btnBack, BorderLayout.EAST);
        return panel;
    }
}
