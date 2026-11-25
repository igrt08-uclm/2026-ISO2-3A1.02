package es.uclm.esi.iso2.client.orders.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.orders.contracts.IClinicalOrderService;
import es.uclm.esi.iso2.common.orders.domain.ClinicalOrder;
import es.uclm.esi.iso2.common.orders.domain.FollowUpOrder;
import es.uclm.esi.iso2.common.patients.contracts.IPatientService;
import es.uclm.esi.iso2.common.patients.domain.Patient;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/**
 * Screen of UC07 (C4, {@code IU_OrderFollowUp}): the physician chooses a patient, fills in the form of a periodic
 * follow-up order —test type eligible for follow-up, clinical indication, justification and recurrence
 * interval— and sees the orders that the patient already has. It only talks to the contracts of {@code common}
 * ({@link IClinicalOrderService}, {@link IPatientService} and {@link ISystemParameterService}), never to the server
 * classes. The screen is a {@link JPanel} so that the tests can create it without a display.
 */
public class IU_OrderFollowUp extends JPanel {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color OK_COLOR = new Color(0, 128, 0);
    private static final Color ERROR_COLOR = new Color(180, 0, 0);
    private static final String DENIED_MESSAGE = "You do not have permission to order procedures.";

    private final transient IClinicalOrderService orderService;
    private final transient IPatientService patientService;
    private final transient ISystemParameterService parameterService;
    private final String requester;
    private final transient Predicate<String> confirm;
    private final transient Runnable onBack;

    private final JTextField txtSearch = new JTextField(18);
    private final DefaultTableModel patientModel = new DefaultTableModel(
            new Object[] { "Identifier", "Full name", "Date of birth" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable patientTable = new JTable(patientModel);
    private final JLabel lblPatientsFound = new JLabel(" ");

    private final JComboBox<Option> cmbTestType = new JComboBox<>();
    /** From 0 to 12 months, so that a 0 can be chosen and the screen shows the reason why it is not valid. */
    private final JSpinner spnInterval = new JSpinner(new SpinnerNumberModel(6, 0, 12, 1));
    private final JTextField txtIndication = new JTextField(20);
    private final JTextField txtJustification = new JTextField(20);
    private final JButton btnSearch = new JButton("Search");
    private final JButton btnPlaceOrder = new JButton("Place order");

    private final DefaultTableModel orderModel = new DefaultTableModel(
            new Object[] { "Date", "Test type", "Interval (months)", "Indication" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable orderTable = new JTable(orderModel);
    private final JLabel lblStatus = new JLabel(" ");

    private final Map<String, String> testTypeNames = new LinkedHashMap<>();

    private transient Patient selectedPatient;
    private boolean denied;
    private int lastPatientRow = -1;

    /**
     * Creates the screen.
     *
     * @param orderService the contract of C4, to place the follow-up orders and to read them
     * @param patientService the contract of C3, to search and read the patients
     * @param parameterService the contract of C2, to read the test types
     * @param requester the username of the physician who uses the screen
     * @param confirm asks the user a question and returns whether they accept (the tests pass a fake)
     * @param onBack what to do when the user presses <em>Back</em>
     */
    public IU_OrderFollowUp(IClinicalOrderService orderService, IPatientService patientService,
            ISystemParameterService parameterService, String requester, Predicate<String> confirm, Runnable onBack) {
        this.orderService = orderService;
        this.patientService = patientService;
        this.parameterService = parameterService;
        this.requester = requester;
        this.confirm = confirm;
        this.onBack = onBack;
        setLayout(new BorderLayout(8, 8));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        onSearch();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        header.add(new JLabel("Order a periodic follow-up procedure"));
        return header;
    }

    private JSplitPane buildBody() {
        JPanel left = new JPanel(new BorderLayout(4, 4));
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        btnSearch.setMnemonic('H');
        searchRow.add(new JLabel("Search"));
        searchRow.add(txtSearch);
        searchRow.add(btnSearch);
        txtSearch.addActionListener(event -> onSearch());
        btnSearch.addActionListener(event -> onSearch());
        left.add(searchRow, BorderLayout.NORTH);
        patientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        patientTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selectedRowChanged();
            }
        });
        left.add(new JScrollPane(patientTable), BorderLayout.CENTER);
        JPanel leftFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        leftFooter.add(lblPatientsFound);
        left.add(leftFooter, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(4, 4));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(3, 4, 3, 4);
        constraints.anchor = GridBagConstraints.WEST;
        addField(form, constraints, 0, "Test type", cmbTestType);
        addField(form, constraints, 1, "Interval (months)", spnInterval);
        addField(form, constraints, 2, "Clinical indication", txtIndication);
        addField(form, constraints, 3, "Justification", txtJustification);
        constraints.gridx = 1;
        constraints.gridy = 4;
        btnPlaceOrder.setMnemonic('P');
        btnPlaceOrder.addActionListener(event -> onSubmitOrder());
        form.add(btnPlaceOrder, constraints);
        right.add(form, BorderLayout.NORTH);

        JPanel orders = new JPanel(new BorderLayout(4, 4));
        orders.add(new JLabel("Orders of the selected patient"), BorderLayout.NORTH);
        orders.add(new JScrollPane(orderTable), BorderLayout.CENTER);
        right.add(orders, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.45);
        setFormEnabled(false);
        return split;
    }

    private void addField(JPanel form, GridBagConstraints constraints, int row, String label,
            java.awt.Component field) {
        constraints.gridx = 0;
        constraints.gridy = row;
        form.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        form.add(field, constraints);
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(4, 4));
        footer.add(lblStatus, BorderLayout.CENTER);
        JPanel back = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        JButton btnBack = new JButton("Back");
        btnBack.setMnemonic('B');
        btnBack.addActionListener(event -> onBackRequested());
        back.add(btnBack);
        footer.add(back, BorderLayout.EAST);
        return footer;
    }

    /** Searches the patients with the text of the box; an empty text lists all of them. */
    public void onSearch() {
        if (denied) {
            return;
        }
        try {
            List<Patient> found = patientService.searchPatients(requester, txtSearch.getText().trim());
            displayPatients(found);
            lblStatus.setText(" ");
        } catch (AccessDeniedException deniedException) {
            handleDenied();
        } catch (RuntimeException exception) {
            showOperationResultError("The patients could not be read: " + exception.getMessage());
        }
    }

    /**
     * Shows the patients in the table.
     *
     * @param patients the patients to show
     */
    public void displayPatients(List<Patient> patients) {
        clearSelection();
        patientModel.setRowCount(0);
        for (Patient patient : patients) {
            patientModel.addRow(new Object[] { patient.getIdentifier(), patient.getFullName(),
                    patient.getDateOfBirth() == null ? "" : DATE.format(patient.getDateOfBirth()) });
        }
        lblPatientsFound.setText(patients.size() == 1 ? "1 patient found." : patients.size() + " patients found.");
        patientTable.clearSelection();
        lastPatientRow = -1;
    }

    private void selectedRowChanged() {
        int row = patientTable.getSelectedRow();
        if (row < 0 || row == lastPatientRow) {
            return;
        }
        if (hasUnsavedChanges() && !confirm.test("Discard the unsaved changes?")) {
            patientTable.getSelectionModel().setSelectionInterval(lastPatientRow, lastPatientRow);
            return;
        }
        lastPatientRow = row;
        onSelectPatient((String) patientModel.getValueAt(row, 0));
    }

    /**
     * Reads the patient and, when it is active, loads the form and its orders.
     *
     * @param identifier the health card number of the patient that the table shows
     */
    public void onSelectPatient(String identifier) {
        if (denied) {
            return;
        }
        Patient patient;
        try {
            patient = findPatient(identifier);
        } catch (AccessDeniedException deniedException) {
            clearSelection();
            handleDenied();
            return;
        } catch (RuntimeException exception) {
            clearFailedSelection();
            showOperationResultError("The patient could not be read: " + exception.getMessage());
            return;
        }
        if (patient == null) {
            clearSelection();
            showOperationResultError("There is no patient with the identifier " + identifier);
            return;
        }
        if (!patient.isActive()) {
            clearSelection();
            selectedPatient = patient;
            showPatientInactive(patient);
            return;
        }
        try {
            List<TestType> testTypes = readTestTypes();
            if (testTypes == null) {
                return;
            }
            List<ClinicalOrder> orders = orderService.getOrdersOfPatient(requester, patient.getPatientId());
            selectedPatient = patient;
            displayOrderForm(testTypes, orders);
        } catch (AccessDeniedException deniedException) {
            clearSelection();
            handleDenied();
        } catch (ValidationException validationException) {
            clearFailedSelection();
            showValidationErrors(validationException.getErrors());
        } catch (RuntimeException exception) {
            clearFailedSelection();
            showOperationResultError("The orders could not be read: " + exception.getMessage());
        }
    }

    /**
     * Forgets the patient like {@link #clearSelection()} and also deselects the row of the table, so that a click on
     * the same patient reads it again after a failed read.
     */
    private void clearFailedSelection() {
        clearSelection();
        patientTable.clearSelection();
        lastPatientRow = -1;
    }

    /**
     * Reads the test types of C2. When the read fails it clears the selection, shows its own message and gives
     * {@code null}, so that the caller stops.
     *
     * @return the test types, or {@code null} when they could not be read
     * @throws AccessDeniedException when the requester does not have the permission to read them
     */
    private List<TestType> readTestTypes() throws AccessDeniedException {
        try {
            return parameterService.getTestTypes(requester);
        } catch (RuntimeException exception) {
            clearFailedSelection();
            showOperationResultError("The test types could not be read: " + exception.getMessage());
            return null;
        }
    }

    /**
     * Forgets the patient that was selected: the form, its texts and the table of orders are emptied, so that
     * nothing of a previous patient stays on the screen or can be sent with the next order.
     */
    private void clearSelection() {
        selectedPatient = null;
        orderModel.setRowCount(0);
        txtIndication.setText("");
        txtJustification.setText("");
        setFormEnabled(false);
    }

    /**
     * Finds the patient whose identifier is exactly the one that the table shows. An {@code AccessDeniedException}
     * of C3 is not caught here: it reaches the {@code catch} of {@link #onSelectPatient(String)}, so that the denial is
     * handled in one place and its message is not overwritten by “There is no patient with the identifier …”.
     *
     * @param identifier the health card number of the patient
     * @return the patient, or {@code null} when C3 does not know it
     * @throws AccessDeniedException when the requester does not have the permission to search patients
     */
    private Patient findPatient(String identifier) throws AccessDeniedException {
        for (Patient patient : patientService.searchPatients(requester, identifier)) {
            if (identifier.equals(patient.getIdentifier())) {
                return patient;
            }
        }
        return null;
    }

    /**
     * Shows the form for the patient that is selected: only the active test types eligible for follow-up are
     * offered, and the orders that the patient already has are listed.
     *
     * @param testTypes the test types of the catalogue
     * @param orders the orders of the patient
     */
    public void displayOrderForm(List<TestType> testTypes, List<ClinicalOrder> orders) {
        cmbTestType.removeAllItems();
        testTypeNames.clear();
        int eligible = 0;
        for (TestType testType : testTypes) {
            testTypeNames.put(testType.getId(), testType.getName());
            if (testType.isActive() && testType.isFollowUpEligible()) {
                cmbTestType.addItem(new Option(testType.getId(), testType.getId() + " · " + testType.getName()));
                eligible++;
            }
        }
        displayOrders(orders);
        txtIndication.setText("");
        txtJustification.setText("");
        spnInterval.setValue(6);
        setFormEnabled(eligible > 0);
        if (eligible == 0) {
            showOperationResultError("There is no active test type eligible for follow-up.");
        } else {
            lblStatus.setText(" ");
            txtIndication.requestFocusInWindow();
        }
    }

    /**
     * Fills the table of orders of the selected patient.
     *
     * @param orders the orders to show
     */
    public void displayOrders(List<ClinicalOrder> orders) {
        orderModel.setRowCount(0);
        for (ClinicalOrder order : orders) {
            orderModel.addRow(new Object[] {
                order.getCreationDate() == null ? "" : DATE.format(order.getCreationDate()),
                testTypeNames.getOrDefault(order.getTestTypeId(), order.getTestTypeId()),
                order instanceof FollowUpOrder followUp ? followUp.getRecurrenceIntervalMonths() : "",
                order.getIndication() });
        }
    }

    /** Validates the form, places the follow-up order and, when the server accepts it, shows it in the table. */
    public void onSubmitOrder() {
        if (denied) {
            return;
        }
        List<String> errors = validateForm();
        if (!errors.isEmpty()) {
            showValidationErrors(errors);
            return;
        }
        FollowUpOrder sent = new FollowUpOrder(null, selectedPatient.getPatientId(),
                ((Option) cmbTestType.getSelectedItem()).id, requester, txtIndication.getText().trim(),
                txtJustification.getText().trim(), null, (Integer) spnInterval.getValue());
        try {
            FollowUpOrder created = orderService.orderFollowUp(requester, sent);
            showOperationResultSuccess("Order " + created.getOrderId() + " created.");
            txtIndication.setText("");
            txtJustification.setText("");
            spnInterval.setValue(6);
            displayOrders(orderService.getOrdersOfPatient(requester, selectedPatient.getPatientId()));
        } catch (ValidationException validationException) {
            showValidationErrors(validationException.getErrors());
        } catch (AccessDeniedException deniedException) {
            handleDenied();
        } catch (RuntimeException exception) {
            showOperationResultError("The order could not be created: " + exception.getMessage());
        }
    }

    private List<String> validateForm() {
        List<String> errors = new ArrayList<>();
        if (selectedPatient == null) {
            errors.add("Select a patient before placing an order.");
        }
        if (cmbTestType.getSelectedItem() == null) {
            errors.add("The test type is required");
        }
        if ((Integer) spnInterval.getValue() <= 0) {
            errors.add("The recurrence interval must be a positive number of months");
        }
        if (txtIndication.getText().isBlank()) {
            errors.add("The clinical indication is required");
        }
        if (txtJustification.getText().isBlank()) {
            errors.add("The justification is required");
        }
        return errors;
    }

    /**
     * Explains that the patient is not active and does not allow ordering.
     *
     * @param patient the patient that is not active
     */
    public void showPatientInactive(Patient patient) {
        orderModel.setRowCount(0);
        setFormEnabled(false);
        showOperationResultError(
                "The patient " + patient.getPatientId() + " is not active, so no order can be placed.");
    }

    /**
     * Shows every error, together and in red.
     *
     * @param errors the messages to show
     */
    public void showValidationErrors(List<String> errors) {
        lblStatus.setForeground(ERROR_COLOR);
        lblStatus.setText(errors.size() == 1 ? "⚠ " + errors.get(0) : "⚠ " + String.join(" ", errors));
    }

    /**
     * Shows a message of success in green.
     *
     * @param message the message
     */
    public void showOperationResultSuccess(String message) {
        lblStatus.setForeground(OK_COLOR);
        lblStatus.setText(message);
    }

    /**
     * Shows a message of error in red.
     *
     * @param message the message
     */
    public void showOperationResultError(String message) {
        lblStatus.setForeground(ERROR_COLOR);
        lblStatus.setText("⚠ " + message);
    }

    /** The user does not have the permission: explains it and leaves only <em>Back</em> enabled. */
    private void handleDenied() {
        denied = true;
        setFormEnabled(false);
        patientTable.setEnabled(false);
        txtSearch.setEnabled(false);
        btnSearch.setEnabled(false);
        showOperationResultError(DENIED_MESSAGE);
    }

    private void onBackRequested() {
        if (hasUnsavedChanges() && !confirm.test("Discard the unsaved changes?")) {
            return;
        }
        onBack.run();
    }

    private boolean hasUnsavedChanges() {
        return selectedPatient != null
                && (!txtIndication.getText().isBlank() || !txtJustification.getText().isBlank());
    }

    private void setFormEnabled(boolean enabled) {
        cmbTestType.setEnabled(enabled);
        spnInterval.setEnabled(enabled);
        txtIndication.setEnabled(enabled);
        txtJustification.setEnabled(enabled);
        btnPlaceOrder.setEnabled(enabled);
    }

    /** The patients table (for the tests). */
    public JTable getPatientsTable() {
        return patientTable;
    }

    /** The orders table (for the tests). */
    public JTable getOrdersTable() {
        return orderTable;
    }

    /** The search box (for the tests). */
    public JTextField getSearchField() {
        return txtSearch;
    }

    /** The box of the recurrence interval (for the tests). */
    public JSpinner getIntervalSpinner() {
        return spnInterval;
    }

    /** The button <em>Place order</em> (for the tests). */
    public JButton getPlaceOrderButton() {
        return btnPlaceOrder;
    }

    /** The text of the message area (for the tests). */
    public String getStatusMessage() {
        return lblStatus.getText();
    }

    /** The patient that is selected, or {@code null} (for the tests). */
    public Patient getSelectedPatient() {
        return selectedPatient;
    }

    /** One item of a list: the id that the service expects and the text that the user reads. */
    private static final class Option {
        private final String id;
        private final String label;

        private Option(String id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
