package lsfusion.server.logics.navigator;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.base.AppServerImage;
import lsfusion.interop.form.remote.serialization.SerializationUtil;
import lsfusion.interop.navigator.window.WindowType;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFComplexOrderSet;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.form.interactive.action.async.AsyncExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncSerializer;
import lsfusion.server.logics.navigator.window.NavigatorWindow;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static lsfusion.base.col.MapFact.mergeOrderMapsExcl;
import static lsfusion.base.col.MapFact.singletonOrder;

public abstract class NavigatorElement {

    public void addOrMove(NavigatorElement element, ComplexLocation location, Version version) {
        removeFromParent(element, version);
        children.add(element, location, version);

        element.setParent(this, version);
    }

    public abstract String getDefaultIcon();

    public NavigatorWindow window = null;
    public boolean parentWindow;

    private final int ID;

    // need supplier to have relevant form captions
    public Property headerProperty;
    public Supplier<LocalizedString> caption;

    public Property propertyImage;
    public Supplier<AppServerImage> image;

    public String elementClass;

    private final String canonicalName;
    private DebugInfo.DebugPoint debugPoint;

    private NFProperty<NavigatorElement> parent = NFFact.property();
    private NFComplexOrderSet<NavigatorElement> children = NFFact.complexOrderSet();

    protected NavigatorElement(String canonicalName) {
        assert canonicalName != null;
        this.canonicalName = canonicalName;
        this.ID = BaseLogicsModule.generateStaticNewID();
    }

    public boolean isParentRoot() {
        NavigatorElement parent = getParent();
        return parent == null || parent.getParent() == null;
    }

    public LocalizedString getCaption() {
        return caption.get(); // can not be null, see createNavigatorElement (forms and actions always have name)
//        return LocalizedString.create(CanonicalNameUtils.getName(getCanonicalName()));
    }

    public AppServerImage getImage() {
        AppServerImage image = this.image.get();
        if(image != null)
            return image;

        return getDefaultImage();
    }

    public AppServerImage getDefaultImage(float rankingThreshold, boolean useDefaultIcon) {
        return AppServerImage.createDefaultImage(rankingThreshold, getName(), () -> useDefaultIcon ? AppServerImage.createNavigatorImage(getDefaultIcon(), NavigatorElement.this) : null);
    }

    private AppServerImage getDefaultImage() {
        return getDefaultImage(Settings.get().getDefaultNavigatorImageRankingThreshold(), Settings.get().isDefaultNavigatorImage());
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

    ImList<NavigatorElement> lazyChildren;
    private ImList<NavigatorElement> getLazyChildren() {
        if (lazyChildren == null) {
            lazyChildren = children.getList().filterList(child -> child.getParent() == NavigatorElement.this);
        }
        return lazyChildren;
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

    private void removeFromParent(NavigatorElement comp, Version version) {
        NavigatorElement container = comp.getNFParent(version);
        if (container != null) {
            container.children.remove(comp, version);
            comp.setParent(null, version);
        }
    }

    public boolean hasChildren() {
        return !getChildren().isEmpty();
    }

    public abstract boolean isLeafElement();

    public abstract byte getTypeID();

    public abstract AsyncExec getAsyncExec();

    public void setPropertyImage(Property imageProperty) {
        this.propertyImage = imageProperty;
    }

    public void setImage(String imagePath) {
        AppServerImage image = AppServerImage.createNavigatorImage(imagePath, this);
        this.image = () -> image;
    }

    public void setHeaderProperty(Property headerProperty) {
        this.headerProperty = headerProperty;
    }

    public void finalizeAroundInit(BaseLogicsModule LM) {
        parent.finalizeChanges();
        children.finalizeChanges();
    }

    @Override
    public String toString() {
        String result = getCanonicalName();
        String caption = ThreadLocalContext.localize(getCaption());
        if (caption != null) {
            result += " '" + caption + "'";
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

        outStream.writeUTF(ThreadLocalContext.localize(getCaption()));
        SerializationUtil.writeString(outStream, elementClass);
        outStream.writeBoolean(hasChildren());
        if (window == null) {
            outStream.writeInt(WindowType.NULL_VIEW);
        } else {
            window.serialize(outStream);
            outStream.writeBoolean(parentWindow);
        }

        AppServerImage.serialize(getImage(), outStream);

        AsyncSerializer.serializeEventExec(getAsyncExec(), outStream);
    }

    public void setDebugPoint(DebugInfo.DebugPoint debugPoint) {
        this.debugPoint = debugPoint;
    }
}