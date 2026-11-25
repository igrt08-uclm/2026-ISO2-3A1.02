package es.uclm.esi.iso2.client.appointments.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.appointments.contracts.IDailyScheduleService;
import es.uclm.esi.iso2.common.appointments.domain.Appointment;
import es.uclm.esi.iso2.common.appointments.domain.ScheduleEntry;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Screen of UC14 (C5, {@code IU_DailySchedule}): the admissions staff chooses a day and sees its schedule per room, in
 * time order, with the patient, the test type and its preparation, and then the worklist of a room or of a physician
 * of that day. Cancelled appointments are never shown (D43). It only talks to the contract
 * {@link IDailyScheduleService} of {@code common}, never to the server classes.
 */
public class IU_DailySchedule extends JPanel {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color ERROR_COLOR = new Color(180, 0, 0);
    private static final String DENIED_MESSAGE = "You do not have permission to see the daily schedule.";

    private final transient IDailyScheduleService service;
    private final String requester;
    private final transient Runnable onBack;
    private final transient Clock clock;

    private final JTextField txtDay = new JTextField(10);
    private final JTextField txtRoom = new JTextField(14);
    private final JTextField txtPhysician = new JTextField(14);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "Room", "Start", "End", "Patient", "Test type", "Preparation" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable scheduleTable = new JTable(model);
    private final List<ScheduleEntry> entries = new ArrayList<>();

    private final JButton btnShowDay = new JButton("Show schedule");
    private final JButton btnRoom = new JButton("Room worklist");
    private final JButton btnPhysician = new JButton("Physician worklist");
    private final JLabel lblStatus = new JLabel(" ");
    private final Color textColor = lblStatus.getForeground();

    /**
     * Creates the screen using the system clock for the default day.
     *
     * @param service the contract of UC14 to read the schedule
     * @param requester the username of the admissions staff
     * @param onBack what to do when the user presses <em>Back</em>
     */
    public IU_DailySchedule(IDailyScheduleService service, String requester, Runnable onBack) {
        this(service, requester, onBack, Clock.systemDefaultZone());
    }

    /**
     * Creates the screen with an explicit clock for the default day.
     *
     * @param service the contract of UC14 to read the schedule
     * @param requester the username of the admissions staff
     * @param onBack what to do when the user presses <em>Back</em>
     * @param clock gives the default day shown when the screen opens
     */
    public IU_DailySchedule(IDailyScheduleService service, String requester, Runnable onBack, Clock clock) {
        this.service = service;
        this.requester = requester;
        this.onBack = onBack;
        this.clock = clock;
        setLayout(new BorderLayout(8, 8));
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        header.add(new JLabel("Daily schedule"));
        add(header, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        txtDay.setText(DAY.format(LocalDate.now(clock)));
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(4, 4));
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(3, 4, 3, 4);
        constraints.gridx = 0;
        constraints.gridy = 0;
        JLabel lblDay = new JLabel("Day");
        lblDay.setDisplayedMnemonic('D');
        lblDay.setLabelFor(txtDay);
        top.add(lblDay, constraints);
        constraints.gridx = 1;
        top.add(txtDay, constraints);
        constraints.gridx = 2;
        btnShowDay.setMnemonic('S');
        btnShowDay.addActionListener(event -> onShowDay());
        top.add(btnShowDay, constraints);
        body.add(top, BorderLayout.NORTH);

        scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        body.add(new JScrollPane(scheduleTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridBagLayout());
        GridBagConstraints second = new GridBagConstraints();
        second.insets = new Insets(3, 4, 3, 4);
        second.gridx = 0;
        second.gridy = 0;
        JLabel lblRoom = new JLabel("Room");
        lblRoom.setDisplayedMnemonic('R');
        lblRoom.setLabelFor(txtRoom);
        bottom.add(lblRoom, second);
        second.gridx = 1;
        bottom.add(txtRoom, second);
        second.gridx = 2;
        btnRoom.setMnemonic('M');
        btnRoom.addActionListener(event -> onRoomPressed());
        bottom.add(btnRoom, second);
        second.gridx = 3;
        JLabel lblPhysician = new JLabel("Physician");
        lblPhysician.setDisplayedMnemonic('P');
        lblPhysician.setLabelFor(txtPhysician);
        bottom.add(lblPhysician, second);
        second.gridx = 4;
        bottom.add(txtPhysician, second);
        second.gridx = 5;
        btnPhysician.setMnemonic('Y');
        btnPhysician.addActionListener(event -> onPhysicianPressed());
        bottom.add(btnPhysician, second);
        body.add(bottom, BorderLayout.SOUTH);
        return body;
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

    private void onShowDay() {
        LocalDate day = readDay();
        if (day != null) {
            onSelectDay(day);
        }
    }

    private void onRoomPressed() {
        LocalDate day = readDay();
        if (day != null) {
            onSelectRoom(day, txtRoom.getText().trim());
        }
    }

    private void onPhysicianPressed() {
        LocalDate day = readDay();
        if (day != null) {
            onSelectPhysician(day, txtPhysician.getText().trim());
        }
    }

    private LocalDate readDay() {
        try {
            return LocalDate.parse(txtDay.getText().trim(), DAY);
        } catch (DateTimeParseException invalid) {
            entries.clear();
            model.setRowCount(0);
            showErrors(List.of("The day must be written as dd/MM/yyyy"));
            return null;
        }
    }

    /**
     * Shows the schedule of a day, per room and in time order.
     *
     * @param date the day
     */
    public void onSelectDay(LocalDate date) {
        try {
            Map<String, List<ScheduleEntry>> schedule = service.getDailySchedule(requester, date);
            List<ScheduleEntry> rows = new ArrayList<>();
            for (List<ScheduleEntry> ofRoom : schedule.values()) {
                rows.addAll(ofRoom);
            }
            rows.sort(Comparator.comparing(entry -> entry.getAppointment().getRoom()));
            display(rows);
            if (rows.isEmpty()) {
                showInformation("There are no appointments on that day.");
            } else {
                showInformation(rows.size() == 1 ? "1 appointment." : rows.size() + " appointments.");
            }
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
        } catch (RuntimeException exception) {
            showErrors(List.of("The schedule could not be read: " + exception.getMessage()));
        }
    }

    /**
     * Shows the worklist of a room for a day.
     *
     * @param date the day
     * @param room the room
     */
    public void onSelectRoom(LocalDate date, String room) {
        try {
            List<ScheduleEntry> rows = service.getRoomWorklist(requester, date, room);
            display(rows);
            showInformation(rows.isEmpty() ? "There are no appointments in that room that day."
                    : "Worklist of the room: " + rows.size() + " appointments.");
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
        } catch (RuntimeException exception) {
            showErrors(List.of("The worklist could not be read: " + exception.getMessage()));
        }
    }

    /**
     * Shows the worklist of a physician for a day.
     *
     * @param date the day
     * @param physician the username of the physician
     */
    public void onSelectPhysician(LocalDate date, String physician) {
        try {
            List<ScheduleEntry> rows = service.getPhysicianWorklist(requester, date, physician);
            display(rows);
            showInformation(rows.isEmpty() ? "There are no appointments of that physician that day."
                    : "Worklist of the physician: " + rows.size() + " appointments.");
        } catch (AccessDeniedException deniedException) {
            showAccessDenied();
        } catch (ValidationException validationException) {
            showErrors(validationException.getErrors());
        } catch (RuntimeException exception) {
            showErrors(List.of("The worklist could not be read: " + exception.getMessage()));
        }
    }

    private void display(List<ScheduleEntry> rows) {
        entries.clear();
        entries.addAll(rows);
        model.setRowCount(0);
        for (ScheduleEntry entry : rows) {
            Appointment appointment = entry.getAppointment();
            model.addRow(new Object[] { appointment.getRoom(), TIME.format(appointment.getStart()),
                TIME.format(appointment.getEnd()), entry.getPatientName(), entry.getTestTypeName(),
                entry.getPreparation() });
        }
    }

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
        btnShowDay.setEnabled(false);
        btnRoom.setEnabled(false);
        btnPhysician.setEnabled(false);
        scheduleTable.setEnabled(false);
        lblStatus.setForeground(ERROR_COLOR);
        lblStatus.setText("⚠ " + DENIED_MESSAGE);
    }

    /** The table of the schedule and of the worklists (for the tests). */
    public JTable getScheduleTable() {
        return scheduleTable;
    }

    /** The text of the message area (for the tests). */
    public String getStatusMessage() {
        return lblStatus.getText();
    }
}
