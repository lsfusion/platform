package platform.server.view.navigator.filter;

import platform.server.data.types.Type;
import platform.server.logics.ObjectValue;
import platform.server.logics.properties.PropertyInterface;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.filter.Filter;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.view.navigator.PropertyObjectNavigator;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public abstract class FilterNavigator {

    public interface Mapper {
        <P extends PropertyInterface> PropertyObjectImplement<P> mapProperty(PropertyObjectNavigator<P> navigator);
        ObjectImplement mapObject(ObjectNavigator navigator);
        ObjectValue mapValue(Object value, Type type) throws SQLException;
    }

    public abstract Filter doMapping(Mapper mapper) throws SQLException;

    protected abstract void fillObjects(Set<ObjectNavigator> objects);
    
    public Set<ObjectNavigator> getObjects() {
        Set<ObjectNavigator> objects = new HashSet<ObjectNavigator>();
        fillObjects(objects);
        return objects;
    }
}
