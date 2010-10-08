package platform.client.descriptor.filter;

import platform.client.logics.ClientRegularFilter;
import platform.client.serialization.ClientCustomSerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.IdentityDescriptor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

public class RegularFilterDescriptor extends IdentityDescriptor implements ClientIdentitySerializable {

    private FilterDescriptor filter;

    ClientRegularFilter client;

    public GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList) {
        return filter.getGroupObject(groupList);
    }

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
