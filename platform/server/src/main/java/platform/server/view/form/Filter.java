package platform.server.view.form;

import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DefaultData;
import platform.server.data.query.JoinQuery;
import platform.server.session.TableChanges;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.sql.SQLException;

public abstract class Filter<P extends PropertyInterface> {

    public PropertyObjectImplement<P> property;

    public Filter(PropertyObjectImplement<P> iProperty) {
        property = iProperty;
    }

    public Filter(DataInputStream inStream, RemoteForm form) throws IOException {
        property = form.getPropertyView(inStream.readInt()).view;
    }

    public GroupObjectImplement getApplyObject() {
        return property.getApplyObject();
    }

    boolean isInInterface(GroupObjectImplement classGroup) {
        return true; // пока будем считать что нету в интерфейсе и включать тоже не будем
        /* ClassSet valueClass = value.getValueClass(classGroup);
        if(valueClass==null)
            return property.isInInterface(classGroup);
        else
            return property.getValueClass(classGroup).intersect(valueClass); */
    }

    boolean classUpdated(GroupObjectImplement classGroup) {
        return property.classUpdated(classGroup);
    }

    boolean objectUpdated(GroupObjectImplement classGroup) {
        return property.objectUpdated(classGroup);
    }

    boolean dataUpdated(Collection<Property> changedProps) {
        return property.dataUpdated(changedProps);
    }

    public abstract void fillSelect(JoinQuery<ObjectImplement, ?> query, Set<GroupObjectImplement> classGroup, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) throws SQLException;

    protected void fillProperties(Collection<Property> properties) {
        property.fillProperties(properties);
    }

    public Collection<Property> getProperties() {
        Collection<Property> properties = new HashSet<Property>();
        fillProperties(properties);
        return properties;
    }

}
