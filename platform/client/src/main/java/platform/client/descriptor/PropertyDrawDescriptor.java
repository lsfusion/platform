package platform.client.descriptor;

import platform.client.logics.ClientPropertyDraw;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class PropertyDrawDescriptor extends IdentityDescriptor implements ClientIdentitySerializable {

    ClientPropertyDraw client;

    private PropertyObjectDescriptor propertyObject;
    public GroupObjectDescriptor toDraw;
    private List<GroupObjectDescriptor> columnGroupObjects;
    private PropertyObjectDescriptor propertyCaption;

    private boolean shouldBeLast;
    private Byte forceViewType;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, propertyObject);
        pool.serializeObject(outStream, toDraw);
        pool.serializeCollection(outStream, columnGroupObjects);
        pool.serializeObject(outStream, propertyCaption);

        outStream.writeBoolean(shouldBeLast);
        outStream.writeBoolean(forceViewType != null);
        if (forceViewType != null) {
            outStream.writeByte(forceViewType);
        }
    }

    public void customDeserialize(ClientSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        propertyObject = (PropertyObjectDescriptor) pool.deserializeObject(inStream);
        toDraw = (GroupObjectDescriptor) pool.deserializeObject(inStream);
        columnGroupObjects = pool.deserializeList(inStream);
        propertyCaption = (PropertyObjectDescriptor) pool.deserializeObject(inStream);

        shouldBeLast = inStream.readBoolean();
        if (inStream.readBoolean()) {
            forceViewType = inStream.readByte();
        }

        client = pool.context.getProperty(ID);
    }

    @Override
    public String toString() {
        return client.caption;
    }
}
