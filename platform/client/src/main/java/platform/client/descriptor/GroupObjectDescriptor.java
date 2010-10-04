package platform.client.descriptor;

import platform.client.logics.ClientForm;
import platform.client.logics.ClientGroupObject;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectDescriptor extends ArrayList<ObjectDescriptor> implements IdentitySerializable {
    private int ID;
    private byte initClassView;
    private byte banClassView;
    private PropertyObjectDescriptor propertyHighlight;

    public int getID() {
        return ID;
    }

    ClientGroupObject client;

    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        //todo:
    }

    public void customDeserialize(SerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        this.ID = ID;

        pool.deserializeCollection(this, inStream);
        initClassView = inStream.readByte();
        banClassView = inStream.readByte();
        propertyHighlight = (PropertyObjectDescriptor) pool.deserializeObject(inStream);


        ClientForm clientForm = (ClientForm) pool.context;
        client = clientForm.getGroupObject(ID);
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
