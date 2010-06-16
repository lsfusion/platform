package platform.server.view.navigator.filter;

import platform.server.view.form.filter.Filter;
import platform.server.view.form.filter.NotFilter;
import platform.server.view.navigator.Mapper;
import platform.server.view.navigator.ObjectNavigator;

import java.sql.SQLException;
import java.util.Set;

public class NotFilterNavigator extends FilterNavigator {

    FilterNavigator filter;

    public NotFilterNavigator(FilterNavigator filter) {
        this.filter = filter;
    }

    public Filter doMapping(Mapper mapper) throws SQLException {
        return new NotFilter(filter.doMapping(mapper));
    }

    protected void fillObjects(Set<ObjectNavigator> objects) {
        filter.fillObjects(objects);
    }
}
