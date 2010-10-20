package platform.client.descriptor;

import platform.base.BaseUtils;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.logics.ClientGroupObject;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectDescriptor extends ArrayList<ObjectDescriptor> implements ClientIdentitySerializable {
    private int ID;
    private byte initClassView;
    private byte banClassView;
    private PropertyObjectDescriptor propertyHighlight;
    public ClientGroupObject client;

    public int getID() {
        return ID;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, this);
        outStream.writeByte(initClassView);
        outStream.writeByte(banClassView);
        pool.serializeObject(outStream, propertyHighlight);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        this.ID = iID;

        pool.deserializeCollection(this, inStream);
        initClassView = inStream.readByte();
        banClassView = inStream.readByte();
        propertyHighlight = (PropertyObjectDescriptor) pool.deserializeObject(inStream);

        client = pool.context.getGroupObject(iID);
    }

    @Override
    public String toString() {
        return client.toString();
    }

    public boolean moveObject(ObjectDescriptor objectFrom, ObjectDescriptor objectTo) {
        return moveObject(objectFrom, indexOf(objectTo) + (indexOf(objectFrom) > indexOf(objectTo) ? 0 : 1));
    }

    public boolean moveObject(ObjectDescriptor objectFrom, int index) {
        BaseUtils.moveElement(this, objectFrom, index);
        BaseUtils.moveElement(client, objectFrom.client, index);
        IncrementDependency.update(this, "objects");
        return true;
    }
}
