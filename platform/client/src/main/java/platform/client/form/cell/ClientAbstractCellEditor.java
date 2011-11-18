package platform.client.form.cell;

import platform.client.ClientResourceBundle;
import platform.client.SwingUtils;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.LogicalPropertyEditor;
import platform.client.form.grid.GridTable;
import platform.client.form.renderer.ActionPropertyRenderer;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.KeyStrokes;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.EventObject;

public class ClientAbstractCellEditor extends AbstractCellEditor
        implements TableCellEditor {

    private EventObject editEvent;

    public PropertyEditorComponent propertyEditor;

    public boolean editPerformed = false;

    public Object getCellEditorValue() {
        try {
            return propertyEditor.getCellEditorValue();
        } catch (RemoteException e) {
            throw new RuntimeException(ClientResourceBundle.getString("errors.error.getting.selected.value"), e);
        }
    }

    public boolean isCellEditable(EventObject e) {
        editEvent = e;
        if (e instanceof KeyEvent) {

            KeyEvent event = (KeyEvent) e;

            if (KeyStrokes.isGroupCorrectionEvent(event)) return true; // групповая корректировка

            if (event.getKeyChar() == KeyEvent.CHAR_UNDEFINED) return false;

            // ESC почему-то считается KEY_TYPED кнопкой, пока обрабатываем отдельно
            if (KeyStrokes.isEscapeEvent(event)) return false;

            //будем считать, что если нажата кнопка ALT то явно пользователь не хочет вводить текст
            //noinspection RedundantIfStatement
            if ((event.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) > 0) return false;

            return true;
        }

        if (e instanceof MouseEvent) {
            if (e.getSource() instanceof GridTable) {
                GridTable table = (GridTable) e.getSource();
                int row = table.rowAtPoint(((MouseEvent) e).getPoint());
                int column = table.columnAtPoint(((MouseEvent) e).getPoint());
                PropertyRendererComponent renderer = table.getProperty(row, column).getRendererComponent();
                return !(renderer instanceof ActionPropertyRenderer);
            } else {
                return true;
            }
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

        ClientPropertyDraw property = cellTable.getProperty(row, column);
        if (property == null) {
            return null;
        }

        try {
            if (cellTable.isDataChanging()) {
                ClientGroupObjectValue columnKey = cellTable.getKey(row, column);
                propertyEditor = KeyStrokes.isObjectEditorDialogEvent(editEvent)
                                 ? property.getObjectEditorComponent(table, cellTable.getForm(), columnKey, ivalue)
                                 : property.getEditorComponent(table, cellTable.getForm(), columnKey, ivalue);
            } else {
                propertyEditor = property.getClassComponent(cellTable.getForm(), ivalue);
            }
        } catch (Exception e) {
            throw new RuntimeException(ClientResourceBundle.getString("errors.error.getting.editing.value"), e);
        }

        Component comp = null;
        if (propertyEditor != null) {
            editPerformed = true;
            try {
                comp = propertyEditor.getComponent(SwingUtils.computeAbsoluteLocation(table), table.getCellRect(row, column, false), editEvent);
            } catch (Exception e) {
                throw new RuntimeException(ClientResourceBundle.getString("errors.error.getting.editing.value"), e);
            }

            if (comp == null && propertyEditor.valueChanged()) {
                table.setValueAt(getCellEditorValue(), row, column);
            }

            if (propertyEditor instanceof LogicalPropertyEditor && comp != null) {
                Object value = getCellEditorValue();
                table.setValueAt(value == null ? true : null, row, column);
                comp = null;
            }
        }

        if (comp == null) {
            stopCellEditing();
        }

        return comp;
    }
}
