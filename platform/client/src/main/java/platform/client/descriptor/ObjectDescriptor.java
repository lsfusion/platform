package platform.client.descriptor;

import platform.client.logics.ClientForm;
import platform.client.logics.ClientObject;
import platform.interop.serialization.CustomSerializable;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ObjectDescriptor extends IdentityDescriptor implements PropertyObjectInterfaceDescriptor, IdentitySerializable {

    ClientObject client;
    private GroupObjectDescriptor groupTo;

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:

    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        groupTo = (GroupObjectDescriptor) pool.deserializeObject(inStream);

        client = ((ClientForm) pool.context).getObject(ID);
    }

    @Override
    public String toString() {
        return client.caption;
    }
}
