package es.uclm.esi.iso2.client.access.presentation;

import es.uclm.esi.iso2.common.access.contracts.AccessDeniedException;
import es.uclm.esi.iso2.common.access.contracts.IAuditService;
import es.uclm.esi.iso2.common.access.domain.AuditLog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class IU_Audit extends JPanel {
    private static final String ALL_ACTIONS = "All actions";
    private static final String[] ACTIONS =
            {ALL_ACTIONS, "LOGIN_OK", "LOGIN_FAILED", "AUDIT_CONSULTED", "AUDIT_ACCESS_DENIED"};
    private static final String[] COLUMNS = {"Date", "User", "Action", "Resource"};
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final Color ERROR_COLOR = new Color(0xB00020);
    private static final String ERROR_ICON = "⚠ ";

    private final transient IAuditService auditService;
    private final String requester;

    private final JTextField filterAuthor = new JTextField(16);
    private final JComboBox<String> filterAction = new JComboBox<>(ACTIONS);
    private final JTextField filterTarget = new JTextField(22);
    private final JTextField filterFrom = new JTextField(10);
    private final JTextField filterTo = new JTextField(10);
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel lblStatus = new JLabel(" ");
    private final JButton btnSearch = new JButton("Search");
    private final JButton btnClear = new JButton("Clear");
    private final JButton btnBack = new JButton("Back");

    public IU_Audit(IAuditService auditService, String requester, Runnable onBack) {
        super(new BorderLayout(0, 12));
        this.auditService = auditService;
        this.requester = requester;
        setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        rejectFilterSeparator(filterAuthor);
        rejectFilterSeparator(filterTarget);
        rejectFilterSeparator(filterFrom);
        rejectFilterSeparator(filterTo);
        btnSearch.setMnemonic('S');
        btnClear.setMnemonic('C');
        btnBack.setMnemonic('B');
        btnBack.addActionListener(event -> onBack.run());
        btnSearch.addActionListener(event -> search());
        btnClear.addActionListener(event -> clear());
        filterAuthor.addActionListener(event -> search());
        filterTarget.addActionListener(event -> search());
        filterFrom.addActionListener(event -> search());
        filterTo.addActionListener(event -> search());

        add(buildFilters(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        applyFilter("");
    }

    public void applyFilter(String filters) {
        try {
            displayActivity(auditService.getActivityLog(requester, filters));
        } catch (AccessDeniedException exception) {
            tableModel.setRowCount(0);
            showError("You do not have permission to consult the audit log.");
        } catch (IllegalArgumentException exception) {
            showError("Check the dates: use the format yyyy-MM-dd.");
        }
    }

    public void displayActivity(List<AuditLog> logs) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        tableModel.setRowCount(0);
        for (AuditLog log : logs) {
            tableModel.addRow(new Object[] {dateFormat.format(log.getTimestamp()), log.getAuthor(), log.getAction(),
                log.getTargetResource()});
        }
        lblStatus.setForeground(getForeground());
        lblStatus.setText(statusFor(logs.size()));
    }

    private static String statusFor(int entries) {
        return switch (entries) {
            case 0 -> "No activity matches these filters.";
            case 1 -> "1 entry";
            default -> entries + " entries";
        };
    }

    private void showError(String message) {
        lblStatus.setForeground(ERROR_COLOR);
        lblStatus.setText(ERROR_ICON + message);
    }

    private void search() {
        applyFilter(buildFilterText());
    }

    private void clear() {
        filterAuthor.setText("");
        filterAction.setSelectedItem(ALL_ACTIONS);
        filterTarget.setText("");
        filterFrom.setText("");
        filterTo.setText("");
        applyFilter("");
    }

    private String buildFilterText() {
        List<String> filters = new ArrayList<>();
        addFilter(filters, "author", filterAuthor.getText());
        if (!ALL_ACTIONS.equals(filterAction.getSelectedItem())) {
            addFilter(filters, "action", (String) filterAction.getSelectedItem());
        }
        addFilter(filters, "target", filterTarget.getText());
        addFilter(filters, "from", filterFrom.getText());
        addFilter(filters, "to", filterTo.getText());
        return String.join(";", filters);
    }

    private static void addFilter(List<String> filters, String key, String value) {
        if (!value.isBlank()) {
            filters.add(key + "=" + value.trim());
        }
    }

    /** A value cannot contain {@code ;}, because it separates the filters sent to the server. */
    private static void rejectFilterSeparator(JTextField field) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes)
                    throws BadLocationException {
                super.insertString(bypass, offset, text.replace(";", ""), attributes);
            }

            @Override
            public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
                    throws BadLocationException {
                super.replace(bypass, offset, length, text == null ? null : text.replace(";", ""), attributes);
            }
        });
    }

    private JPanel buildFilters() {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel lblTitle = new JLabel("Audit log");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 18f));
        add(panel, lblTitle, 0, 0, 3, 0);

        addFilter(panel, "User", filterAuthor, 0, 1);
        addFilter(panel, "Action", filterAction, 1, 1);
        addFilter(panel, "Patient or resource", filterTarget, 2, 1);
        addFilter(panel, "From (yyyy-MM-dd)", filterFrom, 0, 3);
        addFilter(panel, "To (yyyy-MM-dd)", filterTo, 1, 3);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        buttons.add(btnClear);
        buttons.add(btnSearch);
        add(panel, buttons, 2, 4, 1, 4);
        return panel;
    }

    private static void addFilter(JPanel panel, String text, Component field, int column, int row) {
        add(panel, new JLabel(text), column, row, 1, 12);
        add(panel, field, column, row + 1, 1, 4);
    }

    private static void add(JPanel panel, Component component, int column, int row, int width, int topSpace) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.gridwidth = width;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(topSpace, 0, 0, 12);
        panel.add(component, constraints);
    }

    private JScrollPane buildTable() {
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(280);
        table.setPreferredScrollableViewportSize(new Dimension(700, 240));
        return new JScrollPane(table);
    }

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(lblStatus, BorderLayout.WEST);
        panel.add(btnBack, BorderLayout.EAST);
        return panel;
    }
}
