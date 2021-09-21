package lsfusion.gwt.client.form.view;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.Objects;

public class Column {
    public final GPropertyDraw property;
    public final GGroupObjectValue columnKey;

    public Column(GPropertyDraw property, GGroupObjectValue columnKey) {
        this.property = property;
        this.columnKey = columnKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Column)) return false;
        Column column = (Column) o;
        return property.equals(column.property) &&
                GwtClientUtils.nullEquals(columnKey, column.columnKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(property, columnKey);
    }
}
