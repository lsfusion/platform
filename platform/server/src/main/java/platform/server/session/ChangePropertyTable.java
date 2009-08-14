package platform.server.session;

import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.SessionTable;
import platform.server.data.classes.where.ClassWhere;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;

import java.util.HashMap;
import java.util.Map;

public abstract class ChangePropertyTable<P extends PropertyInterface,This extends ChangePropertyTable<P,This>> extends SessionTable<This> {

    public final Map<KeyField,P> mapKeys;
    public final PropertyField value;

    ChangePropertyTable(String iTablePrefix, Property<P> property) {
        super(iTablePrefix+"changetable_"+property.sID);

        mapKeys = new HashMap<KeyField, P>(); 
        for(P propertyInterface : property.interfaces) {
            KeyField objKeyField = new KeyField("object"+propertyInterface.ID, property.getInterfaceType(propertyInterface));
            mapKeys.put(objKeyField,propertyInterface);
            keys.add(objKeyField);
        }

        value = new PropertyField("value",property.getType());
        properties.add(value);
    }

    protected ChangePropertyTable(String iName, Map<KeyField, P> iMapKeys, PropertyField iValue, ClassWhere<KeyField> iClasses, Map<PropertyField, ClassWhere<Field>> iPropertyClasses) {
        super(iName, iClasses, iPropertyClasses);

        mapKeys = iMapKeys;
        keys.addAll(mapKeys.keySet());

        value = iValue;
        properties.add(value);
    }
}
