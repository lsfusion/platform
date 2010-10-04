package platform.client.descriptor.property;

import platform.client.descriptor.IdentityDescriptor;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

public class PropertyDescriptor extends IdentityDescriptor implements IdentitySerializable {
    private String caption;
    private String sID;

    public Collection<PropertyInterfaceDescriptor> interfaces;

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:

    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        sID = inStream.readUTF();
        caption = inStream.readUTF();

        interfaces = pool.deserializeList(inStream);
    }
}
