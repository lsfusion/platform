package platform.client.descriptor.property;

import platform.base.identity.EqualsIdentityObject;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PropertyInterfaceDescriptor extends EqualsIdentityObject implements ClientIdentitySerializable {
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
    }
}
