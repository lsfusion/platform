package platform.client.navigator;

import platform.interop.Constants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientNavigatorElement {

    public int ID;
    private String caption;
    public String sID;

    protected boolean hasChildren = false;

    public boolean hasChildren() {
        return hasChildren;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String toString() {
        return caption;
    }

    public ClientNavigatorElement() {
    }

    public ClientNavigatorElement(int ID, String caption, boolean hasChildren) {
        this.ID = ID;
        this.sID = Constants.getDefaultNavigatorElementSID(ID);
        this.caption = caption;
        this.hasChildren = hasChildren;
    }

    public ClientNavigatorElement(DataInputStream inStream) throws IOException {
        ID = inStream.readInt();
        sID = inStream.readUTF();
        caption = inStream.readUTF();
        hasChildren = inStream.readBoolean();
    }

    public static ClientNavigatorElement deserialize(DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();

        if (type == 0) {
            return new ClientNavigatorForm(inStream);
        }
        if (type == 1) {
            return new ClientNavigatorElement(inStream);
        }

        throw new IOException();
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(ID);
        outStream.writeUTF(caption);
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

    public String getSID() {
        return sID;
    }
}
