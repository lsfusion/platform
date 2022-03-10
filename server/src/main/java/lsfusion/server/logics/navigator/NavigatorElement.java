package lsfusion.server.logics.navigator;

import lsfusion.base.ResourceUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.file.IOUtils;
import lsfusion.base.file.SerializableImageIconHolder;
import lsfusion.interop.form.remote.serialization.SerializationUtil;
import lsfusion.interop.navigator.window.WindowType;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderSet;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.form.interactive.action.async.AsyncExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncSerializer;
import lsfusion.server.logics.navigator.window.NavigatorWindow;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import static lsfusion.base.col.MapFact.mergeOrderMapsExcl;
import static lsfusion.base.col.MapFact.singletonOrder;

public abstract class NavigatorElement {
    
    private SerializableImageIconHolder imageHolder;
    public DefaultIcon defaultIcon;

    public NavigatorWindow window = null;

    private final int ID;
    public LocalizedString caption;
    private final String canonicalName;
    private DebugInfo.DebugPoint debugPoint;

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
        return CanonicalNameUtils.getName(canonicalName); 
    }
    
    public boolean isNamed() {
        return canonicalName != null;
    }

    /** Не обновляет список потомков у parent'а */
    private void setParent(NavigatorElement parent, Version version) {
        this.parent.set(parent, version);
    }

    MList<NavigatorElement> lazyChildren;
    private ImList<NavigatorElement> getLazyChildren() {
        if(lazyChildren == null) {
            lazyChildren = ListFact.mList();
            for (NavigatorElement child : children.getList()) {
                if(child.getParent() == this)
                    lazyChildren.add(child);
            }
        }
        return lazyChildren.immutableList();
    }

    private Iterable<NavigatorElement> getChildrenIt() {
        return getLazyChildren();
    }
    
    public ImList<NavigatorElement> getChildren() {
        return getLazyChildren();
    }
    
    public ImList<NavigatorElement> getChildrenList() {
        return getLazyChildren();
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

    /** Прямой обход (Pre-order traversal) дерева. Возвращает сначала предков, потом его потомков */
    private void fillChildrenRecursive(Collection<NavigatorElement> result) {
        result.add(this);
        for (NavigatorElement child : getChildrenIt()) {
            child.fillChildrenRecursive(result);
        }
    }
    
    public ImOrderMap<NavigatorElement, List<String>> getChildrenMap(SecurityPolicy securityPolicy) {
        if (isLeafElement()) {
            //leaf element
            if(securityPolicy.checkNavigatorPermission(this)) {
                return singletonOrder(this, Collections.emptyList());
            } else {
                return MapFact.EMPTYORDER();
            }
        }

        List<String> childrenSids = new ArrayList<>();
        List<ImOrderMap<NavigatorElement, List<String>>> childrenMaps = new ArrayList<>();
        for (NavigatorElement child : getChildrenList()) {
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
        removeFromParent(comp, version);
        comp.setParent(this, version);
    }

    private void removeFromParent(NavigatorElement comp, Version version) {
        NavigatorElement container = comp.getNFParent(version);
        if (container != null) {
            assert container.children.containsNF(comp, version);
            container.children.remove(comp, version);
            comp.setParent(null, version);
        }
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

    public boolean hasChildren() {
        return !getChildren().isEmpty();
    }

    public abstract boolean isLeafElement();

    public abstract byte getTypeID();

    public abstract AsyncExec getAsyncExec();

    public void setImage(String icon) {
        setImage(icon, null);
    }

    public final void setImage(String imagePath, DefaultIcon defaultIcon) {
        ImageIcon image = ResourceUtils.readImage(imagePath);
        if (image != null) {
            imageHolder = new SerializableImageIconHolder(image, imagePath);
            this.defaultIcon = defaultIcon;
        }
    }

    public void finalizeAroundInit() {
        parent.finalizeChanges();
        children.finalizeChanges();
    }

    @Override
    public String toString() {
        String result = getCanonicalName();
        if (caption != null) {
            result += " '" + ThreadLocalContext.localize(caption) + "'";
        }
        if (debugPoint != null) {
            result += " [" + debugPoint + "]"; 
        }
        return result;
    }

    public String getCreationPath() {
        return debugPoint.toString();
    }

    public String getPath() {
        return debugPoint.path;
    }
    
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());

        SerializationUtil.writeString(outStream, canonicalName);
        SerializationUtil.writeString(outStream, getCreationPath());
        SerializationUtil.writeString(outStream, getPath());

        outStream.writeUTF(ThreadLocalContext.localize(caption));
        outStream.writeBoolean(hasChildren());
        if (window == null) {
            outStream.writeInt(WindowType.NULL_VIEW);
        } else {
            window.serialize(outStream);
        }

        IOUtils.writeImageIcon(outStream, imageHolder);

        AsyncSerializer.serializeEventExec(getAsyncExec(), outStream);
    }

    public void setDebugPoint(DebugInfo.DebugPoint debugPoint) {
        this.debugPoint = debugPoint;
    }
}