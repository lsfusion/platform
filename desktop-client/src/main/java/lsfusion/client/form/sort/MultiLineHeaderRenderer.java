package lsfusion.client.form.sort;

import lsfusion.client.Main;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class MultiLineHeaderRenderer implements TableCellRenderer {

    protected final static ImageIcon arrowUpIcon = new ImageIcon(MultiLineHeaderRenderer.class.getResource("/images/arrowup.png"));
    protected final static ImageIcon arrowDownIcon = new ImageIcon(MultiLineHeaderRenderer.class.getResource("/images/arrowdown.png"));

    private final TableCellRenderer tableCellRenderer;
    private final TableSortableHeaderManager sortableHeaderManager;

    public MultiLineHeaderRenderer(TableCellRenderer originalTableCellRenderer, TableSortableHeaderManager isortableHeaderManager) {
        tableCellRenderer = originalTableCellRenderer;
        sortableHeaderManager = isortableHeaderManager;
    }

    public Component getTableCellRendererComponent(JTable itable,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {

        if (value instanceof String) {
            value = "<html>" + value + "</html>";
        }

        Component comp = tableCellRenderer.getTableCellRendererComponent(itable, value, isSelected, hasFocus, row, column);
        if (comp instanceof JLabel) {
            JLabel label = (JLabel) comp;
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setVerticalAlignment(JLabel.TOP);

            Boolean sortDir = sortableHeaderManager.getSortDirection(column);
            if (sortDir != null) {
                label.setIcon(sortDir ? arrowUpIcon : arrowDownIcon);
            } else {
                label.setIcon(null);
            }
            label.setFont(label.getFont().deriveFont(Font.PLAIN, Main.getUIFontSize(10)));
        }
        return comp;
    }
}
