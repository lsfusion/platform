package lsfusion.server.form.navigator;

import lsfusion.base.IOUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.serialization.SerializationUtil;
import lsfusion.interop.AbstractWindowType;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.form.window.NavigatorWindow;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFOrderSet;
import lsfusion.server.logics.mutables.interfaces.NFProperty;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import static lsfusion.base.col.MapFact.mergeOrderMapsExcl;
import static lsfusion.base.col.MapFact.singletonOrder;

public class NavigatorElement<T extends BusinessLogics<T>> {
    
    public static final String NAVIGATOR_ANONYMOUS_SID_PREFIX = "_NAVIGATORELEMENT_";
    public static final String ACTION_ANONYMOUS_SID_PREFIX = "_NAVIGATORACTION_";
    public static final String FORM_ANONYMOUS_SID_PREFIX = "_FORM_";
    
    private ImageIcon image;

    public String caption = "";

    public NavigatorWindow window = null;

    private final int ID;
    private final String canonicalName;

    private NFProperty<NavigatorElement<T>> parent = NFFact.property();
    private NFOrderSet<NavigatorElement<T>> children = NFFact.orderSet();

    public NavigatorElement(NavigatorElement<T> parent, String canonicalName, String caption, String icon, Version version) {
        this.canonicalName = canonicalName;
        this.ID = BaseLogicsModule.generateStaticNewID();
        this.caption = caption;

        setImage(icon != null ? icon : "/images/open.png");

        if (parent != null) {
            setParent(parent, version);
            parent.add(this, version);
        }
    }

    public int getID() {
        return ID;
    }

    public String getSID() {
        if (canonicalName != null) {
            return canonicalName;
        } else {
            return getAnonymousSIDPrefix() + getID();
        }
    }
    
    protected String getAnonymousSIDPrefix() {
        return NAVIGATOR_ANONYMOUS_SID_PREFIX;
    }

    public String getCanonicalName() {
        return canonicalName;
    }
    
    public boolean isNamed() {
        return canonicalName != null;
    }

    public void setParent(NavigatorElement<T> parent, Version version) {
        this.parent.set(parent, version);
    }

    public Iterable<NavigatorElement<T>> getChildrenIt() {
        return children.getIt();
    }
    
    public ImSet<NavigatorElement<T>> getChildren() {
        return children.getSet();
    }
    
    public ImOrderSet<NavigatorElement<T>> getChildrenList() {
        return children.getOrderSet();
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

    public NavigatorElement<T> getNavigatorElementBySID(String elementSID) {
        if (getSID().equals(elementSID)) return this;

        for (NavigatorElement<T> child : getChildrenIt()) {
            NavigatorElement<T> element = child.getNavigatorElementBySID(elementSID);
            if (element != null) return element;
        }

        return null;
    }

    public NavigatorElement<T> getNavigatorElementByCanonicalName(String elementCanonicalName) {
        if (elementCanonicalName == null) {
            return null;
        }
        
        if (elementCanonicalName.equals(getCanonicalName())) return this;

        for (NavigatorElement<T> child : getChildrenIt()) {
            NavigatorElement<T> element = child.getNavigatorElementByCanonicalName(elementCanonicalName);
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

    public boolean needsToBeSynchronized() {
        return isNamed();
    }

    public void finalizeAroundInit() {
        parent.finalizeChanges();
        children.finalizeChanges();
    }

    @Override
    public String toString() {
        return getSID() + ": " + (caption != null ? caption : "");
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());

        outStream.writeInt(getID());
        outStream.writeUTF(getSID());
        SerializationUtil.writeString(outStream, canonicalName);
        
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
}