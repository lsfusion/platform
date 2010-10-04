package platform.client.descriptor;

import platform.client.logics.ClientObject;
import platform.interop.serialization.CustomSerializable;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectDescriptor extends IdentityDescriptor implements OrderDescriptor, IdentitySerializable {

    ClientObject client;
    private GroupObjectDescriptor groupTo;
    private String caption;

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:

    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        //todo:

        groupTo = (GroupObjectDescriptor) pool.deserializeObject(inStream);
        caption = inStream.readUTF();
    }
}
