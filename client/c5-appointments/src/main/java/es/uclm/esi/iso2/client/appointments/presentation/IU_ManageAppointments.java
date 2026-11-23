package es.uclm.esi.iso2.client.appointments.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.appointments.contracts.IAppointmentService;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.AppointmentStatus;
import es.uclm.esi.iso2.common.appointments.domain.PendingNeedView;
import es.uclm.esi.iso2.common.appointments.domain.TimeSlot;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Screen of UC13 (C5, {@code IU_ManageAppointments}): the admissions staff sees the pending scheduling needs of C4,
 * asks for the free slots of the room of a test type on a day, books a need in a free slot and moves or cancels the
 * appointments. It only talks to the contract {@link IAppointmentService} of {@code common}, never to the server classes.
 */
public class IU_ManageAppointments extends JPanel {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MOMENT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color OK_COLOR = new Color(0, 128, 0);
    private static final Color ERROR_COLOR = new Color(180, 0, 0);
    private static final String DENIED_MESSAGE = "You do not have permission to manage appointments.";

    private final transient IAppointmentService service;
    private final String requester;
    private final transient Predicate<String> confirm;
    private final transient Runnable onBack;
    private final transient Clock clock;

    private final DefaultTableModel needModel = new DefaultTableModel(
            new Object[] { "Need", "Patient", "Test type", "Priority", "Order date", "Due" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable needTable = new JTable(needModel);
    private final List<PendingNeedView> needs = new ArrayList<>();

    private final JTextField txtDate = new JTextField(10);
    private final DefaultTableModel slotModel = new DefaultTableModel(
            new Object[] { "Room", "Start", "End" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable slotTable = new JTable(slotModel);
    private final List<TimeSlot> slots = new ArrayList<>();

    private final DefaultTableModel appointmentModel = new DefaultTableModel(
            new Object[] { "Appointment", "Patient", "Test type", "Start", "Status" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable appointmentTable = new JTable(appointmentModel);
    private final List<Appointment> appointments = new ArrayList<>();

    private final JButton btnShowSlots = new JButton("Find slots");
    private final JButton btnBook = new JButton("Book");
    private final JButton btnReschedule = new JButton("Move");
    private final JButton btnCancel = new JButton("Cancel appointment");
    private final JLabel lblStatus = new JLabel(" ");

    /**
     * Creates the screen.
     *
     * @param service the contract of C5 to manage the appointments
     * @param requester the username of the admissions staff
     * @param confirm asks the user a question and returns whether they accept (the tests pass a fake)
     * @param onBack what to do when the user presses <em>Back</em>
     */
    /**
     * Creates the screen using the system clock for the default day.
     *
     * @param service the contract of C5 to manage the appointments
     * @param requester the username of the admissions staff
     * @param confirm asks the user a question and returns whether they accept (the tests pass a fake)
     * @param onBack what to do when the user presses <em>Back</em>
     */
    public IU_ManageAppointments(IAppointmentService service, String requester, Predicate<String> confirm,
            Runnable onBack) {
        this(service, requester, confirm, onBack, Clock.systemDefaultZone());
    }

    /**
     * Creates the screen with an explicit clock for the default day.
     *
     * @param service the contract of C5 to manage the appointments
     * @param requester the username of the admissions staff
     * @param confirm asks the user a question and returns whether they accept (the tests pass a fake)
     * @param onBack what to do when the user presses <em>Back</em>
     * @param clock gives the default day shown when the screen opens
     */
    public IU_ManageAppointments(IAppointmentService service, String requester, Predicate<String> confirm,
            Runnable onBack, Clock clock) {
        this.service = service;
        this.requester = requester;
        this.confirm = confirm;
        this.onBack = onBack;
        this.clock = clock;
        setLayout(new BorderLayout(8, 8));
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        header.add(new JLabel("Manage appointments"));
        add(header, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        loadData();
    }

    private JSplitPane buildBody() {
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.add(new JLabel("Pending scheduling needs"), BorderLayout.NORTH);
        needTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        needTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                slots.clear();
                slotModel.setRowCount(0);
            }
        });
        left.add(new JScrollPane(needTable), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(4, 4));
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(3, 4, 3, 4);
        constraints.gridx = 0;
        constraints.gridy = 0;
        top.add(new JLabel("Day"), constraints);
        constraints.gridx = 1;
        top.add(txtDate, constraints);
        constraints.gridx = 2;
        btnShowSlots.setMnemonic('F');
        btnShowSlots.addActionListener(event -> onFindSlots());
        top.add(btnShowSlots, constraints);
        constraints.gridx = 3;
        btnBook.setMnemonic('B');
        btnBook.addActionListener(event -> onBookSlot());
        top.add(btnBook, constraints);
        right.add(top, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(4, 4));
        middle.add(new JLabel("Free slots of the test type of the selected need"), BorderLayout.NORTH);
        slotTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        middle.add(new JScrollPane(slotTable), BorderLayout.CENTER);
        right.add(middle, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        btnReschedule.setMnemonic('M');
        btnReschedule.addActionListener(event -> onReschedule());
        btnCancel.setMnemonic('C');
        btnCancel.addActionListener(event -> onCancel());
        buttons.add(btnReschedule);
        buttons.add(btnCancel);
        bottom.add(buttons, BorderLayout.NORTH);
        appointmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appointmentTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                slots.clear();
                slotModel.setRowCount(0);
            }
        });
        bottom.add(new JScrollPane(appointmentTable), BorderLayout.CENTER);
        right.add(bottom, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.4);
        return split;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(4, 4));
        footer.add(lblStatus, BorderLayout.CENTER);
        JPanel back = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        JButton btnBack = new JButton("Back");
        btnBack.setMnemonic('B');
        btnBack.addActionListener(event -> onBack.run());
        back.add(btnBack);
        footer.add(back, BorderLayout.EAST);
        return footer;
    }

    /**
     * Loads the pending needs and the appointments. Returns {@code true} when the data was read and displayed, and
     * {@code false} when it could not be read (the status already shows the reason).
     *
     * @return {@code true} if the data was read successfully
     */
    public boolean loadData() {
        txtDate.setText(txtDate.getText().isEmpty() ? DAY.format(LocalDate.now(clock)) : txtDate.getText());
        List<PendingNeedView> readNeeds;
        List<Appointment> readAppointments;
        try {
            readNeeds = service.getPendingSchedulingNeeds(requester);
            readAppointments = service.getAppointments(requester);
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
            return false;
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
            return false;
        } catch (RuntimeException exception) {
            showErrors(List.of("The data could not be read: " + exception.getMessage()));
            return false;
        }
        needs.clear();
        needs.addAll(readNeeds);
        needModel.setRowCount(0);
        for (PendingNeedView need : needs) {
            needModel.addRow(new Object[] { need.getNeedId(), need.getPatientName(),
                need.getTestTypeName(), need.getPriorityId(),
                need.getOrderDate() == null ? "" : DAY.format(need.getOrderDate()),
                need.getDueDate() == null ? "" : DAY.format(need.getDueDate()) });
        }
        appointments.clear();
        appointments.addAll(readAppointments);
        displayAppointments();
        lblStatus.setText(" ");
        return true;
    }

    private void displayAppointments() {
        appointmentModel.setRowCount(0);
        for (Appointment appointment : appointments) {
            appointmentModel.addRow(new Object[] { appointment.getAppointmentId(), appointment.getPatientId(),
                appointment.getTestTypeId(), MOMENT.format(appointment.getStart()), appointment.getStatus() });
        }
    }

    /** Asks C5 for the free slots of the day and of the test type of the selected need or appointment. */
    public void onFindSlots() {
        String testTypeId = null;
        Appointment selectedAppointment = selectedAppointment();
        if (selectedAppointment != null) {
            testTypeId = selectedAppointment.getTestTypeId();
        }
        PendingNeedView need = selectedNeed();
        if (need == null && testTypeId == null) {
            showErrors(List.of("Select a pending need or an appointment before looking for slots."));
            return;
        }
        if (testTypeId == null) {
            testTypeId = need.getTestTypeId();
        }
        LocalDate day;
        try {
            day = LocalDate.parse(txtDate.getText().trim(), DAY);
        } catch (DateTimeParseException invalid) {
            showErrors(List.of("The day must be written as dd/MM/yyyy"));
            return;
        }
        try {
            slots.clear();
            slots.addAll(service.getFreeSlots(requester, testTypeId, day, day));
            slotModel.setRowCount(0);
            for (TimeSlot slot : slots) {
                slotModel.addRow(new Object[] { slot.getRoom(), MOMENT.format(slot.getStart()),
                    MOMENT.format(slot.getEnd()) });
            }
            if (slots.isEmpty()) {
                showErrors(List.of("There are no free slots on that day for that test type."));
            } else {
                lblStatus.setText(slots.size() == 1 ? "1 free slot." : slots.size() + " free slots.");
            }
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
        }
    }

    private void onBookSlot() {
        PendingNeedView need = selectedNeed();
        TimeSlot slot = selectedSlot();
        if (need == null || slot == null) {
            showErrors(List.of("Select a pending need and one of its free slots."));
            return;
        }
        if (!confirm.test("Book the patient " + need.getPatientName() + " on " + MOMENT.format(slot.getStart())
                + "?")) {
            return;
        }
        onBookSlot(need.getNeedId(), slot);
    }

    /**
     * Books a slot for a need.
     *
     * @param needId the id of the pending need
     * @param slot the slot to book
     */
    public void onBookSlot(String needId, TimeSlot slot) {
        try {
            Appointment created = service.bookAppointment(requester, needId, slot);
            slots.clear();
            slotModel.setRowCount(0);
            if (loadData()) {
                showConfirmation("Appointment " + created.getAppointmentId() + " booked on "
                        + MOMENT.format(created.getStart()) + ".");
            }
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
        }
    }

    private void onReschedule() {
        Appointment appointment = selectedAppointment();
        TimeSlot slot = selectedSlot();
        if (appointment == null || slot == null) {
            showErrors(List.of("Select an appointment and one of the free slots."));
            return;
        }
        if (!confirm.test("Move the appointment " + appointment.getAppointmentId()
                + " to " + MOMENT.format(slot.getStart()) + "?")) {
            return;
        }
        onRescheduleAppointment(appointment.getAppointmentId(), slot);
    }

    /**
     * Moves an appointment to a new slot.
     *
     * @param appointmentId the id of the appointment
     * @param newSlot the new slot
     */
    public void onRescheduleAppointment(String appointmentId, TimeSlot newSlot) {
        try {
            Appointment moved = service.rescheduleAppointment(requester, appointmentId, newSlot);
            slots.clear();
            slotModel.setRowCount(0);
            if (loadData()) {
                showConfirmation("Appointment " + moved.getAppointmentId() + " moved to "
                        + MOMENT.format(moved.getStart()) + ".");
            }
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
        }
    }

    private void onCancel() {
        Appointment appointment = selectedAppointment();
        if (appointment == null) {
            showErrors(List.of("Select the appointment to cancel."));
            return;
        }
        if (!confirm.test("Cancel the appointment " + appointment.getAppointmentId() + "?")) {
            return;
        }
        onCancelAppointment(appointment.getAppointmentId());
    }

    /**
     * Cancels an appointment.
     *
     * @param appointmentId the id of the appointment
     */
    public void onCancelAppointment(String appointmentId) {
        try {
            Appointment cancelled = service.cancelAppointment(requester, appointmentId);
            slots.clear();
            slotModel.setRowCount(0);
            if (loadData()) {
                showConfirmation("Appointment " + cancelled.getAppointmentId() + " cancelled.");
            }
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
        }
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
        btnShowSlots.setEnabled(false);
        btnBook.setEnabled(false);
        btnReschedule.setEnabled(false);
        btnCancel.setEnabled(false);
        needTable.setEnabled(false);
        slotTable.setEnabled(false);
        appointmentTable.setEnabled(false);
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

    private TimeSlot selectedSlot() {
        int row = slotTable.getSelectedRow();
        return row < 0 || row >= slots.size() ? null : slots.get(row);
    }

    private Appointment selectedAppointment() {
        int row = appointmentTable.getSelectedRow();
        return row < 0 || row >= appointments.size() ? null : appointments.get(row);
    }

    /** The table of pending needs (for the tests). */
    public JTable getNeedsTable() {
        return needTable;
    }

    /** The table of free slots (for the tests). */
    public JTable getSlotsTable() {
        return slotTable;
    }

    /** The table of appointments (for the tests). */
    public JTable getAppointmentsTable() {
        return appointmentTable;
    }

    /** The text of the message area (for the tests). */
    public String getStatusMessage() {
        return lblStatus.getText();
    }

    /** Whether the status of an appointment is cancelled (helper for the tests). */
    public static boolean isCancelled(Appointment appointment) {
        return appointment.getStatus() == AppointmentStatus.CANCELLED;
    }
}
