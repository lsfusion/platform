package platform.client.form.sort;

import platform.interop.Order;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public abstract class TableSortableHeaderManager extends MouseAdapter {

    private final JTable table;

    public TableSortableHeaderManager(JTable table) {
        this.table = table;
    }

    public void mouseClicked(MouseEvent me) {

        if (me.getClickCount() != 2) return;
        if (!(me.getButton() == MouseEvent.BUTTON1 || me.getButton() == MouseEvent.BUTTON3)) return;

        TableColumnModel columnModel = table.getColumnModel();
        int viewColumn = columnModel.getColumnIndexAtX(me.getX());
        int column = columnModel.getColumn(viewColumn).getModelIndex();

        if (column != -1) {

            Boolean sortDir = getSortDirection(column);
            if (sortDir == null || !sortDir) {
                if (me.getButton() == MouseEvent.BUTTON1)
                    changeOrder(column, Order.REPLACE);
                 else
                    changeOrder(column, Order.ADD);
            } else {
                if (me.getButton() == MouseEvent.BUTTON1) {
                    changeOrder(column, Order.DIR);
                } else {
                    changeOrder(column, Order.REMOVE);
                }
            }
        }

    }

    private final List<Integer> orders = new ArrayList<Integer>();
    private final List<Boolean> orderDirections = new ArrayList<Boolean>();

    public Boolean getSortDirection(int column) {
        int ordNum = orders.indexOf(column);
        return (ordNum != -1) ? orderDirections.get(ordNum) : null;
    }

    public void changeOrder(int column, Order modiType) {
        int ordNum;
        switch (modiType) {
            case REPLACE:
                orders.clear();
                orderDirections.clear();

                orders.add(column);
                orderDirections.add(true);
                break;
            case ADD:
                orders.add(column);
                orderDirections.add(true);
                break;
            case DIR:
                ordNum = orders.indexOf(column);
                orderDirections.set(ordNum, !orderDirections.get(ordNum));
                break;
            case REMOVE:
                ordNum = orders.indexOf(column);
                orders.remove(ordNum);
                orderDirections.remove(ordNum);
                break;
        }

        orderChanged(column, modiType);
    }

    protected abstract void orderChanged(int column, Order modiType);
}
