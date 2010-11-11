package platform.server.form.instance.filter;

import platform.server.form.instance.FormInstance;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public abstract class PropertyFilterInstance<P extends PropertyInterface> extends FilterInstance {

    public PropertyObjectInstance<P> property;

    public PropertyFilterInstance(PropertyObjectInstance<P> iProperty) {
        property = iProperty;
    }

    public PropertyFilterInstance(DataInputStream inStream, FormInstance form) throws IOException {
        super(inStream,form);
        property = ((PropertyDrawInstance<P>)form.getPropertyDraw(inStream.readInt())).propertyObject;
    }

    public GroupObjectInstance getApplyObject() {
        return property.getApplyObject();
    }

    public boolean classUpdated(GroupObjectInstance classGroup) {
        return property.classUpdated(classGroup);
    }

    public boolean objectUpdated(Set<GroupObjectInstance> skipGroups) {
        return property.objectUpdated(skipGroups);
    }

    public boolean dataUpdated(Collection<Property> changedProps) {
        return property.dataUpdated(changedProps);
    }

    public void fillProperties(Set<Property> properties) {
        property.fillProperties(properties);
    }

    public boolean isInInterface(GroupObjectInstance classGroup) {
        return property.isInInterface(classGroup);
    }
}
