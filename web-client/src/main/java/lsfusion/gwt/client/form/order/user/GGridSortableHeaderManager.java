package lsfusion.gwt.client.form.order.user;

import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class GGridSortableHeaderManager<T> {
    private GGridPropertyTable table;
    private boolean ignoreFirstColumn;
    private LinkedHashMap<T, Boolean> orderDirections = new LinkedHashMap<>();

    public GGridSortableHeaderManager(GGridPropertyTable table, boolean ignoreFirstColumn) {
        this.table = table;
        this.ignoreFirstColumn = ignoreFirstColumn;
    }

    public void headerClicked(int columnIndex, boolean ctrlDown, boolean shiftDown) {
        if (columnIndex != -1 && !(ignoreFirstColumn && columnIndex==0)) {
            T columnKey = getColumnKey(columnIndex);
            Boolean sortDir = orderDirections.get(columnKey);
            if (shiftDown) {
                changeOrder(columnKey, GOrder.REMOVE);
            } else if (ctrlDown) {
                if (sortDir == null) {
                    changeOrder(columnKey, GOrder.ADD);
                } else {
                    changeOrder(columnKey, GOrder.DIR);
                }
            } else {
                changeOrder(columnKey, GOrder.REPLACE);
            }
        }
    }

    public final Boolean getSortDirection(int column) {
        if (column < 0 || column >= table.getColumnCount()) {
            return null;
        }

        return orderDirections.get(getColumnKey(column));
    }

    // a click asks for the orders shown changed by it, which are shown when the form has them (updateOrders)
    public final void changeOrder(T columnKey, GOrder modiType) {
        LinkedHashMap<T, Boolean> orders = new LinkedHashMap<>(orderDirections);
        if (changeOrderDirection(orders, columnKey, modiType))
            ordersChanged(columnKey, orders);
    }

    private boolean changeOrderDirection(LinkedHashMap<T, Boolean> orders, T columnKey, GOrder modiType) {
        if (columnKey == null || noSort(columnKey)) { // columnKey can be null for grid expand column
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

    public Map<T, Boolean> getOrderDirections() {
        return orderDirections;
    }

    // the orders a click on a column asks for - of all the columns of the header, a tree's of several groups; true -
    // ascending
    protected abstract void ordersChanged(T columnKey, LinkedHashMap<T, Boolean> orders);

    protected abstract T getColumnKey(int column);

    // whether the design says the column must not be sorted by (noSort) - asked of the column key, which each kind of
    // table has its own shape of
    protected abstract boolean noSort(T columnKey);
}
