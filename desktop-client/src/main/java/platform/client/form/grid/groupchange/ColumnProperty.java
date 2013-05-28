package platform.client.form.grid.groupchange;

import platform.client.form.grid.GridTableModel;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

public class ColumnProperty {
    public String name;
    public ClientPropertyDraw property;
    public ClientGroupObjectValue columnKey;

    public ColumnProperty(GridTableModel model, int index) {
        name = model.getColumnName(index);
        property = model.getColumnProperty(index);
        columnKey = model.getColumnKey(index);
    }

    public String toString() {
        return property.toString() + (columnKey == null || columnKey.isEmpty() ? "" : " [" + name.trim() + "]");
    }
}
