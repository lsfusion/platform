package lsfusion.server.logics.table;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.SystemClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.PropertyField;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.StructTable;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.*;

public class TableFactory {

    private BaseClass baseClass;
    Map<Integer, List<ImplementTable>> implementTablesMap = new HashMap<Integer, List<ImplementTable>>();

    public TableFactory(BaseClass baseClass) {
        this.baseClass = baseClass;
    }

    public ImplementTable include(String name, ValueClass... classes) {
        if (implementTablesMap.get(classes.length) == null)
            implementTablesMap.put(classes.length, new ArrayList<ImplementTable>());

        ImplementTable newTable = new ImplementTable(name, classes);
        newTable.include(implementTablesMap.get(classes.length), true, SetFact.<ImplementTable>mAddRemoveSet());
        return newTable;
    }

    // получает постоянные таблицы
    public ImSet<ImplementTable> getImplementTables() {
        MExclSet<ImplementTable> result = SetFact.mExclSet();
        for (List<ImplementTable> implementTableEntry : implementTablesMap.values()) {
            MSet<ImplementTable> mIntTables = SetFact.mSet();
            for (ImplementTable implementTable : implementTableEntry)
                implementTable.fillSet(mIntTables);
            result.exclAddAll(mIntTables.immutable());
        }
        return result.immutable();
    }

    public ImRevMap<String, ImplementTable> getImplementTablesMap() {
        return getImplementTables().mapRevKeys(new GetValue<String, ImplementTable>() {
            public String getMapValue(ImplementTable value) {
                return value.name;
            }
        });
    }

    public <T> MapKeysTable<T> getMapTable(ImMap<T, ValueClass> findItem) {
        List<ImplementTable> tables = implementTablesMap.get(findItem.size());
        if (tables != null)
            for (ImplementTable implementTable : tables) {
                MapKeysTable<T> mapTable = implementTable.getMapTable(findItem);
                if (mapTable != null) return mapTable;
            }

        // если не найдена таблица, то создаем новую
        List<ValueClass> valueClasses = new ArrayList<ValueClass>();

        for (int i = 0; i < findItem.size(); i++) {
            ValueClass valueClass = findItem.getValue(i);
            valueClasses.add(valueClass instanceof CustomClass ? baseClass : valueClass);
        }
        Collections.sort(valueClasses, new Comparator<ValueClass>() {
            public int compare(ValueClass o1, ValueClass o2) {
                String sid1 = o1.getSID();
                String sid2 = o2.getSID();
                return sid1.compareTo(sid2);
            }
        });

        int baseClassCount = 0;
        String dataPrefix = "";
        for (ValueClass valueClass : valueClasses) {
            if (valueClass instanceof CustomClass)
                baseClassCount++;
            else
                dataPrefix += "_" + valueClass.getSID();
        }

        MapKeysTable<T> resultTable = include("base_" + baseClassCount + dataPrefix, valueClasses.toArray(new ValueClass[findItem.size()])).getMapTable(findItem);
        if (resultTable != null)
            return resultTable;
        else
            throw new RuntimeException("No table found");
    }


    public void fillDB(SQLSession sql, BaseClass baseClass) throws SQLException {

        sql.startTransaction();

        sql.ensureTable(IDTable.instance);
        sql.ensureTable(StructTable.instance);

        ImMap<Integer, Integer> counters = IDTable.getCounters();
        for (int i = 0, size = counters.size(); i < size; i++)
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
