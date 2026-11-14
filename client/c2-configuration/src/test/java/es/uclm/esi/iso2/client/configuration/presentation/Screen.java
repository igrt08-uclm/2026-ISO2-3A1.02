package es.uclm.esi.iso2.client.configuration.presentation;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * Finds the controls of a screen through the public Swing API (no reflection): a field by the text of its label,
 * a button or check box by its text. The screens are only built, never shown, so the tests need no display.
 */
final class Screen {
    private Screen() {
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

    static JTextField field(Container root, String label) {
        return (JTextField) labelled(root, label);
    }

    @SuppressWarnings("unchecked")
    static <T> JComboBox<T> combo(Container root, String label) {
        return (JComboBox<T>) labelled(root, label);
    }

    private static Component labelled(Container root, String label) {
        return all(root, JLabel.class).stream().filter(l -> label.equals(l.getText()) && l.getLabelFor() != null)
                .findFirst().orElseThrow(() -> new AssertionError("No field with the label " + label))
                .getLabelFor();
    }

    static JButton button(Container root, String text) {
        return all(root, JButton.class).stream().filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("No button " + text));
    }

    static JCheckBox check(Container root, String text) {
        return all(root, JCheckBox.class).stream().filter(c -> text.equals(c.getText())).findFirst()
                .orElseThrow(() -> new AssertionError("No check box " + text));
    }

    static JTable table(Container root) {
        return all(root, JTable.class).get(0);
    }

    static JTabbedPane tabs(Container root) {
        return all(root, JTabbedPane.class).get(0);
    }

    /** The tab with this title, as a container to look for its controls. */
    static Container tab(Container root, String title) {
        JTabbedPane tabs = tabs(root);
        return (Container) tabs.getComponentAt(tabs.indexOfTab(title));
    }

    /** The message area of the screen: the label at the bottom, next to the button Back. */
    static String status(Container root) {
        return statusLabel(root).getText();
    }

    /** The label of the message area, to read its colour. */
    static JLabel statusLabel(Container root) {
        return all(button(root, "Back").getParent(), JLabel.class).get(0);
    }

    static List<String> column(JTable table, int column) {
        List<String> values = new ArrayList<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            values.add(String.valueOf(table.getValueAt(row, column)));
        }
        return values;
    }

    static boolean enabled(Container root, String buttonText) {
        AbstractButton button = button(root, buttonText);
        return button.isEnabled();
    }
}
