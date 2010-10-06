package platform.client.descriptor.filter;

import platform.client.descriptor.IdentityDescriptor;
import platform.client.logics.ClientRegularFilterGroup;
import platform.client.serialization.ClientCustomSerializable;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class RegularFilterGroupDescriptor extends IdentityDescriptor implements ClientIdentitySerializable {

    private List<RegularFilterDescriptor> filters;

    ClientRegularFilterGroup client;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, filters);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;
        filters = pool.deserializeList(inStream);
        client = pool.context.getRegularFilterGroup(ID);
    }
}
