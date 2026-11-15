package es.uclm.esi.iso2.client.patients.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.patients.contracts.DuplicatePatientException;
import es.uclm.esi.iso2.common.patients.contracts.IPatientService;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.patients.domain.Sex;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
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
    private static final Border ERROR_BORDER = BorderFactory.createLineBorder(ERROR_COLOR, 2);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");
    private static final LocalDate EARLIEST_BIRTH = LocalDate.of(1900, 1, 1);
    private static final Pattern PHONE_DIGITS = Pattern.compile("\\d{9,15}");
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
    private final JLabel lblStatus = new WrappedLabel();
    private final JButton btnNew = new JButton("New patient");
    private final JButton btnSave = new JButton("Save");
    private final JButton btnBack = new JButton("Back");
    private final transient Map<JComponent, Border> normalBorders = new HashMap<>();

    private transient List<Patient> patients = new ArrayList<>();
    private boolean denied;
    private boolean loading;
    private boolean restoring;
    private int shownRow = -1;

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
        btnSearch.setMnemonic('H');
        btnBack.setMnemonic('B');
        btnBack.addActionListener(event -> {
            if (confirmDiscard()) {
                onBack.run();
            }
        });
        btnNew.setMnemonic('N');
        btnNew.addActionListener(event -> onNewPatient());
        btnSave.setMnemonic('S');
        btnSave.addActionListener(event -> onSavePatient());
        btnSearch.addActionListener(event -> onSearchRequested());
        txtSearch.addActionListener(event -> onSearchRequested());
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
            if (!event.getValueIsAdjusting() && !loading && !restoring) {
                changeRow();
            }
        });

        JPanel center = new JPanel(new BorderLayout(24, 0));
        center.add(buildList(), BorderLayout.CENTER);
        center.add(buildForm(), BorderLayout.EAST);
        add(lblTitle, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        for (JComponent field : formFields()) {
            normalBorders.put(field, field.getBorder());
        }
        onSearch("");
    }

    /** The user asked for a search: if the form has unsaved changes, it asks before they are thrown away. */
    private void onSearchRequested() {
        if (confirmDiscard()) {
            onSearch(txtSearch.getText());
        }
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

    /** Shows the errors found in the form, all together and in red; nothing was saved. */
    public void showValidationErrors(List<String> errors) {
        showOperationResultError(errors.stream().map(error -> error + ".").collect(Collectors.joining(" ")));
    }

    /**
     * The patient already exists: says so and loads the existing record in the form, so that the user can modify it
     * instead of creating a duplicate.
     */
    public void showExistingPatient(Patient existing) {
        txtSearch.setText("");
        onSearch("");
        select(existing.getPatientId());
        showOperationResultError("A patient with this identifier already exists: the record is loaded so that you can"
                + " modify it.");
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

    /**
     * The user pressed another row. If the form has unsaved changes, it asks whether to discard them; if the user
     * cancels, the selection goes back to the row it had, with its changes.
     */
    private void changeRow() {
        if (table.getSelectedRow() == shownRow) {
            return;
        }
        if (!confirmDiscard()) {
            restoring = true;
            try {
                if (shownRow >= 0) {
                    table.setRowSelectionInterval(shownRow, shownRow);
                } else {
                    table.clearSelection();
                }
            } finally {
                restoring = false;
            }
            return;
        }
        showSelected();
    }

    /** Tells whether it is fine to throw away what is in the form: it has no changes, or the user accepts. */
    private boolean confirmDiscard() {
        return !isDirty() || confirm.test("Discard the unsaved changes?");
    }

    /** Tells whether the form differs from the patient that is shown, or, for a new patient, is not empty. */
    private boolean isDirty() {
        Patient shown = shownRow >= 0 && shownRow < patients.size() ? patients.get(shownRow) : null;
        return !same(txtIdentifier, shown == null ? "" : shown.getIdentifier())
                || !same(txtNationalId, shown == null ? "" : shown.getNationalId())
                || !same(txtFullName, shown == null ? "" : shown.getFullName())
                || !same(txtBirth, shown == null ? "" : shown.getDateOfBirth().format(DATE_FORMAT))
                || cmbSex.getSelectedItem() != (shown == null ? null : shown.getSex())
                || !same(txtAddress, shown == null ? "" : shown.getAddress())
                || !same(txtPhone, shown == null ? "" : shown.getPhone())
                || !same(txtEmail, shown == null ? "" : shown.getEmail());
    }

    private static boolean same(JTextField field, String saved) {
        return field.getText().strip().equals(saved == null ? "" : saved.strip());
    }

    /** Starts a new patient: nothing is selected and the form is empty. */
    private void onNewPatient() {
        if (!confirmDiscard()) {
            return;
        }
        restoring = true;
        try {
            table.clearSelection();
        } finally {
            restoring = false;
        }
        showSelected();
        txtIdentifier.requestFocusInWindow();
    }

    /**
     * Checks the form with the same rules as the server and, if it is right, registers the patient (nothing selected) or
     * modifies the selected one. If anything is wrong, every error is shown together and nothing is sent.
     */
    private void onSavePatient() {
        clearMarks();
        List<String> errors = new ArrayList<>();
        Patient patient = readForm(errors);
        if (!errors.isEmpty()) {
            showValidationErrors(errors);
            return;
        }
        Patient selected = selectedPatient();
        try {
            Patient saved = selected == null ? service.registerPatient(requester, patient)
                    : service.modifyPatient(requester, selected.getPatientId(), patient);
            txtSearch.setText("");
            onSearch("");
            select(saved.getPatientId());
            showOperationResultSuccess(
                    "Patient " + saved.getPatientId() + (selected == null ? " registered." : " saved."));
        } catch (AccessDeniedException exception) {
            accessDenied();
        } catch (ValidationException exception) {
            showValidationErrors(exception.getErrors());
        } catch (DuplicatePatientException exception) {
            showExistingPatient(exception.getExistingPatient());
        }
    }

    /** Reads the form; every error found is added to {@code errors} and its field is marked with a red border. */
    private Patient readForm(List<String> errors) {
        String identifier = txtIdentifier.getText().strip();
        if (identifier.isEmpty()) {
            fail(errors, txtIdentifier, "The identifier is required");
        }
        String fullName = txtFullName.getText().strip();
        if (fullName.isEmpty()) {
            fail(errors, txtFullName, "The full name is required");
        }
        LocalDate birth = readBirth(errors);
        Sex sex = (Sex) cmbSex.getSelectedItem();
        if (sex == null) {
            fail(errors, cmbSex, "The sex is required");
        }
        String email = txtEmail.getText().strip();
        if (!email.isEmpty() && !EMAIL.matcher(email).matches()) {
            fail(errors, txtEmail, "The e-mail must have the form name@domain.ext");
        }
        String phone = txtPhone.getText().strip();
        if (!phone.isEmpty() && !PHONE_DIGITS.matcher(digitsOf(phone)).matches()) {
            fail(errors, txtPhone, "The phone must have 9 to 15 digits");
        }
        return new Patient(null, identifier, txtNationalId.getText().strip(), fullName, birth, sex,
                txtAddress.getText().strip(), phone, email, true);
    }

    private LocalDate readBirth(List<String> errors) {
        String text = txtBirth.getText().strip();
        if (text.isEmpty()) {
            fail(errors, txtBirth, "The date of birth is required");
            return null;
        }
        try {
            LocalDate birth = LocalDate.parse(text, DATE_FORMAT);
            if (birth.isAfter(LocalDate.now())) {
                fail(errors, txtBirth, "The date of birth cannot be in the future");
            } else if (birth.isBefore(EARLIEST_BIRTH)) {
                fail(errors, txtBirth, "The date of birth cannot be before 01/01/1900");
            }
            return birth;
        } catch (DateTimeParseException exception) {
            fail(errors, txtBirth, "The date of birth must be written as dd/MM/yyyy");
            return null;
        }
    }

    /** The phone without its spaces and without a leading plus sign. */
    private static String digitsOf(String phone) {
        String withoutSpaces = phone.replace(" ", "");
        return withoutSpaces.startsWith("+") ? withoutSpaces.substring(1) : withoutSpaces;
    }

    private void fail(List<String> errors, JComponent field, String message) {
        errors.add(message);
        field.setBorder(ERROR_BORDER);
    }

    private void clearMarks() {
        normalBorders.forEach(JComponent::setBorder);
    }

    private List<JComponent> formFields() {
        return List.of(txtIdentifier, txtNationalId, txtFullName, txtBirth, cmbSex, txtAddress, txtPhone, txtEmail);
    }

    /** Selects the patient with that id in the table, which loads it in the form. */
    private void select(String patientId) {
        for (int index = 0; index < patients.size(); index++) {
            if (patients.get(index).getPatientId().equals(patientId)) {
                table.setRowSelectionInterval(index, index);
                return;
            }
        }
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
        clearMarks();
        shownRow = table.getSelectedRow();
        updateControls();
    }

    /** The identifier can only be written for a new patient; everything stops if the permission is denied. */
    private void updateControls() {
        boolean enabled = !denied;
        txtSearch.setEnabled(enabled);
        btnSearch.setEnabled(enabled);
        btnNew.setEnabled(enabled);
        btnSave.setEnabled(enabled);
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
        lblSearch.setLabelFor(txtSearch);
        search.add(lblSearch);
        search.add(txtSearch);
        search.add(btnSearch);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(420, 260));
        JPanel below = new JPanel(new BorderLayout());
        below.add(lblFound, BorderLayout.WEST);
        below.add(btnNew, BorderLayout.EAST);
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
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        buttons.add(btnSave);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 16;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(16, 0, 0, 0);
        panel.add(buttons, constraints);
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
        panel.add(lblStatus, BorderLayout.CENTER);
        panel.add(btnBack, BorderLayout.EAST);
        return panel;
    }

    /**
     * A label for the messages: its text stays plain (so {@code getText()} is the message), but it is drawn in as
     * many lines as it needs for the width it has, so that a long message is never cut.
     */
    private static final class WrappedLabel extends JLabel {
        private static final int PREFERRED_WIDTH = 240;
        private final transient JTextArea area = new JTextArea();

        WrappedLabel() {
            super(" ");
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setOpaque(false);
            area.setBorder(null);
            area.setText(getText());
        }

        @Override
        public void setText(String text) {
            super.setText(text);
            if (area != null) {
                area.setText(text);
                revalidate();
            }
        }

        @Override
        public Dimension getPreferredSize() {
            area.setFont(getFont());
            area.setSize(getWidth() > 0 ? getWidth() : PREFERRED_WIDTH, Short.MAX_VALUE);
            return new Dimension(PREFERRED_WIDTH, area.getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            area.setFont(getFont());
            area.setForeground(getForeground());
            area.setSize(getWidth(), getHeight());
            area.paint(graphics);
            if (getPreferredSize().height != getHeight()) {
                revalidate();
            }
        }
    }
}
