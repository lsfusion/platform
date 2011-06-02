package platform.server.logics.table;

import platform.server.classes.SystemClass;
import platform.server.classes.ValueClass;
import platform.server.classes.BaseClass;
import platform.server.data.StructTable;
import platform.server.data.PropertyField;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.*;

public class TableFactory {

    public static final int MAX_INTERFACE = 6;
    List<ImplementTable>[] implementTables = new List[MAX_INTERFACE+1]; // используем List, чтобы всегда был одинаковый порядок
    ImplementTable[] baseTables = new ImplementTable[MAX_INTERFACE+1];

    public TableFactory() {
        for(int i=0;i<=MAX_INTERFACE;i++)
            implementTables[i] = new ArrayList<ImplementTable>();
    }

    public void include(String name, ValueClass... classes) {
        new ImplementTable(name,classes).include(implementTables[classes.length], true, new HashSet<ImplementTable>());
    }

    // получает постоянные таблицы
    public Map<String,ImplementTable> getImplementTables() {
        Map<String,ImplementTable> result = new HashMap<String, ImplementTable>();
        for(int i=0;i<=MAX_INTERFACE;i++) {
            Set<ImplementTable> intTables = new HashSet<ImplementTable>();
            for(ImplementTable implementTable : implementTables[i])
                implementTable.fillSet(intTables);            
            for(ImplementTable intTable : intTables)
                result.put(intTable.name,intTable);
        }
        return result;
    }

    public <T> MapKeysTable<T> getMapTable(Map<T, ValueClass> findItem) {
        for(ImplementTable implementTable : implementTables[findItem.size()]) {
            MapKeysTable<T> mapTable = implementTable.getMapTable(findItem);
            if(mapTable!=null) return mapTable;
        }
        throw new RuntimeException("No table found");    
    }

    public void fillDB(SQLSession sql, BaseClass baseClass) throws SQLException {

        sql.startTransaction();

        sql.ensureTable(baseClass.table);
        sql.ensureTable(IDTable.instance);
        sql.ensureTable(StructTable.instance);

        for (Integer idType : IDTable.getCounters())
            sql.ensureRecord(IDTable.instance, Collections.singletonMap(IDTable.instance.key,new DataObject(idType, SystemClass.instance)), Collections.singletonMap(IDTable.instance.value,(ObjectValue)new DataObject(0, SystemClass.instance)));

        // создадим dumb
        sql.ensureTable(DumbTable.instance);
        sql.ensureRecord(DumbTable.instance, Collections.singletonMap(DumbTable.instance.key,new DataObject(1, SystemClass.instance)), new HashMap<PropertyField, ObjectValue>());

        sql.ensureTable(EmptyTable.instance);

        sql.commitTransaction();
    }
}
