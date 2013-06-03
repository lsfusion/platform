package lsfusion.client.navigator;

import lsfusion.base.IOUtils;
import lsfusion.interop.SerializableImageIconHolder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientNavigatorElement {
    public static final String BASE_ELEMENT_SID = "baseElement";

    public int ID;
    public String caption;
    public String sID;
    public List<ClientNavigatorElement> parents = new ArrayList<ClientNavigatorElement>();
    public List<ClientNavigatorElement> children = new ArrayList<ClientNavigatorElement>();
    public SerializableImageIconHolder image;
    public String imageFileName;

    protected boolean hasChildren = false;
    public ClientNavigatorWindow window;

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

    public ClientNavigatorElement(int ID, String sID, String caption, boolean hasChildren) {
        this.ID = ID;
        this.sID = sID;
        this.caption = caption;
        this.hasChildren = hasChildren;
    }

    public ClientNavigatorElement(DataInputStream inStream) throws IOException {
        ID = inStream.readInt();
        sID = inStream.readUTF();
        caption = inStream.readUTF();
        hasChildren = inStream.readBoolean();
        window = ClientNavigatorWindow.deserialize(inStream);

        image = IOUtils.readImageIcon(inStream);
        imageFileName = inStream.readUTF();
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeUTF(sID);
        outStream.writeUTF(caption);
    }

    @Override
    public int hashCode() {
        return sID.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClientNavigatorElement) {
            ClientNavigatorElement element = (ClientNavigatorElement) obj;
            return element.sID.equals(sID);
        }
        return false;
    }

    public static ClientNavigatorElement deserialize(DataInputStream inStream, Map<String, ClientNavigatorWindow> windows) throws IOException {
        byte type = inStream.readByte();

        ClientNavigatorElement element;

        switch (type) {
            case 0: element = new ClientNavigatorForm(inStream); break;
            case 1: element = new ClientNavigatorElement(inStream); break;
            case 2: element = new ClientNavigatorAction(inStream); break;
            default:
                throw new IOException("Incorrect navigator element type");
        }

        if (element.window != null) {
            String windowSID = element.window.getSID();
            if (windows.containsKey(windowSID)) {
                element.window = windows.get(windowSID);
            } else {
                windows.put(windowSID, element.window);
            }
        }

        return element;
    }

    public String getSID() {
        return sID;
    }

    //содержатся ли родители текущей вершины в заданном множестве
    public boolean containsParent(Set<ClientNavigatorElement> set) {
        for (ClientNavigatorElement parent : parents) {
            if (set.contains(parent)) {
                return true;
            }
        }
        return false;
    }
}
