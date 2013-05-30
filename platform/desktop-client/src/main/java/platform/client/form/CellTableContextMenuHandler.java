package platform.client.form;

import platform.client.form.cell.CellTableInterface;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static javax.swing.SwingUtilities.isRightMouseButton;

public class CellTableContextMenuHandler {
    private final ClientPropertyContextMenuPopup menu = new ClientPropertyContextMenuPopup();
    private final CellTableInterface cellTable;
    private final JTable jTable;

    public CellTableContextMenuHandler(CellTableInterface cellTable) {
        this.cellTable = cellTable;
        this.jTable = (JTable) cellTable;
    }

    public void install() {
        jTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point point = e.getPoint();
                int rowIndex = jTable.rowAtPoint(point);
                int columnIndex = jTable.columnAtPoint(point);

                if (isRightMouseButton(e) && rowIndex != -1 && columnIndex != -1) {
                    jTable.changeSelection(rowIndex, columnIndex, false, false);
                    showContextMenu(rowIndex, columnIndex, e.getPoint());
                }
            }
        });
        jTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU) {
                    int rowIndex = jTable.getSelectedRow();
                    int columnIndex = jTable.getSelectedColumn();
                    Rectangle rect = jTable.getCellRect(rowIndex, columnIndex, true);
                    Point point = new Point(rect.x, rect.y + rect.height - 1);

                    showContextMenu(rowIndex, columnIndex, point);
                }
            }
        });
    }

    private void showContextMenu(final int rowIndex, final int columnIndex, final Point point) {
        ClientPropertyDraw property = cellTable.getProperty(rowIndex, columnIndex);
        menu.show(property, jTable, point, new ClientPropertyContextMenuPopup.ItemSelectionListener() {
            @Override
            public void onMenuItemSelected(String actionSID) {
                jTable.editCellAt(rowIndex, columnIndex, new InternalEditEvent(jTable, actionSID));
            }
        });
    }
}
