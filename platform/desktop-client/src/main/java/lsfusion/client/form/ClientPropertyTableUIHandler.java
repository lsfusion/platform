package lsfusion.client.form;

import lsfusion.client.SwingUtils;
import sun.swing.SwingUtilities2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;

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

    @SuppressWarnings("deprecation")
    public void mousePressed(MouseEvent e) {
        if (shouldIgnore(e)) {
            return;
        }

        boolean isLeftMouseButton = isLeftMouseButton(e);

        if (table.getForm().isEditing()) {
            //в свинге есть определённый баг с системой фокусов...
            //при удалении эдитора из таблицы фокус автотрансферится на следующий компонент nextComp ...
            //при этом последующие вызовы requestFocusInWindow на других компонентах уже не предотвратит посылку FOCUS_GAINED на этот nextComp.
            //поэтому, если мы сейчас же начнём редактирование в этой новой таблице, то в ней сработает editorRemover, когда FOCUS_GAINED наконец дойдёт...
            //поэтому если мы можем вычислить прошлую таблицу мы сразу переносим фокус в текущую...

            JComponent currentTableEditor = (JComponent) table.getForm().getCurrentEditingTable().getEditorComponent();

            Component currentNextFocusableComponent = currentTableEditor.getNextFocusableComponent();
            currentTableEditor.setNextFocusableComponent(table);

            if (!table.getForm().commitCurrentEditing()) {
                currentTableEditor.setNextFocusableComponent(currentNextFocusableComponent);
                return;
            }
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

        boolean toggle = isLeftMouseButton && SwingUtils.isMenuShortcutKeyDown(e);
        boolean extend = isLeftMouseButton && e.isShiftDown();
        table.changeSelection(pressedRow, pressedCol, toggle, extend);

        boolean rowHasFocus = (oldRow == pressedRow);

        // todo: теперь rowHasFocus не обязательно, работает и без него,
        // todo: поэтому есть возможность корректно реализовать логику editOnSingleClick, если понадобится
        if (isLeftMouseButton && (rowHasFocus || e.getClickCount() > 1)) {
            if (table.editCellAt(pressedRow, pressedCol, e)) {
                setDispatchComponent(e);
                repostEvent(e);
                table.prepareTextEditor();
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (!shouldIgnore(e)) {
            repostEvent(e);
            dispatchComponent = null;
        }
    }

    private boolean shouldIgnore(MouseEvent e) {
        return !table.isEnabled() ||
                e.isConsumed() ||
                !(isLeftMouseButton(e) || isRightMouseButton(e));
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
