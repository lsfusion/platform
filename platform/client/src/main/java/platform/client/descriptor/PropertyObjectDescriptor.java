package platform.client.descriptor;

import platform.client.descriptor.property.PropertyDescriptor;
import platform.interop.serialization.CustomSerializable;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class PropertyObjectDescriptor extends IdentityDescriptor implements IdentitySerializable {
    private PropertyDescriptor property;
    private List<CustomSerializable> mapping;

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:

    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        property = (PropertyDescriptor) pool.deserializeObject(inStream);
        //todo: proper mapping deserialization
//        mapping = pool.deserializeList(inStream);
    }
}
