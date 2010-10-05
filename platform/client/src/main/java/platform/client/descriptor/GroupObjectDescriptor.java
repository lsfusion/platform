package platform.client.descriptor;

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

    public void customDeserialize(ClientSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        this.ID = ID;

        pool.deserializeCollection(this, inStream);
        initClassView = inStream.readByte();
        banClassView = inStream.readByte();
        propertyHighlight = (PropertyObjectDescriptor) pool.deserializeObject(inStream);

        client = pool.context.getGroupObject(ID);
    }

    @Override
    public String toString() {

        String result = "";
        for (ObjectDescriptor object : this) {
            if (!result.isEmpty()) {
                result += ", ";
            }
            result += object.toString();
        }
        return result;
    }
}
