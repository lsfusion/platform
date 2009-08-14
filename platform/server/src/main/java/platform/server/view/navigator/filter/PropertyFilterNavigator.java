package platform.server.view.navigator.filter;

import platform.server.view.navigator.PropertyObjectNavigator;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.view.form.filter.Filter;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.logics.properties.PropertyInterface;

import java.util.Set;
import java.sql.SQLException;

public abstract class PropertyFilterNavigator<P extends PropertyInterface> extends FilterNavigator {

    public PropertyObjectNavigator<P> property;

    public PropertyFilterNavigator(PropertyObjectNavigator<P> iProperty) {
        property = iProperty;
    }

    protected abstract Filter doMapping(PropertyObjectImplement<P> propertyImplement, Mapper mapper) throws SQLException;

    public Filter doMapping(Mapper mapper) throws SQLException {
        return doMapping(mapper.mapProperty(property),mapper);
    }

    protected void fillObjects(Set<ObjectNavigator> objects) {
        property.fillObjects(objects);
    }
}
