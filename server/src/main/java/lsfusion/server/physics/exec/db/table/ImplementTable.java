package lsfusion.server.physics.exec.db.table;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.interop.ProgressBar;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.CacheAspect;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.controller.stack.StackProgress;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.classes.SingleClassExpr;
import lsfusion.server.data.expr.join.classes.IsClassField;
import lsfusion.server.data.expr.join.classes.ObjectClassField;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.PartitionExpr;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.expr.value.StaticValueExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.query.modify.ModifyQuery;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.TableStatKeys;
import lsfusion.server.data.table.*;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.ObjectClassSet;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.SystemProperties;
import lsfusion.server.physics.admin.reflection.ReflectionLogicsModule;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ImplementTable extends DBTable { // последний интерфейс assert что isFull
    private static double topCoefficient = 0.8;

    private final ImMap<KeyField, ValueClass> mapFields;

    public ImMap<KeyField, ValueClass> getMapFields() {
        return mapFields;
    }

    // для обеспечения детерминированности mapping'a (связано с Property.getOrderTableInterfaceClasses)
    private final ImOrderMap<KeyField, ValueClass> orderMapFields;
    public ImOrderMap<KeyField, ValueClass> getOrderMapFields() {
        return orderMapFields;
    }

    private TableStatKeys statKeys = null;
    private ImMap<PropertyField, PropStat> statProps = null;
    private ImSet<PropertyField> indexedProps = SetFact.EMPTY();
    private ImSet<ImOrderSet<Field>> indexes = SetFact.EMPTY();

    public boolean markedFull;
    public boolean markedExplicit; // if true assert !markedFull

    private String canonicalName;
    
    private IsClassField fullField = null; // поле которое всегда не null, и свойство которого обеспечивает , возможно временно потом совместиться с логикой classExpr
    @Override
    public boolean isFull() {
        return fullField != null;
    }
    public IsClassField getFullField() {
        return fullField;
    }
    private void setFullField(IsClassField field) {
        fullField = field;

        ValueClass fieldClass;
        ImMap<KeyField, ValueClass> mapFields = getMapFields();
        if(mapFields.size() == 1 && (fieldClass = mapFields.singleValue()) instanceof CustomClass) {
            ((CustomClass)fieldClass).setIsClassField(field);
        }
    }
    public void setFullField(final PropertyField field) {
        setFullField(new IsClassField() {
            public PropertyField getField() {
                return field;
            }
            public BaseExpr getFollowExpr(BaseExpr joinExpr) {
                return (BaseExpr) joinAnd(MapFact.singleton(keys.single(), joinExpr)).getExpr(field);
            }
            public Where getIsClassWhere(SingleClassExpr expr, ObjectValueClassSet set, IsClassType type) {
                assert isFull();
                assert type == IsClassType.CONSISTENT;
                assert getClasses().getCommonClass(keys.single()).containsAll(set, false) && set.containsAll(getClasses().getCommonClass(keys.single()), false);
                return joinAnd(MapFact.singleton(keys.single(), expr)).getWhere();
            }
        });
    }
    public void setFullField(ObjectClassField isClassField) {
        setFullField((IsClassField) isClassField);
    }

    @Override
    protected boolean isIndexed(PropertyField field) {
        return indexedProps.contains(field);
    }

    public ImplementTable(String name, final ValueClass... implementClasses) {
        super(name);

        ImOrderSet<KeyField> keys;
        keys = SetFact.toOrderExclSet(implementClasses.length, i -> new KeyField("key"+i,implementClasses[i].getType()));
        ImMap<KeyField, ValueClass> mapFields;
        mapFields = keys.mapOrderValues((int i) -> implementClasses[i]);

        parents = NFFact.orderSet();
        classes = classes.or(new ClassWhere<>(mapFields, true));
        this.keys = keys;
        this.mapFields = mapFields;
        orderMapFields = keys.mapOrderMap(mapFields);
    }

    public <P extends PropertyInterface> IQuery<KeyField, Property> getReadSaveQuery(ImSet<Property> properties, PropertyChanges propertyChanges) {
        QueryBuilder<KeyField, Property> changesQuery = new QueryBuilder<>(this);
        WhereBuilder changedWhere = new WhereBuilder();
        for (Property<P> property : properties)
            changesQuery.addProperty(property, property.getIncrementExpr(property.mapTable.mapKeys.join(changesQuery.getMapExprs()), propertyChanges, changedWhere));
        changesQuery.and(changedWhere.toWhere());
        return changesQuery.getQuery();
    }

    public void moveColumn(SQLSession sql, PropertyField field, NamedTable prevTable, ImMap<KeyField, KeyField> mapFields, PropertyField prevField) throws SQLException, SQLHandledException {
        QueryBuilder<KeyField, PropertyField> moveColumn = new QueryBuilder<>(this);
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

    @IdentityLazy
    protected ImSet<ImOrderSet<Field>> getIndexes() {
        return super.getIndexes().addExcl(indexes);
    }

    @NFLazy
    public void addIndex(ImOrderSet<Field> index) { // кривовато конечно, но пока другого варианта нет
        assert CacheAspect.checkNoCaches(this, CacheAspect.Type.SIMPLE, Table.class, "getIndexes");
        indexes = indexes.addExcl(index);

        Field field = index.get(0);
        if(field instanceof PropertyField && !indexedProps.contains((PropertyField) field)) // временно
            indexedProps = indexedProps.addExcl((PropertyField) field);
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

    private <T> boolean recCompare(int operation, ImOrderMap<T, ValueClass> toCompare,int iRec,Map<T,KeyField> mapTo) {
        ImOrderMap<KeyField, ValueClass> orderMapFields = getOrderMapFields();
        if(iRec>=orderMapFields.size()) return true;

        KeyField proceedItem = orderMapFields.getKey(iRec);
        ValueClass proceedClass = orderMapFields.getValue(iRec);
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
    private <T> int compare(ImOrderMap<T, ValueClass> toCompare,Result<ImRevMap<T,KeyField>> mapTo) {

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

    public <T> boolean equalClasses(ImOrderMap<T, ValueClass> mapClasses) {
        int compResult = compare(mapClasses, new Result<>());
        return compResult == COMPARE_EQUAL || compResult == COMPARE_UP;
    }

    public void include(NFOrderSet<ImplementTable> tables, Version version, boolean toAdd, Set<ImplementTable> checks, ImplementTable debugItem) {
        ImList<ImplementTable> current = tables.getNFList(Version.current());
        
        Iterator<ImplementTable> i = current.iterator();
        boolean wasRemove = false; // для assertiona
        while(i.hasNext()) {
            ImplementTable item = i.next();
            int relation = item.compare(getOrderMapFields(), new Result<>());
            if(relation==COMPARE_DOWN) { // снизу в дереве, добавляем ее как промежуточную
                if(checkSiblings(item, parents, this))
                    parents.add(item, version);
                
                if(toAdd) {
                    wasRemove = true;
                    tables.remove(item, Version.current()); // последняя версия нужна, так как в противном случае удаление может пойти до добавления 
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
        for(ImplementTable siblingTable : tables.getNFList(Version.current())) {
            if(BaseUtils.hashEquals(item, siblingTable))
                return false;
            int compare = siblingTable.compare(item.getOrderMapFields(), new Result<>());
            if(compare==COMPARE_UP || compare == COMPARE_DOWN)
                return false;
        }
        return true;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public boolean isNamed() {
        return canonicalName != null;
    }

    public void finalizeAroundInit() {
        parents.finalizeChanges();
    }

    private interface MapTableType {
        boolean skipParents(ImplementTable table);
        boolean skipResult(ImplementTable table);
        boolean onlyFirstParent(ImplementTable table);
        boolean skipCompareUp();
    }

    // поиск таблицы для классов
    private final static MapTableType findTable = new MapTableType() {
        public boolean skipParents(ImplementTable table) {
            return false;
        }

        public boolean skipResult(ImplementTable table) {
            return table.markedExplicit;
        }

        public boolean onlyFirstParent(ImplementTable table) {
            return true;
        }

        public boolean skipCompareUp() { return false; }
    };

    private final static MapTableType findClassTable = new MapTableType() {
        public boolean skipParents(ImplementTable table) {
            return !skipResult(table);
        }

        public boolean skipResult(ImplementTable table) {
            return !table.markedFull;
        }

        public boolean onlyFirstParent(ImplementTable table) {
            return true;
        }
        
        public boolean skipCompareUp() { return false; }
    };

    // поиск сгенерированной таблицы
    private final static MapTableType findAutoTable = new MapTableType() {
        public boolean skipParents(ImplementTable table) {
            return true;
        }

        public boolean skipResult(ImplementTable table) {
            return table.markedExplicit;
        }

        public boolean onlyFirstParent(ImplementTable table) {
            throw new UnsupportedOperationException();
        }

        public boolean skipCompareUp() { return true; }
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

        public boolean skipCompareUp() { return false; }
    }

    public <T> MapKeysTable<T> getSingleMapTable(ImOrderMap<T, ValueClass> findItem, boolean auto) {
        ImSet<MapKeysTable<T>> tables = getMapTables(findItem, auto ? findAutoTable : findTable);
        return tables.isEmpty() ? null : tables.single();
    }

    public <T> MapKeysTable<T> getClassMapTable(ImOrderMap<T, ValueClass> findItem) {
        ImSet<MapKeysTable<T>> tables = getMapTables(findItem, findClassTable);
        return tables.isEmpty() ? null : tables.single(); 
    }

    public <T> ImSet<MapKeysTable<T>> getFullMapTables(ImOrderMap<T, ValueClass> findItem, ImplementTable skipTable) {
        return getMapTables(findItem, new FindFullTables(skipTable));
    }

    public <T> ImSet<MapKeysTable<T>> getMapTables(ImOrderMap<T, ValueClass> findItem, MapTableType type) {
        Result<ImRevMap<T,KeyField>> mapCompare = new Result<>();
        int relation = compare(findItem,mapCompare);
        // если внизу или отличается то не туда явно зашли
        if(relation==COMPARE_DOWN || relation==COMPARE_DIFF || relation==COMPARE_UP && type.skipCompareUp()) return SetFact.EMPTY();

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

        return SetFact.singleton(new MapKeysTable<>(this, mapCompare.result));
    }

    public <T> MapKeysTable<T> getMapKeysTable(ImOrderMap<T, ValueClass> classes) {
        Result<ImRevMap<T,KeyField>> mapCompare = new Result<>();
        int relation = compare(classes, mapCompare);
        if(relation==COMPARE_DOWN || relation==COMPARE_DIFF)
            return null;
        return new MapKeysTable<>(this, mapCompare.result);
    }

    void fillSet(MSet<ImplementTable> tableImplements, Set<String> notRecalculateStatsTableSet) {
        if ((notRecalculateStatsTableSet == null || !notRecalculateStatsTableSet.contains(this.getName())) && tableImplements.add(this)) return;
        for (ImplementTable parent : getParentsIt())
            parent.fillSet(tableImplements, notRecalculateStatsTableSet);
    }

    public TableStatKeys getTableStatKeys() {
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

    private Object readCount(DataSession session, Where where) throws SQLException, SQLHandledException {
        return readCount(session, where, 0, false);
    }

    private Object readCount(DataSession session, Where where, int total, boolean useCoefficient) throws SQLException, SQLHandledException {
        QueryBuilder<Object, Object> query = new QueryBuilder<>(SetFact.EMPTY());
        StaticValueExpr one = ValueExpr.COUNT;
        query.addProperty("count", GroupExpr.create(MapFact.EMPTY(), one,
                where, GroupType.SUM, MapFact.EMPTY()));
        Integer count = (Integer) query.execute(session).singleValue().singleValue();
        return count == null ? Math.min(total, 1) : (useCoefficient ? (int) Math.min(total, (count / topCoefficient) + 1) : count);
    }

    private DataObject safeReadClasses(DataSession session, LP lcp, DataObject... objects) throws SQLException, SQLHandledException {
        ObjectValue value = lcp.readClasses(session, objects);
        if(value instanceof DataObject)
            return (DataObject) value;
//        ServerLoggers.assertLog(false, "SHOULD BE SYNCHRONIZED : " + lcp + ", keys : " + Arrays.toString(objects));
        return null;
    }

    public void recalculateStat(ReflectionLogicsModule reflectionLM, Set<String> disableStatsTableColumnSet, DataSession session) throws SQLException, SQLHandledException {
        recalculateStat(reflectionLM, session, null, disableStatsTableColumnSet, SetFact.EMPTY(), false);
        recalculateStat(reflectionLM, session, null, disableStatsTableColumnSet, SetFact.EMPTY(), true);
    }

    public static class CalcStat {
        public final int rows;
        public final ImMap<String, Integer> keys;
        public final ImMap<String, Pair<Integer, Integer>> props;

        public CalcStat(int rows, ImMap<String, Integer> keys, ImMap<String, Pair<Integer, Integer>> props) {
            this.rows = rows;
            this.keys = keys;
            this.props = props;
        }
    }

    public CalcStat recalculateStat(ReflectionLogicsModule reflectionLM, DataSession session, ImMap<PropertyField, String> props, ImSet<PropertyField> skipRecalculateFields, boolean top) throws SQLException, SQLHandledException {
        return recalculateStat(reflectionLM, session, props, new HashSet<>(), skipRecalculateFields, top);
    }

    public CalcStat recalculateStat(ReflectionLogicsModule reflectionLM, DataSession session, ImMap<PropertyField, String> props,
                                    Set<String> disableStatsTableColumnSet, ImSet<PropertyField> skipRecalculateFields, boolean top) throws SQLException, SQLHandledException {
        ImMap<String, Integer> keyStat = MapFact.EMPTY();
        ImMap<String, Pair<Integer, Integer>> propStats = MapFact.EMPTY();
        int rows = 0;
        if (!SystemProperties.doNotCalculateStats) {
            ImRevMap<KeyField, KeyExpr> mapKeys = getMapKeys();
            lsfusion.server.data.query.build.Join<PropertyField> join = join(mapKeys);

            MExclMap<Object, Object> mResult = MapFact.mExclMap();
            MExclMap<Object, Object> mNotNulls = MapFact.mExclMap();

            Where inWhere = join.getWhere();
            KeyExpr countKeyExpr = new KeyExpr("count");

            boolean skipRecalculateAllFields = props != null && props.size() == skipRecalculateFields.size();
            Integer total = skipRecalculateAllFields ? 0 : (Integer) readCount(session, inWhere);

            for (KeyField key : keys) {
                ImMap<Object, Expr> map = MapFact.singleton(0, mapKeys.get(key));
                mResult.exclAdd(key, skipRecalculateAllFields ? 0 : readCount(session, getCountWhere(session.sql, GroupExpr.create(map, inWhere, map, true),
                            GroupExpr.create(map, inWhere, map, false), mapKeys.get(key), total, top && keys.size() > 1), total, top && keys.size() > 1));
            }

            ImSet<PropertyField> propertyFieldSet = props == null ? properties : props.keys();

            for (PropertyField prop : propertyFieldSet) {
                if(!disableStatsTableColumnSet.contains(prop.getName())) {
                    boolean skipRecalculate = skipRecalculateFields.contains(prop);
                    Integer notNullCount = skipRecalculate ? 0 : (Integer) readCount(session, join.getExpr(prop).getWhere());
                    mNotNulls.exclAdd(prop, notNullCount);

                    if (props != null ? props.containsKey(prop) : !(prop.type instanceof DataClass && !((DataClass) prop.type).calculateStat())) {
                        mResult.exclAdd(prop, skipRecalculate ? 0 : readCount(session, getCountWhere(session.sql,
                                GroupExpr.create(MapFact.singleton(0, join.getExpr(prop)), Where.TRUE(), MapFact.singleton(0, countKeyExpr), true),
                                GroupExpr.create(MapFact.singleton(0, join.getExpr(prop)), Where.TRUE(), MapFact.singleton(0, countKeyExpr), false),
                                countKeyExpr, notNullCount, top), notNullCount, top));
                    }
                }
            }

            mResult.exclAdd(0, total);
            ImMap<Object, Object> result = mResult.immutable();

            DataObject tableObject = safeReadClasses(session, reflectionLM.tableSID, new DataObject(getName()));
            if(tableObject == null && getName() != null) {
                tableObject = session.addObject(reflectionLM.table);
                reflectionLM.sidTable.change(getName(), session, tableObject);
            }
            int quantity = BaseUtils.nvl((Integer)result.get(0), 0);
            rows = quantity;
            reflectionLM.rowsTable.change(quantity, session, (DataObject) tableObject);

            for (KeyField key : keys) {
                DataObject keyObject = safeReadClasses(session, reflectionLM.tableKeySID, new DataObject(getName() + "." + key.getName()));
                if (keyObject == null) {
                    keyObject = session.addObject(reflectionLM.tableKey);
                    reflectionLM.sidTableKey.change(getName() + "." + key.getName(), session, keyObject);
                }
                quantity = BaseUtils.nvl((Integer)result.get(key), 0);
                (top ? reflectionLM.quantityTopTableKey : reflectionLM.quantityTableKey).change(quantity, session, keyObject);
                keyStat = keyStat.addExcl(getName() + "." + key.getName(), quantity);
            }

            ImMap<Object, Object> notNulls = mNotNulls.immutable();

            for (PropertyField property : propertyFieldSet) {
                if(!disableStatsTableColumnSet.contains(property.getName())) {
                    DataObject propertyObject = safeReadClasses(session, reflectionLM.propertyTableSID, new DataObject(getName()), new DataObject(property.getName()));
                    if (propertyObject == null && props != null) {
                        String canonicalName = props.get(property);
                        propertyObject = safeReadClasses(session, reflectionLM.propertyCanonicalName, new DataObject(canonicalName));
                        if (propertyObject == null) {
                            propertyObject = session.addObject(reflectionLM.property);
                            reflectionLM.canonicalNameProperty.change(canonicalName, session, propertyObject);
                        }
                        reflectionLM.storedProperty.change(true, session, propertyObject);
                        reflectionLM.dbNameProperty.change(property.getName(), session, propertyObject);
                        reflectionLM.tableSIDProperty.change(getName(), session, propertyObject);
                    }
                    if (propertyObject != null) {
                        (top ? reflectionLM.quantityTopProperty : reflectionLM.quantityProperty).change((Integer) result.get(property), session, propertyObject); // если не расчитывается статистика запишется null (что в общем то и требуется)

                        int notNull = BaseUtils.nvl((Integer) notNulls.get(property), 0);
                        quantity = BaseUtils.nvl((Integer) result.get(property), 0);
                        reflectionLM.notNullQuantityProperty.change(notNull, session, propertyObject);
                        propStats = propStats.addExcl(getName() + "." + property.getName(), Pair.create(quantity, notNull));
                    }
                }
            }
        }
        return new CalcStat(rows, keyStat, propStats);
    }

    private Where getCountWhere(SQLSession session, Expr quantityTopExpr, Expr quantityNotTopExpr, KeyExpr keyExpr, Integer total, boolean top) {
        if (top) {
            ImList<Expr> exprs = ListFact.singleton(quantityTopExpr);
            ImOrderMap<Expr, Boolean> orders = MapFact.toOrderMap(quantityTopExpr, true, keyExpr, false);
            ImSet<Expr> partitions = SetFact.EMPTY();
            ImMap<KeyExpr, KeyExpr> group = MapFact.singleton(keyExpr, keyExpr);
            Expr partitionExpr = PartitionExpr.create(PartitionType.sum(), exprs, orders, true, partitions, group);

            return partitionExpr.compare(new DataObject(Math.ceil((total == null ? 0 : total) * topCoefficient)), Compare.LESS_EQUALS);
        }
        else
            return quantityNotTopExpr.getWhere();
    }

    @StackProgress
    public boolean overCalculateStat(ReflectionLogicsModule reflectionLM, DataSession session, MSet<Long> propertiesSet, Set<String> disableStatsTableColumnSet, @StackProgress ProgressBar progressBar) throws SQLException, SQLHandledException {
        boolean found = overCalculateStat(reflectionLM, session, propertiesSet, disableStatsTableColumnSet, progressBar, false);
        overCalculateStat(reflectionLM, session, propertiesSet, disableStatsTableColumnSet, progressBar, true);
        return found;
    }

    @StackProgress
    public boolean overCalculateStat(ReflectionLogicsModule reflectionLM, DataSession session, MSet<Long> propertiesSet, Set<String> disableStatsTableColumnSet, @StackProgress ProgressBar progressBar, boolean top) throws SQLException, SQLHandledException {
        boolean found = false;
        if (!SystemProperties.doNotCalculateStats) {

            ImRevMap<KeyField, KeyExpr> mapKeys = getMapKeys();
            lsfusion.server.data.query.build.Join<PropertyField> join = join(mapKeys);

            MExclMap<Object, Object> mResult = MapFact.mExclMap();
            MExclMap<Object, Object> mNotNulls = MapFact.mExclMap();

            KeyExpr countKeyExpr = new KeyExpr("count");

            for(PropertyField prop : properties) {
                if(!disableStatsTableColumnSet.contains(prop.getName())) {
                    Integer notNullCount = (Integer) readCount(session, join.getExpr(prop).getWhere());
                    mNotNulls.exclAdd(prop, notNullCount);

                    if (!(prop.type instanceof DataClass && !((DataClass) prop.type).calculateStat())) {
                        mResult.exclAdd(prop, readCount(session, getCountWhere(session.sql,
                                GroupExpr.create(MapFact.singleton(0, join.getExpr(prop)), Where.TRUE(), MapFact.singleton(0, countKeyExpr), true),
                                GroupExpr.create(MapFact.singleton(0, join.getExpr(prop)), Where.TRUE(), MapFact.singleton(0, countKeyExpr), false),
                                countKeyExpr, notNullCount, top), notNullCount, top));
                    }
                }
            }
            ImMap<Object, Object> result = mResult.immutable();
            ImMap<Object, Object> notNulls = mNotNulls.immutable();

            for (PropertyField property : properties) {
                if(!disableStatsTableColumnSet.contains(property.getName())) {
                    DataObject propertyObject = safeReadClasses(session, reflectionLM.propertyTableSID, new DataObject(getName()), new DataObject(property.getName()));

                    if (propertyObject != null && propertiesSet.contains((Long) propertyObject.getValue())) {
                        (top ? reflectionLM.quantityTopProperty : reflectionLM.quantityProperty).change(BaseUtils.nvl((Integer) result.get(property), 0), session, propertyObject);
                        reflectionLM.notNullQuantityProperty.change(BaseUtils.nvl((Integer) notNulls.get(property), 0), session, propertyObject);
                        found = true;
                    }
                }
            }
        }
        return found;
    }

    public void fillFullClassStat(ImMap<String, Integer> tableStats, ImMap<String, Integer> keyStats, MMap<CustomClass, Integer> mClassFullStats) {
        if(isFull()) {
            Integer tableCount = tableStats.get(getName());
            ImOrderMap<KeyField, ValueClass> mapFields = getOrderMapFields();

            int fillStat = 0; int keyStatsCount = 0; int tableKeyStats = 1;
            for(int i=0,size=mapFields.size();i<size;i++) {
                Integer keyCount = keyStats.get(getName() + "." + mapFields.getKey(i).getName());
                if(keyCount != null && !(tableCount != null && keyStatsCount == size - 1)) { // если последняя, а все остальные заполнены
                    keyStatsCount++;
                    tableKeyStats *= keyCount;

                    mClassFullStats.add((CustomClass)mapFields.getValue(i), keyCount);
                } else
                    fillStat = i;
            }
            if (tableCount != null && keyStatsCount == mapFields.size() - 1) {
                assert fillStat >= 0;
                mClassFullStats.add((CustomClass) mapFields.getValue(fillStat), tableCount / tableKeyStats);
            }
        }
    }

    // последний параметр нужен только, чтобы не закэшировалась совсем неправильная статистика для Reflection таблиц (где все классы, а значит и колонки имеют статистику 0) - она конечно и так закэшируется неправильная, но хотя бы пессимистичная
    public void updateStat(ImMap<String, Integer> tableStats, ImMap<String, Integer> keyStats, ImMap<String, Pair<Integer, Integer>> propStats, ImSet<PropertyField> props, boolean noClassStatsYet) {

        Integer rowCount;
        if (!tableStats.containsKey(getName()))
            rowCount = Stat.DEFAULT.getCount();
        else
            rowCount = BaseUtils.nvl(tableStats.get(getName()), 0);

        if(props == null) {
            ImMap<KeyField, AndClassSet> keyClassStats = getClasses().getCommonClasses(keys.getSet());

            ImSet<KeyField> tableKeys = getTableKeys();
            ImValueMap<KeyField, Integer> mvDistinctKeys = tableKeys.mapItValues(); // exception есть
            for (int i = 0, size = tableKeys.size(); i < size; i++) {
                KeyField tableKey = tableKeys.get(i);
                String keySID = getName() + "." + tableKey.getName();
                Integer keyCount = keyStats.get(keySID);
                if(keyCount == null)
                    keyCount = Stat.DEFAULT.getCount();
                
                if(!noClassStatsYet) {
                    AndClassSet keyClasses = keyClassStats.get(tableKey);
                    if (keyClasses instanceof ObjectClassSet) {
                        int classCount = ((ObjectValueClassSet) keyClasses).getCount();
                        keyCount = BaseUtils.min(keyCount, classCount); // неправильная статистика уменьшаем до числа классов (иначе могут быть непредсказуемые последствия, например infinite push down в getLastSQLQuery 
                    }
                }

                keyCount = BaseUtils.min(keyCount, rowCount);

                mvDistinctKeys.mapValue(i, keyCount);
            }
            statKeys = TableStatKeys.createForTable(rowCount, mvDistinctKeys.immutableValue());
        }

        ImSet<PropertyField> propertyFieldSet = props == null ? properties : props;

        Stat rowStat = statKeys.getRows();
        ImValueMap<PropertyField, PropStat> mvUpdateStatProps = propertyFieldSet.mapItValues();
        for(int i=0,size=propertyFieldSet.size();i<size;i++) {
            PropertyField prop = propertyFieldSet.get(i);
            Stat distinctStat = null;
            Stat notNullStat = null;
            Pair<Integer, Integer> propExStat = propStats.get(getName() + "." + prop.getName());
            if (propExStat != null) {
                if (propExStat.second != null)
                    notNullStat = new Stat(propExStat.second);
                if (propExStat.first != null && propExStat.first > 0) // тут пока неясный контракт с distinct (из-за DataClass.calculateStat), но 0 даже теоретически быть не может (поэтому будем считать не расчитанным), вообще   
                    distinctStat = new Stat(propExStat.first);
            }
            if (distinctStat == null)
                distinctStat = Stat.DEFAULT;

            if(!noClassStatsYet) {
                AndClassSet propClasses = propertyClasses.get(prop).getCommonClass(prop);
                Stat propClassStat = Stat.ALOT;
                if (propClasses instanceof ObjectClassSet) { // неправильная статистика уменьшаем до числа классов (иначе могут быть непредсказуемые последствия, например infinite push down в getLastSQLQuery)
                    propClassStat = new Stat(((ObjectValueClassSet) propClasses).getCount());
                } else if (propClasses instanceof DataClass) {
                    propClassStat = ((DataClass) propClasses).getTypeStat();
                }
                distinctStat = distinctStat.min(propClassStat);
            }

            // неправильная статистика
            distinctStat = distinctStat.min(rowStat);
            if(notNullStat != null) {
                notNullStat = notNullStat.min(rowStat);
                if((distinctStat == null || notNullStat.less(distinctStat))) 
                    distinctStat = notNullStat;
            }
            mvUpdateStatProps.mapValue(i, new PropStat(distinctStat, notNullStat));
        }
        ImMap<PropertyField, PropStat> updateStatProps = mvUpdateStatProps.immutableValue();
        if(props == null)
            statProps = updateStatProps;
        else {
            assert statProps.keys().containsAll(updateStatProps.keys());
            statProps = MapFact.replaceValues(statProps, updateStatProps);
        }

//        assert statDefault || correctStatProps();
    }

    private boolean correctStatProps() {
        for(PropStat stat : statProps.valueIt()) {
            assert stat.distinct.lessEquals(statKeys.getRows());
        }
        return true;
    }

    public static class InconsistentTable extends DBTable {

        private final TableStatKeys statKeys;
        private final ImMap<PropertyField, PropStat> statProps;

        private InconsistentTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, BaseClass baseClass, TableStatKeys statKeys, ImMap<PropertyField, PropStat> statProps) {
            super(name, keys, properties, null, null);
            initBaseClasses(baseClass);
            this.statKeys = statKeys;
            this.statProps = statProps;
        }

        public TableStatKeys getTableStatKeys() {
            return statKeys;
        }

        public ImMap<PropertyField, PropStat> getStatProps() {
            return statProps;
        }
    }

    public NamedTable getInconsistent(BaseClass baseClass) {
        return new InconsistentTable(name, keys, properties, baseClass, statKeys, statProps);
//        return new SerializedTable(name, keys, properties, baseClass);
    }
}
