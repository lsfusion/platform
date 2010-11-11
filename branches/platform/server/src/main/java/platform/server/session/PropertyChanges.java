package platform.server.session;

import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

public class PropertyChanges extends AbstractPropertyChanges<PropertyInterface, Property<PropertyInterface>, PropertyChanges> {

    protected PropertyChanges createThis() {
        return new PropertyChanges(); 
    }

    public PropertyChanges() {
    }

    public PropertyChanges(PropertyChange<PropertyInterface> change, Property property) {
        super(property, change);
    }

    public <T extends PropertyInterface> PropertyChanges(Property<T> property, PropertyChange<T> change) {
        this((PropertyChange<PropertyInterface>) change, property);
    }

    public PropertyChanges(PropertyChanges changes1, PropertyChanges changes2) {
        super(changes1, changes2);
    }

    public PropertyChanges(PropertyChanges changes1, DataChanges changes2) {
        super(changes1, changes2);
    }

    public PropertyChanges add(PropertyChanges add) {
        return new PropertyChanges(this, add);
    }
}
