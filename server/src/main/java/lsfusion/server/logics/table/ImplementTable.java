package lsfusion.server.logics.table;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.SystemProperties;
import lsfusion.server.classes.*;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.*;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.InnerFollows;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ReflectionLogicsModule;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.NFLazy;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFOrderSet;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassField;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.PropertyChanges;

import java.sql.SQLException;
import java.util.*;

public class ImplementTable extends GlobalTable {
    public final ImMap<KeyField, ValueClass> mapFields;
    private StatKeys<KeyField> statKeys = null;
    private ImMap<PropertyField, PropStat> statProps = null;
    private ImSet<PropertyField> indexedProps = SetFact.<PropertyField>EMPTY();

    private InnerFollows.Field fullField = null; // поле которое всегда не null, и свойство которого обеспечивает , возможно временно потом совместиться с логикой classExpr
    public boolean isFull() {
        return fullField != null;
    }
    public InnerFollows.Field getFullField() {
        return fullField;
    }
    public void setFullField(final PropertyField field) {
        fullField = new InnerFollows.Field() {
            public BaseExpr getFollowExpr(BaseExpr joinExpr) {
                return (BaseExpr) joinAnd(MapFact.singleton(keys.single(), joinExpr)).getExpr(field);
            }
        };
    }
    public void setFullField(final ClassField classField) {
        fullField = classField;
    }

    @Override
    protected boolean isIndexed(PropertyField field) {
        return indexedProps.contains(field);
    }

    public ImplementTable(String name, final ValueClass... implementClasses) {
        super(name);

        keys = SetFact.toOrderExclSet(implementClasses.length, new GetIndex<KeyField>() {
            public KeyField getMapValue(int i) {
                return new KeyField("key"+i,implementClasses[i].getType());
            }});
        mapFields = keys.mapOrderValues(new GetIndex<ValueClass>() {
            public ValueClass getMapValue(int i) {
                return implementClasses[i];
            }});
        parents = NFFact.orderSet();

        classes = classes.or(new ClassWhere<KeyField>(mapFields,true));
    }

    public <P extends PropertyInterface> IQuery<KeyField, CalcProperty> getReadSaveQuery(ImSet<CalcProperty> properties, Modifier modifier) {
        return getReadSaveQuery(properties, modifier.getPropertyChanges());
    }

    public <P extends PropertyInterface> IQuery<KeyField, CalcProperty> getReadSaveQuery(ImSet<CalcProperty> properties, PropertyChanges propertyChanges) {
        QueryBuilder<KeyField, CalcProperty> changesQuery = new QueryBuilder<KeyField, CalcProperty>(this);
        WhereBuilder changedWhere = new WhereBuilder();
        for (CalcProperty<P> property : properties)
            changesQuery.addProperty(property, property.getIncrementExpr(property.mapTable.mapKeys.join(changesQuery.getMapExprs()), propertyChanges, changedWhere));
        changesQuery.and(changedWhere.toWhere());
        return changesQuery.getQuery();
    }

    public void moveColumn(SQLSession sql, PropertyField field, Table prevTable, ImMap<KeyField, KeyField> mapFields, PropertyField prevField) throws SQLException, SQLHandledException {
        QueryBuilder<KeyField, PropertyField> moveColumn = new QueryBuilder<KeyField, PropertyField>(this);
        Expr moveExpr = prevTable.join(mapFields.join(moveColumn.getMapExprs())).getExpr(prevField);
        moveColumn.addProperty(field, moveExpr);
        moveColumn.and(moveExpr.getWhere());
        sql.modifyRecords(new ModifyQuery(this, moveColumn.getQuery(), OperationOwner.unknown, TableOwner.global));
    }

    @NFLazy
    public void addField(PropertyField field,ClassWhere<Field> classes) { // кривовато конечно, но пока другого варианта нет
        properties = properties.addExcl(field);
        propertyClasses = propertyClasses.addExcl(field, classes);
    }

    @NFLazy
    public void addIndex(PropertyField field) { // кривовато конечно, но пока другого варианта нет
        indexedProps = indexedProps.addExcl(field);
    }

    private NFOrderSet<ImplementTable> parents;
    public Iterable<ImplementTable> getParentsIt() {
        return parents.getIt();
    }
    public Iterable<ImplementTable> getParentsListIt() {
        return parents.getListIt();
    }

    // operation на что сравниваем
    // 0 - не ToParent
    // 1 - ToParent
    // 2 - равно
    private final static int IS_CHILD = 0;
    private final static int IS_PARENT = 1;
    private final static int IS_EQUAL = 2;

    private <T> boolean recCompare(int operation, ImMap<T, ValueClass> toCompare,int iRec,Map<T,KeyField> mapTo) {
        if(iRec>=mapFields.size()) return true;

        KeyField proceedItem = mapFields.getKey(iRec);
        ValueClass proceedClass = mapFields.getValue(iRec);
        for(int i=0,size=toCompare.size();i<size;i++) {
            T key = toCompare.getKey(i); ValueClass compareClass = toCompare.getValue(i);
            if(!mapTo.containsKey(key) &&
               ((operation==IS_PARENT && compareClass.isCompatibleParent(proceedClass)) ||
               (operation==IS_CHILD && proceedClass.isCompatibleParent(compareClass)) ||
               (operation==IS_EQUAL && compareClass == proceedClass))) {
                    // если parent - есть связь и нету ключа, гоним рекурсию дальше
                    mapTo.put(key,proceedItem);
                    // если нашли карту выходим
                    if(recCompare(operation, toCompare,iRec + 1, mapTo)) return true;
                    mapTo.remove(key);
            }
        }

        return false;
    }

    private final static int COMPARE_DIFF = 0;
    private final static int COMPARE_DOWN = 1;
    private final static int COMPARE_UP = 2;
    private final static int COMPARE_EQUAL = 3;

    // также возвращает карту если не Diff
    private <T> int compare(ImMap<T, ValueClass> toCompare,Result<ImRevMap<T,KeyField>> mapTo) {

        Integer result = null;
        
        Map<T, KeyField> mMapTo = MapFact.mAddRemoveMap();
        if(recCompare(IS_EQUAL,toCompare,0,mMapTo))
            result = COMPARE_EQUAL;
        else
        if(recCompare(IS_CHILD,toCompare,0,mMapTo))
            result = COMPARE_UP;
        else
        if(recCompare(IS_PARENT,toCompare,0,mMapTo))
            result = COMPARE_DOWN;

        if(result!=null) {
            mapTo.set(MapFact.fromJavaRevMap(mMapTo));
            return result;
        }

        return COMPARE_DIFF;
    }

    public <T> boolean equalClasses(ImMap<T, ValueClass> mapClasses) {
        int compResult = compare(mapClasses, new Result<ImRevMap<T, KeyField>>());
        return compResult == COMPARE_EQUAL || compResult == COMPARE_UP;
    }

    public void include(NFOrderSet<ImplementTable> tables, Version version, boolean toAdd, Set<ImplementTable> checks, ImplementTable debugItem) {
        ImList<ImplementTable> current = tables.getNFList(Version.CURRENT);
        
        Iterator<ImplementTable> i = current.iterator();
        boolean wasRemove = false; // для assertiona
        while(i.hasNext()) {
            ImplementTable item = i.next();
            int relation = item.compare(mapFields,new Result<ImRevMap<KeyField, KeyField>>());
            if(relation==COMPARE_DOWN) { // снизу в дереве, добавляем ее как промежуточную
                if(checkSiblings(item, parents, this))
                    parents.add(item, version);
                
                if(toAdd) {
                    wasRemove = true;
                    tables.remove(item, Version.CURRENT); // последняя версия нужна, так как в противном случае удаление может пойти до добавления 
                }
            } else { // сверху в дереве или никак не связаны, передаем дальше
                if(!checks.contains(item)) { // для детерменированности эту проверку придется убрать 
                    checks.add(item);
                    include(item.parents, version, relation==COMPARE_UP,checks, item);
                }
                if(relation==COMPARE_UP) {
                    assert !wasRemove; // так как не может быть одновременно down и up
                    toAdd = false;
                }
            }
        }

        // если снизу добавляем Childs
        if(toAdd) {
            assert checkSiblings(this, tables, debugItem);
            tables.add(this, version);
        }
    }

    private boolean checkSiblings(ImplementTable item, NFOrderSet<ImplementTable> tables, ImplementTable debugItem) {
        for(ImplementTable siblingTable : tables.getNFList(Version.CURRENT)) {
            if(BaseUtils.hashEquals(item, siblingTable))
                return false;
            int compare = siblingTable.compare(item.mapFields, new Result<ImRevMap<KeyField, KeyField>>());
            if(compare==COMPARE_UP || compare == COMPARE_DOWN)
                return false;
        }
        return true;
    }

    private static interface MapTableType {
        boolean skipParents(ImplementTable table);
        boolean skipResult(ImplementTable table);
        boolean onlyFirstParent(ImplementTable table); // assert !skipParents
    }

    // поиск таблицы для классов
    private final static MapTableType findTable = new MapTableType() {
        public boolean skipParents(ImplementTable table) {
            return false;
        }

        public boolean skipResult(ImplementTable table) {
            return false;
        }

        public boolean onlyFirstParent(ImplementTable table) {
            return true;
        }
    };

    // поиск сгенерированной таблицы
    private final static MapTableType findIncludedTable = new MapTableType() {
        public boolean skipParents(ImplementTable table) {
            return true;
        }

        public boolean skipResult(ImplementTable table) {
            return false;
        }

        public boolean onlyFirstParent(ImplementTable table) {
            throw new UnsupportedOperationException();
        }
    };

    // поиск full таблиц
    private final static class FindFullTables implements MapTableType {

        private final ImplementTable skipTable;

        public FindFullTables(ImplementTable skipTable) {
            this.skipTable = skipTable;
        }

        public boolean skipParents(ImplementTable table) {
            return skipTable(table);
        }

        private boolean skipTable(ImplementTable table) {
            return skipTable != null && BaseUtils.hashEquals(table, skipTable);
        }

        public boolean skipResult(ImplementTable table) {
            return !table.isFull() || skipTable(table);
        }

        public boolean onlyFirstParent(ImplementTable table) {
            return false;
        }
    };

    public <T> MapKeysTable<T> getSingleMapTable(ImMap<T, ValueClass> findItem, boolean included) {
        ImSet<MapKeysTable<T>> tables = getMapTables(findItem, included ? findIncludedTable : findTable);
        if(tables.isEmpty())
            return null;
        return tables.single();
    }

    public <T> ImSet<MapKeysTable<T>> getFullMapTables(ImMap<T, ValueClass> findItem, ImplementTable skipTable) {
        return getMapTables(findItem, new FindFullTables(skipTable));
    }

    public <T> ImSet<MapKeysTable<T>> getMapTables(ImMap<T, ValueClass> findItem, MapTableType type) {
        Result<ImRevMap<T,KeyField>> mapCompare = new Result<ImRevMap<T, KeyField>>();
        int relation = compare(findItem,mapCompare);
        // если внизу или отличается то не туда явно зашли
        if(relation==COMPARE_DOWN || relation==COMPARE_DIFF) return SetFact.EMPTY();

        if(!type.skipParents(this)) {
            MSet<MapKeysTable<T>> mResult = SetFact.mSet();
            for(ImplementTable item : getParentsListIt()) {
                ImSet<MapKeysTable<T>> parentTables = item.getMapTables(findItem, type);
                if(type.onlyFirstParent(this) && !parentTables.isEmpty()) {
                    assert parentTables.size() == 1;
                    return parentTables;
                }
                mResult.addAll(parentTables);
            }
            ImSet<MapKeysTable<T>> result = mResult.immutable();
            if(!result.isEmpty())
                return result;
        }

        if(type.skipResult(this)) return SetFact.EMPTY();

        return SetFact.singleton(new MapKeysTable<T>(this,mapCompare.result));
    }

    public <T> MapKeysTable<T> getMapKeysTable(ImMap<T, ValueClass> classes) {
        Result<ImRevMap<T,KeyField>> mapCompare = new Result<ImRevMap<T, KeyField>>();
        int relation = compare(classes, mapCompare);
        if(relation==COMPARE_DOWN || relation==COMPARE_DIFF)
            return null;
        return new MapKeysTable<T>(this,mapCompare.result);
    }

    void fillSet(MSet<ImplementTable> tableImplements) {
        if(tableImplements.add(this)) return;
        for(ImplementTable parent : getParentsIt()) 
            parent.fillSet(tableImplements);
    }

    public StatKeys<KeyField> getStatKeys() {
        if(statKeys!=null)
            return statKeys;
        else
            return SerializedTable.getStatKeys(this);
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        if(statProps!=null)
            return statProps;
        else
            return SerializedTable.getStatProps(this);
    }

    public Object readCount(DataSession session, Where where) throws SQLException, SQLHandledException {
        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(SetFact.EMPTY());
        ValueExpr one = new ValueExpr(1, IntegerClass.instance);
        query.addProperty("count", GroupExpr.create(MapFact.<Integer, Expr>EMPTY(), one,
                where, GroupType.SUM, MapFact.<Integer, Expr>EMPTY()));
        return query.execute(session).singleValue().singleValue();
    }

    public void calculateStat(ReflectionLogicsModule reflectionLM, DataSession session) throws SQLException, SQLHandledException {
        if (!SystemProperties.doNotCalculateStats) {
            ValueExpr one = new ValueExpr(1, IntegerClass.instance);

            ImRevMap<KeyField, KeyExpr> mapKeys = getMapKeys();
            lsfusion.server.data.query.Join<PropertyField> join = join(mapKeys);

            MExclMap<Object, Object> mResult = MapFact.mExclMap();
            Where inWhere = join.getWhere();
            for(KeyField key : keys) {
                ImMap<Object, Expr> map = MapFact.<Object, Expr>singleton(0, mapKeys.get(key));
                mResult.exclAdd(key, readCount(session, GroupExpr.create(map, inWhere, map).getWhere()));
            }

            for(PropertyField prop : properties)
                if (!(prop.type instanceof DataClass && !((DataClass)prop.type).calculateStat()))
                    mResult.exclAdd(prop, readCount(session, GroupExpr.create(MapFact.singleton(0, join.getExpr(prop)), Where.TRUE, MapFact.singleton(0, new KeyExpr("count"))).getWhere()));

            mResult.exclAdd(0, readCount(session, inWhere));
            ImMap<Object, Object> result = mResult.immutable();

            DataObject tableObject = (DataObject) reflectionLM.tableSID.readClasses(session, new DataObject(getName()));
            reflectionLM.rowsTable.change(BaseUtils.nvl(result.get(0), 0), session, tableObject);

            for (KeyField key : keys) {
                DataObject keyObject = (DataObject) reflectionLM.tableKeySID.readClasses(session, new DataObject(getName() + "." + key.getName()));
                reflectionLM.quantityTableKey.change(BaseUtils.nvl(result.get(key), 0), session, keyObject);
            }

            for (PropertyField property : properties) {
                DataObject propertyObject = (DataObject) reflectionLM.tableColumnSID.readClasses(session, new DataObject(getName() + "." + property.getName()));
                reflectionLM.quantityTableColumn.change(BaseUtils.nvl(result.get(property), 0), session, propertyObject);
            }

            // не null значения и разреженность колонок
            MExclMap<Object, Object> mNotNulls = MapFact.mExclMap();
            for (PropertyField property : properties)
                mNotNulls.exclAdd(property, readCount(session, join.getExpr(property).getWhere()));
            ImMap<Object, Object> notNulls = mNotNulls.immutable();
            for (PropertyField property : properties) {
                DataObject propertyObject = (DataObject) reflectionLM.tableColumnSID.readClasses(session, new DataObject(getName() + "." + property.getName()));
                int notNull = (Integer) BaseUtils.nvl(notNulls.get(property), 0);
                reflectionLM.notNullQuantityTableColumn.change(notNull, session, propertyObject);
            }
        }
    }

    public void updateStat(ImMap<String, Integer> tableStats, ImMap<String, Integer> keyStats, ImMap<String, Pair<Integer, Integer>> propStats, boolean statDefault) throws SQLException {
        Stat rowStat;
        if (!tableStats.containsKey(name))
            rowStat = Stat.DEFAULT;
        else
            rowStat = new Stat(BaseUtils.nvl(tableStats.get(name), 0));

        ImValueMap<KeyField, Stat> mvDistinctKeys = getTableKeys().mapItValues(); // exception есть
        for(int i=0,size=keys.size();i<size;i++) {
            String keySID = getName() + "." + keys.get(i).getName();
            Stat keyStat;
            if (!keyStats.containsKey(keySID))
                keyStat = Stat.DEFAULT;
            else {
                Integer keyCount = keyStats.get(keySID);
                keyStat = keyCount != null ? new Stat(keyCount) : rowStat;
            }
            mvDistinctKeys.mapValue(i, keyStat.min(rowStat));
        }
        statKeys = StatKeys.create(rowStat, new DistinctKeys<KeyField>(mvDistinctKeys.immutableValue()));

        ImValueMap<PropertyField, PropStat> mvUpdateStatProps = properties.mapItValues();
        for(int i=0,size=properties.size();i<size;i++) {
            PropertyField prop = properties.get(i);
            Stat distinctStat;
            Stat notNullStat;
            if(propStats.containsKey(getName() + "." + prop.getName())) {
                Pair<Integer, Integer> propStat = propStats.get(getName() + "." + prop.getName());
                notNullStat = propStat.second != null ? new Stat(propStat.second).min(rowStat) : rowStat;
                distinctStat = propStat.first != null ? new Stat(propStat.first).min(notNullStat) : notNullStat;
            } else {
                distinctStat = null;
                notNullStat = null;
            }

            PropStat propStat;
            if (prop.type instanceof DataClass && !((DataClass)prop.type).calculateStat()) {
                if (distinctStat==null) {
                    Stat typeStat = ((DataClass) prop.type).getTypeStat(false).min(rowStat);
                    propStat = new PropStat(typeStat);
                } else
                    propStat = new PropStat(notNullStat, notNullStat);
            } else {
                if (distinctStat==null) {
                    Stat defaultStat = Stat.DEFAULT.min(rowStat);
                    propStat = new PropStat(defaultStat);
                }
                else
                    propStat = new PropStat(distinctStat, notNullStat);
            }
            mvUpdateStatProps.mapValue(i, propStat);
        }
        statProps = mvUpdateStatProps.immutableValue();

        assert statDefault || correctStatProps();
    }

    private boolean correctStatProps() {
        for(PropStat stat : statProps.valueIt()) {
            if(!stat.distinct.lessEquals(statKeys.rows))
                stat = stat;
            assert stat.distinct.lessEquals(statKeys.rows);
        }
        return true;
    }

    public static class InconsistentTable extends GlobalTable {

        private final StatKeys<KeyField> statKeys;
        private final ImMap<PropertyField, PropStat> statProps;

        private InconsistentTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, BaseClass baseClass, StatKeys<KeyField> statKeys, ImMap<PropertyField, PropStat> statProps) {
            super(name, keys, properties, null, null);
            initBaseClasses(baseClass);
            this.statKeys = statKeys;
            this.statProps = statProps;
        }

        public StatKeys<KeyField> getStatKeys() {
            return statKeys;
        }

        public ImMap<PropertyField, PropStat> getStatProps() {
            return statProps;
        }
    }

    public Table getInconsistent(BaseClass baseClass) {
        return new InconsistentTable(name, keys, properties, baseClass, statKeys, statProps);
//        return new SerializedTable(name, keys, properties, baseClass);
    }
}
