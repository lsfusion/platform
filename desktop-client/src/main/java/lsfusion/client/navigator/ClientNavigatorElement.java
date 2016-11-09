package lsfusion.client.navigator;

import lsfusion.base.IOUtils;
import lsfusion.base.serialization.SerializationUtil;
import lsfusion.interop.SerializableImageIconHolder;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClientNavigatorElement {

    private int ID;
    private String sID;
    private String canonicalName;
    
    public String caption;
    
    public List<ClientNavigatorElement> parents = new ArrayList<>();
    public List<ClientNavigatorElement> children = new ArrayList<>();
    public SerializableImageIconHolder image;
    public String imageFileName;

    protected boolean hasChildren = false;
    public ClientNavigatorWindow window;

    public ClientNavigatorElement(DataInputStream inStream) throws IOException {
        ID = inStream.readInt();
        sID = inStream.readUTF();
        canonicalName = SerializationUtil.readString(inStream);
        
        caption = inStream.readUTF();
        hasChildren = inStream.readBoolean();
        window = ClientNavigatorWindow.deserialize(inStream);

        image = IOUtils.readImageIcon(inStream);
        imageFileName = inStream.readUTF();
    }

    public int getID() {
        return ID;
    }

    public String getSID() {
        return sID;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public boolean hasChildren() {
        return hasChildren;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    @Override
    public int hashCode() {
        return ID;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClientNavigatorElement && ((ClientNavigatorElement) obj).ID == ID;
    }

    public String toString() {
        return caption;
    }

    public ClientNavigatorElement findElementByCanonicalName(String canonicalName) {
        if (canonicalName == null) {
            return null;
        }
        if (canonicalName.equals(this.canonicalName)) {
            return this;
        }
        
        for (ClientNavigatorElement child : children) {
            ClientNavigatorElement found = child.findElementByCanonicalName(canonicalName);
            if (found != null) {
                return found;
            }
        }
        return null;
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
