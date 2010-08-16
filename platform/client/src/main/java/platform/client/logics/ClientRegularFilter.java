package platform.client.logics;

import platform.client.SwingUtils;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ClientRegularFilter {

    public int ID;
    public String caption = "";
    public KeyStroke key;
    public boolean showKey;

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
}
