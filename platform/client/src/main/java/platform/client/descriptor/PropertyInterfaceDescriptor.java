package platform.client.descriptor;

import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PropertyInterfaceDescriptor extends IdentityDescriptor implements IdentitySerializable {
    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:
    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
    }
}
