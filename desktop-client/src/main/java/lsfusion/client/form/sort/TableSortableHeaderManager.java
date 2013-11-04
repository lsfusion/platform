package lsfusion.client.form.sort;

import lsfusion.base.OrderedMap;
import lsfusion.client.form.grid.GridTable;
import lsfusion.client.logics.ClientGroupObject;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Order;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

public abstract class TableSortableHeaderManager<T> extends MouseAdapter {

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

        if (me.getClickCount() != 2) return;
        if (!(me.getButton() == MouseEvent.BUTTON1 || me.getButton() == MouseEvent.BUTTON3)) return;

        TableColumnModel columnModel = table.getColumnModel();
        int viewColumn = columnModel.getColumnIndexAtX(me.getX());
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
                    ((GridTable) table).setUserWidth(getColumnProperty(column), width + 5);
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
                        ((GridTable) table).setUserWidth(getColumnProperty(c), newWidth);
                    }
                }
            }

        } else {

            if (column != -1 && !(ignoreFirstColumn && column == 0)) {
                T columnKey = getColumnKey(column);
                Boolean sortDir = orderDirections.get(columnKey);
                if (sortDir == null || !sortDir) {
                    if (me.getButton() == MouseEvent.BUTTON1)
                        changeOrder(columnKey, Order.REPLACE);
                    else
                        changeOrder(columnKey, Order.ADD);
                } else {
                    if (me.getButton() == MouseEvent.BUTTON1) {
                        changeOrder(columnKey, Order.DIR);
                    } else {
                        changeOrder(columnKey, Order.REMOVE);
                    }
                }
            }
        }
    }

    private final Map<T, Boolean> orderDirections = new OrderedMap<T, Boolean>();

    public Map<T, Boolean> getOrderDirections() {
        return orderDirections;
    }

    public final Boolean getSortDirection(int column) {
        if (column < 0 || column >= table.getColumnCount()) {
            return null;
        }

        return orderDirections.get(getColumnKey(column));
    }

    public final void changeOrder(T columnKey, Order modiType) {
        if (columnKey == null) {
            return;
        }

        switch (modiType) {
            case REPLACE:
                orderDirections.clear();
                orderDirections.put(columnKey, true);
                break;
            case ADD:
                orderDirections.put(columnKey, true);
                break;
            case DIR:
                orderDirections.put(columnKey, !orderDirections.get(columnKey));
                break;
            case REMOVE:
                orderDirections.remove(columnKey);
                break;
        }

        orderChanged(columnKey, modiType);
    }

    public final void clearOrders(ClientGroupObject groupObject) {
        orderDirections.clear();
        ordersCleared(groupObject);
    }

    protected abstract void orderChanged(T columnKey, Order modiType);

    protected abstract void ordersCleared(ClientGroupObject groupObject);

    protected abstract T getColumnKey(int column);

    protected abstract ClientPropertyDraw getColumnProperty(int column);
}
