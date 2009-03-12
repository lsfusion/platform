package platform.server.logics.data;

import platform.server.data.Table;
import platform.server.data.KeyField;
import platform.server.logics.properties.PropertyInterface;

import java.util.Map;

public class MapKeysTable<T> {
    public ImplementTable table;
    public Map<KeyField,T> mapKeys;

    public MapKeysTable(ImplementTable iTable, Map<KeyField, T> iMapKeys) {
        table = iTable;
        mapKeys = iMapKeys;
    }
}
