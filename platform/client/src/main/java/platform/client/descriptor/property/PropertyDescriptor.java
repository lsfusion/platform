package platform.client.descriptor.property;

import platform.client.descriptor.IdentityDescriptor;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

public class PropertyDescriptor extends IdentityDescriptor implements ClientIdentitySerializable {
    private String caption;
    private String sID;

    public Collection<PropertyInterfaceDescriptor> interfaces;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //сериализация не нужна, т.к. вместо ссылки на свойства, нужно сериализовать ID
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;
        sID = inStream.readUTF();
        caption = inStream.readUTF();

        interfaces = pool.deserializeList(inStream);
    }
}
