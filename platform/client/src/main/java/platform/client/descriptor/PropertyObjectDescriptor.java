package platform.client.descriptor;

import platform.client.descriptor.property.PropertyDescriptor;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PropertyObjectDescriptor extends IdentityDescriptor implements OrderDescriptor, ClientIdentitySerializable {
    private PropertyDescriptor property;
    private Map<PropertyInterfaceDescriptor, PropertyObjectInterfaceDescriptor> mapping;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, property);

        outStream.writeInt(mapping.size());
        for (Map.Entry<PropertyInterfaceDescriptor, PropertyObjectInterfaceDescriptor> entry : mapping.entrySet()) {
            pool.serializeObject(outStream, entry.getKey());
            pool.serializeObject(outStream, entry.getValue());
        }
    }

    public void customDeserialize(ClientSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        property = (PropertyDescriptor) pool.deserializeObject(inStream);

        mapping = new HashMap<PropertyInterfaceDescriptor, PropertyObjectInterfaceDescriptor>();
        int size = inStream.readInt();
        for (int i = 0; i < size; ++i) {
            PropertyInterfaceDescriptor inter = (PropertyInterfaceDescriptor) pool.deserializeObject(inStream);
            PropertyObjectInterfaceDescriptor value = (PropertyObjectInterfaceDescriptor) pool.deserializeObject(inStream);

            mapping.put(inter, value);
        }
    }
}
