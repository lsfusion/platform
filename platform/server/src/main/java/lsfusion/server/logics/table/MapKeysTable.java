package lsfusion.server.logics.table;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.KeyField;

public class MapKeysTable<T> {
    public ImplementTable table;
    public ImRevMap<T,KeyField> mapKeys;

    public MapKeysTable(ImplementTable table, ImRevMap<T,KeyField> mapKeys) {
        this.table = table;
        this.mapKeys = mapKeys;
        
        assert (table.keys.size()== this.mapKeys.size());
    }
}
