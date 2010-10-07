package platform.client.descriptor.filter;

import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class IsClassFilterDescriptor extends PropertyFilterDescriptor {
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);
    }

    public void customDeserialize(ClientSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, ID, inStream);
    }
}
