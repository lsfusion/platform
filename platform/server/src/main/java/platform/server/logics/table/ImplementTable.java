package platform.server.logics.table;

import platform.base.BaseUtils;
import platform.server.classes.ValueClass;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.query.Query;
import platform.server.data.where.classes.ClassWhere;

import java.sql.SQLException;
import java.util.*;

public class ImplementTable extends DataTable {
    public Map<KeyField, ValueClass> mapFields = new HashMap<KeyField, ValueClass>();

    public ImplementTable(String name, ValueClass... implementClasses) {
        super(name);

        for(int fieldNum=0;fieldNum<implementClasses.length;fieldNum++) {
            KeyField field = new KeyField("key"+fieldNum,implementClasses[fieldNum].getType());
            keys.add(field);
            mapFields.put(field,implementClasses[fieldNum]);
        }
        parents = new ArrayList<ImplementTable>();

        classes = classes.or(new ClassWhere<KeyField>(mapFields,true));
    }

    public void moveColumn(SQLSession sql, PropertyField field, Table prevTable, Map<KeyField, KeyField> mapFields, PropertyField prevField) throws SQLException {
        Query<KeyField, PropertyField> moveColumn = new Query<KeyField, PropertyField>(this);
        Expr moveExpr = prevTable.joinAnd(BaseUtils.join(mapFields, moveColumn.mapKeys)).getExpr(prevField);
        moveColumn.properties.put(field, moveExpr);
        moveColumn.and(moveExpr.getWhere());
        sql.modifyRecords(new ModifyQuery(this, moveColumn));
    }

    public void addField(PropertyField field,ClassWhere<Field> classes) {
        properties.add(field);
        propertyClasses.put(field,classes);
    }

    List<ImplementTable> parents;

    // operation на что сравниваем
    // 0 - не ToParent
    // 1 - ToParent
    // 2 - равно
    private final static int IS_CHILD = 0;
    private final static int IS_PARENT = 1;
    private final static int IS_EQUAL = 2;

    private <T> boolean recCompare(int operation, Map<T, ValueClass> toCompare,ListIterator<KeyField> iRec,Map<T,KeyField> mapTo) {
        if(!iRec.hasNext()) return true;

        KeyField proceedItem = iRec.next();
        ValueClass proceedClass = mapFields.get(proceedItem);
        for(Map.Entry<T, ValueClass> pairItem : toCompare.entrySet()) {
            if(!mapTo.containsKey(pairItem.getKey()) &&
               ((operation==IS_PARENT && pairItem.getValue().isCompatibleParent(proceedClass)) ||
               (operation==IS_CHILD && proceedClass.isCompatibleParent(pairItem.getValue())) ||
               (operation==IS_EQUAL && pairItem.getValue() == proceedClass))) {
                    // если parent - есть связь и нету ключа, гоним рекурсию дальше
                    mapTo.put(pairItem.getKey(),proceedItem);
                    // если нашли карту выходим
                    if(recCompare(operation, toCompare,iRec, mapTo)) return true;
                    mapTo.remove(pairItem.getKey());
            }
        }

        iRec.previous();
        return false;
    }

    private final static int COMPARE_DIFF = 0;
    private final static int COMPARE_DOWN = 1;
    private final static int COMPARE_UP = 2;
    private final static int COMPARE_EQUAL = 3;

    // также возвращает карту если не Diff
    private <T> int compare(Map<T, ValueClass> toCompare,Map<T,KeyField> mapTo) {

        toCompare = new TreeMap<T,ValueClass>(toCompare);
        ListIterator<KeyField> iRec = (new ArrayList<KeyField>(new TreeSet<KeyField>(mapFields.keySet()))).listIterator();

        if(recCompare(IS_EQUAL,toCompare,iRec,mapTo)) return COMPARE_EQUAL;
        if(recCompare(IS_CHILD,toCompare,iRec,mapTo)) return COMPARE_UP;
        if(recCompare(IS_PARENT,toCompare,iRec,mapTo)) return COMPARE_DOWN;

        return COMPARE_DIFF;
    }

    public <T> boolean equalClasses(Map<T, ValueClass> mapClasses) {
        int compResult = compare(mapClasses, new HashMap<T, KeyField>());
        return compResult == COMPARE_EQUAL || compResult == COMPARE_UP;
    }

    public void include(List<ImplementTable> tables, boolean toAdd, Set<ImplementTable> checks) {

        Iterator<ImplementTable> i = tables.iterator();
        while(i.hasNext()) {
            ImplementTable item = i.next();
            Integer relation = item.compare(mapFields,new HashMap<KeyField,KeyField>());
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

    public <T> MapKeysTable<T> getMapTable(Map<T, ValueClass> findItem) {
        Map<T,KeyField> mapCompare = new HashMap<T,KeyField>();
        int relation = compare(findItem,mapCompare);
        // если внизу или отличается то не туда явно зашли
        if(relation==COMPARE_DOWN || relation==COMPARE_DIFF) return null;

        for(ImplementTable item : parents) {
            MapKeysTable<T> parentTable = item.getMapTable(findItem);
            if(parentTable!=null) return parentTable;
        }

        return new MapKeysTable<T>(this,mapCompare);
    }

    public <T> MapKeysTable<T> getMapKeysTable(Map<T, ValueClass> classes) {
        Map<T,KeyField> mapCompare = new HashMap<T,KeyField>();
        int relation = compare(classes, mapCompare);
        if(relation==COMPARE_DOWN || relation==COMPARE_DIFF)
            return null;
        return new MapKeysTable<T>(this,mapCompare);
    }

    void fillSet(Set<ImplementTable> tableImplements) {
        if(!tableImplements.add(this)) return;
        for(ImplementTable parent : parents) parent.fillSet(tableImplements);
    }
}
