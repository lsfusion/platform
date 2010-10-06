package platform.client.descriptor;

import platform.client.logics.ClientObject;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectDescriptor extends IdentityDescriptor implements PropertyObjectInterfaceDescriptor, ClientIdentitySerializable {

    ClientObject client;
    private GroupObjectDescriptor groupTo;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, groupTo);
        client.baseClass.serialize(outStream);
        outStream.writeUTF(client.caption);
    }

    public void customDeserialize(ClientSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        groupTo = (GroupObjectDescriptor) pool.deserializeObject(inStream);

        client = pool.context.getObject(ID);
    }

    @Override
    public String toString() {
        return client.caption;
    }
}
