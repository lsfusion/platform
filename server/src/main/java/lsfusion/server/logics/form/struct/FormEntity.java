package lsfusion.server.logics.form.struct;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.LongMutable;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterRevValueMap;
import lsfusion.base.comb.Subsets;
import lsfusion.base.dnf.AddSet;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.ModalityType;
import lsfusion.interop.form.event.FormEventType;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.*;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptParsingException;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.LogicsModule.InsertType;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.action.async.AsyncAddRemove;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.input.InputFilterEntity;
import lsfusion.server.logics.form.interactive.action.lifecycle.FormToolbarAction;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.stat.FormGroupHierarchyCreator;
import lsfusion.server.logics.form.stat.GroupObjectHierarchy;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.filter.*;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyClassImplement;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;
import lsfusion.server.physics.dev.property.IsDevProperty;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static lsfusion.interop.action.ServerResponse.CHANGE;

public class FormEntity implements FormSelector<ObjectEntity> {
    private final static Logger logger = Logger.getLogger(FormEntity.class);
    
    public static Boolean DEFAULT_NOCANCEL = null;

    public static final IsDevProperty isDev = IsDevProperty.instance;
    public static final SessionDataProperty isDocked = new SessionDataProperty(LocalizedString.create("Is docked"), LogicalClass.instance);
    public static final SessionDataProperty isEditing = new SessionDataProperty(LocalizedString.create("Is embedded"), LogicalClass.instance);
    public static final SessionDataProperty showOk = new SessionDataProperty(LocalizedString.create("Is modal"), LogicalClass.instance);
    public static final SessionDataProperty isAdd = new SessionDataProperty(LocalizedString.create("Is add"), LogicalClass.instance);
    public static final SessionDataProperty manageSession = new SessionDataProperty(LocalizedString.create("Manage session"), LogicalClass.instance);
    public static final SessionDataProperty isExternal = new SessionDataProperty(LocalizedString.create("Is external"), LogicalClass.instance);
    public static final SessionDataProperty showDrop = new SessionDataProperty(LocalizedString.create("Show drop"), LogicalClass.instance);

    public PropertyDrawEntity printActionPropertyDraw;
    public PropertyDrawEntity editActionPropertyDraw;
    public PropertyDrawEntity xlsActionPropertyDraw;
    public PropertyDrawEntity dropActionPropertyDraw;
    public PropertyDrawEntity refreshActionPropertyDraw;
    public PropertyDrawEntity applyActionPropertyDraw;
    public PropertyDrawEntity cancelActionPropertyDraw;
    public PropertyDrawEntity okActionPropertyDraw;
    public PropertyDrawEntity closeActionPropertyDraw;

    public PropertyDrawEntity logMessagePropertyDraw;

    private int ID;
    
    private String canonicalName;
    private LocalizedString caption;
    private DebugInfo.DebugPoint debugPoint; 

    private String defaultImagePath;
    
    public NFMapList<Object, ActionObjectEntity<?>> eventActions = NFFact.mapList();
    public ImMap<Object, ImList<ActionObjectEntity<?>>> getEventActions() {
        return eventActions.getOrderMap();
    }
    public Iterable<ActionObjectEntity<?>> getEventActionsListIt(Object eventObject) {
        return eventActions.getListIt(eventObject);
    }

    private NFOrderSet<GroupObjectEntity> groups = NFFact.orderSet(true); // для script'ов, findObjectEntity в FORM / EMAIL objects
    public Iterable<GroupObjectEntity> getGroupsIt() {
        return groups.getIt();
    }
    public Iterable<GroupObjectEntity> getNFGroupsIt(Version version) { // не finalized
        return groups.getNFIt(version);
    }
    public ImSet<GroupObjectEntity> getGroups() {
        return groups.getSet();
    }
    public ImOrderSet<GroupObjectEntity> getGroupsList() {
        return groups.getOrderSet();
    }
    public Iterable<GroupObjectEntity> getNFGroupsListIt(Version version) {
        return groups.getNFListIt(version);
    }
    
    private NFSet<TreeGroupEntity> treeGroups = NFFact.set();
    public Iterable<TreeGroupEntity> getTreeGroupsIt() {
        return treeGroups.getIt();
    }
    public Iterable<TreeGroupEntity> getNFTreeGroupsIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return treeGroups.getNFIt(version);
    }    
    
    private NFOrderSet<PropertyDrawEntity> propertyDraws = NFFact.orderSet();
    public Iterable<PropertyDrawEntity> getPropertyDrawsIt() {
        return propertyDraws.getIt();
    }
    public Iterable<PropertyDrawEntity> getNFPropertyDrawsIt(Version version) {
        return propertyDraws.getNFIt(version);
    }
    public ImOrderSet<PropertyDrawEntity> getPropertyDrawsList() {
        return propertyDraws.getOrderSet();        
    }
    public Iterable<PropertyDrawEntity> getPropertyDrawsListIt() {
        return propertyDraws.getListIt();        
    }
    public Iterable<PropertyDrawEntity> getNFPropertyDrawsListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return propertyDraws.getNFListIt(version);
    }
    
    private NFSet<FilterEntity> fixedFilters = NFFact.set();
    public ImSet<FilterEntity> getFixedFilters() {
        return fixedFilters.getSet();
    }
    
    private NFOrderSet<RegularFilterGroupEntity> regularFilterGroups = NFFact.orderSet();
    public Iterable<RegularFilterGroupEntity> getRegularFilterGroupsIt() {
        return regularFilterGroups.getIt();
    }
    public ImOrderSet<RegularFilterGroupEntity> getRegularFilterGroupsList() {
        return regularFilterGroups.getOrderSet();
    }
    public Iterable<RegularFilterGroupEntity> getNFRegularFilterGroupsIt(Version version) {
        return regularFilterGroups.getNFIt(version);        
    }
    public Iterable<RegularFilterGroupEntity> getNFRegularFilterGroupsListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return regularFilterGroups.getNFListIt(version);
    }

    public ImSet<FilterEntity> getDefaultRegularFilters() {
        return getRegularFilterGroupsList().filterOrder(element -> element.getDefault() >= 0)
                .mapMergeOrderSetValues(entity -> entity.filters.getOrderSet().get(entity.getDefault()).filter).getSet();
    }

    public NFList<PropertyDrawEntity> userFilters = NFFact.list();
    public Iterable<PropertyDrawEntity> getUserFiltersIt(Version version) {
        return userFilters.getNFListIt(version);
    }

    private NFOrderMap<PropertyDrawEntity<?>,Boolean> defaultOrders = NFFact.orderMap();
    public ImOrderMap<PropertyDrawEntity<?>,Boolean> getDefaultOrdersList() {
        return defaultOrders.getListMap();
    }
    public Boolean getNFDefaultOrder(PropertyDrawEntity<?> entity, Version version) {
        return defaultOrders.getNFValue(entity, version);
    }
    
    private NFOrderMap<OrderEntity<?>,Boolean> fixedOrders = NFFact.orderMap();
    public ImOrderMap<OrderEntity<?>,Boolean> getFixedOrdersList() {
        return fixedOrders.getListMap();
    }

    private NFOrderSet<ImList<PropertyDrawEntity>> pivotColumns = NFFact.orderSet();
    private NFOrderSet<ImList<PropertyDrawEntity>> pivotRows = NFFact.orderSet();
    private NFOrderSet<PropertyDrawEntity> pivotMeasures = NFFact.orderSet();

    public Iterable<ImList<PropertyDrawEntity>> getNFPivotColumnsListIt(Version version) {
        return pivotColumns.getNFListIt(version);
    }

    public ImList<ImList<PropertyDrawEntity>> getPivotColumnsList() {
        return pivotColumns.getList();
    }

    public Iterable<ImList<PropertyDrawEntity>> getNFPivotRowsListIt(Version version) {
        return pivotRows.getNFListIt(version);
    }

    public ImList<ImList<PropertyDrawEntity>> getPivotRowsList() {
        return pivotRows.getList();
    }

    public Iterable<PropertyDrawEntity> getNFPivotMeasuresListIt(Version version) {
        return pivotMeasures.getNFListIt(version);
    }

    public ImList<PropertyDrawEntity> getPivotMeasuresList() {
        return pivotMeasures.getList();
    }

    @IdentityLazy
    public ImMap<GroupObjectEntity, ImSet<PropertyDrawEntity>> getPivotGroupProps() {
        MSet<PropertyDrawEntity> mGroupProps = SetFact.mSet();
        for(ImList<PropertyDrawEntity> pivotRow : getPivotRowsList())
            mGroupProps.addAll(pivotRow.toOrderSet().getSet());
        for(ImList<PropertyDrawEntity> pivotColumn : getPivotColumnsList())
            mGroupProps.addAll(pivotColumn.toOrderSet().getSet());
        return mGroupProps.immutable().group(key -> key.getToDraw(this));
    }
    @IdentityLazy
    public ImMap<GroupObjectEntity, ImSet<PropertyDrawEntity>> getPivotMeasureProps() {
        return getPivotMeasuresList().toOrderSet().getSet().group(key -> key.getToDraw(this));
    }

    public ModalityType modalityType = ModalityType.DOCKED;
    public int autoRefresh = 0;
    public boolean localAsync = false;

    public PropertyObjectEntity<?> reportPathProp;

    public FormEntity(String canonicalName, DebugInfo.DebugPoint debugPoint, LocalizedString caption, String imagePath, Version version) {
        this.ID = BaseLogicsModule.generateStaticNewID();
        this.caption = caption;
        this.canonicalName = canonicalName;
        this.debugPoint = debugPoint;
        
        this.defaultImagePath = imagePath;
        
        logger.debug("Initializing form " + ThreadLocalContext.localize(caption) + "...");

        initDefaultElements(version);
    }

    public void initDefaultElements(Version version) {
        BaseLogicsModule baseLM = ThreadLocalContext.getBaseLM();

        LA<PropertyInterface> formOk = baseLM.getFormOk();
        LA<PropertyInterface> formClose = baseLM.getFormClose();
        LA<PropertyInterface> formApplied = baseLM.getFormApplied();

        editActionPropertyDraw = addPropertyDraw(baseLM.getFormEditReport(), version);
        refreshActionPropertyDraw = addPropertyDraw(baseLM.getFormRefresh(), version);
        applyActionPropertyDraw = addPropertyDraw(baseLM.getFormApply(), version);
        cancelActionPropertyDraw = addPropertyDraw(baseLM.getFormCancel(), version);
        okActionPropertyDraw = addPropertyDraw(formOk, version);
        closeActionPropertyDraw = addPropertyDraw(formClose, version);
        dropActionPropertyDraw = addPropertyDraw(baseLM.getFormDrop(), version);

        logMessagePropertyDraw = addPropertyDraw(baseLM.getLogMessage(), version);
        logMessagePropertyDraw.setPropertyExtra(addPropertyObject(externalShowIf), PropertyDrawExtraType.SHOWIF);

        addActionsOnEvent(FormEventType.AFTERAPPLY, false, version, new ActionObjectEntity<>(formApplied.action, MapFact.EMPTYREV()));
        addActionsOnEvent(FormEventType.QUERYOK, true, version, new ActionObjectEntity<>(formOk.action, MapFact.EMPTYREV()));
        addActionsOnEvent(FormEventType.QUERYCLOSE, true, version, new ActionObjectEntity<>(formClose.action, MapFact.EMPTYREV()));
    }

    private void initDefaultGroupElements(GroupObjectEntity group, Version version) {
        if(group.viewType.isList() && !group.isInTree()) {
            BaseLogicsModule baseLM = ThreadLocalContext.getBaseLM();

            PropertyDrawEntity propertyDraw = addPropertyDraw(baseLM.count, version);
            propertyDraw.setToDraw(group);
            propertyDraw.setIntegrationSID(null); // we want to exclude this property from all integrations / apis / reports (use only in interactive view)
            propertyDraw.setPropertyExtra(addPropertyObject(baseLM.addJProp(baseLM.isPivot, new LP(group.getListViewType(baseLM.listViewType).property))), PropertyDrawExtraType.SHOWIF);
            propertyDraw.ignoreHasHeaders = true;

            addPropertyDrawView(propertyDraw, false, version); // because it's called after form constructor
        }
    }

    public void finalizeInit(Version version) {
//        getNFRichDesign(version);
        setRichDesign(createDefaultRichDesign(version), version);
    }

    private static LP externalShowIf = FormToolbarAction.createIfProperty(new Property[]{FormEntity.isExternal}, new boolean[]{false});

    public void addFixedFilter(FilterEntity filter, Version version) {
        fixedFilters.add(filter, version);
    }

    public void addFixedOrder(OrderEntity order, boolean descending, Version version) {
        fixedOrders.add(order, descending, version);
    }

    public void addRegularFilterGroup(RegularFilterGroupEntity group, Version version) {
        regularFilterGroups.add(group, version);
        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null)
            richDesign.addRegularFilterGroup(group, version);
    }
    
    public void addRegularFilter(RegularFilterGroupEntity filterGroup, RegularFilterEntity filter, boolean isDefault, Version version) {
        filterGroup.addFilter(filter, isDefault, version);
        
        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null)
            richDesign.addRegularFilter(filterGroup, filter, version);
    }

    public int genID() {
        return BaseLogicsModule.generateStaticNewID();
    }

    public GroupObjectEntity getGroupObject(int id) {
        for (GroupObjectEntity group : getGroupsIt()) {
            if (group.getID() == id) {
                return group;
            }
        }

        return null;
    }

    public GroupObjectEntity getGroupObject(String sID) {
        for (GroupObjectEntity group : getGroupsIt()) {
            if (group.getSID().equals(sID)) {
                return group;
            }
        }
        return null;
    }

    public GroupObjectEntity getGroupObjectIntegration(String sID) {
        for (GroupObjectEntity group : getGroupsIt()) {
            if (group.getIntegrationSID().equals(sID)) {
                return group;
            }
        }
        return null;
    }

    public GroupObjectEntity getNFGroupObject(String sID, Version version) {
        for (GroupObjectEntity group : getNFGroupsIt(version)) {
            if (group.getSID().equals(sID)) {
                return group;
            }
        }
        return null;
    }

    public TreeGroupEntity getTreeGroup(int id) {
        for (TreeGroupEntity treeGroup : getTreeGroupsIt()) {
            if (treeGroup.getID() == id) {
                return treeGroup;
            }
        }

        return null;
    }

    public ObjectEntity getObject(int id) {
        for (GroupObjectEntity group : getGroupsIt()) {
            for (ObjectEntity object : group.getObjects()) {
                if (object.getID() == id) {
                    return object;
                }
            }
        }
        return null;
    }

    public ObjectEntity getObject(String sid) {
        for (GroupObjectEntity group : getGroupsIt()) {
            for (ObjectEntity object : group.getObjects()) {
                if (object.getSID().equals(sid)) {
                    return object;
                }
            }
        }
        return null;
    }

    public ObjectEntity getNFObject(String sid, Version version) {
        for (GroupObjectEntity group : getNFGroupsIt(version)) {
            for (ObjectEntity object : group.getObjects()) {
                if (object.getSID().equals(sid)) {
                    return object;
                }
            }
        }
        return null;
    }

    public ObjectEntity getNFObject(ValueClass cls, Version version) {
        for (GroupObjectEntity group : getNFGroupsListIt(version)) { // для детерменированности
            for (ObjectEntity object : group.getObjects()) {
                if (cls.equals(object.baseClass)) {
                    return object;
                }
            }
        }
        return null;
    }

    public List<String> getNFObjectsNamesAndClasses(List<ValueClass> classes, Version version) {
        List<String> names = new ArrayList<>();
        classes.clear();
        
        for (GroupObjectEntity group : getNFGroupsIt(version)) {
            for (ObjectEntity object : group.getObjects()) {
                names.add(object.getSID());
                classes.add(object.baseClass);
            }
        }
        return names;
    }

    public RegularFilterGroupEntity getRegularFilterGroup(int id) {
        for (RegularFilterGroupEntity filterGroup : getRegularFilterGroupsIt()) {
            if (filterGroup.getID() == id) {
                return filterGroup;
            }
        }

        return null;
    }

    public RegularFilterGroupEntity getNFRegularFilterGroup(String sid, Version version) {
        if (sid == null) {
            return null;
        }

        for (RegularFilterGroupEntity filterGroup : getNFRegularFilterGroupsIt(version)) {
            if (sid.equals(filterGroup.getSID())) {
                return filterGroup;
            }
        }

        return null;
    }

    public ImMap<GroupObjectEntity, ImSet<FilterEntity>> getGroupFixedFilters(final ImSet<GroupObjectEntity> excludeGroupObjects) {
        return getGroupFixedFilters(excludeGroupObjects, false);
    }
    @IdentityLazy
    public ImMap<GroupObjectEntity, ImSet<FilterEntity>> getGroupFixedFilters(final ImSet<GroupObjectEntity> excludeGroupObjects, boolean includeDefaultRegular) {
        ImSet<FilterEntity> filters = getFixedFilters();
        if(includeDefaultRegular)
            filters = filters.merge(getDefaultRegularFilters());
        return getGroupFilters(filters, excludeGroupObjects);
    }

    public <T extends FilterEntityInstance> ImMap<GroupObjectEntity, ImSet<T>> getGroupFilters(ImSet<T> filters, ImSet<GroupObjectEntity> excludeGroupObjects) {
        return filters.group(key -> {
            GroupObjectEntity applyObject = key.getApplyObject(FormEntity.this, excludeGroupObjects);
            return applyObject == null ? GroupObjectEntity.NULL : applyObject;
        });
    }
    // in theory upper method could be used but we can't do that with inheritance because of O generic type
    public <P extends PropertyInterface> ImMap<GroupObjectEntity, ImSet<ContextFilterEntity<?, P, ObjectEntity>>> getGroupContextFilters(ImSet<ContextFilterEntity<?, P, ObjectEntity>> filters) {
        return filters.group(key -> {
            GroupObjectEntity applyObject = getApplyObject(key.getObjects(), SetFact.EMPTY());
            return applyObject == null ? GroupObjectEntity.NULL : applyObject;
        });
    }

    @IdentityLazy
    public ImMap<GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> getGroupProperties(final ImSet<GroupObjectEntity> excludeGroupObjects, final boolean supportGroupColumns) {
        return getStaticPropertyDrawsList().groupOrder(key -> {
            GroupObjectEntity applyObject = key.getApplyObject(FormEntity.this, excludeGroupObjects, supportGroupColumns);
            return applyObject == null ? GroupObjectEntity.NULL : applyObject;
        });
    }

    public <P extends PropertyInterface> InputFilterEntity<?, P> getInputFilterEntity(ObjectEntity object, ImSet<ContextFilterEntity<?, P, ObjectEntity>> contextFilters, ImRevMap<ObjectEntity, P> mapObjects) {
        assert object.baseClass instanceof CustomClass;
        GroupObjectEntity groupObject = object.groupTo;
        assert groupObject.getObjects().size() == 1;

        mapObjects = mapObjects.removeRev(object);

        ImSet<FilterEntity> filters = getGroupFixedFilters(SetFact.EMPTY(), true).get(groupObject);
        if(filters == null)
            filters = SetFact.EMPTY();
        ImSet<? extends ContextFilterEntity<?, P, ObjectEntity>> contextFilterEntities = filters.mapSetValues((FilterEntity filterEntity) -> ((FilterEntity<?>) filterEntity).getContext());

        ImSet<ContextFilterEntity<?, P, ObjectEntity>> contextGroupFilters = getGroupContextFilters(contextFilters).get(groupObject);
        if(contextGroupFilters == null)
            contextGroupFilters = SetFact.EMPTY();

        return groupObject.getInputFilterEntity(SetFact.addExclSet(contextFilterEntities, contextGroupFilters), mapObjects);
    }

    // correlated with FormGroupHierarchyCreator.addDependenciesToGraph
//    @IdentityLazy
//    public ImSet<GroupObjectEntity> getGroupToColumns() {
//        // first will leave only groups without properties
//        MSet<GroupObjectEntity> mGroupToColumns = SetFact.mSet();
//        for(PropertyDrawEntity<?> property : getStaticPropertyDrawsList())
//            mGroupToColumns.addAll(property.getObjectInstances().group(o -> o.groupTo).keys().removeIncl(property.getColumnGroupObjects().getSet()));
//        ImSet<GroupObjectEntity> groupToColumns = mGroupToColumns.immutable();
//
//        ImOrderSet<GroupObjectEntity> groupList = getGroupsList();
//        boolean stop = false;
//        while(!stop) {
//            stop = true;
//            for (FilterEntity<?> fixedFilter : getFixedFilters()) {
//                ImOrderSet<GroupObjectEntity> objects = groupList.filterOrderIncl(fixedFilter.getObjects().group(o -> o.groupTo).keys());
//                int maxNotGroup = 0;
//                for(int i = objects.size() - 1; i >= 0; i--) {
//                    if(!groupToColumns.contains(objects.get(i))) {
//                        maxNotGroup = i;
//                        break;
//                    }
//                }
//
//                if(maxNotGroup > 0) {
//                    int prevSize = groupToColumns.size();
//                    groupToColumns = groupToColumns.remove(objects.subOrder(0, maxNotGroup - 1).getSet());
//                    if(groupToColumns.size() < prevSize) { // if changed groupToColumns need to recheck filters once again
//                        stop = false;
//                        break;
//                    }
//                }
//            }
//        }
//        return groupToColumns;
//    }

    @IdentityLazy
    public ImMap<GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> getAllGroupProperties(final ImSet<GroupObjectEntity> excludeGroupObjects, final boolean supportGroupColumns) {
        return ((ImOrderSet<PropertyDrawEntity>)getPropertyDrawsList()).groupOrder(key -> {
            GroupObjectEntity applyObject = key.getApplyObject(FormEntity.this, excludeGroupObjects, supportGroupColumns);
            return applyObject == null ? GroupObjectEntity.NULL : applyObject;
        });
    }

    @IdentityLazy
    public ImSet<GroupObjectEntity> getAllGroupColumns() {
        return getPropertyDrawsList().getCol().mapMergeSetSetValues(propertyDraw -> propertyDraw.getColumnGroupObjects().getSet());
    }

    public ImOrderSet<PropertyDrawEntity> getStaticPropertyDrawsList() {
        return ((ImOrderSet<PropertyDrawEntity>)getPropertyDrawsList()).filterOrder(element -> element.isProperty() && element.getIntegrationSID() != null && element != logMessagePropertyDraw);
    }

    public static class PropMetaExternal {
        private final String caption;
        private final String type;
        public final Boolean newDelete;

        public PropMetaExternal(String caption, String type, Boolean newDelete) {
            this.caption = caption;
            this.type = type;
            this.newDelete = newDelete;
        }

        public JSONObject serialize() {
            JSONObject propData = new JSONObject();
            propData.put("caption", caption);
            propData.put("type", type);
            propData.put("newDelete", newDelete);
            return propData;
        }
    }

    public static class GroupMetaExternal {
        public final ImMap<PropertyDrawEntity, PropMetaExternal> props;

        public GroupMetaExternal(ImMap<PropertyDrawEntity, PropMetaExternal> props) {
            this.props = props;
        }

        public JSONObject serialize() {
            JSONObject groupData = new JSONObject();
            for(int i=0,size=props.size();i<size;i++) {
                String integrationSID = props.getKey(i).getIntegrationSID();
                if (integrationSID != null) {
                    groupData.put(integrationSID, props.getValue(i).serialize());
                }
            }
            return groupData;
        }
    }

    public static class MetaExternal {
        public final ImMap<GroupObjectEntity, GroupMetaExternal> groups;

        public MetaExternal(ImMap<GroupObjectEntity, GroupMetaExternal> groups) {
            this.groups = groups;
        }

        public JSONObject serialize() {
            JSONObject result = new JSONObject();
            for(int i=0,size=groups.size();i<size;i++)
                result.put(groups.getKey(i).getIntegrationSID(), groups.getValue(i).serialize());
            return result;
        }
    }

    public MetaExternal getMetaExternal(final SecurityPolicy policy) {
        final ImMap<GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> groupProperties = getAllGroupProperties(SetFact.EMPTY(), false);

        return new MetaExternal(getGroups().mapValues((GroupObjectEntity group) -> {
            ImOrderSet<PropertyDrawEntity> properties = groupProperties.get(group);
            if(properties == null)
                properties = SetFact.EMPTYORDER();

            return new GroupMetaExternal(properties.getSet().mapValues((PropertyDrawEntity property) ->  {
                    AsyncEventExec asyncEventExec = ((PropertyDrawEntity<?>) property).getAsyncEventExec(FormEntity.this, policy, CHANGE, true);
                    Boolean newDelete = asyncEventExec instanceof AsyncAddRemove ? ((AsyncAddRemove) asyncEventExec).add : null;
                    return new PropMetaExternal(ThreadLocalContext.localize(property.getCaption()), property.isProperty() ? property.getType().getJSONType() : "action", newDelete);
                }));
        }));
    }

    @IdentityLazy
    public ImMap<GroupObjectEntity, ImOrderMap<OrderEntity, Boolean>> getGroupOrdersList(final ImSet<GroupObjectEntity> excludeGroupObjects) {
        return BaseUtils.immutableCast(getDefaultOrdersList().mapOrderKeyValues((Function<PropertyDrawEntity<?>, OrderEntity<?>>) PropertyDrawEntity::getOrder, value -> !value).mergeOrder(getFixedOrdersList()).groupOrder(new BaseUtils.Group<GroupObjectEntity, OrderEntity<?>>() {
            @Override
            public GroupObjectEntity group(OrderEntity<?> key) {
                GroupObjectEntity groupObject = key.getApplyObject(FormEntity.this, excludeGroupObjects);
                if(groupObject == null)
                    return GroupObjectEntity.NULL;
                return groupObject;
            }
        }));
    }

    public RegularFilterEntity getRegularFilter(int id) {
        for (RegularFilterGroupEntity filterGroup : getRegularFilterGroupsIt()) {
            for (RegularFilterEntity filter : filterGroup.getFiltersList()) {
                if (filter.getID() == id) {
                    return filter;
                }
            }
        }

        return null;
    }

    @IdentityLazy
    public boolean hasNoChange() {
        for (PropertyDrawEntity property : getPropertyDrawsIt()) {
            ActionObjectEntity<?> eventAction = property.getEventAction(CHANGE, this, new SecurityPolicy()); // in theory it is possible to support securityPolicy, but in this case we have to drag it through hasFlow + do some complex caching
            if (eventAction != null && eventAction.property.hasFlow(ChangeFlowType.FORMCHANGE) && !eventAction.property.endsWithApplyAndNoChangesAfterBreaksBefore())
                return false;
        }

        return true;
    }

    public ObjectEntity addSingleGroupObject(ValueClass baseClass, Version version, Object... groups) {
        GroupObjectEntity groupObject = new GroupObjectEntity(genID(), (TreeGroupEntity) null);
        ObjectEntity object = new ObjectEntity(genID(), baseClass, baseClass != null ? baseClass.getCaption() : LocalizedString.NONAME, baseClass == null);
        groupObject.add(object);
        addGroupObject(groupObject, version);

        return object;
    }

    public TreeGroupEntity addTreeGroupObject(TreeGroupEntity treeGroup, GroupObjectEntity neighbour, InsertType insertType, String sID, Version version, GroupObjectEntity... tGroups) {
        if (sID != null)
            treeGroup.setSID(sID);
        for (GroupObjectEntity group : tGroups) {
            if(!groups.containsNF(group, version))
                groups.add(group, version);
            treeGroup.add(group);
        }

        treeGroups.add(treeGroup, version);

        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null)
            richDesign.addTreeGroup(treeGroup, neighbour, insertType, version);

        return treeGroup;
    }

    public void addGroupObject(GroupObjectEntity group, GroupObjectEntity neighbour, InsertType insertType, Version version) {
        for (GroupObjectEntity groupOld : getNFGroupsIt(version)) {
            assert group.getID() != groupOld.getID() && !group.getSID().equals(groupOld.getSID());
            for (ObjectEntity obj : group.getObjects()) {
                for (ObjectEntity objOld : groupOld.getObjects()) {
                    assert obj.getID() != objOld.getID() && !obj.getSID().equals(objOld.getSID());
                }
            }
        }
        if (insertType == InsertType.FIRST) {
            groups.addFirst(group, version);
        } else if (neighbour != null) {
            groups.addIfNotExistsToThenLast(group, neighbour, insertType == InsertType.AFTER, version);
        } else {
            groups.add(group, version);    
        }

        initDefaultGroupElements(group, version);

        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null) {
            richDesign.addGroupObject(group, neighbour, insertType, version);
        }
    }

    public void addGroupObject(GroupObjectEntity group, Version version) {
        addGroupObject(group, null, null, version);
    }

    public void addPropertyDraw(ObjectEntity object, Version version, Group group) {
        addPropertyDraw(group, false, version, SetFact.singletonOrder(object));
    }

    protected void addPropertyDraw(Group group, boolean prev, Version version, ImOrderSet<ObjectEntity> objects) {
        ImSet<ObjectEntity> objectsSet = objects.getSet();
        ImFilterRevValueMap<ObjectEntity, ValueClassWrapper> mObjectToClass = objectsSet.mapFilterRevValues();
        for(int i=0,size=objectsSet.size();i<size;i++) {
            ObjectEntity object = objectsSet.get(i);
            if (object.baseClass != null)
                mObjectToClass.mapValue(i, new ValueClassWrapper(object.baseClass));
        }
        ImRevMap<ObjectEntity, ValueClassWrapper> objectToClass = mObjectToClass.immutableRevValue();
        ImSet<ValueClassWrapper> valueClasses = objectToClass.valuesSet();

        // here can be more precise heuristics than implemented in FormDataManager.getPrintTable (calculating expr and putting expr itself (not its values)  in a set)

        ImOrderSet<ValueClassWrapper> orderInterfaces = objects.mapOrder(objectToClass);
        for (ActionOrPropertyClassImplement implement : group.getActionOrProperties(valueClasses, version)) {
            ImSet<ValueClassWrapper> wrappers = implement.mapping.valuesSet();
            ImOrderSet<ObjectEntity> filterObjects = objects.filterOrderIncl(objectToClass.filterValuesRev(wrappers).keys());
            addPropertyDraw(implement.createLP(orderInterfaces.filterOrderIncl(wrappers), prev), version, filterObjects);
        }
    }

    public static ImCol<ImSet<ValueClassWrapper>> getSubsets(ImSet<ValueClassWrapper> valueClasses) {
        if(valueClasses.size() == 1) // optimization
            return SetFact.singleton(valueClasses);
            
        ImCol<ImSet<ValueClassWrapper>> classSubsets;MCol<ImSet<ValueClassWrapper>> mClassSubsets = ListFact.mCol();
        for (ImSet<ValueClassWrapper> set : new Subsets<>(valueClasses)) {
            if (!set.isEmpty()) {
                mClassSubsets.add(set);
            }
        }
        classSubsets = mClassSubsets.immutableCol();
        return classSubsets;
    }

    public PropertyDrawEntity addPropertyDraw(LAP property, Version version, ObjectEntity object) {
        return addPropertyDraw(property, version, SetFact.singletonOrder(object));
    }
    public PropertyDrawEntity addPropertyDraw(LAP property, Version version) {
        return addPropertyDraw(property, version, SetFact.EMPTYORDER());
    }

    public void addPropertyDraw(LAP[] properties, Version version, ObjectEntity object) {
        for (LAP property : properties) {
            addPropertyDraw(property, version, object);
        }
    }

    public <P extends PropertyInterface> PropertyDrawEntity addPropertyDraw(LAP<P, ?> property, Version version, ImOrderSet<ObjectEntity> objects) {
        return addPropertyDraw(property.createObjectEntity(objects), null, property.listInterfaces, false, version);
    }

    public GroupObjectEntity getNFApplyObject(ImSet<ObjectEntity> objects, Version version) {
        return getNFApplyObject(objects, SetFact.EMPTY(), version);
    }

    public GroupObjectEntity getNFApplyObject(ImSet<ObjectEntity> objects, ImSet<GroupObjectEntity> excludeGroupObjects, Version version) {
        GroupObjectEntity result = null;
        for (GroupObjectEntity group : getNFGroupsListIt(version)) {
            if (!excludeGroupObjects.contains(group)) {
                for (ObjectEntity object : group.getObjects()) {
                    if (objects.contains(object)) {
                        result = group;
                        break;
                    }
                }
            }
        }
        return result;
    }

    public GroupObjectEntity getApplyObject(ImSet<ObjectEntity> objects) {
        return getApplyObject(objects, SetFact.EMPTY());
    }
    public GroupObjectEntity getApplyObject(ImSet<ObjectEntity> objects, ImSet<GroupObjectEntity> excludeGroupObjects) {
        GroupObjectEntity result = null;
        for (GroupObjectEntity group : getGroupsList()) {
            if(!excludeGroupObjects.contains(group)) {
                for (ObjectEntity object : group.getObjects()) {
                    if (objects.contains(object)) {
                        result = group;
                        break;
                    }
                }
            }
        }
        return result;
    }

    public <I extends PropertyInterface, P extends ActionOrProperty<I>> PropertyDrawEntity<I> addPropertyDraw(P property, ImRevMap<I, ObjectEntity> mapping, Version version) {
        ActionOrPropertyObjectEntity<I, ?> entity = ActionOrPropertyObjectEntity.create(property, mapping, null, null, null);
        return addPropertyDraw(entity, null, entity.property.getReflectionOrderInterfaces(), false, version);
    }

    public <P extends PropertyInterface> PropertyDrawEntity<P> addPropertyDraw(ActionOrPropertyObjectEntity<P, ?> propertyImplement, String formPath, 
                                                                               ImOrderSet<P> interfaces, boolean addFirst, Version version) {
        String propertySID = null;
        if (propertyImplement.property.isNamed()) 
            propertySID = PropertyDrawEntity.createSID(propertyImplement, interfaces);
        return addPropertyDraw(propertyImplement, formPath, propertySID, null, addFirst, version);
    }
    public <P extends PropertyInterface> PropertyDrawEntity<P> addPropertyDraw(ActionOrPropertyObjectEntity<P, ?> propertyImplement, String formPath, 
                                                                               String propertySID, ActionOrProperty inheritedProperty, boolean addFirst, Version version) {
        if(inheritedProperty == null)
            inheritedProperty = propertyImplement.property;
        final PropertyDrawEntity<P> newPropertyDraw = new PropertyDrawEntity<>(genID(), "propertyDraw" + version.getOrder() + propertyDraws.size(version), propertyImplement, inheritedProperty);
        newPropertyDraw.proceedDefaultDraw(this);

        if (propertySID != null) {
            newPropertyDraw.setSID(propertySID);
            
            newPropertyDraw.setIntegrationSID(inheritedProperty.getName());
        }
        if (addFirst) {
            propertyDraws.addFirst(newPropertyDraw, version);
        } else {
            propertyDraws.add(newPropertyDraw, list -> {
                int ind = list.size() - 1;
                if (!newPropertyDraw.shouldBeLast) {
                    while (ind >= 0) {
                        PropertyDrawEntity property = list.get(ind);
                        if (!property.shouldBeLast) {
                            break;
                        }
                        --ind;
                    }
                }
                return ind + 1;
            }, version);
        }
        newPropertyDraw.setFormPath(formPath);
        return newPropertyDraw;
    }

    public PropertyDrawView addPropertyDrawView(PropertyDrawEntity propertyDraw, boolean addFirst, Version version) {
        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null) {
            return richDesign.addPropertyDraw(propertyDraw, addFirst, version);
        }
        return null;
    }

    public void movePropertyDrawTo(PropertyDrawEntity property, PropertyDrawEntity newNeighbour, boolean isRightNeighbour, Version version) {
        propertyDraws.move(property, newNeighbour, isRightNeighbour, version);

        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null) {
            richDesign.movePropertyDrawTo(property, newNeighbour, isRightNeighbour, version);
        }
    }
    
    public <P extends PropertyInterface> PropertyObjectEntity addPropertyObject(LP<P> property, ImOrderSet<ObjectEntity> objects) {
        return addPropertyObject(property, property.getRevMap(objects));
    }
    public <P extends PropertyInterface> PropertyObjectEntity addPropertyObject(LP<P> property) {
        return addPropertyObject(property, MapFact.EMPTYREV());
    }
    public <P extends PropertyInterface> ActionObjectEntity<P> addPropertyObject(LA<P> property, ImOrderSet<ObjectEntity> objects) {
        return addPropertyObject(property, property.getRevMap(objects));
    }

    public <P extends PropertyInterface> PropertyObjectEntity addPropertyObject(LP<P> property, ImRevMap<P, ObjectEntity> objects) {
        return new PropertyObjectEntity<>(property.property, objects, property.getCreationScript(), property.getCreationPath(), property.getPath());
    }
    public <P extends PropertyInterface> ActionObjectEntity<P> addPropertyObject(LA<P> property, ImRevMap<P, ObjectEntity> objects) {
        return new ActionObjectEntity<>(property.action, objects, property.getCreationScript(), property.getCreationPath(), property.getPath());
    }
    
    public <P extends PropertyInterface> PropertyObjectEntity addPropertyObject(PropertyRevImplement<P, ObjectEntity> impl) {
        return addPropertyObject(impl.property, impl.mapping);
    }
    public <P extends PropertyInterface> PropertyObjectEntity<P> addPropertyObject(Property<P> property, ImRevMap<P, ObjectEntity> objects) {
        return new PropertyObjectEntity<>(property, objects);
    }

    public PropertyDrawEntity<?> getPropertyDraw(int iID) {
        for (PropertyDrawEntity propertyDraw : getPropertyDrawsIt()) {
            if (propertyDraw.getID() == iID) {
                return propertyDraw;
            }
        }

        return null;
    }

    public PropertyDrawEntity<?> getPropertyDraw(String sid, Version version) {
        if (sid == null) {
            return null;
        }
        for (PropertyDrawEntity propertyDraw : getNFPropertyDrawsIt(version)) {
            if (sid.equals(propertyDraw.getSID())) {
                return propertyDraw;
            }
        }

        return null;
    }

    public PropertyDrawEntity<?> getPropertyDrawIntegration(String sid, PropertyDrawEntity checkProperty, Version version) {
        if (sid == null) {
            return null;
        }
        for (PropertyDrawEntity propertyDraw : getNFPropertyDrawsIt(version)) {
            if (sid.equals(propertyDraw.getIntegrationSID()) && BaseUtils.nullEquals(propertyDraw.getNFToDraw(this, version), checkProperty.getNFToDraw(this, version))) {
                return propertyDraw;
            }
        }

        return null;
    }

    public boolean noClasses() {
        return false;
    }

    public static class AlreadyDefined extends Exception {
        public final String formCanonicalName;
        public final String newSID;
        public final String formPath;

        public AlreadyDefined(String formCanonicalName, String newSID, String formPath) {
            this.formCanonicalName = formCanonicalName;
            this.newSID = newSID;
            this.formPath = formPath;
        }
    }

    public void setFinalPropertyDrawSID(PropertyDrawEntity property, String alias) throws AlreadyDefined {
        String newSID = (alias == null ? property.getSID() : alias);
        property.setSID(null);
        PropertyDrawEntity drawEntity;
        if ((drawEntity = getPropertyDraw(newSID, Version.current())) != null) {
            throw new AlreadyDefined(getCanonicalName(), newSID, drawEntity.getFormPath());
        }
        property.setSID(newSID);

        // has to be done somewhere in open operator, because interactive forms can have properties with the same name 
//        String newIntegrationSID = (alias == null ? property.getIntegrationSID() : alias);
//        property.setIntegrationSID(null);
//        if ((drawEntity = getPropertyDrawIntegration(newIntegrationSID, property, Version.CURRENT)) != null) {
//            throw new AlreadyDefined(getCanonicalName(), newIntegrationSID, drawEntity.getFormPath());
//        }
//        property.setIntegrationSID(newIntegrationSID);
        if(alias != null)
            property.setIntegrationSID(alias);
    }


    public PropertyDrawEntity<?> getPropertyDraw(String name, List<String> mapping, Version version) {
        return getPropertyDraw(PropertyDrawEntity.createSID(name, mapping), version);
    }

    private NFSet<Property> hintsIncrementTable = NFFact.set();
    @LongMutable
    public ImSet<Property> getHintsIncrementTable() {
        return hintsIncrementTable.getSet();
    }

    public void addHintsIncrementTable(Version version, LP... props) {
        for (LP prop : props) {
            hintsIncrementTable.add(prop.property, version);
        }
    }

    public void addHintsIncrementTable(Version version, Property... props) {
        for (Property prop : props) {
            hintsIncrementTable.add(prop, version);
        }
    }

    private NFSet<Property> hintsNoUpdate = NFFact.set();
    @LongMutable
    public ImSet<Property> getHintsNoUpdate() {
        return hintsNoUpdate.getSet();
    }

    public void addHintsNoUpdate(Version version, LP... props) {
        for (LP prop : props) {
            addHintsNoUpdate(prop, version);
        }
    }

    protected void addHintsNoUpdate(LP prop, Version version) {
        addHintsNoUpdate(prop.property, version);
    }

    public void addHintsNoUpdate(Property prop, Version version) {
        hintsNoUpdate.add(prop, version);
    }

    public FormView createDefaultRichDesign(Version version) {
        return new DefaultFormView(this, version);
    }

    private NFProperty<FormView> richDesign = NFFact.property();

    public FormView getRichDesign() {
        return richDesign.get(); // assert что не null см. последнюю строку в конструкторе
/*        return richDesign.getDefault(new NFDefault<FormView>() {
            public FormView create() {
                return createDefaultRichDesign(Version.LAST);
            }
        });*/
    }

    public FormView getNFRichDesign(Version version) {
        return richDesign.getNF(version);
    }

    public void setRichDesign(FormView view, Version version) {
        richDesign.set(view, version);
    }

    private StaticDataGenerator.Hierarchy getHierarchy(boolean supportGroupColumns, ImSet<GroupObjectEntity> valueGroups, BiFunction<GroupObjectEntity, ImOrderSet<PropertyDrawEntity>, ImOrderSet<PropertyDrawEntity>> filter) {
        ImMap<GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> groupProperties = getGroupProperties(valueGroups, supportGroupColumns);

        ImMap<GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> filteredGroupProperties = groupProperties;
        if(filter != null)
            filteredGroupProperties = filteredGroupProperties.mapValues(filter);

        return new StaticDataGenerator.Hierarchy(getGroupHierarchy(supportGroupColumns, valueGroups), filteredGroupProperties, valueGroups);
    }

    @IdentityLazy
    public boolean hasNoProperties(GroupObjectEntity group) {
        return getProperties(group).isEmpty();
    }

    private ImOrderSet<PropertyDrawEntity> getProperties(GroupObjectEntity group) {
        ImOrderSet<PropertyDrawEntity> properties = getAllGroupProperties(SetFact.EMPTY(), true).get(group);
        return properties != null ? properties : SetFact.EMPTYORDER();
    }

    @IdentityLazy
    public boolean usedAsGroupColumn(GroupObjectEntity group) {
        return getAllGroupColumns().contains(group);
    }

    @IdentityInstanceLazy
    public StaticDataGenerator.Hierarchy getImportHierarchy() {
        return getHierarchy(false, SetFact.EMPTY(), null);
    }
    public ImMap<GroupObjectEntity, ImSet<FilterEntity>> getImportFixedFilters() {
        return getGroupFixedFilters(SetFact.EMPTY());
    }

    @IdentityInstanceLazy
    private StaticDataGenerator.Hierarchy getCachedStaticHierarchy(boolean isReport, ImSet<GroupObjectEntity> valueGroups) {
        return getHierarchy(isReport, valueGroups, null);
    }
    
    public StaticDataGenerator.Hierarchy getStaticHierarchy(boolean supportGroupColumns, ImSet<GroupObjectEntity> valueGroups, BiFunction<GroupObjectEntity, ImOrderSet<PropertyDrawEntity>, ImOrderSet<PropertyDrawEntity>> filter) {
        if(filter == null) // optimization
            return getCachedStaticHierarchy(supportGroupColumns, valueGroups);
        return getHierarchy(supportGroupColumns, valueGroups, filter);
    }

    @IdentityInstanceLazy
    public GroupObjectHierarchy getGroupHierarchy(boolean supportGroupColumns, ImSet<GroupObjectEntity> excludeGroupObjects) {
        return new FormGroupHierarchyCreator(this, supportGroupColumns).createHierarchy(excludeGroupObjects);
    }

    @IdentityInstanceLazy
    public GroupObjectHierarchy getSingleGroupObjectHierarchy(GroupObjectEntity groupObject) {
        return new GroupObjectHierarchy(groupObject, Collections.singletonMap(groupObject, SetFact.EMPTYORDER()));
    }

    public void addActionsOnEvent(Object eventObject, boolean drop, Version version, ActionObjectEntity<?>... actions) {
        if(drop)
            eventActions.removeAll(eventObject, version);
        eventActions.addAll(eventObject, Arrays.asList(actions), version);
    }

    public ComponentView getDrawComponent(PropertyDrawEntity<?> property) {
        FormView formView = getRichDesign();
        ComponentView drawComponent;
        GroupObjectEntity toDraw;
        if(property.isList(this) && (toDraw = property.getToDraw(this)) != null) {
            if (toDraw.isInTree())
                drawComponent = formView.get(toDraw.treeGroup);
            else
                drawComponent = formView.get(toDraw).grid;
        } else
            drawComponent = formView.get(property);
        return drawComponent;
    }

    @IdentityLazy
    public boolean isMap(GroupObjectEntity entity) {
        return getField(entity, "longitude", "latitude", "polygon") != null;
    }

    @IdentityLazy
    public boolean isCalendarDate(GroupObjectEntity entity) {
        return getField(entity, "date", "dateFrom") != null;
    }

    @IdentityLazy
    public boolean isCalendarDateTime(GroupObjectEntity entity) {
        return getField(entity, "dateTime", "dateTimeFrom") != null;
    }

    @IdentityLazy
    public boolean isCalendarPeriod(GroupObjectEntity entity) {
        return getField(entity, "dateFrom", "dateTimeFrom") != null;
    }

    public PropertyDrawEntity getField(GroupObjectEntity entity, String... fields) {
        List<String> fieldsList = Arrays.asList(fields);
        Iterable<PropertyDrawEntity> propertyDrawsIt = getPropertyDrawsIt();
        for (PropertyDrawEntity property : propertyDrawsIt) {
            if (property.isList(this) && entity.equals(property.getToDraw(this))) {
                String name = property.getIntegrationSID();
                if (name != null && fieldsList.contains(name)) {
                    return property;
                }
            }
        }
        return null;
    }

    @IdentityLazy
    public boolean hasFooters(GroupObjectEntity entity) {
        for (PropertyDrawEntity property : getProperties(entity)) {
            if (property.getPropertyExtra(PropertyDrawExtraType.FOOTER) != null)
                return true;
        }
        return false;
    }

    public void finalizeAroundInit() {

        for(GroupObjectEntity group : getGroupsIt()) {
            if(group.listViewType.isMap() && !isMap(group)) {
                throw new RuntimeException(getCreationPath() + " none of required MAP propertyDraws found (longitude, latitude or polygon)");
            }
        }

        for(GroupObjectEntity group : getGroupsIt()) {
            if(group.listViewType.isCalendar() && !isCalendarDate(group) && !isCalendarDateTime(group)) {
                throw new RuntimeException(getCreationPath() + " none of required CALENDAR propertyDraws found (date, dateFrom, dateTime or dateTimeFrom)");
            }
        }

        groups.finalizeChanges();
        treeGroups.finalizeChanges();
        propertyDraws.finalizeChanges();
        fixedFilters.finalizeChanges();
        eventActions.finalizeChanges();
        userFilters.finalizeChanges();
        defaultOrders.finalizeChanges();
        fixedOrders.finalizeChanges();
        
        hintsIncrementTable.finalizeChanges();
        hintsNoUpdate.finalizeChanges();
        
        for(RegularFilterGroupEntity regularFilterGroup : getRegularFilterGroupsIt())
            regularFilterGroup.finalizeAroundInit();

        try {
            getRichDesign().finalizeAroundInit();
        } catch (ScriptParsingException e) {
            throw new ScriptParsingException("error finalizing form " + this + ":\n" + e.getMessage());
        }

        proceedAllEventActions((action, drawAction) -> {
        }); // need this to generate default event actions (which will generate auto forms, and for example fill GroupObjectEntity.FILTER props, what is important to do before form is used)
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getName() {
        return CanonicalNameUtils.getName(canonicalName);
    }

    private String integrationSID;
    
    public String getIntegrationSID() {
        return integrationSID != null ? integrationSID : canonicalName != null ? getName() : null;
    }

    public void setIntegrationSID(String integrationSID) {
        this.integrationSID = integrationSID;
    }

    public LocalizedString getCaption() {
        return caption;
    }

    public String getCreationPath() {
        if (debugPoint == null) {
            return null;
        } else {
            return debugPoint.toString();
        }
    }

    public int getID() {
        return ID;
    }

    public String getSID() {
        if (canonicalName != null) {
            return canonicalName;
        } else {
            return "_FORM_" + getID();
        }
    }

    public boolean isNamed() {
        return canonicalName != null;
    }

    public boolean needsToBeSynchronized() {
        return isNamed();
    }

    public String getDefaultImagePath() {
        return defaultImagePath;
    }

    // сохраняет нижние компоненты
    public static class ComponentDownSet extends AddSet<ComponentView, ComponentDownSet> {

        public ComponentDownSet() {
        }

        public static ComponentDownSet create(MAddSet<ComponentView> components) {
            ComponentDownSet result = new ComponentDownSet();
            for(ComponentView component : components)
                result = result.addItem(component);
            return result;
        }

        public ComponentDownSet(ComponentView where) {
            super(where);
        }

        public ComponentDownSet(ComponentView[] wheres) {
            super(wheres);
        }

        protected ComponentDownSet createThis(ComponentView[] wheres) {
            return new ComponentDownSet(wheres);
        }

        protected ComponentView[] newArray(int size) {
            return new ComponentView[size];
        }

        protected boolean containsAll(ComponentView who, ComponentView what) {
            return what.isAncestorOf(who);
        }

        public ComponentDownSet addItem(ComponentView container) {
            return add(new ComponentDownSet(container));
        }

        public ComponentDownSet addAll(ComponentDownSet set) {
            return add(set);
        }
    }

    // сохраняет верхние компоненты
    public static class ComponentUpSet extends AddSet<ComponentView, ComponentUpSet> {

        public ComponentUpSet() {
        }

        public ComponentUpSet(ComponentView where) {
            super(where);
        }

        public ComponentUpSet(ComponentView[] wheres) {
            super(wheres);
        }

        protected ComponentUpSet createThis(ComponentView[] wheres) {
            return new ComponentUpSet(wheres);
        }

        protected ComponentView[] newArray(int size) {
            return new ComponentView[size];
        }

        protected boolean containsAll(ComponentView who, ComponentView what) {
            return who.isAncestorOf(what);
        }

        public ComponentUpSet addItem(ComponentView container) {
            return add(new ComponentUpSet(container));
        }
        
        public ComponentUpSet addAll(ComponentUpSet set) {
            return add(set);            
        }
    }

    public boolean isDesignHidden(ComponentView component) { // global
        return component.isDesignHidden();
    }

    @IdentityLazy
    public ComponentUpSet getDrawDynamicHideableContainers(GroupObjectEntity group) {
        ComponentUpSet result = new ComponentUpSet();
        for(PropertyDrawEntity<?> property : getPropertyDrawsIt())
            if(!group.getObjects().disjoint(property.getObjectInstances())) {  // для свойств "зависящих" от группы
                ComponentView drawComponent = getDrawComponent(property);
                if(!isDesignHidden(drawComponent)) {
                    ComponentView localHideableContainer = drawComponent.getDynamicHidableContainer();
                    if (localHideableContainer == null) // cheat / optimization
                        return null;
                    result = result.addItem(localHideableContainer);
                }
            }
        ImSet<FilterEntity> fixedFilters = getFixedFilters();
        MSet<GroupObjectEntity> mFixedGroupObjects = SetFact.mSetMax(fixedFilters.size());
        for(FilterEntity<?> filterEntity : fixedFilters) {
            if(!group.getObjects().disjoint(filterEntity.getObjects())) { // для фильтров "зависящих" от группы
                GroupObjectEntity drawGroup = filterEntity.getApplyObject(this);
                if(!drawGroup.equals(group))
                    mFixedGroupObjects.add(drawGroup); 
            }
        }
        for(GroupObjectEntity fixedGroupObject : mFixedGroupObjects.immutable()) {
            ComponentUpSet drawContainers = getDrawDynamicHideableContainers(fixedGroupObject);
            if(drawContainers==null)
                return null;
                
            result = result.addAll(drawContainers);
        }
        return result;
    }

    @IdentityLazy
    public ImSet<ContainerView> getPropertyContainers() {
        MExclSet<ContainerView> mContainers = SetFact.mExclSet();
        getRichDesign().mainContainer.fillPropertyContainers(mContainers);
        return mContainers.immutable();
    }

    public String getAsyncCaption() {
        return ThreadLocalContext.localize(getRichDesign().mainContainer.caption);
    }

    public void setEditType(PropertyEditType editType) {
        for (PropertyDrawEntity propertyView : getPropertyDrawsIt()) {
            setEditType(propertyView, editType);
        }
    }

    public void setNFEditType(PropertyEditType editType, Version version) {
        for (PropertyDrawEntity propertyView : getNFPropertyDrawsIt(version)) {
            setEditType(propertyView, editType);
        }
    }

    public void setEditType(PropertyDrawEntity property, PropertyEditType editType) {
        property.setEditType(editType);
    }
    
    public void addUserFilter(PropertyDrawEntity property, Version version) {
        userFilters.add(property, version);
        
        FormView richDesign = getNFRichDesign(version);
        if(richDesign !=null)
            richDesign.addFilter(property, version);
    }

    public void addDefaultOrder(PropertyDrawEntity property, boolean ascending, Version version) {
        defaultOrders.add(property, ascending, version);
    }

    public void addDefaultOrderFirst(PropertyDrawEntity property, boolean ascending, Version version) {
        defaultOrders.addFirst(property, ascending, version);
    }

    public void addDefaultOrderView(PropertyDrawEntity property, boolean ascending, Version version) {
        FormView richDesign = getNFRichDesign(version);
        if(richDesign !=null)
            richDesign.addDefaultOrder(property, ascending, version);
    }

    public void setPageSize(int pageSize) {
        for (GroupObjectEntity group : getGroupsIt()) {
            group.pageSize = pageSize;
        }
    }

    public void addPivotColumn(PropertyDrawEntity column, Version version) {
        pivotColumns.add(ListFact.singleton(column), version);
    }

    public void addPivotColumns(List<List<PropertyDrawEntity>> columns, Version version) {
        for(List<PropertyDrawEntity> column : columns) {
            ImList<PropertyDrawEntity> columnList = ListFact.fromJavaList(column);
            pivotColumns.add(columnList, version);
            addPivotColumnView(columnList, version);
        }
    }

    public void addPivotRow(PropertyDrawEntity row, Version version) {
        pivotRows.add(ListFact.singleton(row), version);
    }

    public void addPivotRows(List<List<PropertyDrawEntity>> rows, Version version) {
        for(List<PropertyDrawEntity> row : rows) {
            ImList<PropertyDrawEntity> rowList = ListFact.fromJavaList(row);
            pivotRows.add(rowList, version);
            addPivotRowView(rowList, version);
        }
    }

    public void addPivotMeasure(PropertyDrawEntity measure, Version version) {
        pivotMeasures.add(measure, version);
    }

    public void addPivotMeasures(List<PropertyDrawEntity> measures, Version version) {
        for(PropertyDrawEntity measure : measures) {
            pivotMeasures.add(measure, version);
            addPivotMeasureView(measure, version);
        }
    }

    private void addPivotColumnView(ImList<PropertyDrawEntity> column, Version version) {
        FormView richDesign = getNFRichDesign(version);
        if(richDesign !=null)
            richDesign.addPivotColumn(column, version);
    }
    private void addPivotRowView(ImList<PropertyDrawEntity> column, Version version) {
        FormView richDesign = getNFRichDesign(version);
        if(richDesign !=null)
            richDesign.addPivotRow(column, version);
    }
    private void addPivotMeasureView(PropertyDrawEntity column, Version version) {
        FormView richDesign = getNFRichDesign(version);
        if(richDesign !=null)
            richDesign.addPivotMeasure(column, version);
    }

    public void setNeedVerticalScroll(boolean scroll) {
        for (GroupObjectEntity entity : getGroupsIt()) {
            getRichDesign().get(entity).needVerticalScroll = scroll;
        }
    }

    public ValueClass getBaseClass(ObjectEntity object) {
        return object.baseClass;
    }

    @Override
    public boolean isSingleGroup(ObjectEntity object) {
        return object.groupTo.getObjects().size() == 1;
    }

    @IdentityLazy
    public ImSet<ObjectEntity> getObjects() {
        MExclSet<ObjectEntity> mObjects = SetFact.mExclSet();
        for (GroupObjectEntity group : getGroupsIt())
            mObjects.exclAddAll(group.getObjects());
        return mObjects.immutable();
    }

    @Override
    public String toString() {
        String result = getSID();
        if (caption != null) {
            result += " '" + ThreadLocalContext.localize(caption) + "'";
        }
        if (debugPoint != null) {
            result += " [" + debugPoint + "]";
        }
        return result;
    }

    @Override
    public FormEntity getNFStaticForm() {
        return this;
    }

    @Override
    public FormEntity getStaticForm(BaseLogicsModule LM) {
        return this;
    }

    public Pair<FormEntity, ImRevMap<ObjectEntity, ObjectEntity>> getForm(BaseLogicsModule LM, DataSession session, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues) {
        return new Pair<>(this, getObjects().toRevMap());
    }

    public void proceedAllEventActions(BiConsumer<ActionObjectEntity<?>, PropertyDrawEntity<?>> consumer) {
        for(PropertyDrawEntity<?> propertyDraw : getPropertyDrawsIt()) {
            for(String changeEvent : BaseUtils.mergeIterables(ServerResponse.events, propertyDraw.getContextMenuBindings().keySet())) {
                ActionObjectEntity<?> editAction = propertyDraw.getEventAction(changeEvent, this);
                if (editAction != null)
                    consumer.accept(editAction, changeEvent.equals(CHANGE) && !propertyDraw.isProperty() ? propertyDraw : null);
            }
        }
        for(ImList<ActionObjectEntity<?>> eventActions : getEventActions().valueIt()) {
            for(ActionObjectEntity<?> eventAction : eventActions)
                consumer.accept(eventAction, null);
        }
    }

    @Override
    public FormSelector<ObjectEntity> merge(FormSelector formSelector) {
        if(!(formSelector instanceof FormEntity))
            return null;
        
        if(BaseUtils.hashEquals(this, formSelector))
            return this;
        
        return null;
    }
}