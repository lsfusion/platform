package platform.server.logics.data;

import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.Table;
import platform.server.data.GlobalTable;
import platform.server.data.types.Type;
import platform.server.session.*;
import platform.server.logics.classes.RemoteClass;
import platform.server.view.form.ViewTable;

import java.sql.SQLException;
import java.util.*;

public class TableFactory {

    public static final int MAX_INTERFACE = 5;
    ImplementTable[] baseTables = new ImplementTable[MAX_INTERFACE];

    public ObjectTable objectTable;
    public IDTable idTable;
    public GlobalTable globalTable;
    public List<ViewTable> viewTables;
    List<Map<Type,DataChangeTable>> dataChangeTables = new ArrayList<Map<Type, DataChangeTable>>();
    List<Map<Type,IncrementChangeTable>> changeTables = new ArrayList<Map<Type, IncrementChangeTable>>();

    public AddClassTable addClassTable;
    public RemoveClassTable removeClassTable;

    // для отладки
    public boolean reCalculateAggr = false;
    boolean crash = false;

    public IncrementChangeTable getChangeTable(Integer objects, Type dbType) {
        Map<Type,IncrementChangeTable> objChangeTables = changeTables.get(objects-1);
        IncrementChangeTable table = objChangeTables.get(dbType);
        if(table==null) {
            table = new IncrementChangeTable(objects,dbType);
            objChangeTables.put(dbType,table);
        }
        return table;
    }

    public DataChangeTable getDataChangeTable(Integer objects, Type dbType) {
        Map<Type,DataChangeTable> objChangeTables = dataChangeTables.get(objects-1);
        DataChangeTable table = objChangeTables.get(dbType);
        if(table==null) {
            table = new DataChangeTable(objects,dbType);
            objChangeTables.put(dbType,table);
        }
        return table;
    }

    private final static int MAX_BEAN_OBJECTS = 3;

    public TableFactory() {
        objectTable = new ObjectTable();
        idTable = new IDTable();
        globalTable = new GlobalTable();
        viewTables = new ArrayList<ViewTable>();

        addClassTable = new AddClassTable();
        removeClassTable = new RemoveClassTable();

        for(int i=1;i<= MAX_BEAN_OBJECTS;i++)
            viewTables.add(new ViewTable(i));

        for(int i=0;i< MAX_INTERFACE;i++)
            changeTables.add(new HashMap<Type, IncrementChangeTable>());

        for(int i=0;i< MAX_INTERFACE;i++)
            dataChangeTables.add(new HashMap<Type, DataChangeTable>());

        for(int i=0;i< MAX_INTERFACE;i++) {
            RemoteClass[] baseClasses = new RemoteClass[i];
            for(int j=0;j<i;j++)
                baseClasses[j] = RemoteClass.base;
            baseTables[i] = new ImplementTable("base_"+i,baseClasses);
        }
    }

    public void include(RemoteClass... classes) {
        String name = "table";
        for (RemoteClass fieldClass : classes)
            name += "_" + fieldClass.ID.toString();
        include(name,classes);
    }

    public void include(String name,RemoteClass... classes) {
        baseTables[classes.length].includeIntoGraph(new ImplementTable(name,classes),true,new HashSet<ImplementTable>());
    }

    // получает постоянные таблицы
    public Map<String,ImplementTable> getImplementTables() {
        Map<String,ImplementTable> result = new HashMap<String, ImplementTable>();
        for(int i=0;i<MAX_INTERFACE;i++) {
            Set<ImplementTable> intTables = new HashSet<ImplementTable>();
            baseTables[i].fillSet(intTables);
            for(ImplementTable intTable : intTables)
                result.put(intTable.name,intTable);
        }
        return result;
    }

    public <T> MapKeysTable<T> getMapTable(Map<T,RemoteClass> findItem) {
        return baseTables[findItem.size()].getMapTable(findItem);
    }

    public void fillDB(DataSession session) throws SQLException {

        session.ensureTable(objectTable);
        session.ensureTable(idTable);
        session.ensureTable(globalTable);

        for (Integer idType : IDTable.getCounters())
            session.ensureRecord(idTable,Collections.singletonMap(idTable.key,idType), Collections.singletonMap(idTable.value,(Object)0));

        // создадим dumb
        Table dumbTable = new Table("dumb");
        KeyField dumbKey = new KeyField("dumb", Type.system);
        dumbTable.keys.add(dumbKey);
        session.ensureTable(dumbTable);
        session.ensureRecord(dumbTable,Collections.singletonMap(dumbKey,1), new HashMap<PropertyField, Object>());

        Table emptyTable = new Table("empty");
        emptyTable.keys.add(new KeyField("dumb",Type.system));
        session.ensureTable(emptyTable);
    }

    // счетчик сессий (пока так потом надо из базы или как-то по другому транзакционность сделать
    public void clearSession(DataSession session) throws SQLException {

        session.deleteKeyRecords(addClassTable,new HashMap<KeyField, Integer>());
        session.deleteKeyRecords(removeClassTable,new HashMap<KeyField, Integer>());

        for(Map<Type, IncrementChangeTable> mapTables : changeTables)
            for(ChangeObjectTable changeTable : mapTables.values())
                session.deleteKeyRecords(changeTable,new HashMap<KeyField, Integer>());
        for(Map<Type, DataChangeTable> mapTables : dataChangeTables)
            for(ChangeObjectTable dataChangeTable : mapTables.values())
                session.deleteKeyRecords(dataChangeTable,new HashMap<KeyField, Integer>());
    }
}
