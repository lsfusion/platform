package platform.server.logics.table;

import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.data.KeyField;

public class MapKeysTable<T> {
    public ImplementTable table;
    public ImRevMap<T,KeyField> mapKeys;

    public MapKeysTable(ImplementTable table, ImRevMap<T,KeyField> mapKeys) {
        this.table = table;
        this.mapKeys = mapKeys;
        
        assert (table.keys.size()== this.mapKeys.size());
    }
}
