package platform.server.logics.table;

import platform.base.Result;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.server.classes.ValueClass;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.query.QueryBuilder;
import platform.server.data.where.classes.ClassWhere;

import java.sql.SQLException;
import java.util.*;

public class ImplementTable extends DataTable {
    public final ImMap<KeyField, ValueClass> mapFields;

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
}
