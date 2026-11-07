package lsfusion.server.logics.navigator;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.server.base.AppServerImage;
import lsfusion.interop.form.remote.serialization.SerializationUtil;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFComplexOrderSet;
import lsfusion.server.base.version.interfaces.NFProperty;
import lsfusion.server.language.converters.KeyStrokeConverter;
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

public abstract class NavigatorElement {

    public void addOrMove(NavigatorElement element, ComplexLocation location, Version version) {
        removeFromParent(element, version);
        children.add(element, location, version);

        element.setParent(this, version);
    }

    public abstract String getDefaultIcon();

    private final int ID;

    // the options below are set by statements of modules (NAVIGATOR { element ... }), each by the version of its module :
    // modules that do not require each other are parsed at once, and of two of them setting one option of an element the
    // module later in the module order wins, as when they were parsed one after the other. The pairs a statement sets
    // together come so from one statement
    private final NFProperty<NavigatorWindow> window = NFFact.property();
    private final NFProperty<Boolean> parentWindow = NFFact.property();

    private final NFProperty<Property> headerProperty = NFFact.property();
    private final NFProperty<Property> showIfProperty = NFFact.property();

    // what draws the element, null where it is not said: Custom.NOCUSTOM, the platform's standard button, not a custom
    // view's drawing - so a window a React component draws places the button with <Platform name/> instead of drawing
    // the element, which is then not projected, the same way a form's component with `custom = NULL` is not. In any
    // other window the platform draws every element anyway
    private final NFProperty<String> custom = NFFact.property();
    // need supplier to have relevant form captions
    private final NFProperty<Supplier<LocalizedString>> caption = NFFact.property();

    private final NFProperty<Property> propertyImage = NFFact.property();
    private final NFProperty<AppServerImage.Reader> image = NFFact.property();
    public AppServerImage.Reader defaultImage; // the element's own module's, set as it is made

    private final NFProperty<Property> propertyElementClass = NFFact.property();
    private final NFProperty<String> elementClass = NFFact.property();

    private final NFProperty<InputBindingEvent> changeKey = NFFact.property();
    private final NFProperty<Boolean> showChangeKey = NFFact.property();
    private final NFProperty<InputBindingEvent> changeMouse = NFFact.property();
    private final NFProperty<Boolean> showChangeMouse = NFFact.property();

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
        return ThreadLocalContext.localize(caption.get().get()); // can not be null, see createNavigatorElement (forms and actions always have name)
//        return LocalizedString.create(CanonicalNameUtils.getName(getCanonicalName()));
    }

    public AppServerImage getImage(ConnectionContext context) {
        AppServerImage.Reader image = this.image.get();
        if(image != null)
            return image.get(context);

        if(defaultImage != null)
            return defaultImage.get(context);

        return getDefaultImage(context);
    }

    public AppServerImage getDefaultImage(String name, float rankingThreshold, boolean useDefaultIcon, ConnectionContext context) {
        return AppServerImage.createDefaultImage(rankingThreshold, name, AppServerImage.Style.NAVIGATORELEMENT, AppServerImage.getAutoName(() -> caption.get().get(), this::getName), defaultContext -> useDefaultIcon ? AppServerImage.createNavigatorImage(getDefaultIcon(), NavigatorElement.this).get(defaultContext) : null, context);
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

    // the window this element itself is drawn in - WINDOW names the window of the CHILDREN, so it is the nearest
    // ancestor's, unless PARENT put the element into its own. GNavigatorElement.getDrawWindow is the client's copy
    public NavigatorWindow getDrawWindow() {
        if (isParentWindow())
            return getWindow();

        NavigatorElement parent = getParent();
        if (parent == null)
            return null;
        return parent.getWindow() != null ? parent.getWindow() : parent.getDrawWindow();
    }

    // A folder placed directly in the top NAVIGATOR (System.root, a horizontal toolbar) must specify a WINDOW for its children
    public boolean isTopFolderWithoutWindow() {
        return !isLeafElement() && isParentRoot() && getWindow() == null;
    }

    public NavigatorWindow getWindow() {
        return window.get();
    }

    public boolean isParentWindow() {
        return nvl(parentWindow.get(), false);
    }

    public Property getHeaderProperty() {
        return headerProperty.get();
    }

    public Property getShowIfProperty() {
        return showIfProperty.get();
    }

    public String getCustom() {
        return custom.get();
    }

    public Property getPropertyImage() {
        return propertyImage.get();
    }

    public Property getPropertyElementClass() {
        return propertyElementClass.get();
    }

    public abstract boolean isLeafElement();

    public abstract byte getTypeID();

    public abstract AsyncExec getAsyncExec(ConnectionContext context);

    public void setCaption(Supplier<LocalizedString> caption, Version version) {
        this.caption.set(caption, version);
    }

    public void setWindow(NavigatorWindow window, boolean parentWindow, Version version) {
        this.window.set(window, version);
        this.parentWindow.set(parentWindow, version);
    }

    // a path (AppServerImage.AUTO, NULL or a file), and the property computing the image, if any
    public void setImage(String imagePath, Property imageProperty, Version version) {
        this.propertyImage.set(imageProperty, version);
        this.image.set(AppServerImage.createNavigatorImage(imagePath, this), version);
    }

    public void setPropertyElementClass(Property elementClassProperty, Version version) {
        this.propertyElementClass.set(elementClassProperty, version);
    }

    public void setElementClass(String elementClass, Version version) {
        this.elementClass.set(elementClass, version);
    }

    public void setHeaderProperty(Property headerProperty, Version version) {
        this.headerProperty.set(headerProperty, version);
    }

    public void setShowIfProperty(Property showIfProperty, Version version) {
        this.showIfProperty.set(showIfProperty, version);
    }

    public void setCustom(String custom, Version version) {
        this.custom.set(custom, version);
    }

    public void setChangeKey(String code, boolean showChangeKey, Version version) {
        this.changeKey.set(KeyStrokeConverter.parseInputBindingEvent(code, false), version);
        this.showChangeKey.set(showChangeKey, version);
    }

    public void setChangeMouse(String code, boolean showChangeMouse, Version version) {
        this.changeMouse.set(KeyStrokeConverter.parseInputBindingEvent(code, true), version);
        this.showChangeMouse.set(showChangeMouse, version);
    }

    public void finalizeAroundInit(BaseLogicsModule LM) {
        parent.finalizeChanges();
        children.finalizeChanges();
        for (NFProperty<?> option : Arrays.asList(window, parentWindow, headerProperty, showIfProperty, custom, caption, propertyImage,
                image, propertyElementClass, elementClass, changeKey, showChangeKey, changeMouse, showChangeMouse))
            option.finalizeChanges();
    }

    @Override
    // the caption read as it is, not finalized : an element can be printed while the modules are still parsed - in a
    // message, a debugger -, and a final read then would refuse a later module's caption
    public String toString() {
        String result = getCanonicalName();
        Supplier<LocalizedString> captionSupplier = this.caption.getNF(Version.current(), true);
        String caption = captionSupplier != null ? ThreadLocalContext.localize(captionSupplier.get()) : null;
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
        SerializationUtil.writeString(outStream, elementClass.get());
        outStream.writeBoolean(hasChildren());
        NavigatorWindow window = getWindow();
        String windowCanonicalName = window != null ? window.getCanonicalName() : null;
        SerializationUtil.writeString(outStream, windowCanonicalName);
        if(windowCanonicalName != null) {
            outStream.writeBoolean(isParentWindow());
        }

        BaseUtils.writeObject(outStream, changeKey.get());
        outStream.writeBoolean(nvl(showChangeKey.get(), true));
        BaseUtils.writeObject(outStream, changeMouse.get());
        outStream.writeBoolean(nvl(showChangeMouse.get(), true));

        outStream.writeBoolean(getCustom() != null);

        AppServerImage.serialize(getImage(context), outStream);

        AsyncSerializer.serializeEventExec(getAsyncExec(context), context, outStream);
    }

    public void setDebugPoint(DebugInfo.DebugPoint debugPoint) {
        this.debugPoint = debugPoint;
    }
}