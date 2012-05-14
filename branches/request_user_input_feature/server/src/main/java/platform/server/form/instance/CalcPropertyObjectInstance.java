package platform.server.form.instance;

import platform.server.caches.IdentityLazy;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;

import java.util.Map;

public class CalcPropertyObjectInstance<P extends PropertyInterface> extends PropertyObjectInstance<P, CalcProperty<P>> implements OrderInstance {

    public CalcPropertyObjectInstance(CalcProperty<P> property, Map<P, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

    @IdentityLazy
    public CalcPropertyObjectInstance<P> getRemappedPropertyObject(Map<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return new CalcPropertyObjectInstance<P>(property, remap(mapKeyValues));
    }
}
