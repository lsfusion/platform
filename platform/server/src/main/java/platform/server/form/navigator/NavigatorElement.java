package platform.server.form.navigator;

import platform.base.BaseUtils;
import platform.base.identity.DefaultIDGenerator;
import platform.base.identity.IdentityObject;
import platform.server.logics.BusinessLogics;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class NavigatorElement<T extends BusinessLogics<T>> extends IdentityObject {
    private static DefaultIDGenerator idGenerator = new DefaultIDGenerator();
//    private static Set<String> elementsSIDs = new HashSet<String>();

    public String caption = "";

    public NavigatorElement() {

    }
    public NavigatorElement(String sID, String caption) { this(null, sID, caption); }
    public NavigatorElement(NavigatorElement<T> parent, String sID, String caption) {
        this.sID = sID;
//        assert elementsSIDs.add(sID); // проверка уникальности sID
        setID(idGenerator.idShift());
        this.caption = caption;

        if (parent != null) {
            this.parent = parent;
            parent.add(this);
        }
    }

    private NavigatorElement<T> parent;
    public NavigatorElement<T> getParent() { return parent; }

    private List<NavigatorElement<T>> children = new ArrayList<NavigatorElement<T>>();
    Collection<NavigatorElement<T>> getChildren() { return children; }

    public Collection<NavigatorElement<T>> getChildren(boolean recursive) {

        if (!recursive) return new ArrayList<NavigatorElement<T>>(children);

        Collection<NavigatorElement<T>> result = new ArrayList<NavigatorElement<T>>();
        fillChildren(result);
        return result;
    }

    private void fillChildren(Collection<NavigatorElement<T>> result) {

        if (result.contains(this))
            return;

        result.add(this);

        for (NavigatorElement<T> child : children)
            child.fillChildren(result);
    }

    public void replaceChild(NavigatorElement<T> from, NavigatorElement<T> to) {
        BaseUtils.replaceListElements(children, from, to);
        to.parent = this;
        from.parent = null;
    }

    public void add(NavigatorElement<T> child) {
        children.add(child);
        child.parent = this;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public NavigatorElement<T> getNavigatorElement(String elementSID) {

        if (sID.equals(elementSID)) return this;

        for(NavigatorElement<T> child : children) {
            NavigatorElement<T> element = child.getNavigatorElement(elementSID);
            if (element != null) return element;
        }

        return null;
    }

    public byte getTypeID() {
        return 1;
    }
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());

        outStream.writeInt(getID());
        outStream.writeUTF(getSID());
        outStream.writeUTF(caption);
        outStream.writeBoolean(hasChildren());
    }

    public void removeAllChildren() {
        for (NavigatorElement<T> child : children) {
            child.parent = null;
        }
        children.clear();
    }

    public static NavigatorElement<?> deserialize(byte[] elementState) throws IOException {
        return deserialize(new DataInputStream(new ByteArrayInputStream(elementState)));
    }

    @Override
    public String toString() {
        return sID + ": " + (caption != null ? caption : "");
    }

    public static NavigatorElement<?> deserialize(DataInputStream inStream) throws IOException {
        String sID = inStream.readUTF();
        String caption = inStream.readUTF();

        return new NavigatorElement(sID, caption);
    }
}