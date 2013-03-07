package platform.server.form.navigator;

import platform.base.BaseUtils;
import platform.base.IOUtils;
import platform.base.identity.IdentityObject;
import platform.interop.AbstractWindowType;
import platform.server.auth.SecurityPolicy;
import platform.server.form.window.NavigatorWindow;
import platform.server.logics.BaseLogicsModule;
import platform.server.logics.BusinessLogics;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class NavigatorElement<T extends BusinessLogics<T>> extends IdentityObject {
    private ImageIcon image;

    public String caption = "";

    public NavigatorWindow window = null;

    private NavigatorElement<T> parent;

    private List<NavigatorElement<T>> children = new ArrayList<NavigatorElement<T>>();

    public NavigatorElement() {
        setImage("/images/open.png");
    }

    public NavigatorElement(String sID, String caption) {
        this(null, sID, caption, null);
    }

    public NavigatorElement(NavigatorElement<T> parent, String sID, String caption, String icon) {
        this.sID = sID;
        setID(BaseLogicsModule.generateStaticNewID());
        this.caption = caption;
        setImage(icon != null ? icon : "/images/open.png");
        if (parent != null) {
            this.parent = parent;
            parent.add(this);
        }
    }

    public NavigatorElement<T> getParent() {
        return parent;
    }

    /**
     * Возвращает потомков без повторений
     * @param recursive возвращать ли рекурсивныых потомков
     */
    public Collection<NavigatorElement<T>> getChildren(boolean recursive) {

        if (!recursive) return new ArrayList<NavigatorElement<T>>(children);

        //используем Set, чтобы не было повторений
        Collection<NavigatorElement<T>> result = new LinkedHashSet<NavigatorElement<T>>();
        fillChildren(null, result);
        return result;
    }

    /**
     * Возвращает потомков с повторениями
     * @param recursive возвращать ли рекурсивныых потомков
     */
    public List<NavigatorElement<T>> getChildrenNonUnique(SecurityPolicy securityPolicy) {
        //используем List, тем самым разрешая повторения
        List<NavigatorElement<T>> result = new ArrayList<NavigatorElement<T>>();
        fillChildren(securityPolicy, result);
        return result;
    }

    private void fillChildren(SecurityPolicy securityPolicy, Collection<NavigatorElement<T>> result) {
        if (securityPolicy != null && !securityPolicy.navigator.checkPermission(this)) {
            return;
        }

        result.add(this);

        for (NavigatorElement<T> child : children) {
            child.fillChildren(securityPolicy, result);
        }
    }

    public NavigatorElement<T> getNavigatorElement(String elementSID) {

        if (sID.equals(elementSID)) return this;

        for (NavigatorElement<T> child : children) {
            NavigatorElement<T> element = child.getNavigatorElement(elementSID);
            if (element != null) return element;
        }

        return null;
    }

    public boolean isAncestorOf(NavigatorElement element) {
        return element != null && (equals(element) || isAncestorOf(element.parent));
    }

    public void replace(NavigatorElement<T> from, NavigatorElement<T> to) {
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

    public void clear() {
        for (NavigatorElement<T> child : children) {
            child.parent = null;
        }
        children.clear();
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public void serialize(DataOutputStream outStream) throws IOException {
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

        IOUtils.writeImageIcon(outStream, getImage());
        outStream.writeUTF(getImage().getDescription());
    }

    public byte getTypeID() {
        return 1;
    }

    public void setImage(String icon) {
        image = new ImageIcon(NavigatorElement.class.getResource(icon), icon.lastIndexOf("/") == -1 ? icon : icon.substring(icon.lastIndexOf("/") + 1));
    }

    public ImageIcon getImage() {
        return image;
    }

    @Override
    public String toString() {
        return sID + ": " + (caption != null ? caption : "");
    }

    public static NavigatorElement<?> deserialize(byte[] elementState) throws IOException {
        return deserialize(new DataInputStream(new ByteArrayInputStream(elementState)));
    }

    public static NavigatorElement<?> deserialize(DataInputStream inStream) throws IOException {
        String sID = inStream.readUTF();
        String caption = inStream.readUTF();
        return new NavigatorElement(sID, caption);
    }

}