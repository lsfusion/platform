package platform.client.descriptor.filter;

import platform.client.logics.ClientRegularFilterGroup;
import platform.client.serialization.ClientCustomSerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class RegularFilterGroupDescriptor implements ClientCustomSerializable {

    private List<RegularFilterDescriptor> filters;

    ClientRegularFilterGroup client;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, filters);
    }

    public void customDeserialize(ClientSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        filters = pool.deserializeList(inStream);
        client = pool.context.getRegularFilterGroup(ID);
    }
}
