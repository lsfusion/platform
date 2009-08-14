package platform.server.view.navigator.filter;

import platform.server.view.form.filter.OrFilter;
import platform.server.view.form.filter.Filter;
import platform.server.view.navigator.ObjectNavigator;

import java.sql.SQLException;
import java.util.Set;

public class OrFilterNavigator extends FilterNavigator {

    FilterNavigator op1;
    FilterNavigator op2;

    public OrFilterNavigator(FilterNavigator op1, FilterNavigator op2) {
        this.op1 = op1;
        this.op2 = op2;
    }

    public Filter doMapping(Mapper mapper) throws SQLException {
        return new OrFilter(op1.doMapping(mapper),op2.doMapping(mapper));
    }

    protected void fillObjects(Set<ObjectNavigator> objects) {
        op1.fillObjects(objects);
        op2.fillObjects(objects);
    }
}
