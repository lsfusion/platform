package platform.server.view.navigator.filter;

import platform.server.logics.property.PropertyInterface;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.filter.Filter;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.view.navigator.PropertyObjectNavigator;
import platform.server.view.navigator.Mapper;

import java.sql.SQLException;
import java.util.Set;

public abstract class PropertyFilterNavigator<P extends PropertyInterface> extends FilterNavigator {

    public PropertyObjectNavigator<P> property;

    public PropertyFilterNavigator(PropertyObjectNavigator<P> iProperty) {
        property = iProperty;
    }

    protected abstract Filter doMapping(PropertyObjectImplement<P> propertyImplement, Mapper mapper) throws SQLException;

    public Filter doMapping(Mapper mapper) throws SQLException {
        return doMapping(mapper.mapControl(property),mapper);
    }

    protected void fillObjects(Set<ObjectNavigator> objects) {
        property.fillObjects(objects);
    }
}
