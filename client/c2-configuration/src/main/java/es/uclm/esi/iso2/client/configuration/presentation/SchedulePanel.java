package es.uclm.esi.iso2.client.configuration.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.configuration.contracts.ISystemParameterService;
import es.uclm.esi.iso2.common.configuration.domain.Schedule;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * The tab of the schedules of the configuration screen (UC17). A schedule belongs to a room or resource (its
 * identifier), has the days of the week in which it is open and the start and end times. The table shows the schedules,
 * with the form of the selected one at the right. The messages are not shown here: they go to the screen that owns the
 * panel.
 */
class SchedulePanel extends JPanel {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final String ACTIVE = "active";
    private static final String INACTIVE = "inactive";

    private final transient ISystemParameterService service;
    private final String requester;
    private final transient Predicate<String> confirm;
    private final transient Consumer<String> onSuccess;
    private final transient Consumer<String> onError;
    private final transient Runnable onAccessDenied;

    private final DefaultTableModel tableModel =
            new DefaultTableModel(new String[] {"Room or resource", "Hours", "State"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtResource = new JTextField(16);
    private final transient Map<DayOfWeek, JCheckBox> days = new EnumMap<>(DayOfWeek.class);
    private final JTextField txtStart = new JTextField(8);
    private final JTextField txtEnd = new JTextField(8);
    private final JButton btnNew = new JButton("New");
    private final JButton btnSave = new JButton("Save");
    private final JButton btnDeactivate = new JButton("Deactivate");

    private transient List<Schedule> entries = new ArrayList<>();
    private boolean denied;

    /**
     * Creates the panel and reads the schedules from the server.
     *
     * @param service the service of the parameters of the system
     * @param requester the username of the logged user
     * @param confirm asks the question of a deactivation and returns {@code true} if the user accepts it
     * @param onSuccess receives the message of a change that was made
     * @param onError receives the message of a change that was rejected
     * @param onAccessDenied is run when the server says that the user does not have the permission
     */
    SchedulePanel(ISystemParameterService service, String requester, Predicate<String> confirm,
            Consumer<String> onSuccess, Consumer<String> onError, Runnable onAccessDenied) {
        super(new BorderLayout(16, 0));
        this.service = service;
        this.requester = requester;
        this.confirm = confirm;
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.onAccessDenied = onAccessDenied;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        for (DayOfWeek day : DayOfWeek.values()) {
            days.put(day, new JCheckBox(day.getDisplayName(TextStyle.FULL, Locale.ENGLISH)));
        }
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new ParameterGroupPanel.InactiveGreyRenderer());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelected();
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
        load(null);
    }

    /** Reads the schedules again from the server and keeps the selected one. */
    void reload() {
        load(selectedResource());
    }

    /** Disables every control: the user does not have the permission any more. */
    void disableAll() {
        denied = true;
        updateControls();
    }

    private void load(String resourceToSelect) {
        try {
            entries = new ArrayList<>(service.getSchedules(requester));
        } catch (AccessDeniedException exception) {
            onAccessDenied.run();
            return;
        }
        tableModel.setRowCount(0);
        for (Schedule entry : entries) {
            tableModel.addRow(new Object[] {entry.getResource(), hours(entry), entry.isActive() ? ACTIVE : INACTIVE});
        }
        int row = indexOf(resourceToSelect);
        if (row >= 0) {
            table.setRowSelectionInterval(row, row);
        } else {
            table.clearSelection();
        }
        showSelected();
    }

    private static String hours(Schedule schedule) {
        return schedule.getStartTime().format(TIME_FORMAT) + " - " + schedule.getEndTime().format(TIME_FORMAT);
    }

    private void showSelected() {
        Schedule selected = selectedEntry();
        txtResource.setText(selected == null ? "" : selected.getResource());
        for (DayOfWeek day : DayOfWeek.values()) {
            days.get(day).setSelected(selected != null && selected.getDaysOfWeek().contains(day));
        }
        txtStart.setText(selected == null ? "" : selected.getStartTime().format(TIME_FORMAT));
        txtEnd.setText(selected == null ? "" : selected.getEndTime().format(TIME_FORMAT));
        updateControls();
    }

    /** The resource can only be written for a new schedule, and a deactivated one can only be read. */
    private void updateControls() {
        Schedule selected = selectedEntry();
        boolean editable = !denied && (selected == null || selected.isActive());
        txtResource.setEnabled(!denied && selected == null);
        days.values().forEach(check -> check.setEnabled(editable));
        txtStart.setEnabled(editable);
        txtEnd.setEnabled(editable);
        btnNew.setEnabled(!denied);
        btnSave.setEnabled(editable);
        btnDeactivate.setEnabled(!denied && selected != null && selected.isActive());
    }

    private void startNew() {
        table.clearSelection();
        showSelected();
        txtResource.requestFocusInWindow();
    }

    private void save() {
        Schedule selected = selectedEntry();
        String resource = selected == null ? txtResource.getText().trim() : selected.getResource();
        if (selected == null && indexOf(resource) >= 0) {
            onError.accept("A schedule for " + resource + " already exists. Select it in the table to edit it.");
            return;
        }
        LocalTime start;
        LocalTime end;
        try {
            start = parseTime(txtStart.getText());
            end = parseTime(txtEnd.getText());
        } catch (DateTimeParseException exception) {
            onError.accept("The times must be written as HH:mm, for example 08:30.");
            return;
        }
        Set<DayOfWeek> selectedDays = EnumSet.noneOf(DayOfWeek.class);
        days.forEach((day, check) -> {
            if (check.isSelected()) {
                selectedDays.add(day);
            }
        });
        try {
            service.saveSchedule(requester, new Schedule(resource, selectedDays, start, end, true));
            onSuccess.accept("Schedule of " + resource + " saved.");
            load(resource);
        } catch (AccessDeniedException exception) {
            onAccessDenied.run();
        } catch (ValidationException exception) {
            onError.accept(joined(exception));
        }
    }

    /** An empty text gives {@code null}: the server says that the time is required. */
    private static LocalTime parseTime(String text) {
        return text.isBlank() ? null : LocalTime.parse(text.trim(), TIME_FORMAT);
    }

    private void deactivate() {
        Schedule selected = selectedEntry();
        if (selected == null || !confirm.test("Deactivate the schedule of " + selected.getResource() + "?")) {
            return;
        }
        try {
            service.deactivateSchedule(requester, selected.getResource());
            onSuccess.accept("Schedule of " + selected.getResource() + " deactivated.");
            load(selected.getResource());
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

    private Schedule selectedEntry() {
        int row = table.getSelectedRow();
        return row < 0 || row >= entries.size() ? null : entries.get(row);
    }

    private String selectedResource() {
        Schedule selected = selectedEntry();
        return selected == null ? null : selected.getResource();
    }

    private int indexOf(String resource) {
        for (int index = 0; resource != null && index < entries.size(); index++) {
            if (entries.get(index).getResource().equals(resource)) {
                return index;
            }
        }
        return -1;
    }

    private JPanel buildList() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(380, 220));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        buttons.add(btnNew);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    /** Each label right above its control, and the buttons at the bottom right. */
    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        add(panel, "Room or resource", 'O', txtResource, 0, 0);
        JPanel dayChecks = new JPanel(new GridLayout(0, 2, 16, 0));
        days.values().forEach(dayChecks::add);
        add(panel, "Days of the week", 'W', dayChecks, 2, 12);
        add(panel, "Start time (HH:mm)", 'M', txtStart, 4, 12);
        add(panel, "End time (HH:mm)", 'E', txtEnd, 6, 12);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        buttons.add(btnSave);
        buttons.add(btnDeactivate);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 8;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.LAST_LINE_END;
        constraints.insets = new Insets(16, 0, 0, 0);
        panel.add(buttons, constraints);
        return panel;
    }

    private static void add(JPanel panel, String text, char mnemonic, java.awt.Component control, int row,
            int topSpace) {
        JLabel label = new JLabel(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(control);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.insets = new Insets(topSpace, 0, 0, 0);
        panel.add(label, constraints);
        constraints = new GridBagConstraints();
        constraints.gridy = row + 1;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.insets = new Insets(4, 0, 0, 0);
        panel.add(control, constraints);
    }
}
