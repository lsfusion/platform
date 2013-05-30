package platform.client.descriptor.filter;

import platform.client.ClientResourceBundle;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.nodes.filters.FilterNode;
import platform.client.descriptor.nodes.filters.IsClassFilterNode;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;
import platform.client.logics.classes.ClientObjectClass;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class IsClassFilterDescriptor extends PropertyFilterDescriptor {

    private ClientObjectClass objectClass;
    public void setObjectClass(ClientObjectClass objectClass) {
        this.objectClass = objectClass;

        updateDependency(this, "objectClass");
    }
    public ClientObjectClass getObjectClass() {
        return objectClass;
    }

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        if (property == null) return null;
        return property.getGroupObject(groupList);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);
        
        outStream.writeInt(objectClass.getID());
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        objectClass = (ClientObjectClass) ClientTypeSerializer.deserializeClientClass(inStream);
    }

    @Override
    public FilterNode createNode(Object group) {
        return new IsClassFilterNode((GroupObjectDescriptor) group, this);
    }

    @Override
    public String toString() {
        String result = "";
        if (property != null)
            result += property;
        if (objectClass != null)
            result += " - "+ ClientResourceBundle.getString("descriptor.filters.of.class")+" " + objectClass;
        if (result.isEmpty()) result = ClientResourceBundle.getString("descriptor.filters.class");
        return result;
    }

}
