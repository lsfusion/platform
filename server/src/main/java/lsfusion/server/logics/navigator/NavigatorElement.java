package lsfusion.server.logics.navigator;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.server.base.AppServerImage;
import lsfusion.interop.form.remote.serialization.SerializationUtil;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFComplexOrderSet;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.form.interactive.action.async.AsyncExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncSerializer;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.navigator.window.NavigatorWindow;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static lsfusion.base.BaseUtils.nvl;
import static lsfusion.base.col.MapFact.mergeOrderMapsExcl;
import static lsfusion.base.col.MapFact.singletonOrder;
import static lsfusion.server.language.ScriptingLogicsModule.parseKeyStrokeOptions;

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
    public Property showIfProperty;
    public Supplier<LocalizedString> caption;

    public Property propertyImage;
    public AppServerImage.Reader image;
    public AppServerImage.Reader defaultImage;

    public Property propertyElementClass;
    public String elementClass;

    public InputBindingEvent changeKey;
    public Boolean showChangeKey;
    public InputBindingEvent changeMouse;
    public Boolean showChangeMouse;

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

    public String getCaption() {
        return ThreadLocalContext.localize(caption.get()); // can not be null, see createNavigatorElement (forms and actions always have name)
//        return LocalizedString.create(CanonicalNameUtils.getName(getCanonicalName()));
    }

    public AppServerImage getImage(ConnectionContext context) {
        if(this.image != null)
            return this.image.get(context);

        if(defaultImage != null)
            return defaultImage.get(context);

        return getDefaultImage(context);
    }

    public AppServerImage getDefaultImage(String name, float rankingThreshold, boolean useDefaultIcon, ConnectionContext context) {
        return AppServerImage.createDefaultImage(rankingThreshold, name, AppServerImage.Style.NAVIGATORELEMENT, AppServerImage.getAutoName(() -> caption.get(), this::getName), defaultContext -> useDefaultIcon ? AppServerImage.createNavigatorImage(getDefaultIcon(), NavigatorElement.this).get(defaultContext) : null, context);
    }

    private AppServerImage getDefaultImage(ConnectionContext context) {
        return getDefaultImage(AppServerImage.AUTO, Settings.get().getDefaultNavigatorImageRankingThreshold(), Settings.get().isDefaultNavigatorImage(), context);
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

    public abstract AsyncExec getAsyncExec(ConnectionContext context);

    public void setPropertyImage(Property imageProperty) {
        this.propertyImage = imageProperty;
    }

    public void setImage(String imagePath) {
        image = AppServerImage.createNavigatorImage(imagePath, this);
    }

    public void setPropertyElementClass(Property elementClassProperty) {
        this.propertyElementClass = elementClassProperty;
    }

    public void setElementClass(String elementClass) {
        this.elementClass = elementClass;
    }

    public void setHeaderProperty(Property headerProperty) {
        this.headerProperty = headerProperty;
    }

    public void setShowIfProperty(Property showIfProperty) {
        this.showIfProperty = showIfProperty;
    }

    public void setChangeKey(String code, boolean showChangeKey) {
        ScriptingLogicsModule.KeyStrokeOptions options = parseKeyStrokeOptions(code);
        KeyStroke changeKey = KeyStroke.getKeyStroke(options.keyStroke);
        this.changeKey = new InputBindingEvent(new KeyInputEvent(changeKey, options.bindingModesMap), options.priority);
        this.showChangeKey = showChangeKey;
    }

    public void setChangeMouse(String code, boolean showChangeMouse) {
        ScriptingLogicsModule.KeyStrokeOptions options = parseKeyStrokeOptions(code);
        this.changeMouse = new InputBindingEvent(new MouseInputEvent(options.keyStroke, options.bindingModesMap), options.priority);
        this.showChangeMouse = showChangeMouse;
    }

    public void finalizeAroundInit(BaseLogicsModule LM) {
        parent.finalizeChanges();
        children.finalizeChanges();
    }

    @Override
    public String toString() {
        String result = getCanonicalName();
        String caption = getCaption();
        if (caption != null) {
            result += " '" + caption + "'";
        }
        if (debugPoint != null) {
            result += " [" + debugPoint + "]"; 
        }
        return result;
    }

    public String getCreationPath() {
        return debugPoint != null ? debugPoint.toString() : "";
    }

    public String getPath() {
        return debugPoint != null ? debugPoint.path : "";
    }
    
    public void serialize(ConnectionContext context, DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());

        SerializationUtil.writeString(outStream, canonicalName);
        SerializationUtil.writeString(outStream, getCreationPath());
        SerializationUtil.writeString(outStream, getPath());

        outStream.writeUTF(getCaption());
        SerializationUtil.writeString(outStream, elementClass);
        outStream.writeBoolean(hasChildren());
        String windowCanonicalName = window != null ? window.getCanonicalName() : null;
        SerializationUtil.writeString(outStream, windowCanonicalName);
        if(windowCanonicalName != null) {
            outStream.writeBoolean(parentWindow);
        }

        BaseUtils.writeObject(outStream, changeKey);
        outStream.writeBoolean(nvl(showChangeKey, true));
        BaseUtils.writeObject(outStream, changeMouse);
        outStream.writeBoolean(nvl(showChangeMouse, true));

        AppServerImage.serialize(getImage(context), outStream);

        AsyncSerializer.serializeEventExec(getAsyncExec(context), context, outStream);
    }

    public void setDebugPoint(DebugInfo.DebugPoint debugPoint) {
        this.debugPoint = debugPoint;
    }
}