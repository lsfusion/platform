package platform.client.descriptor;

import platform.base.BaseUtils;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DataObjectDescriptor implements PropertyObjectInterfaceDescriptor {
    Object object;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        BaseUtils.serializeObject(object);
    }

    public void customDeserialize(ClientSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        object = BaseUtils.deserializeObject(inStream);
    }
}
