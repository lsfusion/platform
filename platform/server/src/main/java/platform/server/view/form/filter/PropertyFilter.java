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

/**
 * Created by IntelliJ IDEA.
 * User: ME2
 * Date: 30.07.2009
 * Time: 16:17:40
 * To change this template use File | Settings | File Templates.
 */
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

    protected void fillProperties(Set<Property> properties) {
        property.fillProperties(properties);
    }
}
