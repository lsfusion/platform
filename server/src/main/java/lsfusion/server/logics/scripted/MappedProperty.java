package lsfusion.server.logics.scripted;

import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyObjectInterfaceEntity;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyInterface;

public final class MappedProperty {
    public LP<PropertyInterface, ?> property;
    public ObjectEntity[] mapping;

    public MappedProperty(LP property, ObjectEntity[] mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public Property<PropertyInterface> getProperty() {
        return property.property;
    }
}
