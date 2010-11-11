package platform.server.logics.table;

import platform.server.data.KeyField;

import java.util.Map;

public class MapKeysTable<T> {
    public ImplementTable table;
    public Map<T,KeyField> mapKeys;

    public MapKeysTable(ImplementTable iTable, Map<T,KeyField> iMapKeys) {
        table = iTable;
        mapKeys = iMapKeys;
        
        assert (table.keys.size()==mapKeys.size());
    }
}
