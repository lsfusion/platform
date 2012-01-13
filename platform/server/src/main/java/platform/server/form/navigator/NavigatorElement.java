package platform.server.form.navigator;

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.identity.IdentityObject;
import platform.interop.AbstractWindowType;
import platform.server.auth.SecurityPolicy;
import platform.server.form.window.NavigatorWindow;
import platform.server.logics.BusinessLogics;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class NavigatorElement<T extends BusinessLogics<T>> extends IdentityObject {
    private static Set<String> elementsSIDs = new HashSet<String>();

    public String caption = "";

    public NavigatorWindow window = null;

    private static ImageIcon image = new ImageIcon(NavigatorElement.class.getResource("/images/open.png"));

    public NavigatorElement() {

    }

    public NavigatorElement(String sID, String caption) {
        this(null, sID, caption);
    }

    public NavigatorElement(NavigatorElement<T> parent, String sID, String caption) {
        this.sID = sID;
        assert elementsSIDs.add(sID); // проверка уникальности sID
        setID(BusinessLogics.generateStaticNewID());
        this.caption = caption;

        if (parent != null) {
            this.parent = parent;
            parent.add(this);
        }
    }

    private NavigatorElement<T> parent;

    public NavigatorElement<T> getParent() {
        return parent;
    }

    private List<NavigatorElement<T>> children = new ArrayList<NavigatorElement<T>>();

    Collection<NavigatorElement<T>> getChildren() {
        return children;
    }

    public Collection<NavigatorElement<T>> getChildren(boolean recursive) {

        if (!recursive) return new ArrayList<NavigatorElement<T>>(children);

        Collection<NavigatorElement<T>> result = new ArrayList<NavigatorElement<T>>();
        fillChildren(result);
        return result;
    }

    public boolean isAncestorOf(NavigatorElement element) {
        return element != null && (equals(element) || isAncestorOf(element.parent));
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

    public void add(NavigatorElement child) {
        add(children.size(), child);
    }

    public void add(int index, NavigatorElement child) {
        int currIndex = children.indexOf(child);
        if (currIndex != -1) {
            children.remove(child);
            if (currIndex < index) {
                index--;
            }
        } else if (child.parent != null) {
            child.parent.remove(child);
        }

        children.add(index, child);
        child.parent = this;
    }

    public void addBefore(NavigatorElement child, NavigatorElement elemBefore) {
        add(elemBefore != null ? children.indexOf(elemBefore) : children.size(), child);
    }

    public void addAfter(NavigatorElement child, NavigatorElement elemAfter) {
        add(elemAfter != null ? children.indexOf(elemAfter) + 1 : children.size(), child);
    }

    public boolean remove(NavigatorElement child) {
        return child != null && children.remove(child);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public NavigatorElement<T> getNavigatorElement(String elementSID) {

        if (sID.equals(elementSID)) return this;

        for (NavigatorElement<T> child : children) {
            NavigatorElement<T> element = child.getNavigatorElement(elementSID);
            if (element != null) return element;
        }

        return null;
    }

    public byte getTypeID() {
        return 1;
    }

    public void serialize(DataOutputStream outStream, Collection<NavigatorElement> elements) throws IOException {
        outStream.writeByte(getTypeID());

        outStream.writeInt(getID());
        outStream.writeUTF(getSID());
        outStream.writeUTF(caption);
        outStream.writeBoolean(hasChildren());
        if (window == null) {
            outStream.writeInt(AbstractWindowType.NULL_VIEW);
        } else {
            window.serialize(outStream);
        }

        int count = 0;
        for (NavigatorElement<T> child : children)
            if (elements.contains(child))
                count++;

        outStream.writeInt(count);
        for (NavigatorElement<T> child : children) {
            if (elements.contains(child))
                outStream.writeUTF(child.getSID());
        }
        IOUtils.writeImageIcon(outStream, getImage());
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

    public Collection<NavigatorElement> addSubTree(SecurityPolicy securityPolicy, Collection<NavigatorElement> collection) {
        if (!securityPolicy.navigator.checkPermission(this))
            return collection;

        collection.add(this);
        for (NavigatorElement<T> child : children) {
            child.addSubTree(securityPolicy, collection);
        }

        return collection;
    }

    public ImageIcon getImage() {
        return image;
    }
}