package platform.server.session;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.SessionTable;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.table.ImplementTable;
import platform.server.logics.property.Property;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class IncrementChangeTable extends SessionTable<IncrementChangeTable> {

    public IncrementChangeTable(String name, ImplementTable table, Map<KeyField, KeyField> mapKeys, Map<Property,PropertyField> changes, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        super(name, classes, propertyClasses, rows);
        this.table = table;
        this.mapKeys = mapKeys;
        this.changes = changes;

        this.keys.addAll(mapKeys.values());
        this.properties.addAll(changes.values());
    }

    public IncrementChangeTable createThis(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        return new IncrementChangeTable(name, table, mapKeys, changes, classes, propertyClasses, rows);
    }

    public final ImplementTable table;
    public final Map<KeyField,KeyField> mapKeys; // map ключей table на свои
    public final Map<Property,PropertyField> changes;

    private static String getName(Collection<Property> properties) {
        String name = "";                   
        for(Property property : properties)
            name = (name.length()==0?"":name+"_") + property.ID;
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
            PropertyField field = new PropertyField("p"+property.ID, property.getType());
            changes.put(property, field);
            properties.add(field);
        }
    }
}
