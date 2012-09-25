package platform.client.form;

import platform.client.form.cell.CellTableInterface;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static javax.swing.SwingUtilities.isRightMouseButton;

public class CellTableContextMenuHandler extends MouseAdapter implements KeyListener {
    private ContextMenuPopup menu = new ContextMenuPopup();
    private final CellTableInterface cellTable;
    private final JTable jTable;

    public CellTableContextMenuHandler(CellTableInterface cellTable) {
        this.cellTable = cellTable;
        this.jTable = (JTable) cellTable;
    }

    public void install() {
        jTable.addMouseListener(this);
        jTable.addKeyListener(this);
    }

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

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    private void showContextMenu(int rowIndex, int columnIndex, Point point) {
        ClientPropertyDraw property = cellTable.getProperty(rowIndex, columnIndex);
        if (property != null) {
            LinkedHashMap<String, String> items = property.getContextMenuItems();
            if (items != null && !items.isEmpty()) {
                menu.show(rowIndex, columnIndex, point, items);
            }
        }
    }

    public class ContextMenuPopup extends JPopupMenu {
        public ContextMenuPopup() {
            setBackground(PropertyRendererComponent.SELECTED_ROW_BACKGROUND);
            setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, PropertyRendererComponent.SELECTED_ROW_BORDER_COLOR,
                                                      getBackground(), Color.GRAY, PropertyRendererComponent.SELECTED_ROW_BORDER_COLOR));
        }

        public void show(final int rowIndex, final int columnIndex, Point point, LinkedHashMap<String, String> items) {
            removeAll();

            for (Map.Entry<String, String> e : items.entrySet()) {
                final String action = e.getKey();
                final String caption = e.getValue();

                //todo: icon
                JMenuItem item = new JMenuItem(caption, null);
                item.setOpaque(false);
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        jTable.editCellAt(rowIndex, columnIndex, new InternalEditEvent(jTable, action));
                    }
                });
                add(item);
            }

            show(jTable, point.x, point.y);
        }
    }
}
