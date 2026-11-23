package es.uclm.esi.iso2.client.orders.presentation;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * Finds the controls of a screen through the public Swing API (no reflection): a field by the text of the label
 * that is just before it, a button by its text. The screens are only built, never shown, so the tests need no
 * display. It is the helper of the tests of UC07, with the same methods as the one of UC06 plus {@link #spinner}.
 */
final class FollowUpScreen {
    private FollowUpScreen() {
    }

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

    /** The control that is placed just after the label with this text, in the same panel. */
    static Component after(Container root, String label) {
        JLabel found = all(root, JLabel.class).stream().filter(l -> label.equals(l.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("No label " + label));
        Container parent = found.getParent();
        for (int index = 0; index < parent.getComponentCount() - 1; index++) {
            if (parent.getComponent(index) == found) {
                return parent.getComponent(index + 1);
            }
        }
        throw new AssertionError("No field after the label " + label);
    }

    static JTextField field(Container root, String label) {
        return (JTextField) after(root, label);
    }

    static JSpinner spinner(Container root, String label) {
        return (JSpinner) after(root, label);
    }

    @SuppressWarnings("unchecked")
    static JComboBox<Object> combo(Container root, String label) {
        return (JComboBox<Object>) after(root, label);
    }

    /** The texts that a list offers, in order. */
    static List<String> items(JComboBox<Object> combo) {
        List<String> items = new ArrayList<>();
        for (int index = 0; index < combo.getItemCount(); index++) {
            items.add(String.valueOf(combo.getItemAt(index)));
        }
        return items;
    }

    /** Selects the item of the list that shows this text. */
    static void choose(JComboBox<Object> combo, String text) {
        int index = items(combo).indexOf(text);
        if (index < 0) {
            throw new AssertionError("The list does not offer " + text);
        }
        combo.setSelectedIndex(index);
    }

    static JButton button(Container root, String text) {
        return all(root, JButton.class).stream().filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("No button " + text));
    }

    /** The texts of the buttons of the screen, leaving out the arrows of the lists and scroll bars, which have none. */
    static List<String> buttonTexts(Container root) {
        return all(root, JButton.class).stream().map(JButton::getText).filter(text -> !text.isEmpty()).toList();
    }

    /** The label that shows exactly this text. */
    static JLabel label(Container root, String text) {
        return all(root, JLabel.class).stream().filter(l -> text.equals(l.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("No label " + text));
    }

    /** The line with the number of patients found. */
    static String found(Container root) {
        return all(root, JLabel.class).stream().map(JLabel::getText).filter(text -> text.endsWith(" found."))
                .findFirst().orElseThrow(() -> new AssertionError("No line of patients found"));
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

    static List<String> columns(JTable table) {
        List<String> names = new ArrayList<>();
        for (int column = 0; column < table.getColumnCount(); column++) {
            names.add(table.getColumnName(column));
        }
        return names;
    }
}
