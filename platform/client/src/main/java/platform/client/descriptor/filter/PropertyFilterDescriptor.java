package platform.client.descriptor.filter;

import platform.client.descriptor.PropertyObjectDescriptor;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class PropertyFilterDescriptor extends FilterDescriptor {
    public PropertyObjectDescriptor property;

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        return property.getGroupObject(groupList);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, property);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        property = (PropertyObjectDescriptor) pool.deserializeObject(inStream);
    }
}
