package platform.client.navigator;

import platform.base.IOUtils;
import platform.gwt.view.GNavigatorElement;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class ClientNavigatorElement {
    private static Map<String, HashSet<ClientNavigatorElement>> parents = new HashMap<String, HashSet<ClientNavigatorElement>>();

    public int ID;
    private String caption;
    public String sID;
    public List<String> childrenSid = new LinkedList<String>();
    public List<ClientNavigatorElement> children = new LinkedList<ClientNavigatorElement>();
    public ImageIcon image;
    public static ClientNavigatorElement root;

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
        int cnt = inStream.readInt();
        for (int i = 0; i < cnt; i++) {
            String childSID = inStream.readUTF();
            addChild(childSID);
        }

        image = IOUtils.readImageIcon(inStream);

        if (window != null) {
            window.elements.add(this);
        }
        if (sID.equals(AbstractNavigator.BASE_ELEMENT_SID)) {
            root = this;
        }
    }

    private void addChild(String childSID) {
        childrenSid.add(childSID);
        HashSet<ClientNavigatorElement> parentsSet = parents.containsKey(childSID) ? parents.get(childSID) : new HashSet<ClientNavigatorElement>();
        parentsSet.add(this);
        parents.put(childSID, parentsSet);
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
        outStream.writeUTF(sID);
        outStream.writeUTF(caption);
        //outStream.write
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

    public String getSID() {
        return sID;
    }

    //содержатся ли родители текущей вершины в заданном множестве
    public boolean containsParent(Set<ClientNavigatorElement> set) {
        Set<ClientNavigatorElement> parentSet = parents.get(sID);
        if (parentSet != null) {
            for (ClientNavigatorElement element : parentSet) {
                if (set.contains(element)) return true;
            }
        }
        return false;
    }

    private GNavigatorElement gwtNavigatorElement;
    public GNavigatorElement getGwtElement() {
        if (gwtNavigatorElement == null) {
            gwtNavigatorElement = new GNavigatorElement();
            gwtNavigatorElement.sid = sID;
            gwtNavigatorElement.caption = caption;
            gwtNavigatorElement.children = new ArrayList<GNavigatorElement>();
            gwtNavigatorElement.icon = "open.png";
            gwtNavigatorElement.isForm = false;
            for (ClientNavigatorElement child : children) {
                gwtNavigatorElement.children.add(child.getGwtElement());
            }
        }
        return gwtNavigatorElement;
    }
}
