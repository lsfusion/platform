package platform.client.descriptor.filter;

import platform.client.descriptor.PropertyObjectDescriptor;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PropertyFilterDescriptor extends FilterDescriptor {
    private PropertyObjectDescriptor property;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, property);
    }

    public void customDeserialize(ClientSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        property = (PropertyObjectDescriptor) pool.deserializeObject(inStream);
    }
}
