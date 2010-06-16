package platform.server.view.navigator.filter;

import platform.server.view.form.filter.Filter;
import platform.server.view.navigator.Mapper;
import platform.server.view.navigator.ObjectNavigator;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public abstract class FilterNavigator {

    public abstract Filter doMapping(Mapper mapper) throws SQLException;

    protected abstract void fillObjects(Set<ObjectNavigator> objects);
    
    public Set<ObjectNavigator> getObjects() {
        Set<ObjectNavigator> objects = new HashSet<ObjectNavigator>();
        fillObjects(objects);
        return objects;
    }
}
