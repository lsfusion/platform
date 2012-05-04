package platform.client.form;

import platform.client.SwingUtils;
import sun.swing.SwingUtilities2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Основа этого класса - copy/paste из BasicTableUI$Handler
 */
final class ClientPropertyTableUIHandler extends MouseAdapter {
    private final ClientPropertyTable table;

    private Component dispatchComponent;
    private int pressedRow;
    private int pressedCol;

    public ClientPropertyTableUIHandler(ClientPropertyTable table) {
        this.table = table;
    }

    public void mousePressed(MouseEvent e) {
        if (SwingUtilities2.shouldIgnore(e, table)) {
            return;
        }

        boolean hasFocus = SwingUtils.isFocusOwnerDescending(table);

        if (!table.getForm().commitCurrentEditing()) {
            return;
        }

        //забираем фокус после того как завершили редактирвоание
        SwingUtilities2.adjustFocus(table);

        Point p = e.getPoint();
        pressedRow = table.rowAtPoint(p);
        pressedCol = table.columnAtPoint(p);

        // The autoscroller can generate drag events outside the table's range.
        if ((pressedCol == -1) || (pressedRow == -1)) {
            return;
        }


        int oldRow = table.getCurrentRow();

        //сначала сами устанавливаем текущую строку,
        //если после обновления ключей она исчезла, то не редактируем вообще
        boolean keySelected = table.trySelectCell(pressedRow, pressedCol, e);

        //реально будем работать с новым текущим рядом, а не с запрошенным,
        //что позволит спокойно обновить ключи в GridTable
        pressedRow = table.getCurrentRow();

        boolean rowHasFocus = hasFocus && (oldRow == pressedRow);

        if (keySelected && (rowHasFocus || e.getClickCount() > 1)) {
            if (table.editCellAt(pressedRow, pressedCol, e)) {
                setDispatchComponent(e);
                repostEvent(e);
                table.prepareTextEditor();
            }
        }

        table.changeSelection(pressedRow, pressedCol, SwingUtils.isMenuShortcutKeyDown(e), e.isShiftDown());
    }

    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities2.shouldIgnore(e, table)) {
            return;
        }

        repostEvent(e);
        dispatchComponent = null;
    }

    private void setDispatchComponent(MouseEvent e) {
        Component editorComponent = table.getEditorComponent();
        Point p = e.getPoint();
        Point p2 = SwingUtilities.convertPoint(table, p, editorComponent);
        dispatchComponent = SwingUtilities.getDeepestComponentAt(editorComponent, p2.x, p2.y);
        SwingUtilities2.setSkipClickCount(dispatchComponent, e.getClickCount() - 1);
    }

    private void repostEvent(MouseEvent e) {
        if (dispatchComponent == null || !table.isEditing()) {
            return;
        }
        MouseEvent e2 = SwingUtilities.convertMouseEvent(table, e, dispatchComponent);
        dispatchComponent.dispatchEvent(e2);
    }
}
