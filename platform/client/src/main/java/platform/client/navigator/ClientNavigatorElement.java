package platform.client.navigator;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientNavigatorElement {

    public int ID;
    private String caption;

    protected boolean hasChildren = false;
    public boolean hasChildren() { return hasChildren; }

    public String toString() { return caption; }

    public ClientNavigatorElement() {
    }

    public ClientNavigatorElement(int ID, String caption, boolean hasChildren) {
        this.ID = ID;
        this.caption = caption;
        this.hasChildren = hasChildren;
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

    @Override
    public int hashCode() {
        return new Integer(ID).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClientNavigatorElement) {
            ClientNavigatorElement element = (ClientNavigatorElement) obj;
            return element.ID == ID;
        }
        return false;
    }
}
