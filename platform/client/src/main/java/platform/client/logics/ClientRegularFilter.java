package platform.client.logics;

import platform.client.SwingUtils;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ClientRegularFilter implements ClientIdentitySerializable {

    public int ID;
    public String caption = "";
    public KeyStroke key;
    public boolean showKey;

    public ClientRegularFilter() {

    }
    
    public ClientRegularFilter(DataInputStream inStream) throws IOException, ClassNotFoundException {
        ID = inStream.readInt();
        caption = inStream.readUTF();

        key = (KeyStroke) new ObjectInputStream(inStream).readObject();
        showKey = inStream.readBoolean();
    }

    public String getFullCaption() {

        String fullCaption = caption;
        if (showKey && key != null)
            fullCaption += " (" + SwingUtils.getKeyStrokeCaption(key) + ")";
        return fullCaption;
    }

    public String toString() {
        return getFullCaption();
    }

    public int getID() {
        return ID;
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeString(outStream, caption);
        pool.writeObject(outStream, key);
        outStream.writeBoolean(showKey);
    }

    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;
        caption = pool.readString(inStream);

        key = pool.readObject(inStream);
        showKey = inStream.readBoolean();
    }
}
