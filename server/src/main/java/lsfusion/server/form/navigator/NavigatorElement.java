package lsfusion.server.form.navigator;

import lsfusion.base.IOUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.serialization.SerializationUtil;
import lsfusion.interop.AbstractWindowType;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.form.window.NavigatorWindow;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.ElementCanonicalNameUtils;
import lsfusion.server.logics.i18n.LocalizedString;
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

public abstract class NavigatorElement {
    
    private ImageIcon image;
    public DefaultIcon defaultIcon;

    public NavigatorWindow window = null;

    private final int ID;
    public LocalizedString caption;
    private final String canonicalName;
    private String creationPath = null;

    private NFProperty<NavigatorElement> parent = NFFact.property();
    private NFOrderSet<NavigatorElement> children = NFFact.orderSet();

    protected NavigatorElement(String canonicalName, LocalizedString caption) {
        assert canonicalName != null;
        this.canonicalName = canonicalName;
        this.ID = BaseLogicsModule.generateStaticNewID();
        this.caption = caption;
    }

    public int getID() {
        return ID;
    }

    public String getCanonicalName() {
        return canonicalName;
    }
    
    public String getName() { 
        return ElementCanonicalNameUtils.getName(canonicalName); 
    }
    
    public boolean isNamed() {
        return canonicalName != null;
    }

    /** Не обновляет список потомков у parent'а */
    private void setParent(NavigatorElement parent, Version version) {
        this.parent.set(parent, version);
    }

    private Iterable<NavigatorElement> getChildrenIt() {
        return children.getIt();
    }
    
    public ImSet<NavigatorElement> getChildren() {
        return children.getSet();
    }
    
    public ImOrderSet<NavigatorElement> getChildrenList() {
        return children.getOrderSet();
    }

    public NavigatorElement getParent() {
        return parent.get();
    }
    
    public NavigatorElement getNFParent(Version version) {
        return parent.getNF(version);
    }

    /** Возвращает потомков без повторений */
    public Set<NavigatorElement> getChildrenRecursive() {
        Set<NavigatorElement> result = new LinkedHashSet<>();
        fillChildrenRecursive(result);
        return result;
    }

    private void fillChildrenRecursive(Collection<NavigatorElement> result) {
        result.add(this);
        for (NavigatorElement child : getChildrenIt()) {
            child.fillChildrenRecursive(result);
        }
    }
    
    public ImOrderMap<NavigatorElement, List<String>> getChildrenMap(SecurityPolicy securityPolicy) {
        if (securityPolicy != null && !securityPolicy.navigator.checkPermission(this)) {
            return MapFact.EMPTYORDER();
        }

        if (isLeafElement()) {
            //leaf element
            return singletonOrder(this, Collections.<String>emptyList());
        }


        List<String> childrenSids = new ArrayList<>();
        List<ImOrderMap<NavigatorElement, List<String>>> childrenMaps = new ArrayList<>();
        for (NavigatorElement child : getChildrenIt()) {
            ImOrderMap<NavigatorElement, List<String>> childMap = child.getChildrenMap(securityPolicy);
            if (!childMap.isEmpty()) {
                childrenMaps.add(childMap);
                childrenSids.add(child.getCanonicalName());
            }
        }

        if (!childrenSids.isEmpty()) {
            childrenMaps.add(0, singletonOrder(this, childrenSids));
            return mergeOrderMapsExcl(childrenMaps);
        }

        return MapFact.EMPTYORDER();
    }

    public NavigatorElement getChildElement(String elementCanonicalName) {
        if (elementCanonicalName == null) {
            return null;
        }
        
        if (elementCanonicalName.equals(getCanonicalName())) return this;

        for (NavigatorElement child : getChildrenIt()) {
            NavigatorElement element = child.getChildElement(elementCanonicalName);
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

    public abstract boolean isLeafElement();

    public abstract byte getTypeID(); 

    public void setImage(String icon) {
        setImage(icon, null);
    }

    public final void setImage(String icon, DefaultIcon defaultIcon) {
        this.image = new ImageIcon(NavigatorElement.class.getResource(icon), icon.lastIndexOf("/") == -1 ? icon : icon.substring(icon.lastIndexOf("/") + 1));
        this.defaultIcon = defaultIcon;
    }

    public ImageIcon getImage() {
        return image;
    }

    public void setCreationPath(String creationPath) {
        this.creationPath = creationPath;
    }
   
    public void finalizeAroundInit() {
        parent.finalizeChanges();
        children.finalizeChanges();
    }

    @Override
    public String toString() {
        return getCanonicalName() + ": " + (caption != null ? ThreadLocalContext.localize(caption) : "");
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());

        outStream.writeInt(getID());
        SerializationUtil.writeString(outStream, canonicalName);
        SerializationUtil.writeString(outStream, creationPath);

        outStream.writeUTF(ThreadLocalContext.localize(caption));
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