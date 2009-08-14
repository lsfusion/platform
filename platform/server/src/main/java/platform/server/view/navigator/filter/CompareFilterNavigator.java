package platform.server.view.navigator.filter;

import platform.server.logics.properties.PropertyInterface;
import platform.server.view.navigator.PropertyObjectNavigator;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.view.form.filter.Filter;
import platform.server.view.form.filter.CompareFilter;
import platform.server.view.form.filter.CompareValue;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.data.types.Type;
import platform.interop.Compare;
import platform.base.BaseUtils;

import java.util.Set;
import java.sql.SQLException;

public class CompareFilterNavigator<P extends PropertyInterface> extends PropertyFilterNavigator<P> {

    public Compare compare;

    public CompareValueNavigator value;

    public CompareFilterNavigator(PropertyObjectNavigator<P> iProperty, Compare iCompare, CompareValueNavigator iValue) {
        super(iProperty);
        value = iValue;
        compare = iCompare;
    }

    private static class UserValue implements CompareValueNavigator {

        public UserValue(Object value, Type type) {
            this.value = value;
            this.type = type;

            assert BaseUtils.isData(value);
        }

        public final Object value;
        public final Type type;

        public CompareValue doMapping(Mapper mapper) throws SQLException {
            return mapper.mapValue(value,type);
        }

        public void fillObjects(Set<ObjectNavigator> objects) {
        }
    }
    
    public CompareFilterNavigator(PropertyObjectNavigator<P> iProperty, Compare iCompare, Object iValue) {
        this(iProperty,iCompare,new UserValue(iValue,iProperty.property.getType()));
    }

    protected Filter doMapping(PropertyObjectImplement<P> propertyImplement, Mapper mapper) throws SQLException {
        return new CompareFilter<P>(propertyImplement,compare,value.doMapping(mapper));
    }

    @Override
    protected void fillObjects(Set<ObjectNavigator> objects) {
        super.fillObjects(objects);
        value.fillObjects(objects);
    }
}
