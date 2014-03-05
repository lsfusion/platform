package lsfusion.server.form.navigator;

import lsfusion.base.IOUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.AbstractWindowType;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.form.window.NavigatorWindow;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import static lsfusion.base.col.MapFact.mergeOrderMapsExcl;
import static lsfusion.base.col.MapFact.singletonOrder;

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

    public List<NavigatorElement<T>> getChildren() {
        return new ArrayList<NavigatorElement<T>>(children);
    }

    /**
     * Возвращает потомков без повторений
     */
    public Set<NavigatorElement<T>> getChildrenRecursive() {
        //используем Set, чтобы не было повторений
        Set<NavigatorElement<T>> result = new LinkedHashSet<NavigatorElement<T>>();
        fillChildrenRecursive(result);
        return result;
    }

    private void fillChildrenRecursive(Collection<NavigatorElement<T>> result) {
        result.add(this);
        for (NavigatorElement<T> child : children) {
            child.fillChildrenRecursive(result);
        }
    }
    
    public ImOrderMap<NavigatorElement<T>, List<String>> getChildrenMap(SecurityPolicy securityPolicy) {
        if (securityPolicy != null && !securityPolicy.navigator.checkPermission(this)) {
            return MapFact.EMPTYORDER();
        }

        if (isLeafElement()) {
            //leaf element
            return singletonOrder(this, Collections.<String>emptyList());
        }


        List<String> childrenSids = new ArrayList<String>();
        List<ImOrderMap<NavigatorElement<T>, List<String>>> childrenMaps = new ArrayList<ImOrderMap<NavigatorElement<T>, List<String>>>();
        for (NavigatorElement<T> child : children) {
            ImOrderMap<NavigatorElement<T>, List<String>> childMap = child.getChildrenMap(securityPolicy);
            if (child.isLeafElement() || !childMap.isEmpty()) {
                childrenMaps.add(childMap);
                childrenSids.add(child.getSID());
            }
        }

        if (!childrenSids.isEmpty()) {
            childrenMaps.add(0, singletonOrder(this, childrenSids));
            return mergeOrderMapsExcl(childrenMaps);
        }

        return MapFact.EMPTYORDER();
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

    public void addFirst(NavigatorElement child) {
        add(0, child);
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
    
    public boolean isLeafElement() {
        return this.getClass() != NavigatorElement.class;
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