package platform.server.form.entity.filter;

import platform.server.form.entity.CalcPropertyObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.logics.property.PropertyInterface;

public class NotNullFilterEntity<P extends PropertyInterface> extends PropertyFilterEntity<P> {

    public boolean checkChange;

    // нельзя удалять - используется при сериализации
    public NotNullFilterEntity() {
    }

    public NotNullFilterEntity(CalcPropertyObjectEntity<P> property) {
        this(property, false, false);
    }

    public NotNullFilterEntity(CalcPropertyObjectEntity<P> property, boolean resolveAdd) {
        this(property, false, resolveAdd);
    }

    public NotNullFilterEntity(CalcPropertyObjectEntity<P> property, boolean checkChange, boolean resolveAdd) {
        super(property, resolveAdd);
        this.checkChange = checkChange;
    }

    public FilterInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    @Override
    public FilterEntity getRemappedFilter(ObjectEntity oldObject, ObjectEntity newObject, InstanceFactory instanceFactory) {
        return new NotNullFilterEntity<P>(property.getRemappedEntity(oldObject, newObject, instanceFactory), checkChange, resolveAdd);
    }
}
