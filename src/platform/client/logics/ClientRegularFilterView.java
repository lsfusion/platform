package platform.client.logics;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ClientRegularFilterView {
    public int ID;
    public String name = "";
    public KeyStroke key;

    public ClientRegularFilterView(DataInputStream inStream) throws IOException, ClassNotFoundException {
        ID = inStream.readInt();
        name = inStream.readUTF();

        key = (KeyStroke) new ObjectInputStream(inStream).readObject();
    }

    public String toString() {
        return name;
    }
}
