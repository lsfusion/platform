package platform.client.descriptor;

import platform.base.BaseUtils;
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

    public int getID() {
        return ID;
    }

    ClientGroupObject client;

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
        BaseUtils.moveElement(this, objectFrom, objectTo);
        BaseUtils.moveElement(client, objectFrom.client, objectTo.client);
        return true;
    }
}
