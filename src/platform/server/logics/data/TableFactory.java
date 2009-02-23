package platform.server.logics.data;

import platform.server.data.types.Type;
import platform.server.data.KeyField;
import platform.server.data.Table;
import platform.server.data.PropertyField;
import platform.server.view.form.ViewTable;
import platform.server.logics.session.*;
import platform.server.logics.properties.DataPropertyInterface;

import java.util.*;
import java.sql.SQLException;

public class TableFactory extends TableImplement{

    public ObjectTable objectTable;
    public IDTable idTable;
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

    int maxBeanObjects = 3;
    public int maxInterface = 5;

    public TableFactory() {
        objectTable = new ObjectTable();
        idTable = new IDTable();
        viewTables = new ArrayList<ViewTable>();

        addClassTable = new AddClassTable();
        removeClassTable = new RemoveClassTable();

        for(int i=1;i<= maxBeanObjects;i++)
            viewTables.add(new ViewTable(i));

        for(int i=0;i<maxInterface;i++)
            changeTables.add(new HashMap<Type, IncrementChangeTable>());

        for(int i=0;i<maxInterface;i++)
            dataChangeTables.add(new HashMap<Type, DataChangeTable>());
    }

    public void includeIntoGraph(TableImplement IncludeItem) {
        Set<TableImplement> checks = new HashSet<TableImplement>();
        recIncludeIntoGraph(IncludeItem,true,checks);
    }

    public void fillDB(DataSession session, boolean createTable) throws SQLException {
        Set<TableImplement> tableImplements = new HashSet<TableImplement>();
        fillSet(tableImplements);

        for(TableImplement node : tableImplements) {
            node.table = new Table("table"+node.getID());
            node.mapFields = new HashMap<DataPropertyInterface, KeyField>();
            Integer fieldNum = 0;
            for(DataPropertyInterface propertyInterface : node) {
                fieldNum++;
                KeyField field = new KeyField("key"+fieldNum.toString(),Type.object);
                node.table.keys.add(field);
                node.mapFields.put(propertyInterface,field);
            }
        }

        if (createTable) {

            session.createTable(objectTable);
            session.createTable(idTable);

            for (Integer idType : IDTable.getCounters()) {
                // закинем одну запись
                Map<KeyField,Integer> insertKeys = new HashMap<KeyField,Integer>();
                insertKeys.put(idTable.key, idType);
                Map<PropertyField,Object> insertProps = new HashMap<PropertyField,Object>();
                insertProps.put(idTable.value,0);
                session.insertRecord(idTable,insertKeys,insertProps);
            }
        }

    }

    // заполняет временные таблицы
    public void fillSession(DataSession session) throws SQLException {

        session.createTemporaryTable(addClassTable);
        session.createTemporaryTable(removeClassTable);

        for(Map<Type, IncrementChangeTable> mapTables : changeTables)
            for(ChangeObjectTable changeTable : mapTables.values())
                session.createTemporaryTable(changeTable);
        for(Map<Type, DataChangeTable> mapTables : dataChangeTables)
            for(ChangeObjectTable dataChangeTable : mapTables.values())
                session.createTemporaryTable(dataChangeTable);

        for(ViewTable ViewTable : viewTables)
            session.createTemporaryTable(ViewTable);
    }

    // счетчик сессий (пока так потом надо из базы или как-то по другому транзакционность сделать
    public void clearSession(DataSession Session) throws SQLException {

        Session.deleteKeyRecords(addClassTable,new HashMap<KeyField, Integer>());
        Session.deleteKeyRecords(removeClassTable,new HashMap<KeyField, Integer>());

        for(Map<Type, IncrementChangeTable> mapTables : changeTables)
            for(ChangeObjectTable changeTable : mapTables.values())
                Session.deleteKeyRecords(changeTable,new HashMap<KeyField, Integer>());
        for(Map<Type, DataChangeTable> mapTables : dataChangeTables)
            for(ChangeObjectTable dataChangeTable : mapTables.values())
                Session.deleteKeyRecords(dataChangeTable,new HashMap<KeyField, Integer>());
    }
}
