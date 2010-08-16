package platform.server.form.entity.filter;

import platform.server.classes.CustomClass;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.instance.Mapper;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.IsClassFilterInstance;
import platform.server.logics.property.PropertyInterface;
import platform.server.form.instance.PropertyObjectInstance;

public class IsClassFilterEntity<P extends PropertyInterface> extends PropertyFilterEntity<P> {

    CustomClass isClass;

    public IsClassFilterEntity(PropertyObjectEntity<P> iProperty, CustomClass isClass) {
        super(iProperty);
        this.isClass = isClass;
    }

    protected FilterInstance doMapping(PropertyObjectInstance<P> propertyImplement, Mapper mapper) {
        return new IsClassFilterInstance<P>(propertyImplement,isClass);
    }
}
