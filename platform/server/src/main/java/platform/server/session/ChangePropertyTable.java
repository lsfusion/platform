package platform.server.session;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.SessionTable;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.util.HashMap;
import java.util.Map;

public abstract class ChangePropertyTable<P extends PropertyInterface,This extends ChangePropertyTable<P,This>> extends SessionTable<This> {

    public final Map<KeyField,P> mapKeys;
    public final PropertyField value;

    ChangePropertyTable(String iTablePrefix, Property<P> property) {
        super(iTablePrefix+"changetable_"+property.sID);

        mapKeys = new HashMap<KeyField, P>(); 
        for(P propertyInterface : property.interfaces) {
            KeyField objKeyField = new KeyField("property"+propertyInterface.ID, property.getInterfaceType(propertyInterface));
            mapKeys.put(objKeyField,propertyInterface);
            keys.add(objKeyField);
        }

        value = new PropertyField("value",property.getType());
        properties.add(value);
    }

    protected ChangePropertyTable(String name, Map<KeyField, P> mapKeys, PropertyField value, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        super(name, classes, propertyClasses, rows);

        this.mapKeys = mapKeys;
        keys.addAll(this.mapKeys.keySet());

        this.value = value;
        properties.add(this.value);
    }
}
