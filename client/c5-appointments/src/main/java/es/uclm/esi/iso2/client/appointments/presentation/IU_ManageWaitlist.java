package es.uclm.esi.iso2.client.appointments.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.appointments.contracts.IAppointmentService;
import es.uclm.esi.iso2.common.appointments.contracts.IWaitlistService;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.PendingNeedView;
import es.uclm.esi.iso2.common.appointments.domain.TimeSlot;
import es.uclm.esi.iso2.common.appointments.domain.WaitlistEntry;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.Priority;
import es.uclm.esi.iso2.common.configuration.domain.TestType;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Screen of UC15 (C5, {@code IU_ManageWaitlist}): the admissions staff chooses a test type, sees its waitlist ordered
 * by priority tier and by the date in which each patient was added, adds a pending scheduling need to it or changes
 * its tier, removes a patient (asking for confirmation, D-022), asks for the openings of the calendar of a day,
 * proposes the first patient of the waitlist for an opening and fills it, which books the appointment through UC13. It
 * only talks to the contracts of {@code common}, never to the server classes. When the waitlist or the openings are
 * empty it says so instead of showing an empty table.
 */
public class IU_ManageWaitlist extends JPanel {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MOMENT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color OK_COLOR = new Color(0, 128, 0);
    private static final Color ERROR_COLOR = new Color(180, 0, 0);
    private static final String DENIED_MESSAGE = "You do not have permission to manage the waitlist.";

    private final transient IWaitlistService waitlistService;
    private final transient IAppointmentService appointmentService;
    private final transient ISystemParameterService parameterService;
    private final String requester;
    private final transient Predicate<String> confirm;
    private final transient Runnable onBack;
    private final transient Clock clock;

    private final JComboBox<String> cboTestType = new JComboBox<>();
    private final JComboBox<String> cboPriority = new JComboBox<>();
    private final List<String> testTypeIds = new ArrayList<>();
    private final List<String> priorityIds = new ArrayList<>();

    private final DefaultTableModel needModel = new DefaultTableModel(
            new Object[] { "Need", "Patient", "Test type" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable needTable = new JTable(needModel);
    private final List<PendingNeedView> needs = new ArrayList<>();

    private final DefaultTableModel entryModel = new DefaultTableModel(
            new Object[] { "Entry", "Patient", "Tier", "Added" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable entryTable = new JTable(entryModel);
    private final List<WaitlistEntry> entries = new ArrayList<>();

    private final JTextField txtDay = new JTextField(10);
    private final DefaultTableModel openingModel = new DefaultTableModel(
            new Object[] { "Room", "Start", "End" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable openingTable = new JTable(openingModel);
    private final List<TimeSlot> openings = new ArrayList<>();

    /** True while the combos are being filled, so that their events do not ask for the waitlist. */
    private boolean fillingCombos;

    private final JButton btnFindOpenings = new JButton("Find openings");
    private final JButton btnAdd = new JButton("Add to waitlist");
    private final JButton btnChangeTier = new JButton("Change tier");
    private final JButton btnRemove = new JButton("Remove");
    private final JButton btnPropose = new JButton("Propose patient");
    private final JButton btnFill = new JButton("Fill opening");
    private final JLabel lblStatus = new JLabel(" ");
    private final Color textColor = lblStatus.getForeground();

    /**
     * Creates the screen using the system clock for the default day.
     *
     * @param waitlistService the contract of UC15 to manage the waitlist
     * @param appointmentService the contract of UC13, to read the pending needs of C4
     * @param parameterService the contract of C2, to read the test types and the priority tiers
     * @param requester the username of the admissions staff
     * @param confirm asks the user a question and returns whether they accept (the tests pass a fake)
     * @param onBack what to do when the user presses <em>Back</em>
     */
    public IU_ManageWaitlist(IWaitlistService waitlistService, IAppointmentService appointmentService,
            ISystemParameterService parameterService, String requester, Predicate<String> confirm,
            Runnable onBack) {
        this(waitlistService, appointmentService, parameterService, requester, confirm, onBack,
                Clock.systemDefaultZone());
    }

    /**
     * Creates the screen with an explicit clock for the default day.
     *
     * @param waitlistService the contract of UC15 to manage the waitlist
     * @param appointmentService the contract of UC13, to read the pending needs of C4
     * @param parameterService the contract of C2, to read the test types and the priority tiers
     * @param requester the username of the admissions staff
     * @param confirm asks the user a question and returns whether they accept (the tests pass a fake)
     * @param onBack what to do when the user presses <em>Back</em>
     * @param clock gives the default day shown when the screen opens
     */
    public IU_ManageWaitlist(IWaitlistService waitlistService, IAppointmentService appointmentService,
            ISystemParameterService parameterService, String requester, Predicate<String> confirm,
            Runnable onBack, Clock clock) {
        this.waitlistService = waitlistService;
        this.appointmentService = appointmentService;
        this.parameterService = parameterService;
        this.requester = requester;
        this.confirm = confirm;
        this.onBack = onBack;
        this.clock = clock;
        setLayout(new BorderLayout(8, 8));
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        header.add(new JLabel("Manage waitlist"));
        add(header, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        loadData();
    }

    private JSplitPane buildBody() {
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.add(new JLabel("Pending scheduling needs"), BorderLayout.NORTH);
        needTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        left.add(new JScrollPane(needTable), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(4, 4));
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(3, 4, 3, 4);
        constraints.gridx = 0;
        constraints.gridy = 0;
        JLabel lblTestType = new JLabel("Test type");
        lblTestType.setDisplayedMnemonic('T');
        lblTestType.setLabelFor(cboTestType);
        top.add(lblTestType, constraints);
        constraints.gridx = 1;
        cboTestType.addActionListener(event -> {
            if (!fillingCombos && cboTestType.getSelectedItem() != null) {
                onSelectTestType((String) cboTestType.getSelectedItem());
            }
        });
        top.add(cboTestType, constraints);
        constraints.gridx = 2;
        JLabel lblPriority = new JLabel("Tier");
        lblPriority.setDisplayedMnemonic('E');
        lblPriority.setLabelFor(cboPriority);
        top.add(lblPriority, constraints);
        constraints.gridx = 3;
        top.add(cboPriority, constraints);
        constraints.gridx = 4;
        btnAdd.setMnemonic('W');
        btnAdd.addActionListener(event -> onAdd());
        top.add(btnAdd, constraints);
        constraints.gridx = 5;
        btnChangeTier.setMnemonic('G');
        btnChangeTier.addActionListener(event -> onChangeTier());
        top.add(btnChangeTier, constraints);
        constraints.gridx = 6;
        btnRemove.setMnemonic('R');
        btnRemove.addActionListener(event -> onRemove());
        top.add(btnRemove, constraints);
        right.add(top, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(4, 4));
        middle.add(new JLabel("Waitlist, by priority tier and date"), BorderLayout.NORTH);
        entryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        middle.add(new JScrollPane(entryTable), BorderLayout.CENTER);
        right.add(middle, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        JPanel dayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JLabel lblDay = new JLabel("Day");
        lblDay.setDisplayedMnemonic('D');
        lblDay.setLabelFor(txtDay);
        dayPanel.add(lblDay);
        dayPanel.add(txtDay);
        btnFindOpenings.setMnemonic('F');
        btnFindOpenings.addActionListener(event -> onFindOpenings());
        dayPanel.add(btnFindOpenings);
        btnPropose.setMnemonic('P');
        btnPropose.addActionListener(event -> onPropose());
        dayPanel.add(btnPropose);
        btnFill.setMnemonic('I');
        btnFill.addActionListener(event -> onFill());
        dayPanel.add(btnFill);
        bottom.add(dayPanel, BorderLayout.NORTH);
        openingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bottom.add(new JScrollPane(openingTable), BorderLayout.CENTER);
        right.add(bottom, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.35);
        return split;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(4, 4));
        footer.add(lblStatus, BorderLayout.CENTER);
        JPanel back = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        JButton btnBack = new JButton("Back");
        btnBack.setMnemonic('K');
        btnBack.addActionListener(event -> onBack.run());
        back.add(btnBack);
        footer.add(back, BorderLayout.EAST);
        return footer;
    }

    /**
     * Loads the test types, the priority tiers and the pending needs, and shows the waitlist of the selected test
     * type. Returns {@code true} when the data was read and displayed, and {@code false} when it could not be read
     * (the status already shows the reason).
     *
     * @return {@code true} if the data was read successfully
     */
    public boolean loadData() {
        txtDay.setText(txtDay.getText().isEmpty() ? DAY.format(LocalDate.now(clock)) : txtDay.getText());
        List<TestType> testTypes;
        List<Priority> priorities;
        List<PendingNeedView> readNeeds;
        try {
            testTypes = parameterService.getTestTypes(requester);
            priorities = parameterService.getPriorities(requester);
            readNeeds = appointmentService.getPendingSchedulingNeeds(requester);
        } catch (AccessDeniedException deniedException) {
            clearData();
            showAccessDenied();
            return false;
        } catch (ValidationException validationException) {
            clearData();
            showErrors(validationException.getErrors());
            return false;
        } catch (RuntimeException exception) {
            clearData();
            showErrors(List.of("The data could not be read: " + exception.getMessage()));
            return false;
        }
        fillingCombos = true;
        String selectedTestType = (String) cboTestType.getSelectedItem();
        fillCombo(cboTestType, testTypeIds, testTypes.stream().map(TestType::getId).toList(), selectedTestType);
        String selectedPriority = (String) cboPriority.getSelectedItem();
        fillCombo(cboPriority, priorityIds, priorities.stream().map(Priority::getId).toList(), selectedPriority);
        fillingCombos = false;
        needs.clear();
        needs.addAll(readNeeds);
        needModel.setRowCount(0);
        for (PendingNeedView need : needs) {
            needModel.addRow(new Object[] { need.getNeedId(), need.getPatientName(), need.getTestTypeName() });
        }
        showInformation(" ");
        return refreshWaitlist();
    }

    private static void fillCombo(JComboBox<String> combo, List<String> ids, List<String> values,
            String selected) {
        ids.clear();
        ids.addAll(values);
        combo.removeAllItems();
        for (String value : values) {
            combo.addItem(value);
        }
        if (selected != null && values.contains(selected)) {
            combo.setSelectedItem(selected);
        }
    }

    /**
     * Selects a test type and shows its waitlist.
     *
     * @param testTypeId the id of the test type
     */
    public void onSelectTestType(String testTypeId) {
        if (cboTestType.getSelectedItem() == null || !testTypeId.equals(cboTestType.getSelectedItem())) {
            cboTestType.setSelectedItem(testTypeId);
        }
        clearOpenings();
        showInformation(" ");
        refreshWaitlist();
    }

    private boolean refreshWaitlist() {
        String testTypeId = (String) cboTestType.getSelectedItem();
        clearEntries();
        if (testTypeId == null) {
            return false;
        }
        List<WaitlistEntry> read;
        try {
            read = waitlistService.getWaitlist(requester, testTypeId);
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
            return false;
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
            return false;
        } catch (RuntimeException exception) {
            showErrors(List.of("The waitlist could not be read: " + exception.getMessage()));
            return false;
        }
        entries.addAll(read);
        displayEntries();
        if (entries.isEmpty()) {
            showInformation("The waitlist of that test type is empty.");
        }
        return true;
    }

    private void clearEntries() {
        entries.clear();
        entryModel.setRowCount(0);
    }

    private void clearData() {
        needs.clear();
        needModel.setRowCount(0);
        clearEntries();
    }

    private void displayEntries() {
        entryModel.setRowCount(0);
        for (WaitlistEntry entry : entries) {
            entryModel.addRow(new Object[] { entry.getEntryId(), entry.getPatientId(), entry.getPriorityId(),
                MOMENT.format(entry.getAddedAt()) });
        }
    }

    /** Asks UC15 for the openings of the day of the selected test type. */
    public void onFindOpenings() {
        String testTypeId = (String) cboTestType.getSelectedItem();
        if (testTypeId == null) {
            showErrors(List.of("Select a test type before looking for openings."));
            return;
        }
        LocalDate day;
        try {
            day = LocalDate.parse(txtDay.getText().trim(), DAY);
        } catch (DateTimeParseException invalid) {
            clearOpenings();
            showErrors(List.of("The day must be written as dd/MM/yyyy"));
            return;
        }
        try {
            openings.clear();
            openings.addAll(waitlistService.getCalendarOpenings(requester, testTypeId, day, day));
            displayOpenings();
            if (openings.isEmpty()) {
                showInformation("There are no openings on that day for that test type.");
            } else {
                showInformation(openings.size() == 1 ? "1 opening." : openings.size() + " openings.");
            }
        } catch (AccessDeniedException deniedException) {
            clearOpenings();
            showAccessDenied();
        } catch (ValidationException validationException) {
            clearOpenings();
            showErrors(validationException.getErrors());
        } catch (RuntimeException exception) {
            clearOpenings();
            showErrors(List.of("The openings could not be read: " + exception.getMessage()));
        }
    }

    private void displayOpenings() {
        openingModel.setRowCount(0);
        for (TimeSlot opening : openings) {
            openingModel.addRow(new Object[] { opening.getRoom(), MOMENT.format(opening.getStart()),
                MOMENT.format(opening.getEnd()) });
        }
    }

    private void onAdd() {
        PendingNeedView need = selectedNeed();
        String priorityId = (String) cboPriority.getSelectedItem();
        if (need == null || priorityId == null) {
            showErrors(List.of("Select a pending need and a tier before adding it to the waitlist."));
            return;
        }
        onAddEntry(need.getNeedId(), priorityId);
    }

    /**
     * Adds a pending scheduling need to the waitlist of the selected test type.
     *
     * @param needId the id of the pending need
     * @param priorityId the id of the priority tier
     */
    public void onAddEntry(String needId, String priorityId) {
        try {
            WaitlistEntry added = waitlistService.addToWaitlist(requester, needId, priorityId);
            if (loadData()) {
                showConfirmation("Entry " + added.getEntryId() + " added to the waitlist.");
            }
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
        } catch (RuntimeException exception) {
            showErrors(List.of("The need could not be added: " + exception.getMessage()));
        }
    }

    private void onChangeTier() {
        WaitlistEntry entry = selectedEntry();
        String priorityId = (String) cboPriority.getSelectedItem();
        if (entry == null || priorityId == null) {
            showErrors(List.of("Select an entry of the waitlist and a tier before changing it."));
            return;
        }
        onChangePriority(entry.getEntryId(), priorityId);
    }

    /**
     * Changes the priority tier of an entry of the waitlist.
     *
     * @param entryId the id of the entry
     * @param priorityId the id of the new tier
     */
    public void onChangePriority(String entryId, String priorityId) {
        try {
            WaitlistEntry changed = waitlistService.changeWaitlistPriority(requester, entryId, priorityId);
            if (refreshWaitlist()) {
                showConfirmation("Entry " + changed.getEntryId() + " moved to the tier "
                        + changed.getPriorityId() + ".");
            }
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
        } catch (RuntimeException exception) {
            showErrors(List.of("The tier could not be changed: " + exception.getMessage()));
        }
    }

    private void onRemove() {
        WaitlistEntry entry = selectedEntry();
        if (entry == null) {
            showErrors(List.of("Select the entry to remove from the waitlist."));
            return;
        }
        if (!confirm.test("Remove the patient " + entry.getPatientId() + " from the waitlist?")) {
            return;
        }
        onRemoveEntry(entry.getEntryId());
    }

    /**
     * Removes an entry from the waitlist, keeping its record (D-022).
     *
     * @param entryId the id of the entry
     */
    public void onRemoveEntry(String entryId) {
        try {
            WaitlistEntry removed = waitlistService.removeFromWaitlist(requester, entryId);
            if (refreshWaitlist()) {
                showConfirmation("Entry " + removed.getEntryId() + " removed from the waitlist.");
            }
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
        } catch (RuntimeException exception) {
            showErrors(List.of("The entry could not be removed: " + exception.getMessage()));
        }
    }

    private void onPropose() {
        TimeSlot opening = selectedOpening();
        String testTypeId = (String) cboTestType.getSelectedItem();
        if (opening == null || testTypeId == null) {
            showErrors(List.of("Look for the openings of the day and select one before proposing a patient."));
            return;
        }
        onSelectOpening(opening);
    }

    /**
     * Proposes the first patient of the waitlist for an opening and selects its row.
     *
     * @param opening the opening to fill
     */
    public void onSelectOpening(TimeSlot opening) {
        String testTypeId = (String) cboTestType.getSelectedItem();
        try {
            WaitlistEntry proposed = waitlistService.proposePatientForOpening(requester, testTypeId, opening);
            if (proposed == null) {
                entryTable.clearSelection();
                showInformation("There is no patient who can fill this opening.");
                return;
            }
            int row = indexOfEntry(proposed.getEntryId());
            if (row >= 0) {
                entryTable.setRowSelectionInterval(row, row);
            }
            showInformation("The patient " + proposed.getPatientId() + " (entry " + proposed.getEntryId()
                    + ") is proposed for " + MOMENT.format(opening.getStart()) + ".");
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
        } catch (RuntimeException exception) {
            showErrors(List.of("The patient could not be proposed: " + exception.getMessage()));
        }
    }

    private void onFill() {
        WaitlistEntry entry = selectedEntry();
        TimeSlot opening = selectedOpening();
        if (entry == null || opening == null) {
            showErrors(List.of("Select an entry of the waitlist and an opening before filling it."));
            return;
        }
        if (!confirm.test("Fill the opening of " + MOMENT.format(opening.getStart()) + " with the patient "
                + entry.getPatientId() + "?")) {
            return;
        }
        onConfirmFillOpening(entry.getEntryId(), opening);
    }

    /**
     * Fills an opening with an entry of the waitlist; UC13 books the appointment.
     *
     * @param entryId the id of the entry
     * @param opening the opening to fill
     */
    public void onConfirmFillOpening(String entryId, TimeSlot opening) {
        try {
            Appointment booked = waitlistService.fillOpening(requester, entryId, opening);
            clearOpenings();
            if (loadData()) {
                showConfirmation("Opening filled: appointment " + booked.getAppointmentId() + " booked on "
                        + MOMENT.format(booked.getStart()) + ".");
            }
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
        } catch (RuntimeException exception) {
            showErrors(List.of("The opening could not be filled: " + exception.getMessage()));
        }
    }

    private void clearOpenings() {
        openings.clear();
        openingModel.setRowCount(0);
    }

    private int indexOfEntry(String entryId) {
        for (int row = 0; row < entries.size(); row++) {
            if (entries.get(row).getEntryId().equals(entryId)) {
                return row;
            }
        }
        return -1;
    }

    /**
     * Shows a message of information in the default colour of the screen.
     *
     * @param message the message
     */
    private void showInformation(String message) {
        lblStatus.setForeground(textColor);
        lblStatus.setText(message);
    }

    /**
     * Shows every error, together and in red.
     *
     * @param errors the messages to show
     */
    public void showErrors(List<String> errors) {
        lblStatus.setForeground(ERROR_COLOR);
        lblStatus.setText(errors.size() == 1 ? "⚠ " + errors.get(0) : "⚠ " + String.join(" ", errors));
    }

    /** Explains that the user does not have the permission and disables the actions. */
    public void showAccessDenied() {
        btnFindOpenings.setEnabled(false);
        btnAdd.setEnabled(false);
        btnChangeTier.setEnabled(false);
        btnRemove.setEnabled(false);
        btnPropose.setEnabled(false);
        btnFill.setEnabled(false);
        needTable.setEnabled(false);
        entryTable.setEnabled(false);
        openingTable.setEnabled(false);
        cboTestType.setEnabled(false);
        cboPriority.setEnabled(false);
        lblStatus.setForeground(ERROR_COLOR);
        lblStatus.setText("⚠ " + DENIED_MESSAGE);
    }

    /**
     * Shows a message of success in green.
     *
     * @param message the message
     */
    public void showConfirmation(String message) {
        lblStatus.setForeground(OK_COLOR);
        lblStatus.setText(message);
    }

    private PendingNeedView selectedNeed() {
        int row = needTable.getSelectedRow();
        return row < 0 || row >= needs.size() ? null : needs.get(row);
    }

    private WaitlistEntry selectedEntry() {
        int row = entryTable.getSelectedRow();
        return row < 0 || row >= entries.size() ? null : entries.get(row);
    }

    private TimeSlot selectedOpening() {
        int row = openingTable.getSelectedRow();
        return row < 0 || row >= openings.size() ? null : openings.get(row);
    }

    /** The table of pending needs (for the tests). */
    public JTable getNeedsTable() {
        return needTable;
    }

    /** The table of the waitlist (for the tests). */
    public JTable getWaitlistTable() {
        return entryTable;
    }

    /** The table of openings (for the tests). */
    public JTable getOpeningsTable() {
        return openingTable;
    }

    /** The text of the message area (for the tests). */
    public String getStatusMessage() {
        return lblStatus.getText();
    }
}
