package platform.client.form.cell;

import platform.client.ClientResourceBundle;
import platform.client.SwingUtils;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.grid.GridTable;
import platform.client.form.renderer.ActionPropertyRenderer;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.KeyStrokes;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.EventObject;

public class ClientAbstractCellEditor extends AbstractCellEditor
        implements TableCellEditor {

    private EventObject editEvent;

    public PropertyEditorComponent propertyEditor;

    public boolean editPerformed = false;

    public Popup popup;

    public Object getCellEditorValue() {
        try {
            return propertyEditor.getCellEditorValue();
        } catch (RemoteException e) {
            throw new RuntimeException(ClientResourceBundle.getString("errors.error.getting.selected.value"), e);
        }
    }


    public boolean stopCellEditing() {
        try {
            String message = propertyEditor.checkValue(propertyEditor.getCellEditorValue());
            if (message == null) {
                hidePopupIfNotNull();
                return super.stopCellEditing();
            } else {

                Point location = SwingUtils.computeAbsoluteLocation((Component) propertyEditor);

                JLabel component = new JLabel(message);
                component.setFont(new Font("Tahoma", Font.PLAIN, 12));
                component.setBackground(Color.YELLOW);
                component.setFocusable(false);

                JPanel pane = new JPanel();
                pane.add(component);
                pane.setBackground(Color.YELLOW);
                pane.setFocusable(false);
                pane.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                if (popup != null) popup.hide();

                popup = new PopupFactory().getPopup(null, pane, location.x, location.y + ((Component) propertyEditor).getHeight());
                popup.show();
                return false;
            }
        } catch (RemoteException e) {
            return false;
        }
    }

    public void hidePopupIfNotNull() {
        if (popup != null) {
            popup.hide();
            popup = null;
        }
    }

    public boolean isCellEditable(EventObject e) {
        editEvent = e;
        if (e instanceof KeyEvent) {

            KeyEvent event = (KeyEvent) e;

            if (KeyStrokes.isGroupCorrectionEvent(event)) return true; // групповая корректировка

            if (event.getKeyChar() == KeyEvent.CHAR_UNDEFINED) return false;

            // ESC почему-то считается KEY_TYPED кнопкой, пока обрабатываем отдельно
            if (KeyStrokes.isEscapeEvent(event)) {
                hidePopupIfNotNull();
                return false;
            }

            //будем считать, что если нажата кнопка ALT или CTRL то явно пользователь не хочет вводить текст
            //noinspection RedundantIfStatement
            if ((event.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) > 0 ||
                (event.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0) return false;

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
        }

        if (comp == null) {
            stopCellEditing();
        }

        return comp;
    }
}
