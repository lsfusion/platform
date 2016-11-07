package lsfusion.server.logics.property;

import com.google.common.base.Throwables;
import lsfusion.base.ListPermutations;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.simple.EmptyOrderMap;
import lsfusion.base.col.implementations.simple.EmptyRevMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddCol;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndexValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.lru.LRUSVSMap;
import lsfusion.base.col.lru.LRUUtil;
import lsfusion.interop.ClassViewType;
import lsfusion.server.Settings;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.classes.ActionClass;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.*;
import lsfusion.server.logics.debug.DebugInfo;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.NFLazy;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.property.actions.edit.DefaultChangeActionProperty;
import lsfusion.server.logics.property.group.AbstractGroup;
import lsfusion.server.logics.property.group.AbstractPropertyNode;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.PropertyChanges;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.Callable;

import static lsfusion.interop.form.ServerResponse.*;

public abstract class Property<T extends PropertyInterface> extends AbstractPropertyNode {
    public static final GetIndex<PropertyInterface> genInterface = new GetIndex<PropertyInterface>() {
        public PropertyInterface getMapValue(int i) {
            return new PropertyInterface(i);
        }};

    private int ID = 0;
    private String dbName;
    private String name;
    private String canonicalName;
    public String annotation;

    private boolean local = false;
    
    // вот отсюда идут свойства, которые отвечают за логику представлений и подставляются автоматически для PropertyDrawEntity и PropertyDrawView
    public LocalizedString caption;

    public String toString() {
        String result = ThreadLocalContext.localize(caption);
        if(canonicalName != null)
            result = result + " (" + canonicalName + ")";
        return result;
    }

    protected DebugInfo debugInfo;

    public boolean isField() {
        return false;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public Type getType() {
        ValueClass valueClass = getValueClass(ClassType.typePolicy);
        return valueClass != null ? valueClass.getType() : null;
    }

    public abstract ValueClass getValueClass(ClassType classType);

    public ValueClass[] getInterfaceClasses(ImOrderSet<T> listInterfaces, ClassType classType) { // notification, load, lazy, dc, obsolete, в конструкторах при определении классов действий в основном
        return listInterfaces.mapList(getInterfaceClasses(classType)).toArray(new ValueClass[listInterfaces.size()]);
    }
    public abstract ImMap<T, ValueClass> getInterfaceClasses(ClassType type);

    public abstract boolean isInInterface(ImMap<T, ? extends AndClassSet> interfaceClasses, boolean isAny);

    public Property(LocalizedString caption, ImOrderSet<T> interfaces) {
        this.ID = BaseLogicsModule.generateStaticNewID();
        this.caption = caption;
        this.interfaces = interfaces.getSet();
        this.orderInterfaces = interfaces;

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
    
    public ImOrderSet<T> getFriendlyPropertyOrderInterfaces() { 
        return orderInterfaces; 
    }

    public static Modifier defaultModifier = new Modifier() {
        public PropertyChanges getPropertyChanges() {
            return PropertyChanges.EMPTY;
        }
    };

    @IdentityLazy
    public Type getInterfaceType(T propertyInterface) {
        return getInterfaceClasses(ClassType.materializeChangePolicy).get(propertyInterface).getType();
    }

    public abstract boolean isSetNotNull();

    public String getDBName() {
        return dbName;
    }

    public String getName() {
        return name;
    }

    public boolean cached = false;

    // для всех    
    private String mouseBinding;
    private Object keyBindings;
    private Object contextMenuBindings;
    private Object editActions;

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
    public void setEditAction(String editActionSID, ActionPropertyMapImplement<?, T> editActionImplement) {
        if (editActions == null || editActions instanceof EmptyRevMap) {
            editActions = MapFact.mMap(MapFact.override());
        }
        ((MMap<String, ActionPropertyMapImplement<?, T>>)editActions).add(editActionSID, editActionImplement);
    }

    @LongMutable
    private ImMap<String, ActionPropertyMapImplement<?, T>> getEditActions() {
        return (ImMap<String, ActionPropertyMapImplement<?, T>>)(editActions == null ? MapFact.EMPTY() : editActions);
    }

    public ActionPropertyMapImplement<?, T> getEditAction(String editActionSID) {
        return getEditAction(editActionSID, null);
    }

    public ActionPropertyMapImplement<?, T> getEditAction(String editActionSID, CalcProperty filterProperty) {
        ActionPropertyMapImplement<?, T> editAction = getEditActions().get(editActionSID);
        if (editAction != null) {
            return editAction;
        }

        if (GROUP_CHANGE.equals(editActionSID)) {
            //будем определять на уровне PropertyDraw
            assert false;
        } else if (CHANGE_WYS.equals(editActionSID)) {
//            возвращаем дефолт
        }

        return getDefaultEditAction(editActionSID, filterProperty);
    }
    
    public boolean ignoreReadOnlyPolicy() {
        return false;    
    }

    public boolean isChangeWYSOverriden() {
        return getEditActions().containsKey(CHANGE_WYS);
    }

    public boolean isEditObjectActionDefined() {
        if (getEditActions().containsKey(EDIT_OBJECT)) {
            return true;
        }

        ActionPropertyMapImplement<?, T> editObjectAction = getDefaultEditAction(EDIT_OBJECT, null);
        if (editObjectAction != null && editObjectAction.property instanceof DefaultChangeActionProperty) {
            DefaultChangeActionProperty defaultEditAction = (DefaultChangeActionProperty) editObjectAction.property;
            return defaultEditAction.getImplementType() instanceof ObjectType;
        }

        return false;
    }

    public abstract ActionPropertyMapImplement<?, T> getDefaultEditAction(String editActionSID, CalcProperty filterProperty);

    public boolean checkEquals() {
        return this instanceof CalcProperty;
    }

    public ImRevMap<T, T> getIdentityInterfaces() {
        return interfaces.toRevMap();
    }

    public boolean hasChild(Property prop) {
        return prop.equals(this);
    }

    public boolean hasNFChild(Property prop, Version version) {
        return hasChild(prop);
    }
    
    public ImOrderSet<Property> getProperties() {
        return SetFact.singletonOrder((Property) this);
    }
    
    public static void cleanPropCaches() {
        hashProps.clear();
    }

    private static class CacheEntry {
        private final ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses;
        private final boolean useObjSets;
        private final boolean anyInInterface;
        
        private ImList<PropertyClassImplement> result;
        
        public CacheEntry(ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, boolean useObjSets, boolean anyInInterface) {
            this.mapClasses = mapClasses;
            this.useObjSets = useObjSets;
            this.anyInInterface = anyInInterface;
        }

        public ImRevMap<ValueClassWrapper, ValueClassWrapper> map(CacheEntry entry) {
            if(!(useObjSets == entry.useObjSets && anyInInterface == entry.anyInInterface && mapClasses.size() == entry.mapClasses.size()))
                return null;

            MRevMap<ValueClassWrapper, ValueClassWrapper> mResult = MapFact.mRevMap();
            for(int i=0,size=mapClasses.size();i<size;i++) {
                ImSet<ValueClassWrapper> wrappers = mapClasses.getValue(i);
                ImSet<ValueClassWrapper> entryWrappers = entry.mapClasses.get(mapClasses.getKey(i));
                if(entryWrappers == null || wrappers.size() != entryWrappers.size())
                    return null;
                for(int j=0,sizeJ=wrappers.size();j<sizeJ;j++)
                    mResult.revAdd(wrappers.get(j), entryWrappers.get(j));
            }
            return mResult.immutableRev();
        }
        
        public int hash() {
            int result = 0;
            for(int i=0,size=mapClasses.size();i<size;i++) {
                result += mapClasses.getKey(i).hashCode() ^ mapClasses.getValue(i).size();
            }
            
            return 31 * ( 31 * result + (useObjSets ? 1 : 0)) + (anyInInterface ? 1 : 0); 
        }
    }    
    final static LRUSVSMap<Integer, MAddCol<CacheEntry>> hashProps = new LRUSVSMap<>(LRUUtil.G2);

    // вся оптимизация в общем то для drillDown
    protected ImList<PropertyClassImplement> getProperties(ImSet<ValueClassWrapper> valueClasses, ImMap<ValueClass, ImSet<ValueClassWrapper>> mapClasses, boolean useObjSubsets, boolean anyInInterface, Version version) {
        if(valueClasses.size() == 1) { // доп оптимизация для DrillDown
            if(interfaces.size() == 1 && isInInterface(MapFact.singleton(interfaces.single(), valueClasses.single().valueClass.getUpSet()), anyInInterface))
                return ListFact.<PropertyClassImplement>singleton(createClassImplement(valueClasses.toOrderSet(), SetFact.singletonOrder(interfaces.single())));
            return ListFact.EMPTY();
        }            
            
        CacheEntry entry = new CacheEntry(mapClasses, useObjSubsets, anyInInterface); // кэширование
        int hash = entry.hash();
        MAddCol<CacheEntry> col = hashProps.get(hash);
        if(col == null) {
            col = ListFact.mAddCol();
            hashProps.put(hash, col);                    
        } else {
            synchronized (col) {
                for (CacheEntry cachedEntry : col.it()) {
                    final ImRevMap<ValueClassWrapper, ValueClassWrapper> map = cachedEntry.map(entry);
                    if (map != null) {
                        return cachedEntry.result.mapListValues(new GetValue<PropertyClassImplement, PropertyClassImplement>() {
                            public PropertyClassImplement getMapValue(PropertyClassImplement value) {
                                return value.map(map);
                            }
                        });
                    }
                }
            }
        }
        
        ImList<PropertyClassImplement> result = getProperties(FormEntity.getSubsets(valueClasses, useObjSubsets), anyInInterface); 
        
        entry.result = result;
        synchronized (col) {
            col.add(entry);
        }
        
        return result;
    }
    
    private ImList<PropertyClassImplement> getProperties(ImCol<ImSet<ValueClassWrapper>> classLists, boolean anyInInterface) {
        MList<PropertyClassImplement> mResultList = ListFact.mList();
        for (ImSet<ValueClassWrapper> classes : classLists) {
            if (interfaces.size() == classes.size()) {
                final ImOrderSet<ValueClassWrapper> orderClasses = classes.toOrderSet();
                for (ImOrderSet<T> mapping : new ListPermutations<>(getOrderInterfaces())) {
                    ImMap<T, AndClassSet> propertyInterface = mapping.mapOrderValues(new GetIndexValue<AndClassSet, T>() {
                        public AndClassSet getMapValue(int i, T value) {
                            return orderClasses.get(i).valueClass.getUpSet();
                        }});
                    if (isInInterface(propertyInterface, anyInInterface)) {
                        mResultList.add(createClassImplement(orderClasses, mapping));
                    }
                }
            }
        }
        return mResultList.immutableList();
    }
    
    protected abstract PropertyClassImplement<T, ?> createClassImplement(ImOrderSet<ValueClassWrapper> classes, ImOrderSet<T> mapping);

    public T getInterfaceById(int iID) {
        for (T inter : interfaces) {
            if (inter.getID() == iID) {
                return inter;
            }
        }

        return null;
    }

    @Override
    public List<AbstractGroup> fillGroups(List<AbstractGroup> groupsList) {
        return groupsList;
    }

    protected boolean finalized = false;
    public void finalizeInit() {
        assert !finalized;
        finalized = true;
    }

//    private static ConcurrentHashMap<Property, String> notFinalized = new ConcurrentHashMap<Property, String>();

    public void finalizeAroundInit() {
        super.finalizeAroundInit();

//        notFinalized.remove(this);
        
        editActions = editActions == null ? MapFact.EMPTY() : ((MMap)editActions).immutable();
        keyBindings = keyBindings == null ? MapFact.EMPTY() : ((MMap)keyBindings).immutable();
        contextMenuBindings = contextMenuBindings == null ? MapFact.EMPTYORDER() : ((MOrderMap)contextMenuBindings).immutableOrder();
    }

    public void prereadCaches() {
        getInterfaceClasses(ClassType.strictPolicy);
        getInterfaceClasses(ClassType.signaturePolicy);
    }

    protected abstract ImCol<Pair<Property<?>, LinkType>> calculateLinks(boolean events);

    private ImSet<Link> links;
    @ManualLazy
    public ImSet<Link> getLinks(boolean events) { // чисто для лексикографики
        if(links==null) {
            links = calculateLinks(events).mapMergeSetValues(new GetValue<Link, Pair<Property<?>, LinkType>>() {
                public Link getMapValue(Pair<Property<?>, LinkType> value) {
                    return new Link(Property.this, value.first, value.second);
                }});
        }
        return links;
    }
    public void dropLinks() {
        links = null;
    }
    public abstract ImSet<SessionCalcProperty> getSessionCalcDepends(boolean events);

    public abstract ImSet<OldProperty> getParseOldDepends(); // именно так, а не через getSessionCalcDepends, так как может использоваться до инициализации логики

    public ImSet<OldProperty> getOldDepends() {
        // без событий, так как либо используется в глобальных событиях когда вычисляемые события \ удаления отдельно отрабатываются
        // в локальных же событиях вычисляемые и должны браться на начало сессии
        return getSessionCalcDepends(false).mapMergeSetValues(new GetValue<OldProperty, SessionCalcProperty>() {
            public OldProperty getMapValue(SessionCalcProperty value) {
                return value.getOldProperty();
            }});
    }

    // не сильно структурно поэтому вынесено в метод
    public <V> ImRevMap<T, V> getMapInterfaces(final ImOrderSet<V> list) {
        return getOrderInterfaces().mapOrderRevValues(new GetIndexValue<V, T>() {
            public V getMapValue(int i, T value) {
                return list.get(i);
            }
        });
    }

    public boolean drillDownInNewSession() {
        return false;
    }

    public Property showDep; // assert что не null когда events не isEmpty

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName, PropertyDBNamePolicy policy) {
        this.canonicalName = canonicalName;
        try {
            this.name = PropertyCanonicalNameParser.getName(canonicalName);
        } catch (AbstractPropertyNameParser.ParseException e) {
            Throwables.propagate(e);
        }
        this.dbName = policy.transformToDBName(canonicalName);
    }
    
    final public boolean isNamed() {
        return canonicalName != null;
    }
    
    public void setCanonicalName(String namespace, String name, List<ResolveClassSet> signature, ImOrderSet<T> signatureOrder, PropertyDBNamePolicy policy) {
        this.name = name;
        this.canonicalName = PropertyCanonicalNameUtils.createName(namespace, name, signature);
        this.dbName = policy.transformToDBName(canonicalName);

        setExplicitClasses(signatureOrder, signature);
    }

    protected static <T extends PropertyInterface> ImMap<T, ResolveClassSet> getPackedSignature(ImOrderSet<T> interfaces, List<ResolveClassSet> signature) {
        return interfaces.mapList(ListFact.fromJavaList(signature)).removeNulls();
    }

    public void setExplicitClasses(ImOrderSet<T> interfaces, List<ResolveClassSet> signature) {
        this.explicitClasses = getPackedSignature(interfaces, signature);
    }
    
    public String getSID() {
        return canonicalName != null ? canonicalName : ("p" + ID); 
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

    //
    protected static <T, V> ImMap<T, V> getExplicitCalcInterfaces(ImSet<T> interfaces, ImMap<T, V> explicitInterfaces, Callable<ImMap<T,V>> calcInterfaces, String caption, Property property, Checker<V> checker) {
        
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

    public void inheritCaption(Property property) {
        caption = property.caption;         
    }
    
    public interface DefaultProcessor {
        // из-за inherit entity и view могут быть другого свойства
        void proceedDefaultDraw(PropertyDrawEntity entity, FormEntity<?> form);
        void proceedDefaultDesign(PropertyDrawView propertyView);
    }

    // + caption, который одновременно и draw и не draw
    public static class DrawOptions {
        
        // свойства, но пока реализовано как для всех
        private int minimumCharWidth;
        private int maximumCharWidth;
        private int preferredCharWidth;

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
        private ImageIcon image;
        private String iconPath;

        // для всех
        private KeyStroke editKey;
        private Boolean showEditKey;

        // для всех
        private boolean drawToToolbar;

        // для всех
        private Boolean shouldBeLast;

        // для всех
        private ClassViewType forceViewType;
        
        // для всех 
        private ImList<DefaultProcessor> processors = ListFact.EMPTY();
        
        public void proceedDefaultDraw(PropertyDrawEntity<?> entity, FormEntity<?> form) {
            if (shouldBeLast != null)
                entity.shouldBeLast = shouldBeLast;
            if (forceViewType != null)
                entity.forceViewType = forceViewType;
            if (askConfirm != null)
                entity.askConfirm = askConfirm;
            if (askConfirmMessage != null)
                entity.askConfirmMessage = askConfirmMessage;
            if (eventID != null)
                entity.eventID = eventID;
            if (drawToToolbar)
                entity.setDrawToToolbar(true);
            for(DefaultProcessor processor : processors)
                processor.proceedDefaultDraw(entity, form);
        }

        public void proceedDefaultDesign(PropertyDrawView propertyView) {
            if(propertyView.getType() instanceof LogicalClass)
                propertyView.editOnSingleClick = Settings.get().getEditLogicalOnSingleClick();
            if(propertyView.getType() instanceof ActionClass)
                propertyView.editOnSingleClick = Settings.get().getEditActionOnSingleClick();

            if(minimumCharWidth != 0)
                propertyView.setMinimumCharWidth(minimumCharWidth);
            if(maximumCharWidth != 0)
                propertyView.setMaximumCharWidth(maximumCharWidth);
            if(preferredCharWidth != 0)
                propertyView.setPreferredCharWidth(preferredCharWidth);
            if (iconPath != null) {
                propertyView.design.imagePath = iconPath;
                propertyView.design.setImage(image);
            }
            if (editKey != null)
                propertyView.editKey = editKey;
            if (showEditKey != null)
                propertyView.showEditKey = showEditKey;
            if (regexp != null)
                propertyView.regexp = regexp;
            if (regexpMessage != null)
                propertyView.regexpMessage = regexpMessage;
            if (echoSymbols != null)
                propertyView.echoSymbols = echoSymbols;
            for(DefaultProcessor processor : processors)
                processor.proceedDefaultDesign(propertyView);
        }
        
        public void inheritDrawOptions(DrawOptions options) {
            setMinimumCharWidth(options.minimumCharWidth);
            setMaximumCharWidth(options.maximumCharWidth);
            setPreferredCharWidth(options.preferredCharWidth);

            setImage(options.image);
            setIconPath(options.iconPath);
            
            setRegexp(options.regexp);
            setRegexpMessage(options.regexpMessage);
            setEchoSymbols(options.echoSymbols);
            
            setAskConfirm(options.askConfirm);
            setAskConfirmMessage(options.askConfirmMessage);
            
            setEventID(options.eventID);
            
            setEditKey(options.editKey);
            setShowEditKey(options.showEditKey);
            
            setDrawToToolbar(options.drawToToolbar);
            
            setShouldBeLast(options.shouldBeLast);
            
            setForceViewType(options.forceViewType);
            
            processors = options.processors.addList(processors);
        }

        // setters
        
        public void addProcessor(DefaultProcessor processor) {
            processors = processors.addList(processor);
        }

        public void setFixedCharWidth(int charWidth) {
            setMinimumCharWidth(charWidth);
            setMaximumCharWidth(charWidth);
            setPreferredCharWidth(charWidth);
        }

        public void setImage(String iconPath) {
            this.setIconPath(iconPath);
            setImage(new ImageIcon(Property.class.getResource("/images/" + iconPath)));
        }


        public void setMinimumCharWidth(int minimumCharWidth) {
            this.minimumCharWidth = minimumCharWidth;
        }

        public void setMaximumCharWidth(int maximumCharWidth) {
            this.maximumCharWidth = maximumCharWidth;
        }

        public void setPreferredCharWidth(int preferredCharWidth) {
            this.preferredCharWidth = preferredCharWidth;
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

        public void setImage(ImageIcon image) {
            this.image = image;
        }

        public void setIconPath(String iconPath) {
            this.iconPath = iconPath;
        }

        public void setEditKey(KeyStroke editKey) {
            this.editKey = editKey;
        }

        public void setShowEditKey(Boolean showEditKey) {
            this.showEditKey = showEditKey;
        }

        public void setDrawToToolbar(boolean drawToToolbar) {
            this.drawToToolbar = drawToToolbar;
        }

        public void setShouldBeLast(Boolean shouldBeLast) {
            this.shouldBeLast = shouldBeLast;
        }

        public void setForceViewType(ClassViewType forceViewType) {
            this.forceViewType = forceViewType;
        }
    }

    public DrawOptions drawOptions = new DrawOptions();
}
