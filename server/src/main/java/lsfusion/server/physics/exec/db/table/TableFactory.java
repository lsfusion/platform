package lsfusion.server.physics.exec.db.table;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.NamedTable;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.table.TableOwner;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.SystemClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.dev.id.name.DBNamingPolicy;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

public class TableFactory implements FullTablesInterface {
    private Map<Integer, NFOrderSet<ImplementTable>> implementTablesMap = new HashMap<>();
    private Map<Integer, List<ImplementTable>> includedTablesMap = new HashMap<>(); // для решения \ выделения проблем с mutability и детерминированностью, без этого можно было бы implementTablesMap обойтись 

    public TableFactory() {
    }

    @NFLazy // только NFOrderSet'ов в implementTablesMap и parents недостаточно, так алгоритм include не thread-safe (хотя и устойчив к перестановкам версий)
    public ImplementTable include(String name, Version version, ValueClass... classes) {
        if (implementTablesMap.get(classes.length) == null)
            implementTablesMap.put(classes.length, NFFact.orderSet());

        ImplementTable newTable = new ImplementTable(name, classes);
        newTable.include(implementTablesMap.get(classes.length), version, true, SetFact.mAddRemoveSet(), null);
        return newTable;
    }

    // получает постоянные таблицы
    public ImSet<ImplementTable> getImplementTables() {
        return getImplementTables((Set<String>) null);
    }

    public ImSet<ImplementTable> getImplementTables(Set<String> disableTableSet) {
        MExclSet<ImplementTable> result = SetFact.mExclSet();
        for (NFOrderSet<ImplementTable> implementTableEntry : implementTablesMap.values()) {
            MSet<ImplementTable> mIntTables = SetFact.mSet();
            for (ImplementTable implementTable : implementTableEntry.getIt()) {
                implementTable.fillSet(mIntTables, disableTableSet);
            }
            result.exclAddAll(mIntTables.immutable());
        }
        for (List<ImplementTable> implementTableEntry : includedTablesMap.values()) {
            for (ImplementTable implementTable : implementTableEntry) {
                if(disableTableSet == null || !disableTableSet.contains(implementTable.getName()))
                    result.exclAdd(implementTable);
            }
        }
        return result.immutable();
    }

    public ImRevMap<String, ImplementTable> getImplementTablesMap() {
        return getImplementTables().mapRevKeys((Function<ImplementTable, String>) NamedTable::getName);
    }

    public <T> MapKeysTable<T> getMapTable(ImOrderMap<T, ValueClass> findItem, DBNamingPolicy policy) {
        NFOrderSet<ImplementTable> tables = implementTablesMap.get(findItem.size());
        if (tables != null)
            for (ImplementTable implementTable : tables.getListIt()) {
                MapKeysTable<T> mapTable = implementTable.getSingleMapTable(findItem, false);
                if (mapTable != null) return mapTable;
            }

        return getAutoMapTable(findItem, policy);
    }

    public <T> MapKeysTable<T> getClassMapTable(ImOrderMap<T, ValueClass> findItem, DBNamingPolicy policy) {
        NFOrderSet<ImplementTable> tables = implementTablesMap.get(findItem.size());
        if (tables != null) {
            for (ImplementTable implementTable : tables.getListIt()) {
                MapKeysTable<T> table = implementTable.getClassMapTable(findItem);
                if (table != null)
                    return table;
            }
            
            for (ImplementTable implementTable : tables.getListIt()) {
                MapKeysTable<T> table = implementTable.getSingleMapTable(findItem, false);
                if (table != null)
                    return table;
            }
        }
        return getAutoMapTable(findItem, policy);
    }

    public <T> ImSet<MapKeysTable<T>> getFullMapTables(ImOrderMap<T, ValueClass> findItem, ImplementTable table) {
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
    public ImSet<ImplementTable> getFullTables(ObjectValueClassSet findItem, ImplementTable skipTable) {
        ValueClass valueClass;
        ImMap<KeyField, ValueClass> mapFields;
        if(skipTable != null && (mapFields = skipTable.getMapFields()).size() == 1 && skipTable.isFull()) // recursion guard, проверка на isFull нужна, потому что иначе пойдем вверх, а потом вернемся на эту же таблиц
            valueClass = mapFields.singleValue();
        else {
            valueClass = findItem.getOr().getCommonClass(true);
            skipTable = null;
        }
        return getFullMapTables(MapFact.singletonOrder("key", valueClass), skipTable).mapSetValues(value -> value.table);
    }

    // получает "автоматическую таблицу"
    @NFLazy
    private <T> MapKeysTable<T> getAutoMapTable(ImOrderMap<T, ValueClass> findItem, DBNamingPolicy policy) {
        int classCount = findItem.size();
        List<ImplementTable> incTables = includedTablesMap.get(classCount);
        if(incTables==null) {
            incTables = new ArrayList<>();
            includedTablesMap.put(classCount, incTables);
        }
        for (ImplementTable implementTable : incTables) {
            MapKeysTable<T> mapTable = implementTable.getSingleMapTable(findItem, true);
            if (mapTable != null) return mapTable;
        }

        // если не найдена таблица, то создаем новую
        List<ValueClass> valueClasses = new ArrayList<>();
        for (int i = 0; i < classCount; i++) {
            valueClasses.add(findItem.getValue(i));
        }
        valueClasses.sort(ValueClass.comparator);

        ImplementTable implementTable = new ImplementTable(policy.createAutoTableDBName(valueClasses), valueClasses.toArray(new ValueClass[classCount]));
        incTables.add(implementTable);
        return implementTable.getSingleMapTable(findItem, true);
    }

    public void fillDB(SQLSession sql, BaseClass baseClass) throws SQLException, SQLHandledException {

        try {
            sql.startTransaction(DBManager.START_TIL, OperationOwner.unknown);

            sql.ensureTable(IDTable.instance);
            sql.ensureTable(StructTable.instance);

            ImMap<Integer, Long> counters = IDTable.getCounters();
            for (int i = 0, size = counters.size(); i < size; i++)
                sql.ensureRecord(IDTable.instance, MapFact.singleton(IDTable.instance.key, new DataObject(counters.getKey(i), IDTable.idTypeClass)), MapFact.singleton(IDTable.instance.value, new DataObject(counters.getValue(i), SystemClass.instance)), TableOwner.global, OperationOwner.unknown);

            // создадим dumb
            sql.ensureTable(DumbTable.instance);
            sql.ensureRecord(DumbTable.instance, MapFact.singleton(DumbTable.instance.key, new DataObject(1L, SystemClass.instance)), MapFact.EMPTY(), TableOwner.global, OperationOwner.unknown);

            sql.ensureTable(EmptyTable.instance);

            sql.commitTransaction();
        } catch (Throwable e) {
            sql.rollbackTransaction();
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        }
    }

    @IdentityLazy
    public ImSet<ImplementTable> getImplementTables(final ImSet<CustomClass> cls) {
        return getImplementTables().filterFn(element -> !element.getMapFields().values().toSet().disjoint(cls));
    }

}
