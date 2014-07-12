package lsfusion.server.form.navigator;

import lsfusion.base.BaseUtils;
import lsfusion.base.IOUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.AbstractWindowType;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.form.window.NavigatorWindow;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.interfaces.NFOrderSet;
import lsfusion.server.logics.mutables.interfaces.NFProperty;
import lsfusion.server.logics.mutables.Version;

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

    private NFProperty<NavigatorElement<T>> parent = NFFact.property();
    public void setParent(NavigatorElement<T> parent, Version version) {
        this.parent.set(parent, version);
    }

    private NFOrderSet<NavigatorElement<T>> children = NFFact.orderSet();
    public Iterable<NavigatorElement<T>> getChildrenIt() {
        return children.getIt();
    }
    public ImSet<NavigatorElement<T>> getChildren() {
        return children.getSet();
    }
    public ImOrderSet<NavigatorElement<T>> getChildrenList() {
        return children.getOrderSet();
    }

    public NavigatorElement() {
        setImage("/images/open.png");
    }

    public NavigatorElement(String sID, String caption) {
        this(null, sID, caption, null, Version.DESCRIPTOR);
    }

    public NavigatorElement(NavigatorElement<T> parent, String sID, String caption, String icon, Version version) {
        this.sID = sID;
        setID(BaseLogicsModule.generateStaticNewID());
        this.caption = caption;
        setImage(icon != null ? icon : "/images/open.png");
        if (parent != null) {
            setParent(parent, version);
            parent.add(this, version);
        }
    }

    public NavigatorElement<T> getParent() {
        return parent.get();
    }
    public NavigatorElement<T> getNFParent(Version version) {
        return parent.getNF(version);
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
        for (NavigatorElement<T> child : getChildrenIt()) {
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
        for (NavigatorElement<T> child : getChildrenIt()) {
            ImOrderMap<NavigatorElement<T>, List<String>> childMap = child.getChildrenMap(securityPolicy);
            if (!childMap.isEmpty()) {
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

        for (NavigatorElement<T> child : getChildrenIt()) {
            NavigatorElement<T> element = child.getNavigatorElement(elementSID);
            if (element != null) return element;
        }

        return null;
    }

    public boolean isAncestorOf(NavigatorElement element, Version version) {
        return element != null && (equals(element) || isAncestorOf(element.getNFParent(version), version));
    }

    private void changeContainer(NavigatorElement comp, Version version) {
        NavigatorElement container = comp.getNFParent(version);
        if (container != null)
            container.remove(comp, version);
        
        comp.setParent(this, version);
    }

    public void addFirst(NavigatorElement child, Version version) {
        changeContainer(child, version);
        children.addFirst(child, version);
    }

    public void add(NavigatorElement child, Version version) {
        changeContainer(child, version);
        children.add(child, version);
    }

    public void addBefore(NavigatorElement child, NavigatorElement elemBefore, Version version) {
        changeContainer(child, version);
        children.addIfNotExistsToThenLast(child, elemBefore, false, version);
    }

    public void addAfter(NavigatorElement child, NavigatorElement elemAfter, Version version) {
        changeContainer(child, version);
        children.addIfNotExistsToThenLast(child, elemAfter, true, version);
    }

    public boolean remove(NavigatorElement child, Version version) {
        if(child == null)
            return false;

        if (children.containsNF(child, version)) {
            children.remove(child, version);
            child.setParent(null, version);
            return true;
        } else {
            return false;
        }
    }

    public boolean hasChildren() {
        return !getChildren().isEmpty();
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

   public boolean needsToBeSynchronized() {
        return true;
    }

    public void finalizeAroundInit() {
        parent.finalizeChanges();
        children.finalizeChanges();
    }
}