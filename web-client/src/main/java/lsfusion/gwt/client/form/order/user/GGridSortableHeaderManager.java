package lsfusion.gwt.client.form.order.user;

import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.HashMap;
import java.util.Iterator;
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

    public final void changeOrder(T columnKey, GOrder modiType) {
        if(changeOrderDirection(columnKey, modiType))
            orderChanged(columnKey, modiType);
    }

    private boolean changeOrderDirection(T columnKey, GOrder modiType) {
        if (columnKey == null || noSort(columnKey)) { // columnKey can be null for grid expand column
            return false;
        }

        switch (modiType) {
            case REPLACE:
                boolean direction = orderDirections.getOrDefault(columnKey, false);
                orderDirections.clear();
                orderDirections.put(columnKey, !direction);
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
        return true;
    }

    public final boolean changeOrders(GGroupObject groupObject, LinkedHashMap<T, Boolean> set, boolean alreadySet) {
        if(!GwtSharedUtils.hashEquals(orderDirections, set)) {
            orderDirections.clear();
            for (Iterator<Map.Entry<T, Boolean>> iterator = set.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<T, Boolean> entry = iterator.next();
                if (!changeOrderDirection(entry.getKey(), GOrder.ADD))
                    iterator.remove();
                else if (!entry.getValue()) {
                        boolean changed = changeOrderDirection(entry.getKey(), GOrder.DIR);
                        assert changed;
                    }
            }

            if (!alreadySet)
                ordersSet(groupObject, set);

            return true;
        }
        return false;
    }

    private boolean noSort(T columnKey) {
        if (columnKey instanceof HashMap) {
            for (Object entry : ((HashMap) columnKey).keySet()) {
                if (entry instanceof GPropertyDraw && ((GPropertyDraw) entry).noSort)
                    return true;
            }
        }
        return false;
    }

    public Map<T, Boolean> getOrderDirections() {
        return orderDirections;
    }

    protected abstract void orderChanged(T columnKey, GOrder modiType);

    protected abstract void ordersSet(GGroupObject groupObject, LinkedHashMap<T, Boolean> orders);

    protected abstract T getColumnKey(int column);
}
