package platform.client.form.sort;

import platform.interop.Order;

import javax.swing.table.TableColumnModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public abstract class GridHeaderMouseListener extends MouseAdapter {

    public void mouseClicked(MouseEvent me) {

        if (me.getClickCount() != 2) return;
        if (!(me.getButton() == MouseEvent.BUTTON1 || me.getButton() == MouseEvent.BUTTON3)) return;

        TableColumnModel columnModel = getColumnModel();
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

    protected abstract Boolean getSortDirection(int column);

    protected abstract TableColumnModel getColumnModel();

    protected abstract void changeOrder(int column, Order modiType);
}
