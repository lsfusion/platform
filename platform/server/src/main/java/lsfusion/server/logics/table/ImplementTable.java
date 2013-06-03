package lsfusion.server.logics.table;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;
import lsfusion.server.SystemProperties;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.IntegerClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.*;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ReflectionLogicsModule;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class ImplementTable extends GlobalTable {
    public final ImMap<KeyField, ValueClass> mapFields;
    private StatKeys<KeyField> statKeys = null;
    private ImMap<PropertyField, PropStat> statProps = null;

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
        parents = new ArrayList<ImplementTable>();

        classes = classes.or(new ClassWhere<KeyField>(mapFields,true));
    }

    public void moveColumn(SQLSession sql, PropertyField field, Table prevTable, ImMap<KeyField, KeyField> mapFields, PropertyField prevField) throws SQLException {
        QueryBuilder<KeyField, PropertyField> moveColumn = new QueryBuilder<KeyField, PropertyField>(this);
        Expr moveExpr = prevTable.join(mapFields.join(moveColumn.getMapExprs())).getExpr(prevField);
        moveColumn.addProperty(field, moveExpr);
        moveColumn.and(moveExpr.getWhere());
        sql.modifyRecords(new ModifyQuery(this, moveColumn.getQuery()));
    }

    public void addField(PropertyField field,ClassWhere<Field> classes) { // кривовато конечно, но пока другого варианта нет
        properties = properties.addExcl(field);
        propertyClasses = propertyClasses.addExcl(field, classes);
    }

    List<ImplementTable> parents;

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

    public void include(List<ImplementTable> tables, boolean toAdd, Set<ImplementTable> checks) {

        Iterator<ImplementTable> i = tables.iterator();
        while(i.hasNext()) {
            ImplementTable item = i.next();
            Integer relation = item.compare(mapFields,new Result<ImRevMap<KeyField, KeyField>>());
            if(relation==COMPARE_DOWN) { // снизу в дереве, добавляем ее как промежуточную
                parents.add(item);
                if(toAdd)
                    i.remove();
            } else { // сверху в дереве или никак не связаны, передаем дальше
                if(relation!=COMPARE_EQUAL && !checks.contains(item)) {
                    checks.add(item);
                    include(item.parents,relation==COMPARE_UP,checks);
                }
                if(relation==COMPARE_UP || relation==COMPARE_EQUAL) toAdd = false;
            }
        }

        // если снизу добавляем Childs
        if(toAdd)
            tables.add(this);
    }

    public <T> MapKeysTable<T> getMapTable(ImMap<T, ValueClass> findItem) {
        Result<ImRevMap<T,KeyField>> mapCompare = new Result<ImRevMap<T, KeyField>>();
        int relation = compare(findItem,mapCompare);
        // если внизу или отличается то не туда явно зашли
        if(relation==COMPARE_DOWN || relation==COMPARE_DIFF) return null;

        for(ImplementTable item : parents) {
            MapKeysTable<T> parentTable = item.getMapTable(findItem);
            if(parentTable!=null) return parentTable;
        }

        return new MapKeysTable<T>(this,mapCompare.result);
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
        for(ImplementTable parent : parents) parent.fillSet(tableImplements);
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

    public Object readCount(DataSession session, Where where) throws SQLException {
        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(SetFact.EMPTY());
        ValueExpr one = new ValueExpr(1, IntegerClass.instance);
        query.addProperty("count", GroupExpr.create(MapFact.<Integer, Expr>EMPTY(), one,
                where, GroupType.SUM, MapFact.<Integer, Expr>EMPTY()));
        return query.execute(session).singleValue().singleValue();
    }

    public void calculateStat(ReflectionLogicsModule reflectionLM, DataSession session) throws SQLException {
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

            DataObject tableObject = (DataObject) reflectionLM.tableSID.readClasses(session, new DataObject(name));
            reflectionLM.rowsTable.change(BaseUtils.nvl(result.get(0), 0), session, tableObject);

            for (KeyField key : keys) {
                DataObject keyObject = (DataObject) reflectionLM.tableKeySID.readClasses(session, new DataObject(name + "." + key.name));
                reflectionLM.quantityTableKey.change(BaseUtils.nvl(result.get(key), 0), session, keyObject);
            }

            for (PropertyField property : properties) {
                DataObject propertyObject = (DataObject) reflectionLM.tableColumnSID.readClasses(session, new DataObject(property.name));
                reflectionLM.quantityTableColumn.change(BaseUtils.nvl(result.get(property), 0), session, propertyObject);
            }

            // не null значения и разреженность колонок
            MExclMap<Object, Object> mNotNulls = MapFact.mExclMap();
            for (PropertyField property : properties)
                mNotNulls.exclAdd(property, readCount(session, join.getExpr(property).getWhere()));
            ImMap<Object, Object> notNulls = mNotNulls.immutable();
            for (PropertyField property : properties) {
                DataObject propertyObject = (DataObject) reflectionLM.tableColumnSID.readClasses(session, new DataObject(property.name));
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
            String keySID = name + "." + keys.get(i).name;
            if (!keyStats.containsKey(keySID))
                mvDistinctKeys.mapValue(i, Stat.DEFAULT);
            else
                mvDistinctKeys.mapValue(i, new Stat(BaseUtils.nvl(keyStats.get(keySID), 0)));
        }
        statKeys = new StatKeys<KeyField>(rowStat, new DistinctKeys<KeyField>(mvDistinctKeys.immutableValue()));

        ImValueMap<PropertyField, PropStat> mvUpdateStatProps = properties.mapItValues();
        for(int i=0,size=properties.size();i<size;i++) {
            PropertyField prop = properties.get(i);
            Stat distinctStat;
            Stat notNullStat;
            if(propStats.containsKey(prop.name)) {
                Pair<Integer, Integer> propStat = propStats.get(prop.name);
                distinctStat = new Stat(BaseUtils.nvl(propStat.first, 0));
                notNullStat = new Stat(BaseUtils.nvl(propStat.second, 0));
            } else {
                distinctStat = null;
                notNullStat = null;
            }

            if (prop.type instanceof DataClass && !((DataClass)prop.type).calculateStat()) {
                if (distinctStat==null) {
                    Stat typeStat = ((DataClass) prop.type).getTypeStat().min(rowStat);
                    mvUpdateStatProps.mapValue(i, new PropStat(typeStat));
                } else
                    mvUpdateStatProps.mapValue(i, new PropStat(notNullStat, notNullStat));
            } else {
                if (distinctStat==null)
                    mvUpdateStatProps.mapValue(i, PropStat.DEFAULT);
                else
                    mvUpdateStatProps.mapValue(i, new PropStat(distinctStat, notNullStat));
            }
        }
        statProps = mvUpdateStatProps.immutableValue();
    }
}
