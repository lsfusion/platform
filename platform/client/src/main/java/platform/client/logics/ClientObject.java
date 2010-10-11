package platform.client.logics;

import platform.client.logics.classes.ClientClass;
import platform.client.logics.classes.ClientTypeSerializer;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

public class ClientObject implements Serializable, ClientIdentitySerializable {

    private int ID = 0;

    public int getID() {
        return ID;
    }

    public String caption;
    public boolean addOnTransaction;

    // вручную заполняется
    public ClientGroupObject groupObject;

    public ClientClass baseClass;

    public ClientClassChooser classChooser;

    public ClientObject() {

    }

    public ClientObject(DataInputStream inStream, Collection<ClientContainer> containers, ClientGroupObject iGroupObject) throws ClassNotFoundException, IOException {

        groupObject = iGroupObject;

        ID = inStream.readInt();
        caption = inStream.readUTF();
        addOnTransaction = inStream.readBoolean();

        baseClass = ClientTypeSerializer.deserializeClientClass(inStream);

        classChooser = new ClientClassChooser(inStream,containers);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, groupObject);
        pool.writeString(outStream, caption);

        outStream.writeBoolean(addOnTransaction);

        baseClass.serialize(outStream);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;

        groupObject = pool.deserializeObject(inStream);

        caption = pool.readString(inStream);

        addOnTransaction = inStream.readBoolean();

        baseClass = ClientTypeSerializer.deserializeClientClass(inStream);

        classChooser = pool.deserializeObject(inStream);
    }

    @Override
    public String toString() {
        return caption;
    }

}
