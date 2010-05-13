package platform.server.view.navigator;

import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyImplement;
import platform.server.view.form.filter.CompareValue;
import platform.server.view.navigator.filter.CompareValueNavigator;

import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Set;

public class PropertyObjectNavigator<P extends PropertyInterface> extends PropertyImplement<PropertyInterfaceNavigator,P> implements CompareValueNavigator {

    public Collection<ObjectNavigator> getObjectImplements() {
        Collection<ObjectNavigator> result = new ArrayList<ObjectNavigator>();
        for(PropertyInterfaceNavigator object : mapping.values())
            if(object instanceof ObjectNavigator)
                result.add((ObjectNavigator) object);
        return result;
    }

    public void fillObjects(Set<ObjectNavigator> objects) {
        objects.addAll(getObjectImplements());
    }

    public PropertyObjectNavigator(LP<P> property, PropertyInterfaceNavigator... objects) {
        super(property.property);

        for(int i=0;i<property.listInterfaces.size();i++)
            mapping.put(property.listInterfaces.get(i),objects[i]);
    }

    public PropertyObjectNavigator(Property<P> property, Map<P, PropertyInterfaceNavigator> mapping) {
        super(property, mapping);
    }

    public CompareValue doMapping(Mapper mapper) {
        return mapper.mapProperty(this);
    }
}
