package platform.client.descriptor.filter;

import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OrFilterDescriptor extends FilterDescriptor {
    public FilterDescriptor op1;
    public FilterDescriptor op2;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, op1);
        pool.serializeObject(outStream, op2);
    }

    public void customDeserialize(ClientSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        op1 = (FilterDescriptor) pool.deserializeObject(inStream);
        op2 = (FilterDescriptor) pool.deserializeObject(inStream);
    }
}
