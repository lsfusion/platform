package lsfusion.server.data.sql.table;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.PropertyField;

// what makes two session tables interchangeable : the same keys and the same properties
public class TemporaryTableStruct {

    public final ImOrderSet<KeyField> keys;
    public final ImSet<PropertyField> properties;

    public TemporaryTableStruct(ImOrderSet<KeyField> keys, ImSet<PropertyField> properties) {
        this.keys = keys;
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof TemporaryTableStruct && keys.equals(((TemporaryTableStruct) o).keys) && properties.equals(((TemporaryTableStruct) o).properties);
    }

    @Override
    public int hashCode() {
        return 31 * keys.hashCode() + properties.hashCode();
    }

    public static Object getDBStatistics(long count) {
        return new Stat(count);
    }
}
