package platform.server.logics.data;

import platform.server.data.KeyField;
import platform.server.data.Table;
import platform.server.data.types.Type;
import platform.server.logics.classes.RemoteClass;

import java.util.*;

public class ImplementTable extends Table {
    Map<KeyField,RemoteClass> mapFields = new HashMap<KeyField, RemoteClass>();

    public ImplementTable(String iName,RemoteClass... classes) {
        super(iName);

        for(int fieldNum=0;fieldNum<classes.length;fieldNum++) {
            KeyField field = new KeyField("key"+fieldNum, Type.object);
            keys.add(field);
            mapFields.put(field,classes[fieldNum]);
        }
        childs = new HashSet<ImplementTable>();
        parents = new HashSet<ImplementTable>();
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

    <T> boolean recCompare(int operation, Map<T,RemoteClass> toCompare,ListIterator<KeyField> iRec,Map<KeyField,T> mapTo) {
        if(!iRec.hasNext()) return true;

        KeyField proceedItem = iRec.next();
        RemoteClass proceedClass = mapFields.get(proceedItem);
        for(Map.Entry<T,RemoteClass> pairItem : toCompare.entrySet()) {
            if(!mapTo.containsValue(pairItem.getKey()) &&
               ((operation==IS_PARENT && proceedClass.isParent(pairItem.getValue()) ||
               (operation==IS_CHILD && pairItem.getValue().isParent(proceedClass))) ||
               (operation==IS_EQUAL && pairItem.getValue() == proceedClass))) {
                    // если parent - есть связь и нету ключа, гоним рекурсию дальше
                    mapTo.put(proceedItem,pairItem.getKey());
                    // если нашли карту выходим
                    if(recCompare(operation, toCompare,iRec, mapTo)) return true;
                    mapTo.remove(proceedItem);
            }
        }

        iRec.previous();
        return false;
    }

    private final static int COMPARE_DIFF = 0;
    private final static int COMPARE_DOWN = 1;
    private final static int COMPARE_UP = 2;
    private final static int COMPARE_EQUAL = 3;

    // 0 никак не связаны, 1 - параметр снизу в дереве, 2 - параметр сверху в дереве, 3 - равно
    // также возвращает карту если 2
    <T> int compare(Map<T,RemoteClass> toCompare,Map<KeyField,T> mapTo) {

        // перебором и не будем страдать фигней
        // сначала что не 1 проверим

        ListIterator<KeyField> iRec = (new ArrayList<KeyField>(mapFields.keySet())).listIterator();
        if(recCompare(IS_EQUAL,toCompare,iRec,mapTo)) return COMPARE_EQUAL;
        if(recCompare(IS_CHILD,toCompare,iRec,mapTo)) return COMPARE_UP;
        if(recCompare(IS_EQUAL,toCompare,iRec,mapTo)) return COMPARE_DOWN;

        // !!!! должна заполнять MapTo только если уже нашла
        return COMPARE_DIFF;
    }

    void includeIntoGraph(ImplementTable includeItem,boolean toAdd,Set<ImplementTable> checks) {

        if(checks.contains(this)) return;
        checks.add(this);

        Iterator<ImplementTable> i = parents.iterator();
        while(i.hasNext()) {
            ImplementTable item = i.next();
            Integer relation = item.compare(includeItem.mapFields,new HashMap<KeyField,KeyField>());
            if(relation==COMPARE_DOWN) {
                // снизу в дереве
                // добавляем ее как промежуточную
                item.childs.add(includeItem);
                includeItem.parents.add(item);
                if(toAdd) {
                    item.childs.remove(this);
                    i.remove();
                }
            } else {
                // сверху в дереве или никак не связаны
                // передаем дальше
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

    public <T> MapKeysTable<T> getMapTable(Map<T,RemoteClass> findItem) {
        Map<KeyField,T> mapCompare = new HashMap<KeyField, T>();
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
