package es.uclm.esi.iso2.client.configuration.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.shared.ValidationException;
import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * One tab of the configuration screen for a group whose entries have only an id, a name and a description: the
 * protocols, the appointment policies and the priorities (UC17). It shows the entries in a table, with the form of the
 * selected one at the right, and creates, changes and deactivates them through the server. The messages are not shown
 * here: they go to the screen that owns the panel.
 *
 * @param <T> the class of the entries of the group
 */
class ParameterGroupPanel<T> extends JPanel {
    private static final Color INACTIVE_COLOR = new Color(0x757575);
    private static final String ACTIVE = "active";
    private static final String INACTIVE = "inactive";
    private static final int STATE_COLUMN = 2;

    /** The operations that the server offers for one group. */
    interface Operations<T> {
        /** Returns every entry of the group, including the deactivated ones. */
        List<T> list() throws AccessDeniedException;

        /** Creates the entry, or changes it if its id already exists. */
        void save(T entry) throws AccessDeniedException, ValidationException;

        /** Deactivates the entry with that id. */
        void deactivate(String id) throws AccessDeniedException, ValidationException;
    }

    /** How to read an entry of the group and how to build a new one. */
    interface Adapter<T> {
        /** Returns the id of the entry. */
        String id(T entry);

        /** Returns the name of the entry. */
        String name(T entry);

        /** Returns the description of the entry. */
        String description(T entry);

        /** Tells whether the entry is active. */
        boolean isActive(T entry);

        /** Builds an active entry with these values. */
        T create(String id, String name, String description);
    }

    private final String kind;
    private final transient Operations<T> operations;
    private final transient Adapter<T> adapter;
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
    private final JTextField txtId = new JTextField(20);
    private final JTextField txtName = new JTextField(20);
    private final JTextField txtDescription = new JTextField(20);
    private final JButton btnNew = new JButton("New");
    private final JButton btnSave = new JButton("Save");
    private final JButton btnDeactivate = new JButton("Deactivate");

    private transient List<T> entries = new ArrayList<>();
    private boolean denied;

    /**
     * Creates the panel and reads the entries from the server.
     *
     * @param kind the name of one entry in lower case, for the messages (for example {@code "protocol"})
     * @param operations the operations of the server for the group
     * @param adapter how to read and build the entries
     * @param confirm asks the question of a deactivation and returns {@code true} if the user accepts it; the tests
     *        pass an answer, so that they do not open a window
     * @param onSuccess receives the message of a change that was made
     * @param onError receives the message of a change that was rejected
     * @param onAccessDenied is run when the server says that the user does not have the permission
     */
    ParameterGroupPanel(String kind, Operations<T> operations, Adapter<T> adapter, Predicate<String> confirm,
            Consumer<String> onSuccess, Consumer<String> onError, Runnable onAccessDenied) {
        super(new BorderLayout(16, 0));
        this.kind = kind;
        this.operations = operations;
        this.adapter = adapter;
        this.confirm = confirm;
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.onAccessDenied = onAccessDenied;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new InactiveGreyRenderer());
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

    /** Reads the entries again from the server and keeps the selected one. */
    void reload() {
        load(selectedId());
    }

    /** Disables every control: the user does not have the permission any more. */
    void disableAll() {
        denied = true;
        updateControls();
    }

    private void load(String idToSelect) {
        try {
            entries = new ArrayList<>(operations.list());
        } catch (AccessDeniedException exception) {
            onAccessDenied.run();
            return;
        }
        tableModel.setRowCount(0);
        for (T entry : entries) {
            tableModel.addRow(new Object[] {adapter.id(entry), adapter.name(entry),
                    adapter.isActive(entry) ? ACTIVE : INACTIVE});
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
        T selected = selectedEntry();
        txtId.setText(selected == null ? "" : adapter.id(selected));
        txtName.setText(selected == null ? "" : adapter.name(selected));
        txtDescription.setText(selected == null ? "" : adapter.description(selected));
        updateControls();
    }

    /** The id can only be written for a new entry, and a deactivated entry can only be read. */
    private void updateControls() {
        T selected = selectedEntry();
        boolean editable = !denied && (selected == null || adapter.isActive(selected));
        txtId.setEnabled(!denied && selected == null);
        txtName.setEnabled(editable);
        txtDescription.setEnabled(editable);
        btnNew.setEnabled(!denied);
        btnSave.setEnabled(editable);
        btnDeactivate.setEnabled(!denied && selected != null && adapter.isActive(selected));
    }

    private void startNew() {
        table.clearSelection();
        showSelected();
        txtId.requestFocusInWindow();
    }

    private void save() {
        T selected = selectedEntry();
        String id = selected == null ? txtId.getText().trim() : adapter.id(selected);
        if (selected == null && indexOf(id) >= 0) {
            onError.accept(article(kind) + " " + kind + " with the id " + id
                    + " already exists. Select it in the table to edit it.");
            return;
        }
        try {
            operations.save(adapter.create(id, txtName.getText().trim(), txtDescription.getText().trim()));
            onSuccess.accept(capitalized(kind) + " " + id + " saved.");
            load(id);
        } catch (AccessDeniedException exception) {
            onAccessDenied.run();
        } catch (ValidationException exception) {
            onError.accept(exception.getErrors().stream().map(error -> error + ".").collect(Collectors.joining(" ")));
        }
    }

    private void deactivate() {
        T selected = selectedEntry();
        if (selected == null) {
            return;
        }
        String id = adapter.id(selected);
        if (!confirm.test("Deactivate the " + kind + " " + id + "?")) {
            return;
        }
        try {
            operations.deactivate(id);
            onSuccess.accept(capitalized(kind) + " " + id + " deactivated.");
            load(id);
        } catch (AccessDeniedException exception) {
            onAccessDenied.run();
        } catch (ValidationException exception) {
            onError.accept(exception.getErrors().stream().map(error -> error + ".").collect(Collectors.joining(" ")));
            load(null);
        }
    }

    private T selectedEntry() {
        int row = table.getSelectedRow();
        return row < 0 || row >= entries.size() ? null : entries.get(row);
    }

    private String selectedId() {
        T selected = selectedEntry();
        return selected == null ? null : adapter.id(selected);
    }

    private int indexOf(String id) {
        for (int index = 0; id != null && index < entries.size(); index++) {
            if (adapter.id(entries.get(index)).equals(id)) {
                return index;
            }
        }
        return -1;
    }

    /** "An" before a vowel ("An appointment policy"), "A" otherwise ("A protocol"). */
    private static String article(String noun) {
        return "aeiou".indexOf(Character.toLowerCase(noun.charAt(0))) >= 0 ? "An" : "A";
    }

    private static String capitalized(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
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

    /** Each label right above its field, and the buttons at the bottom right. */
    private JPanel buildForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        addField(panel, "Id", txtId, 0, 'I');
        addField(panel, "Name", txtName, 2, 'M');
        addField(panel, "Description", txtDescription, 4, 'E');
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        buttons.add(btnSave);
        buttons.add(btnDeactivate);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 6;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.LAST_LINE_END;
        constraints.insets = new Insets(16, 0, 0, 0);
        panel.add(buttons, constraints);
        return panel;
    }

    private static void addField(JPanel panel, String text, JTextField field, int row, char mnemonic) {
        JLabel label = new JLabel(text);
        label.setDisplayedMnemonic(mnemonic);
        label.setLabelFor(field);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.insets = new Insets(row == 0 ? 0 : 12, 0, 0, 0);
        panel.add(label, constraints);
        constraints = new GridBagConstraints();
        constraints.gridy = row + 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(4, 0, 0, 0);
        panel.add(field, constraints);
    }

    /** Shows the deactivated entries in grey: the column "State" also says it with a word, not only with colour. */
    static class InactiveGreyRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            boolean inactive = INACTIVE.equals(table.getModel().getValueAt(row, STATE_COLUMN));
            if (!selected) {
                cell.setForeground(inactive ? INACTIVE_COLOR : table.getForeground());
            }
            return cell;
        }
    }
}
