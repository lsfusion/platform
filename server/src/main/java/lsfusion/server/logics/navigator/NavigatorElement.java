package lsfusion.server.logics.navigator;

import lsfusion.base.ResourceUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
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
import lsfusion.server.logics.navigator.window.NavigatorWindow;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;
import org.kordamp.ikonli.swing.FontIcon;
import org.kordamp.ikonli.fontawesome.FontAwesome;

import javax.swing.*;
import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static lsfusion.base.col.MapFact.mergeOrderMapsExcl;
import static lsfusion.base.col.MapFact.singletonOrder;

public abstract class NavigatorElement {

    public static final int DEFAULT_ICON_SIZE = 32;
    public static final Color DEFAULT_ICON_COLOR = Color.DARK_GRAY;

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

    /** Возвращает предков перед их потомками */
    public List<NavigatorElement> getOrderedChildrenList() {
        List<NavigatorElement> orderedList = new ArrayList<>();
        fillChildrenRecursive(orderedList);
        return orderedList;
    }

    /** Прямой обход (Pre-order traversal) дерева. Возвращает сначала предков, потом его потомков */
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
            return singletonOrder(this, Collections.emptyList());
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

    public void setImage(String icon) {
        setImage(icon, null);
    }

    public final void setImage(String imagePath, DefaultIcon defaultIcon) {
        ImageIcon image = null;

        //FontAwesome pack - pattern ^fa-[^\.]*
        if (imagePath.startsWith("fa-") && !imagePath.contains(".")) {
            FontIcon fi  = FontIcon.of(FontAwesome.findByDescription(imagePath), DEFAULT_ICON_SIZE, DEFAULT_ICON_COLOR);
            image = new ImageIcon(fi.getImage());
        } else {
            image = ResourceUtils.readImage(imagePath);
        }

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
    
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());

        SerializationUtil.writeString(outStream, canonicalName);
        SerializationUtil.writeString(outStream, getCreationPath());

        outStream.writeUTF(ThreadLocalContext.localize(caption));
        outStream.writeBoolean(hasChildren());
        if (window == null) {
            outStream.writeInt(WindowType.NULL_VIEW);
        } else {
            window.serialize(outStream);
        }

        IOUtils.writeImageIcon(outStream, imageHolder);
    }

    public void setDebugPoint(DebugInfo.DebugPoint debugPoint) {
        this.debugPoint = debugPoint;
    }
}