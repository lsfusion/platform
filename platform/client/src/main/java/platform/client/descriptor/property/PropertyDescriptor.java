package platform.client.descriptor.property;

import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

public class PropertyDescriptor extends AbstractNodeDescriptor implements ClientIdentitySerializable {
    public String caption;
    private String sID;
    public String toolTip;
    public String code;
    public boolean isField;

    public Collection<PropertyInterfaceDescriptor> interfaces;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //сериализация не нужна, т.к. вместо ссылки на свойства, нужно сериализовать ID
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        sID = inStream.readUTF();
        caption = inStream.readUTF();
        if (inStream.readBoolean())
            toolTip = inStream.readUTF();
        code = inStream.readUTF();
        isField = inStream.readBoolean();

        interfaces = pool.deserializeList(inStream);

        parent = pool.deserializeObject(inStream);
    }

    @Override
    public String toString() {
        return caption;
    }

    public String getSID() {
        return sID;
    }

}
