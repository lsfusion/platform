package platform.server.session;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.SessionTable;
import platform.server.data.classes.where.ClassWhere;
import platform.server.logics.data.ImplementTable;
import platform.server.logics.properties.Property;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class IncrementChangeTable extends SessionTable<IncrementChangeTable> {
/*
    public IncrementChangeTable(String iName, Map<KeyField, P> iMapKeys, PropertyField iValue, ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        super(iName, iMapKeys, iValue, iClasses, iPropertyClasses);
    }

    public IncrementChangeTable<P> createThis(ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        return new IncrementChangeTable<P>(name, mapKeys, value, iClasses, iPropertyClasses);
    }*/

    public IncrementChangeTable createThis(ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        throw new RuntimeException("not supported");
    }

    public final ImplementTable table;
    public final Map<KeyField,KeyField> mapKeys; // map ключей table на свои
    public final Map<Property,PropertyField> changes;

    private static String getName(Collection<Property> properties) {
        String name = "";                   
        for(Property property : properties)
            name = (name.length()==0?"":name+"_") + property.sID;
        return "inc_ch_" + name;
    }

    public IncrementChangeTable(Collection<Property> incrementProps) {
        super(getName(incrementProps));

        table = incrementProps.iterator().next().mapTable.table; // первую таблицу берем, остальные assert'ся
        mapKeys = new HashMap<KeyField, KeyField>();
        for(KeyField implementKey : table.keys) {
            KeyField key = new KeyField(implementKey.name,implementKey.type); 
            keys.add(key);
            mapKeys.put(implementKey,key);
        }

        changes = new HashMap<Property, PropertyField>();
        for(Property property : incrementProps) {
            assert property.mapTable.table == table;
            PropertyField field = new PropertyField(property.sID, property.getType());
            changes.put(property, field);
            properties.add(field);
        }
    }

}
