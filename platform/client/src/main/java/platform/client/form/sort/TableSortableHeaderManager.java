package platform.client.form.sort;

import platform.client.logics.ClientGroupObject;
import platform.interop.Order;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
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

        if (column != -1 && !(ignoreFirstColumn && column==0)) {
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

    private final Map<T, Boolean> orderDirections = new HashMap<T, Boolean>();

    public Map<T, Boolean> getOrderDirections(){
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
}
