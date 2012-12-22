package platform.server.logics.table;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MExclSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.IdentityLazy;
import platform.server.classes.BaseClass;
import platform.server.classes.CustomClass;
import platform.server.classes.SystemClass;
import platform.server.classes.ValueClass;
import platform.server.data.PropertyField;
import platform.server.data.SQLSession;
import platform.server.data.StructTable;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
        newTable.include(implementTables[classes.length], true, SetFact.<ImplementTable>mAddRemoveSet());
        return newTable;
    }

    public ImSet<DataTable> getDataTables(BaseClass baseClass) {
        return SetFact.addExcl(getImplementTables(), baseClass.table);
    }

    // получает постоянные таблицы
    public ImSet<ImplementTable> getImplementTables() {
        MExclSet<ImplementTable> result = SetFact.mExclSet();
        for(int i=0;i<=MAX_INTERFACE;i++) {
            MSet<ImplementTable> mIntTables = SetFact.mSet();
            for(ImplementTable implementTable : implementTables[i])
                implementTable.fillSet(mIntTables);
            result.exclAddAll(mIntTables.immutable());
        }
        return result.immutable();
    }
    
    public ImRevMap<String, ImplementTable> getImplementTablesMap() {
        return getImplementTables().mapRevKeys(new GetValue<String, ImplementTable>() {
            public String getMapValue(ImplementTable value) {
                return value.name;
            }});
    }

    public <T> ImplementTable getImplementTable(String tableName) {
        for (ImplementTable table : getImplementTables()) {
            if (table.name.equals(tableName)) {
                return table;
            }
        }
        return null;
    }

    public <T> MapKeysTable<T> getMapTable(ImMap<T, ValueClass> findItem) {
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

        ImMap<Integer, Integer> counters = IDTable.getCounters();
        for (int i=0,size=counters.size();i<size;i++)
            sql.ensureRecord(IDTable.instance, MapFact.singleton(IDTable.instance.key, new DataObject(counters.getKey(i), SystemClass.instance)), MapFact.singleton(IDTable.instance.value, (ObjectValue) new DataObject(counters.getValue(i), SystemClass.instance)));

        // создадим dumb
        sql.ensureTable(DumbTable.instance);
        sql.ensureRecord(DumbTable.instance, MapFact.singleton(DumbTable.instance.key, new DataObject(1, SystemClass.instance)), MapFact.<PropertyField, ObjectValue>EMPTY());

        sql.ensureTable(EmptyTable.instance);

        sql.commitTransaction();
    }
    
    @IdentityLazy
    public List<ImplementTable> getImplementTables(ImSet<CustomClass> cls) {
        List<ImplementTable> result = new ArrayList<ImplementTable>();
        for (ImplementTable table : getImplementTables()) {
            if (!table.mapFields.values().toSet().disjoint(cls)) {
                result.add(table);
            }
        }
        return result;
    }

}
