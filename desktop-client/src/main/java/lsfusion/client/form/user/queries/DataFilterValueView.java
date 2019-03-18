package lsfusion.client.form.user.queries;

import lsfusion.client.CaptureKeyEventsDispatcher;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.object.table.GroupObjectLogicsSupplier;
import lsfusion.client.form.filter.ClientDataFilterValue;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientLogicalClass;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public abstract class DataFilterValueView extends FilterValueView {
    private final ClientDataFilterValue filterValue;
    private final DataFilterValueViewTable valueTable;

    // нужен для получения текущих значений в таблице
    private final GroupObjectLogicsSupplier logicsSupplier;

    public DataFilterValueView(FilterValueListener listener, ClientDataFilterValue ifilterValue, ClientPropertyDraw property, GroupObjectLogicsSupplier ilogicsSupplier) {
        super(listener);

        filterValue = ifilterValue;
        logicsSupplier = ilogicsSupplier;

        // непосредственно объект для изменения значения свойств
        valueTable = new DataFilterValueViewTable(this, property, ilogicsSupplier);

        add(valueTable, BorderLayout.CENTER);
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

    public void propertyChanged(ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        valueTable.setProperty(property);
        setValue(logicsSupplier.getSelectedValue(property, columnKey));
    }

    public void valueChanged(Object newValue) {
        setValue(newValue);
        listener.valueChanged();
    }

    private void setValue(Object value) {
        if(value instanceof String && ((String)value).isEmpty())
            value = null;
        filterValue.setValue(value);
        valueTable.setValue(value);
    }

    public void startEditing(KeyEvent initFilterKeyEvent) {
        if (valueTable.getProperty().baseType != ClientLogicalClass.instance) {
            // Не начинаем редактирование для check-box, т.к. оно бессмысленно
            valueTable.editCellAt(0, 0);
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

    public ClientFormController getForm() {
        return logicsSupplier.getFormController();
    }

    public abstract void applyQuery();
}