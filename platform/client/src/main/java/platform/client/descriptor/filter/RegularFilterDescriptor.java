package platform.client.descriptor.filter;

import platform.client.descriptor.IdentityDescriptor;
import platform.client.logics.ClientRegularFilter;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class RegularFilterDescriptor extends IdentityDescriptor implements ClientIdentitySerializable {

    private FilterDescriptor filter;

    ClientRegularFilter client;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, filter);

        outStream.writeUTF(client.caption);
        new ObjectOutputStream(outStream).writeObject(client.key);
        outStream.writeBoolean(client.showKey);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;
        filter = (FilterDescriptor) pool.deserializeObject(inStream);
        client = pool.context.getRegularFilter(ID);
    }
}
