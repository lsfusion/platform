package platform.server.view.navigator.filter;

import platform.server.logics.properties.PropertyInterface;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.filter.Filter;
import platform.server.view.form.filter.NotNullFilter;
import platform.server.view.navigator.PropertyObjectNavigator;

public class NotNullFilterNavigator<P extends PropertyInterface> extends PropertyFilterNavigator<P> {

    public NotNullFilterNavigator(PropertyObjectNavigator<P> iProperty) {
        super(iProperty);
    }

    protected Filter doMapping(PropertyObjectImplement<P> propertyImplement, Mapper mapper) {
        return new NotNullFilter<P>(propertyImplement);
    }
}
