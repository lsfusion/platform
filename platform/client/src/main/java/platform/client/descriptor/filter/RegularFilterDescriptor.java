package platform.client.descriptor.filter;

import platform.client.logics.ClientRegularFilter;
import platform.client.serialization.ClientCustomSerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegularFilterDescriptor implements ClientCustomSerializable {

    private FilterDescriptor filter;

    ClientRegularFilter client;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, filter);
    }

    public void customDeserialize(ClientSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        filter = (FilterDescriptor) pool.deserializeObject(inStream);
        client = pool.context.getRegularFilter(ID);
    }
}
