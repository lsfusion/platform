package lsfusion.server.logics.table;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.*;
import lsfusion.server.data.*;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.NFLazy;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFOrderSet;

import java.sql.SQLException;
import java.util.*;

public class TableFactory implements FullTablesInterface {

    private BaseClass baseClass;
    private Map<Integer, NFOrderSet<ImplementTable>> implementTablesMap = new HashMap<Integer, NFOrderSet<ImplementTable>>();
    private Map<Integer, List<ImplementTable>> includedTablesMap = new HashMap<Integer, List<ImplementTable>>(); // для решения \ выделения проблем с mutability и детерминированностью, без этого можно было бы implementTablesMap обойтись 

    public TableFactory(BaseClass baseClass) {
        this.baseClass = baseClass;
    }

    @NFLazy // только NFOrderSet'ов в implementTablesMap и parents недостаточно, так алгоритм include не thread-safe (хотя и устойчив к перестановкам версий)
    public ImplementTable include(String name, Version version, ValueClass... classes) {
        if (implementTablesMap.get(classes.length) == null)
            implementTablesMap.put(classes.length, NFFact.<ImplementTable>orderSet());

        ImplementTable newTable = new ImplementTable(name, classes);
        newTable.include(implementTablesMap.get(classes.length), version, true, SetFact.<ImplementTable>mAddRemoveSet(), null);
        return newTable;
    }

    // получает постоянные таблицы
    public ImSet<ImplementTable> getImplementTables() {
        MExclSet<ImplementTable> result = SetFact.mExclSet();
        for (NFOrderSet<ImplementTable> implementTableEntry : implementTablesMap.values()) {
            MSet<ImplementTable> mIntTables = SetFact.mSet();
            for (ImplementTable implementTable : implementTableEntry.getIt())
                implementTable.fillSet(mIntTables);
            result.exclAddAll(mIntTables.immutable());
        }
        for (List<ImplementTable> implementTableEntry : includedTablesMap.values())
            result.exclAddAll(SetFact.fromJavaOrderSet(implementTableEntry).getSet());
        return result.immutable();
    }

    public ImRevMap<String, ImplementTable> getImplementTablesMap() {
        return getImplementTables().mapRevKeys(new GetValue<String, ImplementTable>() {
            public String getMapValue(ImplementTable value) {
                return value.getName();
            }
        });
    }

    public <T> MapKeysTable<T> getMapTable(ImMap<T, ValueClass> findItem) {
        NFOrderSet<ImplementTable> tables = implementTablesMap.get(findItem.size());
        if (tables != null)
            for (ImplementTable implementTable : tables.getListIt()) {
                MapKeysTable<T> mapTable = implementTable.getSingleMapTable(findItem, false);
                if (mapTable != null) return mapTable;
            }

        return getIncludedMapTable(findItem);
    }


    public <T> ImSet<MapKeysTable<T>> getFullMapTables(ImMap<T, ValueClass> findItem, ImplementTable table) {
        NFOrderSet<ImplementTable> tables = implementTablesMap.get(findItem.size());
        if(tables == null)
            return SetFact.EMPTY();

        MSet<MapKeysTable<T>> mResult = SetFact.mSet();
        for (ImplementTable implementTable : tables.getListIt()) {
            mResult.addAll(implementTable.getFullMapTables(findItem, table));
        }
        return mResult.immutable();
    }

    @IdentityLazy
    public ImSet<ImplementTable> getFullTables(ObjectValueClassSet findItem, ImplementTable implementTable) {
        ValueClass valueClass;
        if(implementTable != null && implementTable.mapFields.size() == 1 && implementTable.isFull()) // recursion guard, проверка на isFull нужна, потому что иначе пойдем вверх, а потом вернемся на эту же таблиц
            valueClass = implementTable.mapFields.singleValue();
        else {
            valueClass = findItem.getOr().getCommonClass(true);
            implementTable = null;
        }
        return getFullMapTables(MapFact.<String, ValueClass>singleton("key", valueClass), implementTable).mapSetValues(new GetValue<ImplementTable, MapKeysTable<String>>() {
            public ImplementTable getMapValue(MapKeysTable<String> value) {
                return value.table;
            }
        });
    }

    // получает "автоматическую таблицу"
    @NFLazy
    private <T> MapKeysTable<T> getIncludedMapTable(ImMap<T, ValueClass> findItem) {
        int classCount = findItem.size();
        List<ImplementTable> incTables = includedTablesMap.get(classCount);
        if(incTables==null) {
            incTables = new ArrayList<ImplementTable>();
            includedTablesMap.put(classCount, incTables);
        }
        for (ImplementTable implementTable : incTables) {
            MapKeysTable<T> mapTable = implementTable.getSingleMapTable(findItem, true);
            if (mapTable != null) return mapTable;
        }

        // если не найдена таблица, то создаем новую
        List<ValueClass> valueClasses = new ArrayList<ValueClass>();

        for (int i = 0; i < classCount; i++) {
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

        ImplementTable implementTable = new ImplementTable("base_" + baseClassCount + dataPrefix, valueClasses.toArray(new ValueClass[classCount]));
        incTables.add(implementTable);
        return implementTable.getSingleMapTable(findItem, true);
    }


    public void fillDB(SQLSession sql, BaseClass baseClass) throws SQLException, SQLHandledException {

        try {
            sql.startTransaction(DBManager.START_TIL, OperationOwner.unknown);

            sql.ensureTable(IDTable.instance);
            sql.ensureTable(StructTable.instance);

            ImMap<Integer, Integer> counters = IDTable.getCounters();
            for (int i = 0, size = counters.size(); i < size; i++)
                sql.ensureRecord(IDTable.instance, MapFact.singleton(IDTable.instance.key, new DataObject(counters.getKey(i), SystemClass.instance)), MapFact.singleton(IDTable.instance.value, (ObjectValue) new DataObject(counters.getValue(i), SystemClass.instance)), TableOwner.global, OperationOwner.unknown);

            // создадим dumb
            sql.ensureTable(DumbTable.instance);
            sql.ensureRecord(DumbTable.instance, MapFact.singleton(DumbTable.instance.key, new DataObject(1, SystemClass.instance)), MapFact.<PropertyField, ObjectValue>EMPTY(), TableOwner.global, OperationOwner.unknown);

            sql.ensureTable(EmptyTable.instance);

            sql.commitTransaction();
        } catch (Throwable e) {
            sql.rollbackTransaction();
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        }
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
