package platform.client.descriptor;

import platform.client.descriptor.property.PropertyDescriptor;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PropertyObjectDescriptor extends IdentityDescriptor implements OrderDescriptor, IdentitySerializable {
    private PropertyDescriptor property;
    private Map<PropertyInterfaceDescriptor, PropertyObjectInterfaceDescriptor> mapping;

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:

    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
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
