package platform.server.form.instance;

import platform.server.caches.IdentityLazy;
import platform.server.logics.DataObject;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;

import java.util.Map;

public class ActionPropertyObjectInstance extends PropertyObjectInstance<ClassPropertyInterface, ActionProperty> {

    public ActionPropertyObjectInstance(ActionProperty property, Map<ClassPropertyInterface, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

    @IdentityLazy
    public ActionPropertyObjectInstance getRemappedPropertyObject(Map<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return new ActionPropertyObjectInstance(property, remap(mapKeyValues));
    }
}
