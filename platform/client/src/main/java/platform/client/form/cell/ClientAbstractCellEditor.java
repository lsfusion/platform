package platform.client.form.cell;

import platform.client.SwingUtils;
import platform.client.form.PropertyEditorComponent;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.KeyStrokes;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.util.EventObject;

public class ClientAbstractCellEditor extends AbstractCellEditor
        implements TableCellEditor {

    private EventObject editEvent;

    private PropertyEditorComponent propertyEditor;

    public boolean editPerformed = false;

    public Object getCellEditorValue() {
        try {
            return propertyEditor.getCellEditorValue();
        } catch (RemoteException e) {
            throw new RuntimeException("Ошибка при получении выбранного значения", e);
        }
    }

    public boolean isCellEditable(EventObject e) {
        editEvent = e;
        if (e instanceof KeyEvent) {

            KeyEvent event = (KeyEvent) e;

            if (KeyStrokes.isGroupCorrectionEvent(event)) return true; // групповая корректировка

            if (KeyStrokes.isKeyEvent(event, KeyEvent.CHAR_UNDEFINED)) return false;

            // ESC почему-то считается KEY_TYPED кнопкой, пока обрабатываем отдельно
            if (KeyStrokes.isEscapeEvent(event)) return false;

            //будем считать, что если нажата кнопка ALT то явно пользователь не хочет вводить текст
            //noinspection RedundantIfStatement
            if ((event.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) > 0) return false;

            return true;
        }

        if (e instanceof MouseEvent) {
            return true;
        }

        //noinspection RedundantIfStatement
        if (e == null) {
            // значит программно вызвали редактирование
            return true;
        }

        return false;
    }

    public Component getTableCellEditorComponent(JTable table,
                                                 Object ivalue,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {

        // лучше сбросить editEvent, иначе может происходить "залипание", если по какой-то причине не будет вызван isCellEditable
        EventObject editEvent = this.editEvent;
        this.editEvent = null;

        CellTableInterface cellTable = (CellTableInterface) table;

        if (cellTable.getForm().isReadOnlyMode() && cellTable.isDataChanging()) return null;

        ClientPropertyDraw property = cellTable.getProperty(column);

        try {
            if (cellTable.isDataChanging()) {
                propertyEditor = KeyStrokes.isObjectEditorDialogEvent(editEvent)
                                 ? property.getObjectEditorComponent(cellTable.getForm(), ivalue)
                                 : property.getEditorComponent(cellTable.getForm(), ivalue);
            } else {
                propertyEditor = property.getClassComponent(cellTable.getForm(), ivalue);
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении редактируемого значения", e);
        }

        Component comp = null;
        if (propertyEditor != null) {
            editPerformed = true;
            try {
                comp = propertyEditor.getComponent(SwingUtils.computeAbsoluteLocation(table), table.getCellRect(row, column, false), editEvent);
            } catch (Exception e) {
                throw new RuntimeException("Ошибка при получении редактируемого значения", e);
            }

            if (comp == null && propertyEditor.valueChanged()) {
                table.setValueAt(getCellEditorValue(), row, column);
            }
        }

        if (comp == null) {
            stopCellEditing();
        }

        return comp;
    }
}
