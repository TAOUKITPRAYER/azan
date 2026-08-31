package net.tawkit.remotebox.ui;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.util.function.IntConsumer;

/**
 * Renders a JButton in a table column and invokes {@code onClick} with the model row index.
 * An empty cell value renders as a blank cell (no button).
 */
class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

    private final JButton renderButton = new JButton();
    private final JButton editButton = new JButton();
    private final IntConsumer onClick;
    private final JTable table;
    private int editingRow = -1;

    ButtonColumn(JTable table, int column, IntConsumer onClick) {
        this.table = table;
        this.onClick = onClick;
        renderButton.setFocusable(false);
        editButton.setFocusable(false);
        editButton.addActionListener(e -> {
            int row = editingRow;
            fireEditingStopped();
            if (row >= 0) {
                SwingUtilities.invokeLater(() -> onClick.accept(table.convertRowIndexToModel(row)));
            }
        });
        table.getColumnModel().getColumn(column).setCellRenderer(this);
        table.getColumnModel().getColumn(column).setCellEditor(this);
    }

    @Override
    public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
        String text = value == null ? "" : value.toString();
        renderButton.setText(text);
        renderButton.setVisible(!text.isEmpty());
        return renderButton;
    }

    @Override
    public Component getTableCellEditorComponent(JTable t, Object value, boolean sel, int row, int col) {
        editingRow = row;
        String text = value == null ? "" : value.toString();
        editButton.setText(text);
        editButton.setVisible(!text.isEmpty());
        return editButton;
    }

    @Override
    public Object getCellEditorValue() {
        return editButton.getText();
    }
}
