package platform.client.form.sort;

import platform.client.Main;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public abstract class GridHeaderRenderer implements TableCellRenderer {

    private final ImageIcon arrowUpIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/arrowup.gif"));
    private final ImageIcon arrowDownIcon = new ImageIcon(getClass().getResource("/platform/client/form/images/arrowdown.gif"));

    private final TableCellRenderer tableCellRenderer;

    public GridHeaderRenderer(TableCellRenderer tableCellRenderer) {
        this.tableCellRenderer = tableCellRenderer;
    }

    public Component getTableCellRendererComponent(JTable itable,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {

        if (Main.module.isFull() && value instanceof String)
            value = "<html>" + value + "</html>";

        Component comp = tableCellRenderer.getTableCellRendererComponent(itable,
                value, isSelected, hasFocus, row, column);
        if (comp instanceof JLabel) {
            JLabel label = (JLabel) comp;
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setVerticalAlignment(JLabel.TOP);

            Boolean sortDir = getSortDirection(column);
            if (sortDir != null)
                label.setIcon(sortDir ? arrowUpIcon : arrowDownIcon);

            label.setFont(label.getFont().deriveFont(Font.PLAIN, 10));
        }
        return comp;
    }

    protected abstract Boolean getSortDirection(int column);
}
