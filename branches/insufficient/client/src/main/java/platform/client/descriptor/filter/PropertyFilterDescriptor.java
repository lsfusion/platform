package platform.client.descriptor.filter;

import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.PropertyObjectDescriptor;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public abstract class PropertyFilterDescriptor extends FilterDescriptor {

    protected PropertyObjectDescriptor property;

    public PropertyObjectDescriptor getProperty() {
        return property;
    }

    public void setProperty(PropertyObjectDescriptor property) {
        this.property = property;
        updateDependency(this, "property");
    }

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        if (property == null) return null;
        return property.getGroupObject(groupList);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, property);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        property = (PropertyObjectDescriptor) pool.deserializeObject(inStream);
    }
}
