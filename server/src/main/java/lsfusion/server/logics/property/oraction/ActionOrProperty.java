package lsfusion.server.logics.property.oraction;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.simple.EmptyOrderMap;
import lsfusion.base.col.implementations.simple.EmptyRevMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.LongMutable;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MOrderMap;
import lsfusion.base.comb.ListPermutations;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.PivotOptions;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFLazy;
import lsfusion.server.base.version.Version;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.action.session.changed.SessionProperty;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.event.ApplyGlobalEvent;
import lsfusion.server.logics.event.Link;
import lsfusion.server.logics.event.LinkType;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.ValueClassWrapper;
import lsfusion.server.logics.form.struct.group.AbstractNode;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyClassImplement;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.AlgType;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameParser;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameUtils;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.IntFunction;

import static lsfusion.interop.action.ServerResponse.*;
import static lsfusion.server.physics.admin.log.ServerLoggers.startLog;

public abstract class ActionOrProperty<T extends PropertyInterface> extends AbstractNode {
    public static final IntFunction<PropertyInterface> genInterface = PropertyInterface::new;

    private int ID = 0;
    protected String canonicalName;
    public String annotation;

    private boolean local = false;
    
    // вот отсюда идут свойства, которые отвечают за логику представлений и подставляются автоматически для PropertyDrawEntity и PropertyDrawView
    public LocalizedString caption; // assert not null

    public AppServerImage.Reader image;

    public void setImage(String imagePath) {
        this.image = AppServerImage.createPropertyImage(imagePath, AppServerImage.getAutoName(() -> caption, this::getName));
    }

    private String customRenderFunction;

    public String getCustomRenderFunction() {
        return customRenderFunction;
    }

    public void setCustomRenderFunction(String customRenderFunction) {
        this.customRenderFunction = customRenderFunction;
    }

    public void setSelect(String select) {
        setCustomRenderFunction(select == null || select.equals("No") ? null : ("select" + select));
    }

    public LocalizedString localizedToString() {
        LocalizedString result = LocalizedString.create(getSID());
        if (caption != null) {
            result = LocalizedString.concatList(result, " '", caption, "'");    
        }
        if (debugInfo != null) {
            result = LocalizedString.concat(result, " [" + debugInfo + "]");
        }
        return result;
    } 
    
    private String fullString;
    @ManualLazy
    public String toString() {
        if(fullString == null)
            fullString = calcToString();
        return fullString;
    }
    private String calcToString() {
        String result;
        if (canonicalName != null) {
            result = canonicalName;
        } else {
            String topName = getTopName();
            result = topName != null ? "at " + topName : getPID();
        }
        
        LocalizedString caption;
        if (this.caption != null && this.caption != LocalizedString.NONAME) {
            caption = this.caption;
        } else {
            caption = getTopCaption();
        }
        if (caption != null) {
            result += " '" + ThreadLocalContext.localize(caption) + "'";
        }

        if (debugInfo != null) {
            result += " [" + debugInfo + "]";
        }
        return result;
    }

    protected DebugInfo debugInfo;
    
    public abstract DebugInfo getDebugInfo();

    public boolean isField() {
        return false;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public ValueClass[] getInterfaceClasses(ImOrderSet<T> listInterfaces, ClassType classType) { // notification, load, lazy, dc, obsolete, в конструкторах при определении классов действий в основном
        return listInterfaces.mapList(getInterfaceClasses(classType)).toArray(new ValueClass[listInterfaces.size()]);
    }
    public abstract ImMap<T, ValueClass> getInterfaceClasses(ClassType type);

    public abstract boolean isInInterface(ImMap<T, ? extends AndClassSet> interfaceClasses, boolean isAny);

    public ActionOrProperty(LocalizedString caption, ImOrderSet<T> interfaces) {
        assert caption != null;
        this.ID = BaseLogicsModule.generateStaticNewID();
        this.caption = caption;
        this.interfaces = interfaces.getSet();
        this.orderInterfaces = interfaces;

        setContextMenuAction(ServerResponse.GROUP_CHANGE, LocalizedString.create("{logics.property.groupchange}"));
        setContextMenuAction(ServerResponse.EDIT_OBJECT, LocalizedString.create("{logics.property.editobject}"));

//        notFinalized.put(this, ExceptionUtils.getStackTrace());
    }

    public final ImSet<T> interfaces;
    private final ImOrderSet<T> orderInterfaces;
    protected ImOrderSet<T> getOrderInterfaces() {
        return orderInterfaces;
    }

    public int getInterfaceCount() {
        return interfaces.size();
    }
    
    public ImOrderSet<T> getReflectionOrderInterfaces() {
        return orderInterfaces;
    }
    
    public ImOrderSet<T> getFriendlyOrderInterfaces() { 
        return orderInterfaces; 
    }

    public Type getInterfaceType(T propertyInterface) {
        return getInterfaceType(propertyInterface, ClassType.materializeChangePolicy);
    }

    public Type getWhereInterfaceType(T propertyInterface) {
        return getInterfaceType(propertyInterface, ClassType.wherePolicy);
    }

    public Type getInterfaceType(T propertyInterface, ClassType classType) {
        ValueClass valueClass = getInterfaceClasses(classType).get(propertyInterface);
        return valueClass != null ? valueClass.getType() : null;
    }

    public abstract boolean isDrawNotNull();
    
    public String getName() {
        if (isNamed()) {
            return PropertyCanonicalNameParser.getName(canonicalName);
        }
        return null;
    }

    public static AppServerImage getDefaultImage(String name, AppServerImage.AutoName autoName, float rankingThreshold, boolean useDefaultIcon, ConnectionContext context) {
        return AppServerImage.createDefaultImage(rankingThreshold, name, AppServerImage.Style.PROPERTY, autoName,
                defaultContext -> useDefaultIcon ? AppServerImage.createPropertyImage(AppServerImage.ACTION, autoName).get(defaultContext) : null, context);
    }

    public String getNamespace() {
        if (isNamed()) {
            return PropertyCanonicalNameParser.getNamespace(canonicalName);
        }
        return null;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String namespace, String name, List<ResolveClassSet> signature, ImOrderSet<T> signatureOrder) {
        assert name != null && namespace != null;
        assert canonicalName == null;

        this.canonicalName = PropertyCanonicalNameUtils.createName(namespace, name, signature);

        setExplicitClasses(signatureOrder, signature);
    }

    final public boolean isNamed() {
        return canonicalName != null;
    }

    // для всех    
    private String mouseBinding;
    private Object keyBindings;
    private Object contextMenuBindings;
    private Object eventActions;

    public void setMouseAction(String actionSID) {
        setMouseBinding(actionSID);
    }

    public void setMouseBinding(String mouseBinding) {
        this.mouseBinding = mouseBinding;
    }

    public void setKeyAction(KeyStroke ks, String actionSID) {
        if (keyBindings == null) {
            keyBindings = MapFact.mMap(MapFact.override());
        }
        ((MMap<KeyStroke, String>)keyBindings).add(ks, actionSID);
    }

    public String getMouseBinding() {
        return mouseBinding;
    }

    public ImMap<KeyStroke, String> getKeyBindings() {
        return (ImMap<KeyStroke, String>)(keyBindings == null ? MapFact.EMPTY() : keyBindings);
    }

    @NFLazy
    public void setContextMenuAction(String actionSID, LocalizedString caption) {
        if (contextMenuBindings == null || contextMenuBindings instanceof EmptyOrderMap) {
            contextMenuBindings = MapFact.mOrderMap(MapFact.override());
        }
        ((MOrderMap<String, LocalizedString>)contextMenuBindings).add(actionSID, caption);
    }

    public ImOrderMap<String, LocalizedString> getContextMenuBindings() {
        return (ImOrderMap<String, LocalizedString>)(contextMenuBindings == null ? MapFact.EMPTYORDER() : contextMenuBindings);
    }

    @NFLazy
    public void setEventAction(String eventActionSID, ActionMapImplement<?, T> eventActionImplement) {
        if(eventActionSID.equals(CHANGE_WYS)) { // CHANGE_WYS, temp check
            startLog("WARNING! CHANGE_WYS is deprecated, use LIST clause in INPUT / DIALOG operator instead " + this);
            return;
        }

        if (eventActions == null || eventActions instanceof EmptyRevMap) {
            eventActions = MapFact.mMap(MapFact.override());
        }
        ((MMap<String, ActionMapImplement<?, T>>) eventActions).add(eventActionSID, eventActionImplement);
    }

    @LongMutable
    private ImMap<String, ActionMapImplement<?, T>> getEventActions() {
        return (ImMap<String, ActionMapImplement<?, T>>)(eventActions == null ? MapFact.EMPTY() : eventActions);
    }

    // actually protected (friend of PropertyMapImplement)
    public ActionMapImplement<?, T> getEventAction(String eventActionSID, FormSessionScope defaultChangeEventScope, ImList<Property> viewProperties, String customChangeFunction) {
        ActionMapImplement<?, T> eventAction = getExplicitEventAction(eventActionSID);
        if (eventAction != null)
            return eventAction;

        assert CHANGE.equals(eventActionSID) || EDIT_OBJECT.equals(eventActionSID); // explicit event actions can be also CONTEXTMENU
        return getDefaultEventAction(eventActionSID, defaultChangeEventScope, viewProperties, customChangeFunction);
    }

    public ActionMapImplement<?, T> getExplicitEventAction(String eventActionSID) {
        return getEventActions().get(eventActionSID);
    }

    public abstract ActionMapImplement<?, T> getDefaultEventAction(String eventActionSID, FormSessionScope defaultChangeEventScope, ImList<Property> viewProperties, String customChangeFunction);

    public boolean checkEquals() {
        return this instanceof Property;
    }

    public ImRevMap<T, T> getIdentityInterfaces() {
        return interfaces.toRevMap();
    }

    public boolean hasChild(ActionOrProperty prop) {
        return prop.equals(this);
    }

    public boolean hasNFChild(ActionOrProperty prop, Version version) {
        return hasChild(prop);
    }

    public ImOrderSet<ActionOrProperty> getActionOrProperties() {
        return SetFact.singletonOrder(this);
    }

    public ImList<ActionOrPropertyClassImplement> getActionOrProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses) {
        if(valueClasses.size() == 1) { // optimization primarily for DrillDown
            if(interfaces.size() == 1 && isInValueClassInterface(getOrderInterfaces(), valueClasses.toOrderSet()))
                return ListFact.singleton(createClassImplement(valueClasses.toOrderSet(), SetFact.singletonOrder(interfaces.single())));
            return ListFact.EMPTY();
        }

        return super.getActionOrProperties(valueClasses, mapClasses);
    }

    @Override
    public ImList<ActionOrPropertyClassImplement> calcActionOrProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses) {
        return getActionOrProperties(FormEntity.getSubsets(valueClasses));
    }

    private ImList<ActionOrPropertyClassImplement> getActionOrProperties(ImCol<ImSet<ValueClassWrapper>> classLists) {
        MList<ActionOrPropertyClassImplement> mResultList = ListFact.mList();
        for (ImSet<ValueClassWrapper> classes : classLists) {
            if (interfaces.size() == classes.size()) {
                final ImOrderSet<ValueClassWrapper> orderClasses = classes.toOrderSet();
                for (ImOrderSet<T> mapping : new ListPermutations<>(getOrderInterfaces())) {
                    if (isInValueClassInterface(mapping, orderClasses)) {
                        mResultList.add(createClassImplement(orderClasses, mapping));
                    }
                }
            }
        }
        return mResultList.immutableList();
    }

    private boolean isInValueClassInterface(ImOrderSet<T> mapping, ImOrderSet<ValueClassWrapper> orderClasses) {
        // if isAny true for very abstract classes (for example Object) there are too many props added, which has a really bad performance
        int classCount = 0;
        for(ValueClassWrapper orderClass : orderClasses) {
            ValueClass valueClass = orderClass.valueClass;
            if(valueClass instanceof CustomClass)
                classCount += ((CustomClass) valueClass).getAllChildren().size();
        }
        boolean isAny = false; //previously there was optimization isAny = classCount < 100;
        return isInInterface(mapping.mapOrderValues((i, value) -> orderClasses.get(i).valueClass.getUpSet()), isAny);
    }

    protected abstract ActionOrPropertyClassImplement<T, ?> createClassImplement(ImOrderSet<ValueClassWrapper> classes, ImOrderSet<T> mapping);

    public T getInterfaceById(int iID) {
        for (T inter : interfaces) {
            if (inter.getID() == iID) {
                return inter;
            }
        }

        return null;
    }

    protected boolean finalized = false;
    public void finalizeInit() {
        assert !finalized;
        finalized = true;
    }

//    private static ConcurrentHashMap<Property, String> notFinalized = new ConcurrentHashMap<Property, String>();

    private boolean finalizedChanges;

    protected void finalizeChanges() {
        eventActions = eventActions == null ? MapFact.EMPTY() : ((MMap) eventActions).immutable();
        keyBindings = keyBindings == null ? MapFact.EMPTY() : ((MMap)keyBindings).immutable();
        contextMenuBindings = contextMenuBindings == null ? MapFact.EMPTYORDER() : ((MOrderMap)contextMenuBindings).immutableOrder();
    }

    public void finalizeAroundInit() {
        super.finalizeAroundInit();

//        notFinalized.remove(this);
        // we need this synchronization since property finalization first marks modules, and only then reads all properties (so property can be finalized twice)
        // the same approach is used in FormEntity
        if(!finalizedChanges) {
            synchronized (this) { // in theory there can be separate lock, but for now there is no need for this
                if(!finalizedChanges) {
                    finalizeChanges();
                    finalizedChanges = true;
                }
            }
        }
    }

    public void prereadCaches() {
        getInterfaceClasses(ClassType.strictPolicy);
        getInterfaceClasses(ClassType.signaturePolicy);
    }

    protected abstract ImCol<Pair<ActionOrProperty<?>, LinkType>> calculateLinks(boolean events);

    public ImOrderSet<Link> links; // public for preread multithread optimization
    @ManualLazy
    public ImOrderSet<Link> getSortedLinks(boolean events) { // чисто для лексикографики
        if(links==null)
            links = calculateLinks(events).mapMergeSetValues(value -> new Link(ActionOrProperty.this, value.first, value.second)).sortSet(BusinessLogics.linkToComparator); // sorting for determenism, no need to cache because it's called once for each property
        return links;
    }
    public void dropLinks() {
        links = null;
    }
    public abstract ImSet<SessionProperty> getSessionCalcDepends(boolean events);

    public abstract ImSet<OldProperty> getParseOldDepends(); // именно так, а не через getSessionCalcDepends, так как может использоваться до инициализации логики

    public ImSet<OldProperty> getOldDepends() {
        // без событий, так как либо используется в глобальных событиях когда вычисляемые события \ удаления отдельно отрабатываются
        // в локальных же событиях вычисляемые и должны браться на начало сессии
        return getSessionCalcDepends(false).mapMergeSetValues(SessionProperty::getOldProperty);
    }

    // hack, that's why in separate methods
    public <V> ImRevMap<T, V> getMapInterfaces(final ImOrderSet<V> list) {
        return getOrderInterfaces().mapOrderRevValues((i, value) -> list.get(i));
    }
    public <V> ImRevMap<T, V> getMapInterfaces(final ImOrderSet<V> list, int offset) {
        ImOrderSet<T> orderInterfaces = getOrderInterfaces();
        return orderInterfaces.subOrder(offset, orderInterfaces.size()).mapOrderRevValues((i, value) -> list.get(i));
    }

    public boolean drillDownInNewSession() {
        return false;
    }

    protected static <T extends PropertyInterface> ImMap<T, ResolveClassSet> getPackedSignature(ImOrderSet<T> interfaces, List<ResolveClassSet> signature) {
        return interfaces.mapList(ListFact.fromJavaList(signature)).removeNulls();
    }

    public List<ResolveClassSet> getExplicitClasses(ImOrderSet<T> interfaces) {
        if(explicitClasses == null)
            return null;
        return interfaces.mapList(explicitClasses).toJavaList();
    }

    public void setExplicitClasses(ImOrderSet<T> interfaces, List<ResolveClassSet> signature) {
        this.explicitClasses = getPackedSignature(interfaces, signature);
    }
    
    public String getPID() {
        return "p" + ID;
    }
    
    public String getSID() {
        return canonicalName != null ? canonicalName : getPID(); 
    }
    
    public String getTopName() {
        if (debugInfo != null) {
            return debugInfo.getTopName();
        }
        return null;
    }
    
    public LocalizedString getTopCaption() {
        if (debugInfo != null) {
            return debugInfo.getTopCaption();
        }
        return null;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    protected ImMap<T, ResolveClassSet> explicitClasses; // без nulls

    protected interface Checker<V> {
        boolean checkEquals(V expl, V calc);
    }

    protected BaseLogicsModule getBaseLM() {
        return ThreadLocalContext.getBaseLM();
    }

    //
    protected static <T, V> ImMap<T, V> getExplicitCalcInterfaces(ImSet<T> interfaces, ImMap<T, V> explicitInterfaces, Callable<ImMap<T,V>> calcInterfaces, String caption, ActionOrProperty property, Checker<V> checker) {
        
        ImMap<T, V> inferred = null;
        if (explicitInterfaces != null)
            inferred = explicitInterfaces;

        if (inferred == null || inferred.size() < interfaces.size() || AlgType.checkExplicitInfer) {
            try {
                ImMap<T, V> calcInferred = calcInterfaces.call();
                if (calcInferred == null) {
                    return null;
                }
                if (inferred == null)
                    inferred = calcInferred;
                else {
                    if (AlgType.checkExplicitInfer) checkExplicitCalcInterfaces(checker, caption + property, inferred, calcInferred);
                    inferred = calcInferred.override(inferred); // тут возможно replaceValues достаточно, но не так просто оценить
                }
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return inferred;
    }

    private static  <T, V> boolean checkExplicitCalcInterfaces(Checker<V> checker, String caption, ImMap<T, V> inferred, ImMap<T, V> calcInferred) {
        for(int i=0, size = inferred.size(); i<size; i++) {
            T key = inferred.getKey(i);
            V calcValue = calcInferred.get(key);
            V inferValue = inferred.getValue(i);
            if((calcValue != null || inferValue != null) && (calcValue == null || inferValue == null || !checker.checkEquals(calcValue, inferValue))) {
                System.out.println(caption + ", CALC : " + calcInferred + ", INF : " + inferred);
                return false;
            }
        }
        return true;
    }

    public String getChangeExtSID() {
        return null;
    }

    public interface DefaultProcessor {
        // из-за inherit entity и view могут быть другого свойства
        void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity form, Version version);
        void proceedDefaultDesign(PropertyDrawView propertyView);
    }

    // + caption, который одновременно и draw и не draw
    public static class DrawOptions {
        
        // свойства, но пока реализовано как для всех
        private int charWidth;
        private Integer valueWidth;
        private Integer valueHeight;
        private Integer captionWidth;
        private Integer captionHeight;
        private Boolean valueFlex;

        // свойства, но пока реализовано как для всех
        private String regexp;
        private String regexpMessage;
        private Boolean echoSymbols;

        // действия, но пока реализовано как для всех
        private Boolean askConfirm;
        private String askConfirmMessage;

        // свойства, но пока реализовано как для всех
        private String eventID;

        // для всех
        private Compare defaultCompare;

        // для всех
        private KeyStroke changeKey;
        private Map<String, BindingMode> keyBindingsModes;
        private Integer changeKeyPriority;
        private Boolean showChangeKey;
        private String changeMouse;
        private Map<String, BindingMode> mouseBindingsModes;
        private Integer changeMousePriority;

        // для всех
        private ClassViewType viewType;
        private String customEditorFunction;
        private PivotOptions pivotOptions;

        private Boolean sticky;
        private Boolean sync;

        // для всех 
        private ImList<DefaultProcessor> processors = ListFact.EMPTY();
        
        public void proceedDefaultDraw(PropertyDrawEntity<?> entity, FormEntity form, Version version) {
            entity.viewType = viewType;
            entity.customChangeFunction = customEditorFunction;
            entity.askConfirm = BaseUtils.nvl(askConfirm, false);
            entity.askConfirmMessage = askConfirmMessage;
            entity.eventID = eventID;

            for(DefaultProcessor processor : processors)
                processor.proceedDefaultDraw(entity, form, version);
        }

        public void proceedDefaultDesign(PropertyDrawView propertyView) {
            if(propertyView.charWidth == 0)
                propertyView.setCharWidth(charWidth);
            if(propertyView.getValueFlex() == null)
                propertyView.setValueFlex(valueFlex);
            if(propertyView.valueWidth == null)
                propertyView.setValueWidth(valueWidth);
            if(propertyView.valueHeight == null)
                propertyView.setValueHeight(valueHeight);
            if(propertyView.captionWidth == null)
                propertyView.setCaptionWidth(captionWidth);
            if(propertyView.captionHeight == null)
                propertyView.setCaptionHeight(captionHeight);
            if (propertyView.changeKey == null)
                propertyView.changeKey = changeKey != null ? new KeyInputEvent(changeKey, keyBindingsModes) : null;
            if (propertyView.changeKeyPriority == null)
                propertyView.changeKeyPriority = changeKeyPriority;
            if (propertyView.showChangeKey == null)
                propertyView.showChangeKey = BaseUtils.nvl(showChangeKey, true);
            if (propertyView.changeMouse == null)
                propertyView.changeMouse = changeMouse != null ? new MouseInputEvent(changeMouse, mouseBindingsModes) : null;
            if (propertyView.changeMousePriority == null)
                propertyView.changeMousePriority = changeMousePriority;

            if (propertyView.echoSymbols == null)
                propertyView.echoSymbols = BaseUtils.nvl(echoSymbols, false);
            
            if(propertyView.defaultCompare == null)
                propertyView.defaultCompare = defaultCompare;

            if(propertyView.sticky == null)
                propertyView.sticky = sticky;
            if(propertyView.sync == null)
                propertyView.sync = sync;

            for(DefaultProcessor processor : processors)
                processor.proceedDefaultDesign(propertyView);
        }
        
        public void inheritDrawOptions(DrawOptions options) {
            if(charWidth == 0)
                setCharWidth(options.charWidth);

            if(defaultCompare == null)
                setDefaultCompare(options.defaultCompare);

            if(regexp == null)
                setRegexp(options.regexp);
            if(regexpMessage == null)
                setRegexpMessage(options.regexpMessage);
            if(echoSymbols == null)
                setEchoSymbols(options.echoSymbols);
            
            if(askConfirm == null)
                setAskConfirm(options.askConfirm);
            if(askConfirmMessage == null)
                setAskConfirmMessage(options.askConfirmMessage);
            
            if(eventID == null)
                setEventID(options.eventID);
            
            if(changeKey == null)
                setChangeKey(options.changeKey, options.keyBindingsModes);
            if(changeKeyPriority == null)
                setChangeKeyPriority(options.changeKeyPriority);
            if(showChangeKey == null)
                setShowChangeKey(options.showChangeKey);
            if(changeMouse == null)
                setChangeMouse(options.changeMouse, options.mouseBindingsModes);
            if(changeMousePriority == null)
                setChangeMousePriority(options.changeMousePriority);

            if(viewType == null)
                setViewType(options.viewType);
            if(pivotOptions == null)
                setPivotOptions(options.pivotOptions);
            if(sticky == null)
                setSticky(options.sticky);
            if(sync == null)
                setSync(options.sync);
            
            processors = options.processors.addList(processors);
        }

        // setters
        
        public void addProcessor(DefaultProcessor processor) {
            processors = processors.addList(processor);
        }

        public void setFlexCharWidth(int charWidth, Boolean flex) {
            setCharWidth(charWidth);
            setValueFlex(flex);
        }

        public Compare getDefaultCompare() {
            return defaultCompare;
        }

        public void setDefaultCompare(String defaultCompare) {
            this.defaultCompare = ActionOrPropertyUtils.stringToCompare(defaultCompare);
        }

        public void setDefaultCompare(Compare defaultCompare) {
            this.defaultCompare = defaultCompare;
        }


        public void setCharWidth(int charWidth) {
            this.charWidth = charWidth;
        }
        public void setValueFlex(Boolean flex) {
            this.valueFlex = flex;
        }

        public void setRegexp(String regexp) {
            this.regexp = regexp;
        }

        public void setRegexpMessage(String regexpMessage) {
            this.regexpMessage = regexpMessage;
        }

        public void setEchoSymbols(Boolean echoSymbols) {
            this.echoSymbols = echoSymbols;
        }

        public void setAskConfirm(Boolean askConfirm) {
            this.askConfirm = askConfirm;
        }

        public void setAskConfirmMessage(String askConfirmMessage) {
            this.askConfirmMessage = askConfirmMessage;
        }

        public void setEventID(String eventID) {
            this.eventID = eventID;
        }

        public void setChangeKey(KeyStroke changeKey) {
            setChangeKey(changeKey, null);
        }

        public void setChangeKey(KeyStroke changeKey, Map<String, BindingMode> bindingsModes) {
            this.changeKey = changeKey;
            this.keyBindingsModes = bindingsModes;
        }

        public void setChangeKeyPriority(Integer changeKeyPriority) {
            this.changeKeyPriority = changeKeyPriority;
        }

        public void setShowChangeKey(Boolean showChangeKey) {
            this.showChangeKey = showChangeKey;
        }

        public void setChangeMouse(String changeMouse) {
            this.changeMouse = changeMouse;
        }

        public void setChangeMouse(String changeMouse, Map<String, BindingMode> bindingsModes) {
            this.changeMouse = changeMouse;
            this.mouseBindingsModes = bindingsModes;
        }

        public void setChangeMousePriority(Integer changeMousePriority) {
            this.changeMousePriority = changeMousePriority;
        }

        public void setViewType(ClassViewType viewType) {
            this.viewType = viewType;
        }

        public void setCustomEditorFunction(String customEditorFunction) {
            this.customEditorFunction = customEditorFunction;
        }

        public void setPivotOptions(PivotOptions pivotOptions) {
            this.pivotOptions = pivotOptions;
        }

        public void setSticky(Boolean sticky) {
            this.sticky = sticky;
        }

        public void setSync(Boolean sync) {
            this.sync = sync;
        }
    }

    public DrawOptions drawOptions = new DrawOptions();
    
    protected ApplyGlobalEvent event;
    // важно кэшировать так как equals'ов пока нет, а они важны (в общем то только для Stored, и для RemoveClasses )
    public ApplyGlobalEvent getApplyEvent() {
        return null;        
    }

    protected boolean checkProps(ImCol<? extends PropertyInterfaceImplement<T>> col) {
        return col.filterCol(element -> !interfaces.containsAll(element.getInterfaces().toSet())).isEmpty();
    }
    protected boolean checkActions(ImCol<ActionMapImplement<?, T>> col) {
        return col.filterCol(element -> !interfaces.containsAll(element.mapping.valuesSet())).isEmpty();
    }
}
