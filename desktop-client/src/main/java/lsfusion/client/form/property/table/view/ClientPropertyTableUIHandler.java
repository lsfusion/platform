package lsfusion.client.form.property.table.view;

import lsfusion.base.ReflectionUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.classes.data.ClientImageClass;
import lsfusion.client.classes.data.link.ClientImageLinkClass;
import lsfusion.client.controller.remote.RmiQueue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.view.ImagePropertyRenderer;
import lsfusion.client.form.property.cell.classes.view.link.ImageLinkPropertyRenderer;

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
    public void mousePressed(final MouseEvent e) {
        if (shouldIgnore(e)) {
            return;
        }

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
        //SwingUtilities2.adjustFocus(table);
        Class swingUtilities2Class = ReflectionUtils.classForName("sun.swing.SwingUtilities2");
        if(swingUtilities2Class != null) {
            ReflectionUtils.getStaticMethodValue(swingUtilities2Class, "adjustFocus", new Class[] {JComponent.class}, new Object[] {table});
        }

        Point p = e.getPoint();
        pressedRow = table.rowAtPoint(p);
        pressedCol = table.columnAtPoint(p);

        // The autoscroller can generate drag events outside the table's range.
        if ((pressedCol == -1) || (pressedRow == -1)) {
            return;
        }

        int oldRow = table.getCurrentRow();

        boolean isLeftMouseButton = isLeftMouseButton(e);
        boolean withCtrl = MouseEvent.getModifiersExText(e.getModifiersEx()).contains("Ctrl");
        boolean withShift = MouseEvent.getModifiersExText(e.getModifiersEx()).contains("Shift");

        boolean toggle = isLeftMouseButton && SwingUtils.isMenuShortcutKeyDown(e);
        boolean extend = isLeftMouseButton && e.isShiftDown();
        table.changeSelection(pressedRow, pressedCol, toggle, extend);

        boolean rowHasFocus = (oldRow == pressedRow);

        // todo: теперь rowHasFocus не обязательно, работает и без него,
        // todo: поэтому есть возможность корректно реализовать логику editOnSingleClick, если понадобится
        if (isLeftMouseButton && !(withCtrl || withShift) && (rowHasFocus || e.getClickCount() > 1)) {
            final ClientPropertyDraw property = table.getProperty(pressedRow, pressedCol);
            RmiQueue.runAction(new Runnable() {
                @Override
                public void run() {
                    if (table.editCellAt(pressedRow, pressedCol, e)) {
                        setDispatchComponent(e);
                        repostEvent(e);
                        table.prepareTextEditor();
                    } else {
                        if (property.baseType instanceof ClientImageClass) {
                            ImagePropertyRenderer.expandImage(((RawFileData) table.getValueAt(pressedRow, pressedCol)));
                            e.consume();
                        } else if (property.baseType instanceof ClientImageLinkClass) {
                            String value = (String) table.getValueAt(pressedRow, pressedCol);
                            RawFileData imageBytes = ImageLinkPropertyRenderer.readImage(property, value);
                            if (imageBytes != null) {
                                ImagePropertyRenderer.expandImage(imageBytes);
                                e.consume();
                            }    
                        }
                    }
                }
            });
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
        //SwingUtilities2.setSkipClickCount(dispatchComponent, e.getClickCount() - 1);
        Class swingUtilities2Class = ReflectionUtils.classForName("sun.swing.SwingUtilities2");
        if(swingUtilities2Class != null) {
            ReflectionUtils.invokeStaticMethod(swingUtilities2Class, "setSkipClickCount",
                    new Class[]{Component.class, int.class}, new Object[] {dispatchComponent, e.getClickCount() - 1});
        }
    }

    private void repostEvent(MouseEvent e) {
        if (dispatchComponent == null || !table.isEditing()) {
            return;
        }
        MouseEvent e2 = SwingUtilities.convertMouseEvent(table, e, dispatchComponent);
        dispatchComponent.dispatchEvent(e2);
    }
}
