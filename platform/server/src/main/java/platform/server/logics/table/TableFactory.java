package platform.server.logics.table;

import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.classes.BaseClass;
import platform.server.classes.CustomClass;
import platform.server.classes.SystemClass;
import platform.server.classes.ValueClass;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.SQLSession;
import platform.server.data.StructTable;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.DataSession;
import platform.server.session.SingleKeyNoPropertyUsage;

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

    public ImplementTable include(String name, ValueClass... classes) {
        ImplementTable newTable = new ImplementTable(name, classes);
        newTable.include(implementTables[classes.length], true, new HashSet<ImplementTable>());
        return newTable;
    }

    public Collection<DataTable> getDataTables(BaseClass baseClass) {
        return BaseUtils.add(getImplementTables(), baseClass.table);
    }

    // получает постоянные таблицы
    public Collection<ImplementTable> getImplementTables() {
        Collection<ImplementTable> result = new ArrayList<ImplementTable>();
        for(int i=0;i<=MAX_INTERFACE;i++) {
            Set<ImplementTable> intTables = new HashSet<ImplementTable>();
            for(ImplementTable implementTable : implementTables[i])
                implementTable.fillSet(intTables);            
            for(ImplementTable intTable : intTables)
                result.add(intTable);
        }
        return result;
    }
    
    public Map<String, ImplementTable> getImplementTablesMap() {
        Map<String, ImplementTable> result = new HashMap<String, ImplementTable>();
        for(ImplementTable impTable : getImplementTables())
            result.put(impTable.name, impTable);
        return result;
    }

    public <T> ImplementTable getImplementTable(String tableName) {
        for (ImplementTable table : getImplementTables()) {
            if (table.name.equals(tableName)) {
                return table;
            }
        }
        return null;
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

        for (Map.Entry<Integer, Integer> idType : IDTable.getCounters().entrySet())
            sql.ensureRecord(IDTable.instance, Collections.singletonMap(IDTable.instance.key,new DataObject(idType.getKey(), SystemClass.instance)), Collections.singletonMap(IDTable.instance.value,(ObjectValue)new DataObject(idType.getValue(), SystemClass.instance)));

        // создадим dumb
        sql.ensureTable(DumbTable.instance);
        sql.ensureRecord(DumbTable.instance, Collections.singletonMap(DumbTable.instance.key,new DataObject(1, SystemClass.instance)), new HashMap<PropertyField, ObjectValue>());

        sql.ensureTable(EmptyTable.instance);

        sql.commitTransaction();
    }
    
    @IdentityLazy
    public List<ImplementTable> getImplementTables(Set<CustomClass> cls) {
        List<ImplementTable> result = new ArrayList<ImplementTable>();
        for (ImplementTable table : getImplementTables()) {
            if (!Collections.disjoint(table.mapFields.values(), cls)) {
                result.add(table);
            }
        }
        return result;
    }

}
