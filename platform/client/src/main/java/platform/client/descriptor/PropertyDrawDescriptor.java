package platform.client.descriptor;

import platform.client.logics.ClientPropertyDraw;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class PropertyDrawDescriptor extends IdentityDescriptor implements IdentitySerializable {

    ClientPropertyDraw client;

    private PropertyObjectDescriptor propertyObject;
    private GroupObjectDescriptor toDraw;
    private List<GroupObjectDescriptor> columnGroupObjects;
    private PropertyObjectDescriptor propertyCaption;

    private boolean shouldBeLast;
    private Byte forceViewType;

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:

    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        propertyObject = (PropertyObjectDescriptor) pool.deserializeObject(inStream);
        toDraw = (GroupObjectDescriptor) pool.deserializeObject(inStream);
        columnGroupObjects = pool.deserializeList(inStream);
        propertyCaption = (PropertyObjectDescriptor) pool.deserializeObject(inStream);

        shouldBeLast = inStream.readBoolean();
        if (inStream.readBoolean()) {
            forceViewType = inStream.readByte();
        }
    }
}
