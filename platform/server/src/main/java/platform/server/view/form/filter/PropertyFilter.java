package platform.server.view.form.filter;

import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.RemoteForm;
import platform.server.view.form.GroupObjectImplement;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public abstract class PropertyFilter<P extends PropertyInterface> extends Filter {

    public PropertyObjectImplement<P> property;

    public PropertyFilter(PropertyObjectImplement<P> iProperty) {
        property = iProperty;
    }

    public PropertyFilter(DataInputStream inStream, RemoteForm form) throws IOException {
        super(inStream,form);
        property = form.getPropertyView(inStream.readInt()).view;
    }

    public GroupObjectImplement getApplyObject() {
        return property.getApplyObject();
    }

    public boolean classUpdated(GroupObjectImplement classGroup) {
        return property.classUpdated(classGroup);
    }

    public boolean objectUpdated(GroupObjectImplement classGroup) {
        return property.objectUpdated(classGroup);
    }

    public boolean dataUpdated(Collection<Property> changedProps) {
        return property.dataUpdated(changedProps);
    }

    public void fillProperties(Set<Property> properties) {
        property.fillProperties(properties);
    }

    public boolean isInInterface(GroupObjectImplement classGroup) {
        return property.isInInterface(classGroup);
    }
}
