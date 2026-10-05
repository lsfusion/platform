package lsfusion.client.form.order.user;

import lsfusion.base.Pair;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.form.order.user.Order;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
                if (me.isShiftDown()) {
                    changeOrder(columnKey, Order.REMOVE);
                } else if (me.isControlDown()) {
                    if (sortDir == null) {
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

    private final Map<T, Boolean> orderDirections = new OrderedMap<>();

    public Map<T, Boolean> getOrderDirections() {
        return orderDirections;
    }

    public final Boolean getSortDirection(int column) {
        if (column < 0 || column >= table.getColumnCount()) {
            return null;
        }

        return orderDirections.get(getColumnKey(column));
    }

    // a click asks for the orders shown changed by it, which are shown when the form has them (updateOrders)
    public final void changeOrder(T columnKey, Order modiType) {
        LinkedHashMap<T, Boolean> orders = new LinkedHashMap<>(orderDirections);
        if (changeOrderDirection(orders, columnKey, modiType))
            ordersChanged(columnKey, orders);
    }

    private boolean changeOrderDirection(Map<T, Boolean> orders, T columnKey, Order modiType) {
        if (columnKey == null || noSort(columnKey)) {
            return false;
        }

        switch (modiType) {
            case REPLACE:
                boolean direction = orders.getOrDefault(columnKey, false);
                orders.clear();
                orders.put(columnKey, !direction);
                break;
            case ADD:
                orders.put(columnKey, true);
                break;
            case DIR:
                orders.put(columnKey, !orders.get(columnKey));
                break;
            case REMOVE:
                orders.remove(columnKey);
                break;
        }

        return true;
    }

    // the orders the form has, in their priority order - on a column the user may not sort by (noSort) as well: shown,
    // nothing is sent; true - ascending. The same columns in another order are other orders
    public final boolean updateOrders(LinkedHashMap<T, Boolean> orders) {
        if(!new ArrayList<>(orderDirections.entrySet()).equals(new ArrayList<>(orders.entrySet()))) {
            orderDirections.clear();
            orderDirections.putAll(orders);
            return true;
        }
        return false;
    }

    private boolean noSort (T columnKey) {
        return columnKey instanceof Pair && ((Pair) columnKey).first instanceof ClientPropertyDraw && ((ClientPropertyDraw) ((Pair) columnKey).first).noSort;
    }

    // the orders a click on a column asks for - of all the columns of the header, a tree's of several groups; true -
    // ascending
    protected abstract void ordersChanged(T columnKey, LinkedHashMap<T, Boolean> orders);

    protected abstract T getColumnKey(int column);

    protected abstract ClientPropertyDraw getColumnProperty(int column);
}
