package platform.client.form.cell;

import platform.base.BaseUtils;
import platform.client.SwingUtils;
import platform.client.form.PropertyEditorComponent;
import platform.client.logics.ClientPropertyDraw;

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

    private PropertyEditorComponent currentComp;

    public Object getCellEditorValue() {
        try {
            return currentComp.getCellEditorValue();
        } catch (RemoteException e) {
            throw new RuntimeException("Ошибка при получении выбранного значения", e);
        }
    }

    public boolean isCellEditable(EventObject e) {

        editEvent = e;
        if (e instanceof KeyEvent) {

            KeyEvent event = (KeyEvent) e;

            if (event.getKeyCode() == KeyEvent.VK_F12) return true; // групповая корректировка

            if (event.getKeyChar() == KeyEvent.CHAR_UNDEFINED) return false;

            // ESC почему-то считается KEY_TYPED кнопкой, пока обрабатываем отдельно
            if (event.getKeyCode() == KeyEvent.VK_ESCAPE) return false;

            //будем считать, что если нажата кнопка ALT то явно пользователь не хочет вводить текст
            //noinspection RedundantIfStatement
            if ((event.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) > 0) return false;

            return true;
        }

        if (e instanceof MouseEvent) {

            MouseEvent event = (MouseEvent) e;

            //noinspection RedundantIfStatement
            if (event.getClickCount() < 2) return false;

            return true;
        }

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
            if (cellTable.isDataChanging())
                currentComp = property.getEditorComponent(cellTable.getForm(), ivalue);
            else
                currentComp = property.getClassComponent(cellTable.getForm(), ivalue);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении редактируемого значения", e);
        }

        Component comp = null;
        if (currentComp != null) {

            try {
                comp = currentComp.getComponent(SwingUtils.computeAbsoluteLocation(table), table.getCellRect(row, column, false), editEvent);
            } catch (Exception e) {
                throw new RuntimeException("Ошибка при получении редактируемого значения", e);
            }

            if (comp == null && currentComp.valueChanged()) {

                Object newValue = getCellEditorValue();
                if (!BaseUtils.nullEquals(ivalue, newValue))
                    table.setValueAt(newValue, row, column);
            }
        }

        if (comp != null) {
            return comp;
        } else {
            this.stopCellEditing();
            return null;
        }
    }

}
