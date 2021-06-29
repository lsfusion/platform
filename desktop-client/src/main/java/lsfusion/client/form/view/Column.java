package lsfusion.client.form.view;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;

import java.util.Objects;

public class Column {
    public final ClientPropertyDraw property;
    public final ClientGroupObjectValue columnKey;

    public Column(ClientPropertyDraw property, ClientGroupObjectValue columnKey) {
        this.property = property;
        this.columnKey = columnKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Column)) return false;
        Column column = (Column) o;
        return property.equals(column.property) &&
                BaseUtils.nullEquals(columnKey, column.columnKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(property, columnKey);
    }
}
