package platform.server.logics.data;

import java.util.*;

import platform.server.logics.properties.DataPropertyInterface;
import platform.server.data.Table;
import platform.server.data.KeyField;

public class TableImplement extends ArrayList<DataPropertyInterface> {
    // заполняются пока автоматически
    Table table;
    Map<DataPropertyInterface, KeyField> mapFields;

    public TableImplement() {
        this(null);
    }

    TableImplement(String iID) {
        ID = iID;
        childs = new HashSet<TableImplement>();
        parents = new HashSet<TableImplement>();
    }

    String ID = null;
    String getID() {

        if (ID != null) return ID;

        String result = "";
        for (DataPropertyInterface propint : this) {
            result += "_" + propint.interfaceClass.ID.toString();
        }
        return result;
    }

    // кэшированный граф
    Set<TableImplement> childs;
    Set<TableImplement> parents;

    // Operation на что сравниваем
    // 0 - не ToParent
    // 1 - ToParent
    // 2 - равно

    boolean recCompare(int operation, Collection<DataPropertyInterface> toCompare,ListIterator<DataPropertyInterface> iRec,Map<DataPropertyInterface,DataPropertyInterface> mapTo) {
        if(!iRec.hasNext()) return true;

        DataPropertyInterface proceedItem = iRec.next();
        for(DataPropertyInterface pairItem : toCompare) {
            if((operation ==1 && proceedItem.interfaceClass.isParent(pairItem.interfaceClass) || (operation ==0 && pairItem.interfaceClass.isParent(proceedItem.interfaceClass))) || (operation ==2 && pairItem.interfaceClass == proceedItem.interfaceClass)) {
                if(!mapTo.containsKey(pairItem)) {
                    // если parent - есть связь и нету ключа, гоним рекурсию дальше
                    mapTo.put(pairItem, proceedItem);
                    // если нашли карту выходим
                    if(recCompare(operation, toCompare,iRec, mapTo)) return true;
                    mapTo.remove(pairItem);
                }
            }
        }

        iRec.previous();
        return false;
    }
    // 0 никак не связаны, 1 - параметр снизу в дереве, 2 - параметр сверху в дереве, 3 - равно
    // также возвращает карту если 2
    int compare(Collection<DataPropertyInterface> toCompare,Map<KeyField,DataPropertyInterface> mapTo) {

        if(toCompare.size() != size()) return 0;

        // перебором и не будем страдать фигней
        // сначала что не 1 проверим

        HashMap<DataPropertyInterface,DataPropertyInterface> mapProceed = new HashMap<DataPropertyInterface, DataPropertyInterface>();

        ListIterator<DataPropertyInterface> iRec = (new ArrayList<DataPropertyInterface>(this)).listIterator();
        int relation = 0;
        if(recCompare(2,toCompare,iRec, mapProceed)) relation = 3;
        if(relation==0 && recCompare(0,toCompare,iRec, mapProceed)) relation = 2;
        if(relation>0) {
            if(mapTo !=null) {
                mapTo.clear();
                for(DataPropertyInterface dataInterface : toCompare)
                    mapTo.put(mapFields.get(mapProceed.get(dataInterface)),dataInterface);
            }

            return relation;
        }

        // MapProceed и так чистый и iRec также в начале
        if(recCompare(1,toCompare,iRec, mapProceed)) relation = 1;

        // !!!! должна заполнять MapTo только если уже нашла
        return relation;
    }

    void recIncludeIntoGraph(TableImplement includeItem,boolean toAdd,Set<TableImplement> checks) {

        if(checks.contains(this)) return;
        checks.add(this);

        Iterator<TableImplement> i = parents.iterator();
        while(i.hasNext()) {
            TableImplement item = i.next();
            Integer relation = item.compare(includeItem,null);
            if(relation==1) {
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
                if(relation!=3) item.recIncludeIntoGraph(includeItem,relation==2,checks);
                if(relation==2 || relation==3) toAdd = false;
            }
        }

        // если снизу добавляем Childs
        if(toAdd) {
            includeItem.childs.add(this);
            parents.add(includeItem);
        }
    }

    public Table getTable(Collection<DataPropertyInterface> findItem,Map<KeyField,DataPropertyInterface> mapTo) {
        for(TableImplement item : parents) {
            int relation = item.compare(findItem,mapTo);
            if(relation==2 || relation==3)
                return item.getTable(findItem,mapTo);
        }

        return table;
    }

    void fillSet(Set<TableImplement> tableImplements) {
        if(!tableImplements.add(this)) return;
        for(TableImplement parent : parents) parent.fillSet(tableImplements);
    }

    void outClasses() {
        for(DataPropertyInterface propertyInterface : this)
            System.out.print(propertyInterface.interfaceClass.ID.toString()+" ");
    }
    void out() {
        //выводим себя
        System.out.print("NODE - ");
        outClasses();
        System.out.println("");

        for(TableImplement child : childs) {
            System.out.print("children - ");
            child.outClasses();
            System.out.println();
        }

        for(TableImplement parent : parents) {
            System.out.print("parents - ");
            parent.outClasses();
            System.out.println();
        }

        for(TableImplement parent : parents) parent.out();
    }
}
