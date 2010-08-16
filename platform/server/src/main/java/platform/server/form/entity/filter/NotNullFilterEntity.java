package platform.server.form.entity.filter;

import platform.server.logics.property.PropertyInterface;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.NotNullFilterInstance;
import platform.server.form.instance.Mapper;
import platform.server.form.entity.PropertyObjectEntity;

public class NotNullFilterEntity<P extends PropertyInterface> extends PropertyFilterEntity<P> {

    public NotNullFilterEntity(PropertyObjectEntity<P> iProperty) {
        super(iProperty);
    }

    protected FilterInstance doMapping(PropertyObjectInstance<P> propertyImplement, Mapper mapper) {
        return new NotNullFilterInstance<P>(propertyImplement);
    }
}
