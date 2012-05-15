package platform.server.logics.scripted;

import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.logics.linear.LP;

public final class MappedProperty {
    public LP property;
    public PropertyObjectInterfaceEntity[] mapping;

    public MappedProperty(LP property, PropertyObjectInterfaceEntity[] mapping) {
        this.property = property;
        this.mapping = mapping;
    }
}
