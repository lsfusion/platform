package platform.server.logics.table;

import platform.server.classes.CustomClass;
import platform.server.classes.SystemClass;
import platform.server.classes.ValueClass;
import platform.server.data.GlobalTable;
import platform.server.data.PropertyField;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class TableFactory {

    public static final int MAX_INTERFACE = 5;
    Set<ImplementTable>[] implementTables = new Set[MAX_INTERFACE];
    ImplementTable[] baseTables = new ImplementTable[MAX_INTERFACE];

    public TableFactory() {
        for(int i=0;i<MAX_INTERFACE;i++)
            implementTables[i] = new HashSet<ImplementTable>();
    }

    public void include(ValueClass... classes) {
        String name = "table";
        for (ValueClass fieldClass : classes)
            name += "_" + ((CustomClass)fieldClass).ID.toString();
        include(name,classes);
    }

    public void include(String name, ValueClass... classes) {
        new ImplementTable(name,classes).include(implementTables[classes.length], true, new HashSet<ImplementTable>());
    }

    // получает постоянные таблицы
    public Map<String,ImplementTable> getImplementTables() {
        Map<String,ImplementTable> result = new HashMap<String, ImplementTable>();
        for(int i=0;i<MAX_INTERFACE;i++) {
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

    public void fillDB(DataSession session) throws SQLException {

        session.ensureTable(session.baseClass.table);
        session.ensureTable(IDTable.instance);
        session.ensureTable(GlobalTable.instance);

        for (Integer idType : IDTable.getCounters())
            session.ensureRecord(IDTable.instance,Collections.singletonMap(IDTable.instance.key,new DataObject(idType, SystemClass.instance)),
                    Collections.singletonMap(IDTable.instance.value,(ObjectValue)new DataObject(0, SystemClass.instance)));

        // создадим dumb
        session.ensureTable(DumbTable.instance);
        session.ensureRecord(DumbTable.instance,Collections.singletonMap(DumbTable.instance.key,new DataObject(1, SystemClass.instance)), new HashMap<PropertyField, ObjectValue>());

        session.ensureTable(EmptyTable.instance);
    }
}
