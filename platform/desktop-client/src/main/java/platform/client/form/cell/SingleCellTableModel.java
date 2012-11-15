package platform.client.form.cell;

import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.table.AbstractTableModel;

final class SingleCellTableModel extends AbstractTableModel {
    private final ClientGroupObjectValue columnKey;
    private ClientPropertyDraw property;
    private Object value;

    public SingleCellTableModel(ClientGroupObjectValue columnKey) {
        this.columnKey = columnKey;
    }

    public ClientPropertyDraw getProperty() {
        return property;
    }

    public void setProperty(ClientPropertyDraw property) {
        this.property = property;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public ClientGroupObjectValue getColumnKey() {
        return columnKey;
    }

    public int getRowCount() {
        return 1;
    }

    public int getColumnCount() {
        return 1;
    }

    public boolean isCellEditable(int row, int col) {
        return true;
    }

    public Object getValueAt(int row, int col) {
        return value;
    }

    public void setValueAt(Object nValue, int row, int col) {
        value = nValue;
    }
}
