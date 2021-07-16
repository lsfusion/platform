package lsfusion.client.form.filter.user.view;

import lsfusion.client.classes.data.ClientLogicalClass;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.filter.user.ClientDataFilterValue;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.panel.view.CaptureKeyEventsDispatcher;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.EventObject;

public abstract class DataFilterValueView extends JPanel {
    private final ClientDataFilterValue filterValue;
    public DataFilterValueViewTable valueTable;

    // нужен для получения текущих значений в таблице
    private final TableController logicsSupplier;

    private ClientGroupObjectValue columnKey;

    public DataFilterValueView(ClientDataFilterValue filterValue, ClientPropertyDraw property, ClientGroupObjectValue columnKey, TableController ilogicsSupplier, boolean readSelectedValue) {
        setLayout(new BorderLayout());

        this.filterValue = filterValue;
        logicsSupplier = ilogicsSupplier;

        this.columnKey = columnKey;

        // непосредственно объект для изменения значения свойств
        valueTable = new DataFilterValueViewTable(this, property, ilogicsSupplier);

        add(valueTable, BorderLayout.CENTER);
        
        changeProperty(property, columnKey, readSelectedValue);
    }

    public boolean requestFocusInWindow() {
        return valueTable.requestFocusInWindow();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(30, getPreferredSize().height);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
    
    public void changeProperty(ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        filterValue.setValue(null);
        TableCellEditor cellEditor = valueTable.getCellEditor();
        if (cellEditor != null) {
            cellEditor.cancelCellEditing();
        }
        changeProperty(property, columnKey, true);
    }

    public void changeProperty(ClientPropertyDraw property, ClientGroupObjectValue columnKey, boolean readSelectedValue) {
        valueTable.setProperty(property);
        if (readSelectedValue) {
            setValue(logicsSupplier.getSelectedValue(property, columnKey));
        } else {
            setValue(filterValue.value);
        }
    }

    public void valueChanged(Object newValue) {
        filterValue.setValue(newValue);
        setValue(newValue);
    }

    private void setValue(Object value) {
        if(value instanceof String && ((String)value).isEmpty())
            value = null;
        valueTable.setValue(value);
    }

    public void startEditing(EventObject initFilterKeyEvent) {
        if (valueTable.getProperty().baseType != ClientLogicalClass.instance) {
            // Не начинаем редактирование для check-box, т.к. оно бессмысленно
            valueTable.editCellAt(0, 0, initFilterKeyEvent);
        } else {
            // to be able to apply on Enter
            filterValue.value = valueTable.getValue();
        }
        final Component editor = valueTable.getEditorComponent();
        if (editor != null) {
            editor.requestFocusInWindow();
            if (initFilterKeyEvent != null && editor instanceof JTextField) {
                CaptureKeyEventsDispatcher.get().setCapture(editor);
            }
        } else {
            valueTable.requestFocusInWindow();
        }
    }

    public void editingCancelled() {
        setValue(filterValue.value);
    }
    
    public ClientFormController getForm() {
        return logicsSupplier.getFormController();
    }

    public abstract void applyFilters(boolean focusFirstComponent);

    public ClientGroupObjectValue getColumnKey() {
        return columnKey;
    }
}