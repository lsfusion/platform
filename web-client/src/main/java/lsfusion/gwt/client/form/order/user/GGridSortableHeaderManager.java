package lsfusion.gwt.client.form.order.user;

import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.view.Column;

import java.util.LinkedHashMap;

// what a header shows of the sortings and what a click on it asks for. The form keeps the orders of each group, and the
// header reads them as it draws a column - a property in one of its columns -, which its property's group is sorted by:
// a tree's header has the columns of several groups. true - ascending
public abstract class GGridSortableHeaderManager {
    private final boolean ignoreFirstColumn;

    public GGridSortableHeaderManager(boolean ignoreFirstColumn) {
        this.ignoreFirstColumn = ignoreFirstColumn;
    }

    public void headerClicked(int columnIndex, boolean ctrlDown, boolean shiftDown) {
        if (columnIndex != -1 && !(ignoreFirstColumn && columnIndex==0)) {
            Column column = getColumn(columnIndex);
            if (shiftDown) {
                changeOrder(column, GOrder.REMOVE);
            } else if (ctrlDown) {
                if (getSortDirection(column) == null) {
                    changeOrder(column, GOrder.ADD);
                } else {
                    changeOrder(column, GOrder.DIR);
                }
            } else {
                changeOrder(column, GOrder.REPLACE);
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
    public final void changeOrder(Column column, GOrder modiType) {
        if (column == null || column.property.noSort)
            return;

        GGroupObject group = column.property.groupObject;
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
    protected abstract LinkedHashMap<Column, Boolean> getOrders(GGroupObject group);

    // the orders a click asks for, all of the group's
    protected abstract void changeOrders(GGroupObject group, LinkedHashMap<Column, Boolean> orders);

    // the column the header shows at an index, null for none
    protected abstract Column getColumn(int column);
}
