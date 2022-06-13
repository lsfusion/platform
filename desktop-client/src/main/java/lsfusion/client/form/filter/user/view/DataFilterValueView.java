package lsfusion.client.form.filter.user.view;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.classes.data.ClientLogicalClass;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.filter.user.ClientDataFilterValue;
import lsfusion.client.form.filter.user.ClientPropertyFilter;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.property.panel.view.CaptureKeyEventsDispatcher;
import lsfusion.interop.form.property.Compare;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.EventObject;

public abstract class DataFilterValueView extends FlexPanel {
    private final ClientDataFilterValue filterValue;
    public DataFilterValueViewTable valueTable;

    // нужен для получения текущих значений в таблице
    private final TableController logicsSupplier;

    private ClientGroupObjectValue columnKey;

    public DataFilterValueView(ClientPropertyFilter condition, TableController ilogicsSupplier, boolean readSelectedValue) {
        this.filterValue = condition.value;
        logicsSupplier = ilogicsSupplier;

        this.columnKey = condition.columnKey;

        // непосредственно объект для изменения значения свойств
        valueTable = new DataFilterValueViewTable(this, condition.property, condition.compare, ilogicsSupplier);

        addFill(valueTable);
        
        changeProperty(condition, readSelectedValue);
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
    
    public void changeProperty(ClientPropertyFilter condition) {
        filterValue.setValue(null);
        TableCellEditor cellEditor = valueTable.getCellEditor();
        if (cellEditor != null) {
            cellEditor.cancelCellEditing();
        }
        changeProperty(condition, true);
    }

    public void changeProperty(ClientPropertyFilter condition, boolean readSelectedValue) {
        valueTable.setProperty(condition.property);
        if (readSelectedValue) {
            setValue(SwingUtils.escapeComma(logicsSupplier.getSelectedValue(condition.property, condition.columnKey), condition.compare));
        } else {
            setValue(filterValue.value);
        }
    }

    public void valueChanged(Object newValue) {
        newValue = SwingUtils.escapeComma(newValue, valueTable.getCurrentInputList().compare);
        filterValue.setValue(newValue);
        setValue(newValue);
    }
    
    public void changeCompare(Compare compare) {
        valueTable.changeInputList(compare);
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

    public ClientGroupObjectValue getColumnKey() {
        return columnKey;
    }

    public void setApplied(boolean applied) {
        valueTable.setApplied(applied);
    }
}