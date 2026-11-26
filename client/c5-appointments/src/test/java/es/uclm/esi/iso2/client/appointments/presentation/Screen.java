package es.uclm.esi.iso2.client.appointments.presentation;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * Finds the controls of a screen through the public Swing API (no reflection): a field by the text of the label
 * that is just before it, a button by its text. The screens are only built, never shown, so the tests need no
 * display.
 */
final class Screen {
    private Screen() {
    }

    /** Every component of this type inside the container, at any depth, in the order of the layout. */
    static <T extends Component> List<T> all(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                found.add(type.cast(component));
            }
            if (component instanceof Container container) {
                found.addAll(all(container, type));
            }
        }
        return found;
    }

    /** The text field that is placed just after the label with this text, in the same panel. */
    static JTextField field(Container root, String label) {
        JLabel found = label(root, label);
        Container parent = found.getParent();
        for (int index = 0; index < parent.getComponentCount() - 1; index++) {
            if (parent.getComponent(index) == found) {
                return (JTextField) parent.getComponent(index + 1);
            }
        }
        throw new AssertionError("No field after the label " + label);
    }

    /** The button with this text. */
    static JButton button(Container root, String text) {
        return all(root, JButton.class).stream().filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("No button " + text));
    }

    /** The texts of the buttons of the screen, leaving out the arrows of the split pane and scroll bars. */
    static List<String> buttonTexts(Container root) {
        return all(root, JButton.class).stream().map(JButton::getText).filter(t -> t != null && !t.isEmpty())
                .toList();
    }

    /** The texts of the buttons that are enabled. */
    static List<String> enabledButtons(Container root) {
        return all(root, JButton.class).stream().filter(JButton::isEnabled).map(JButton::getText)
                .filter(t -> t != null && !t.isEmpty()).toList();
    }

    /** The label that shows exactly this text. */
    static JLabel label(Container root, String text) {
        return all(root, JLabel.class).stream().filter(l -> text.equals(l.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("No label " + text));
    }

    /** Every row of a table, each cell as text. */
    static List<List<String>> rows(JTable table) {
        List<List<String>> rows = new ArrayList<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            List<String> cells = new ArrayList<>();
            for (int column = 0; column < table.getColumnCount(); column++) {
                cells.add(String.valueOf(table.getValueAt(row, column)));
            }
            rows.add(cells);
        }
        return rows;
    }

    /** The names of the columns of a table, in order. */
    static List<String> columns(JTable table) {
        List<String> names = new ArrayList<>();
        for (int column = 0; column < table.getColumnCount(); column++) {
            names.add(table.getColumnName(column));
        }
        return names;
    }

    /** The text of the cell of a row in the column with this name. */
    static String cell(JTable table, int row, String column) {
        int index = columns(table).indexOf(column);
        if (index < 0) {
            throw new AssertionError("No column " + column);
        }
        return String.valueOf(table.getValueAt(row, index));
    }
}
