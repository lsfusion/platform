package platform.client.navigator;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientNavigatorElement {

    public int ID;
    private String caption;

    private boolean hasChildren = false;
    boolean allowChildren() { return hasChildren; }

    public String toString() { return caption; }

    public ClientNavigatorElement() {
    }
    
    public ClientNavigatorElement(DataInputStream inStream) throws IOException {
        ID = inStream.readInt();
        caption = inStream.readUTF();
        hasChildren = inStream.readBoolean();
    }

    public static ClientNavigatorElement deserialize(DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();

        if(type==0) return new ClientNavigatorForm(inStream);
        if(type==1) return new ClientNavigatorElement(inStream);

        throw new IOException();
    }
}
