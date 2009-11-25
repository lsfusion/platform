package platform.server.view.navigator.filter;

import platform.server.classes.CustomClass;
import platform.server.logics.property.PropertyInterface;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.filter.Filter;
import platform.server.view.form.filter.IsClassFilter;
import platform.server.view.navigator.PropertyObjectNavigator;
import platform.server.view.navigator.Mapper;

public class IsClassFilterNavigator<P extends PropertyInterface> extends PropertyFilterNavigator<P> {

    CustomClass isClass;

    public IsClassFilterNavigator(PropertyObjectNavigator<P> iProperty, CustomClass isClass) {
        super(iProperty);
        this.isClass = isClass;
    }

    protected Filter doMapping(PropertyObjectImplement<P> propertyImplement, Mapper mapper) {
        return new IsClassFilter<P>(propertyImplement,isClass);
    }
}
