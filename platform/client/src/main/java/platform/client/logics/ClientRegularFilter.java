package platform.client.logics;

import platform.base.identity.IdentityObject;
import platform.client.SwingUtils;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientRegularFilter extends IdentityObject implements ClientIdentitySerializable {

    public String caption = "";
    public KeyStroke key;
    public boolean showKey;

    public ClientRegularFilter() {
    }

    public ClientRegularFilter(int ID) {
        super(ID);
    }

    public String getFullCaption() {

        String fullCaption = caption;
        if (showKey && key != null) {
            fullCaption += " (" + SwingUtils.getKeyStrokeCaption(key) + ")";
        }
        return fullCaption;
    }

    @Override
    public String toString() {
        return getFullCaption() + " (" + getID() + ")";
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        caption = pool.readString(inStream);

        key = pool.readObject(inStream);
        showKey = inStream.readBoolean();
    }
}
