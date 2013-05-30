package platform.server.logics.scripted;

import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

public final class MappedProperty {
    public LP<PropertyInterface, ?> property;
    public PropertyObjectInterfaceEntity[] mapping;

    public MappedProperty(LP property, PropertyObjectInterfaceEntity[] mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    public Property<PropertyInterface> getProperty() {
        return property.property;
    }
}
