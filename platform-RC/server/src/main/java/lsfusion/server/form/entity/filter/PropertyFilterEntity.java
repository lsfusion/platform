package lsfusion.server.form.entity.filter;

import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.logics.property.PropertyInterface;

import java.util.Set;

public abstract class PropertyFilterEntity<P extends PropertyInterface> extends FilterEntity {

    public CalcPropertyObjectEntity<P> property;
    public boolean resolveAdd;

    // нельзя удалять - используется при сериализации
    protected PropertyFilterEntity() {
    }

    public PropertyFilterEntity(CalcPropertyObjectEntity<P> property, boolean resolveAdd) {
        this.property = property;
        this.resolveAdd = resolveAdd;
    }

    protected void fillObjects(Set<ObjectEntity> objects) {
        property.fillObjects(objects);
    }
}
