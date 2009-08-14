package platform.server.view.navigator.filter;

import platform.server.view.form.filter.CompareValue;
import platform.server.view.navigator.ObjectNavigator;

import java.sql.SQLException;
import java.util.Set;

public interface CompareValueNavigator {

    CompareValue doMapping(FilterNavigator.Mapper mapper) throws SQLException;

    void fillObjects(Set<ObjectNavigator> objects);
}
