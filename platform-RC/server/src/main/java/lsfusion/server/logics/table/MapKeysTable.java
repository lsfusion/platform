package lsfusion.server.logics.table;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.KeyField;

public class MapKeysTable<T> extends TwinImmutableObject {
    public final ImplementTable table;
    public final ImRevMap<T,KeyField> mapKeys;

    public MapKeysTable(ImplementTable table, ImRevMap<T,KeyField> mapKeys) {
        this.table = table;
        this.mapKeys = mapKeys;
        
        assert (table.keys.size()== this.mapKeys.size());
    }

    protected boolean calcTwins(TwinImmutableObject o) {
        return table.equals(((MapKeysTable)o).table) && mapKeys.equals(((MapKeysTable)o).mapKeys);
    }

    public int immutableHashCode() {
        return table.hashCode() * 31 + mapKeys.hashCode();
    }
}
