package platform.server.logics.data;

import platform.server.data.GlobalTable;
import platform.server.data.PropertyField;
import platform.server.data.classes.CustomClass;
import platform.server.data.classes.SystemClass;
import platform.server.data.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class TableFactory {

    public static final int MAX_INTERFACE = 5;
    ImplementTable[] baseTables = new ImplementTable[MAX_INTERFACE];

    private final static int MAX_BEAN_OBJECTS = 3;

    public TableFactory(CustomClass customClass) {
        for(int i=0;i< MAX_INTERFACE;i++) {
            CustomClass[] baseClasses = new CustomClass[i];
            for(int j=0;j<i;j++)
                baseClasses[j] = customClass;
            baseTables[i] = new ImplementTable("base_"+i,baseClasses);
        }
    }

    public void include(ValueClass... classes) {
        String name = "table";
        for (ValueClass fieldClass : classes)
            name += "_" + ((CustomClass)fieldClass).ID.toString();
        include(name,classes);
    }

    public void include(String name, ValueClass... classes) {
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

    public <T> MapKeysTable<T> getMapTable(Map<T, ValueClass> findItem) {
        return baseTables[findItem.size()].getMapTable(findItem);
    }

    public void fillDB(DataSession session) throws SQLException {

        session.ensureTable(session.baseClass.table);
        session.ensureTable(IDTable.instance);
        session.ensureTable(GlobalTable.instance);

        for (Integer idType : IDTable.getCounters())
            session.ensureRecord(IDTable.instance,Collections.singletonMap(IDTable.instance.key,new DataObject(idType, SystemClass.instance)),
                    Collections.singletonMap(IDTable.instance.value,(ObjectValue)new DataObject(0, SystemClass.instance)));

        // создадим dumb
        session.ensureTable(DumbTable.instance);
        session.ensureRecord(DumbTable.instance,Collections.singletonMap(DumbTable.instance.key,new DataObject(1, SystemClass.instance)), new HashMap<PropertyField, ObjectValue>());

        session.ensureTable(EmptyTable.instance);
    }
}
