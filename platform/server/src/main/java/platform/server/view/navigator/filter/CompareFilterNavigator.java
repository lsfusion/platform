package platform.server.view.navigator.filter;

import platform.interop.Compare;
import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.DataObject;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.filter.CompareFilter;
import platform.server.view.form.filter.Filter;
import platform.server.view.navigator.ObjectNavigator;
import platform.server.view.navigator.PropertyObjectNavigator;
import platform.server.view.navigator.Mapper;
import platform.server.data.classes.DataClass;

import java.sql.SQLException;
import java.util.Set;

public class CompareFilterNavigator<P extends PropertyInterface> extends PropertyFilterNavigator<P> {

    public Compare compare;

    public CompareValueNavigator value;

    public CompareFilterNavigator(PropertyObjectNavigator<P> iProperty, Compare iCompare, CompareValueNavigator iValue) {
        super(iProperty);
        value = iValue;
        compare = iCompare;
    }

    public CompareFilterNavigator(PropertyObjectNavigator<P> iProperty, Compare iCompare, Object iValue) {
        this(iProperty,iCompare,new DataObject(iValue,(DataClass)iProperty.property.getType()));
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
