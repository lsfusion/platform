package platform.server.view.navigator;

import platform.server.logics.linear.properties.LP;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyImplement;
import platform.server.logics.properties.PropertyInterface;
import platform.server.view.form.filter.CompareValue;
import platform.server.view.navigator.filter.CompareValueNavigator;
import platform.server.view.navigator.filter.FilterNavigator;

import java.sql.SQLException;
import java.util.Set;
import java.util.Map;

public class PropertyObjectNavigator<P extends PropertyInterface> extends PropertyImplement<ObjectNavigator,P> implements CompareValueNavigator {

    public PropertyObjectNavigator(LP<P, Property<P>> property,ObjectNavigator... objects) {
        super(property.property);
        
        for(int i=0;i<property.listInterfaces.size();i++)
            mapping.put(property.listInterfaces.get(i),objects[i]);
    }

    public PropertyObjectNavigator(Property<P> property, Map<P, ObjectNavigator> mapping) {
        super(property, mapping);
    }

    public void fillObjects(Set<ObjectNavigator> objects) {
        objects.addAll(mapping.values());
    }

    public CompareValue doMapping(FilterNavigator.Mapper mapper) throws SQLException {
        return mapper.mapProperty(this);
    }
}
