package lsfusion.client.descriptor.property;

import lsfusion.base.identity.EqualsIdentityObject;
import lsfusion.client.serialization.ClientIdentitySerializable;
import lsfusion.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PropertyInterfaceDescriptor extends EqualsIdentityObject implements ClientIdentitySerializable {
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
    }
}
