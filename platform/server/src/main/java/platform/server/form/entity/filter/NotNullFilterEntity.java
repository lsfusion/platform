package platform.server.form.entity.filter;

import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.logics.property.PropertyInterface;

public class NotNullFilterEntity<P extends PropertyInterface> extends PropertyFilterEntity<P> {

    public boolean checkChange = false;

    public NotNullFilterEntity(PropertyObjectEntity<P> property) {
        super(property);
    }

    public NotNullFilterEntity(PropertyObjectEntity<P> property, boolean checkChange) {
        super(property);
        this.checkChange = checkChange;
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    @Override
    public FilterEntity getRemappedFilter(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
        return new NotNullFilterEntity<P>(property.getRemappedEntity(oldObject, newObject, instanceFactory), checkChange);
    }
}
