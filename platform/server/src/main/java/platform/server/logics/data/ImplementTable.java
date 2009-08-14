package platform.server.logics.data;

import platform.server.data.KeyField;
import platform.server.data.Table;
import platform.server.data.classes.ValueClass;
import platform.server.data.classes.where.ClassWhere;

import java.util.*;

public class ImplementTable extends Table {
    Map<KeyField, ValueClass> mapFields = new HashMap<KeyField, ValueClass>();

    public ImplementTable(String iName, ValueClass... implementClasses) {
        super(iName);

        for(int fieldNum=0;fieldNum<implementClasses.length;fieldNum++) {
            KeyField field = new KeyField("key"+fieldNum,implementClasses[fieldNum].getType());
            keys.add(field);
            mapFields.put(field,implementClasses[fieldNum]);
        }
        childs = new HashSet<ImplementTable>();
        parents = new HashSet<ImplementTable>();

        classes = classes.or(new ClassWhere<KeyField>(mapFields,true));
    }

    // кэшированный граф
    Set<ImplementTable> childs;
    Set<ImplementTable> parents;

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
        if(recCompare(IS_EQUAL,toCompare,iRec,mapTo)) return COMPARE_DOWN;

        return COMPARE_DIFF;
    }

    void includeIntoGraph(ImplementTable includeItem,boolean toAdd,Set<ImplementTable> checks) {

        if(checks.contains(this)) return;
        checks.add(this);

        Iterator<ImplementTable> i = parents.iterator();
        while(i.hasNext()) {
            ImplementTable item = i.next();
            Integer relation = item.compare(includeItem.mapFields,new HashMap<KeyField,KeyField>());
            if(relation==COMPARE_DOWN) { // снизу в дереве, добавляем ее как промежуточную
                item.childs.add(includeItem);
                includeItem.parents.add(item);
                if(toAdd) {
                    item.childs.remove(this);
                    i.remove();
                }
            } else { // сверху в дереве или никак не связаны, передаем дальше
                if(relation!=COMPARE_EQUAL) item.includeIntoGraph(includeItem,relation==COMPARE_UP,checks);
                if(relation==COMPARE_UP || relation==COMPARE_EQUAL) toAdd = false;
            }
        }

        // если снизу добавляем Childs
        if(toAdd) {
            includeItem.childs.add(this);
            parents.add(includeItem);
        }
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

    void fillSet(Set<ImplementTable> tableImplements) {
        if(!tableImplements.add(this)) return;
        for(ImplementTable parent : parents) parent.fillSet(tableImplements);
    }
}
