package lsfusion.client.form.order.user;

import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.form.view.Column;
import lsfusion.interop.form.order.user.Order;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;

// what a header shows of the sortings and what a click on it asks for. The form keeps the orders of each group, and the
// header reads them as it draws a column - a property in one of its columns -, which its property's group is sorted by:
// a tree's header has the columns of several groups. true - ascending
public abstract class TableSortableHeaderManager extends MouseAdapter {

    private final JTable table;
    private final boolean ignoreFirstColumn;

    public TableSortableHeaderManager(JTable table) {
        this(table, false);
    }

    public TableSortableHeaderManager(JTable table, boolean ignoreFirstColumn) {
        this.table = table;
        this.ignoreFirstColumn = ignoreFirstColumn;
    }

    public final void mouseClicked(MouseEvent me) {

        if (me.getClickCount() != 2 || me.getButton() != MouseEvent.BUTTON1) return;

        TableColumnModel columnModel = table.getColumnModel();
        int viewColumn = columnModel.getColumnIndexAtX(me.getX());
        if(viewColumn == -1)
            return;
        int column = columnModel.getColumn(viewColumn).getModelIndex();

        if (table.getTableHeader().getCursor().getType() == Cursor.E_RESIZE_CURSOR) {
            int width = 0;
            for (int row = 0; row < Math.min(table.getRowCount(), 30); row++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width, width);
            }
            if (width > 0) {
                columnModel.getColumn(column).setPreferredWidth(width + 5);
                if (table instanceof GridTable) {
                    ((GridTable) table).setUserWidth(getColumn(column).property, width + 5);
                }
            }
            int totalPreferredWidth = 0;
            for (int c = 0; c < columnModel.getColumnCount(); c++) {
                if (c != column)
                    totalPreferredWidth += columnModel.getColumn(c).getPreferredWidth();
            }
            double coef = (double) (columnModel.getTotalColumnWidth() - width) / totalPreferredWidth;
            for (int c = 0; c < columnModel.getColumnCount(); c++) {
                if (c != column) {
                    int newWidth = (int) (columnModel.getColumn(c).getPreferredWidth() * coef);
                    columnModel.getColumn(c).setPreferredWidth(newWidth);
                    columnModel.getColumn(column).setPreferredWidth(width + 5);
                    if (table instanceof GridTable) {
                        ((GridTable) table).setUserWidth(getColumn(c).property, newWidth);
                    }
                }
            }

        } else {

            if (column != -1 && !(ignoreFirstColumn && column == 0)) {
                Column columnKey = getColumn(column);
                if (me.isShiftDown()) {
                    changeOrder(columnKey, Order.REMOVE);
                } else if (me.isControlDown()) {
                    if (getSortDirection(columnKey) == null) {
                        changeOrder(columnKey, Order.ADD);
                    } else {
                        changeOrder(columnKey, Order.DIR);
                    }
                } else {
                    changeOrder(columnKey, Order.REPLACE);
                }
            }
        }
    }

    public final Boolean getSortDirection(int column) {
        return getSortDirection(getColumn(column));
    }

    private Boolean getSortDirection(Column column) {
        return column != null ? getOrders(column.property.groupObject).get(column) : null;
    }

    // a click asks for the orders of the column's group changed by it, which are shown once the form has them - on a
    // column the design says not to sort by (noSort) nothing is asked; null is a tree's expand column
    public final void changeOrder(Column column, Order modiType) {
        if (column == null || column.property.noSort)
            return;

        ClientGroupObject group = column.property.groupObject;
        LinkedHashMap<Column, Boolean> orders = new LinkedHashMap<>(getOrders(group));
        switch (modiType) {
            case REPLACE:
                boolean direction = orders.getOrDefault(column, false);
                orders.clear();
                orders.put(column, !direction);
                break;
            case ADD:
                orders.put(column, true);
                break;
            case DIR:
                orders.put(column, !orders.get(column));
                break;
            case REMOVE:
                orders.remove(column);
                break;
        }
        changeOrders(group, orders);
    }

    // the orders the form has for a group, in their priority order - on a column the user may not sort by (noSort) too
    protected abstract LinkedHashMap<Column, Boolean> getOrders(ClientGroupObject group);

    // the orders a click asks for, all of the group's
    protected abstract void changeOrders(ClientGroupObject group, LinkedHashMap<Column, Boolean> orders);

    // the column the header shows at an index, null for none
    protected abstract Column getColumn(int column);
}
