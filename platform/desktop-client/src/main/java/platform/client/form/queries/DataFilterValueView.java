package platform.client.form.queries;

import platform.client.form.ClientFormController;
import platform.client.form.GroupObjectLogicsSupplier;
import platform.client.logics.ClientDataFilterValue;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class DataFilterValueView extends FilterValueView {
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

        // приходится в явную указывать RowHeight, поскольку это JTable и он сам не растянется
        valueTable.setRowHeight(QueryConditionView.PREFERRED_HEIGHT);

        add(valueTable, BorderLayout.CENTER);
    }

    public boolean requestFocusInWindow() {
        return valueTable.requestFocusInWindow();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(30, getPreferredSize().height);
    }

    public void propertyChanged(ClientPropertyDraw property) {
        valueTable.setProperty(property);
        setValue(logicsSupplier.getSelectedValue(property, null));
    }

    public void valueChanged(Object newValue) {
        setValue(newValue);
        listener.valueChanged();
    }

    private void setValue(Object value) {
        filterValue.setValue(value);
        valueTable.setValue(value);
    }

    public void startEditing(KeyEvent initFilterKeyEvent) {
        valueTable.editCellAt(0, 0);
        Component editor = valueTable.getEditorComponent();
        if (editor != null) {
            editor.requestFocusInWindow();
            if (initFilterKeyEvent != null && editor instanceof JTextField) {
                ((JTextField) editor).setText("" + initFilterKeyEvent.getKeyChar());
            }
        }
    }

    public ClientFormController getForm() {
        return logicsSupplier.getForm();
    }
}