package lsfusion.server.logics.form.struct;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.comb.Subsets;
import lsfusion.base.dnf.AddSet;
import lsfusion.base.identity.DefaultIDGenerator;
import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.form.event.FormEvent;
import lsfusion.server.base.AppServerImage;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.base.version.ComplexLocation;
import lsfusion.server.base.version.NFFact;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.*;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.EvalScriptingLogicsModule;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FormChangeFlowType;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.FormEventType;
import lsfusion.server.logics.form.interactive.MappingInterface;
import lsfusion.server.logics.form.interactive.action.async.AsyncAddRemove;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.AsyncNoWaitExec;
import lsfusion.server.logics.form.interactive.action.input.InputFilterEntity;
import lsfusion.server.logics.form.interactive.action.input.InputOrderEntity;
import lsfusion.server.logics.form.interactive.action.lifecycle.FormToolbarAction;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.FormInstanceContext;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.auto.DefaultFormView;
import lsfusion.server.logics.form.interactive.event.FormServerEvent;
import lsfusion.server.logics.form.interactive.event.FormServerScheduler;
import lsfusion.server.logics.form.open.FormSelector;
import lsfusion.server.logics.form.stat.FormGroupHierarchyCreator;
import lsfusion.server.logics.form.stat.GroupObjectHierarchy;
import lsfusion.server.logics.form.stat.StaticDataGenerator;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.filter.*;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.*;
import lsfusion.server.logics.form.struct.property.oraction.ActionOrPropertyObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.monitor.SystemEventsLogicsModule;
import lsfusion.server.physics.dev.debug.DebugInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.CanonicalNameUtils;
import lsfusion.server.physics.dev.integration.external.to.CallHTTPAction;
import lsfusion.server.physics.dev.integration.external.to.ExternalLSFAction;
import lsfusion.server.physics.dev.integration.external.to.InternalClientAction;
import lsfusion.server.physics.dev.property.IsDevProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static lsfusion.base.BaseUtils.nvl;
import static lsfusion.interop.action.ServerResponse.CHANGE;

public class FormEntity extends IdentityEntity<FormEntity, FormEntity> implements FormSelector<ObjectEntity> {
    private final static Logger logger = Logger.getLogger(FormEntity.class);
    
    public static Boolean DEFAULT_NOCANCEL = null;

    public static final IsDevProperty isDev = IsDevProperty.instance;
    public static final SessionDataProperty isDocked = new SessionDataProperty(LocalizedString.create("Is docked"), LogicalClass.instance);
    public static final SessionDataProperty isEditing = new SessionDataProperty(LocalizedString.create("Is embedded"), LogicalClass.instance);
    public static final SessionDataProperty showOk = new SessionDataProperty(LocalizedString.create("Is modal"), LogicalClass.instance);
    public static final SessionDataProperty isAdd = new SessionDataProperty(LocalizedString.create("Is add"), LogicalClass.instance);
    public static final SessionDataProperty isManageSession = new SessionDataProperty(LocalizedString.create("Is manage session"), LogicalClass.instance);
    public static final SessionDataProperty isExternal = new SessionDataProperty(LocalizedString.create("Is external"), LogicalClass.instance);
    public static final SessionDataProperty showDrop = new SessionDataProperty(LocalizedString.create("Show drop"), LogicalClass.instance);

    public PropertyDrawEntity editActionPropertyDraw;
    public PropertyDrawEntity dropActionPropertyDraw;
    public PropertyDrawEntity refreshActionPropertyDraw;
    public PropertyDrawEntity applyActionPropertyDraw;
    public PropertyDrawEntity cancelActionPropertyDraw;
    public PropertyDrawEntity okActionPropertyDraw;
    public PropertyDrawEntity closeActionPropertyDraw;

    public PropertyDrawEntity shareActionPropertyDraw;
    public PropertyDrawEntity customizeActionPropertyDraw;

    public PropertyDrawEntity logMessagePropertyDraw;

    public String canonicalName;

    private EvalScriptingLogicsModule customizeLM;
    public EvalScriptingLogicsModule getCustomizeLM() {
        return customizeLM;
    }

    public List<String> formOrDesignStatementList = new ArrayList<>();
    public void addFormOrDesignStatementTokens(List<String> tokens) {
        String result = StringUtils.join(tokens, "");
        while(result.startsWith("\r") || result.startsWith("\n"))
            result = result.substring(1);
        formOrDesignStatementList.add(result);
    }
    public String getCode() {
        return StringUtils.join(formOrDesignStatementList, "\n");
    }

    public static class EventAction implements MappingInterface<EventAction> {
        public final FormServerEvent event;
        public final ActionObjectEntity action;

        public EventAction(FormServerEvent event, ActionObjectEntity<?> action) {
            this.event = event;
            this.action = action;
        }

        private EventAction(EventAction src, ObjectMapping mapping) {
            this.event = mapping.get(src.event);
            this.action = mapping.get(src.action);
        }

        @Override
        public EventAction get(ObjectMapping mapping) {
            return new EventAction(this, mapping);
        }
    }
    public NFList<EventAction> eventActions = NFFact.list();
    public Iterable<EventAction> getEventActionsIt() {
        return eventActions.getIt();
    }

    public Iterable<ActionObjectEntity<?>> getEventActionsListIt(FormServerEvent eventObject) {
        return getGroupEventActionsList().get(eventObject);
    }
    public Iterable<ActionObjectEntity<?>> getNFEventActionsListIt(FormServerEvent eventObject, Version version) {
        List<ActionObjectEntity<?>> list = new ArrayList<>();
        for(EventAction eventAction : eventActions.getNFListIt(version))
            if(eventAction.event.equals(eventObject))
                list.add(eventAction.action);
        return list;
    }
    @IdentityLazy
    public ImMap<FormServerEvent, ImList<ActionObjectEntity<?>>> getGroupEventActionsList() {
        return eventActions.getList().groupList(key -> key.event).mapValues(key -> key.mapListValues(event -> event.action));
    }

    private NFOrderSet<ObjectEntity> objects = NFFact.orderSet(); // для script'ов, findObjectEntity в FORM / EMAIL objects
    public Iterable<ObjectEntity> getNFObjectsIt(Version version) { // не finalized
        return objects.getNFIt(version);
    }
    public Iterable<ObjectEntity> getNFObjectsIt(Version version, boolean allowRead) { // не finalized
        return objects.getNFIt(version, allowRead);
    }

    private NFComplexOrderSet<GroupObjectEntity> groups = NFFact.complexOrderSet(); // для script'ов, findObjectEntity в FORM / EMAIL objects
    public Iterable<GroupObjectEntity> getGroupsIt() {
        return groups.getIt();
    }
    public Iterable<GroupObjectEntity> getNFGroupsIt(Version version) { // не finalized
        return groups.getNFIt(version);
    }
    public Iterable<GroupObjectEntity> getNFGroupsIt(Version version, boolean allowRead) { // не finalized
        return groups.getNFIt(version, allowRead);
    }
    public ImSet<GroupObjectEntity> getGroups() {
        return groups.getSet();
    }
    public ImOrderSet<GroupObjectEntity> getGroupsList() {
        return groups.getOrderSet();
    }
    public Iterable<GroupObjectEntity> getGroupsListIt() {
        return groups.getListIt();
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
    
    private NFComplexOrderSet<PropertyDrawEntity> propertyDraws = NFFact.complexOrderSet();
    public Iterable<PropertyDrawEntity> getPropertyDrawsIt() {
        return propertyDraws.getIt();
    }
    public Iterable<PropertyDrawEntity> getNFPropertyDrawsIt(Version version) {
        return propertyDraws.getNFIt(version);
    }
    public Iterable<PropertyDrawEntity> getNFPropertyDrawsIt(Version version, boolean allowRead) {
        return propertyDraws.getNFIt(version, allowRead);
    }
    public ImOrderSet<PropertyDrawEntity> getPropertyDrawsList() {
        return propertyDraws.getOrderSet();        
    }
    public Iterable<PropertyDrawEntity> getPropertyDrawsListIt() {
        return propertyDraws.getListIt();        
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
    public Iterable<RegularFilterGroupEntity> getNFRegularFilterGroupsIt(Version version, boolean allowRead) {
        return regularFilterGroups.getNFIt(version, true);
    }
    public Iterable<RegularFilterGroupEntity> getNFRegularFilterGroupsListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return regularFilterGroups.getNFListIt(version);
    }

    public ImSet<FilterEntity> getDefaultRegularFilters() {
        return getRegularFilterGroupsList().filterOrder(element -> element.getDefault() >= 0)
                .mapMergeOrderSetValues(entity -> entity.filters.getOrderSet().get(entity.getDefault()).filter).getSet();
    }

    private NFOrderMap<PropertyDrawEntity,Boolean> defaultOrders = NFFact.orderMap();
    public ImOrderMap<PropertyDrawEntity,Boolean> getDefaultOrdersList() {
        return defaultOrders.getListMap();
    }
    public Boolean getNFDefaultOrder(PropertyDrawEntity<?, ?> entity, Version version) {
        return defaultOrders.getNFValue(entity, version);
    }
    
    private NFOrderMap<OrderEntity,Boolean> fixedOrders = NFFact.orderMap();
    public ImOrderMap<OrderEntity,Boolean> getFixedOrdersList() {
        return fixedOrders.getListMap();
    }

    private NFOrderSet<ImList<PropertyDrawEntityOrPivotColumn>> pivotColumns = NFFact.orderSet();
    private NFOrderSet<ImList<PropertyDrawEntityOrPivotColumn>> pivotRows = NFFact.orderSet();
    private NFOrderSet<PropertyDrawEntity> pivotMeasures = NFFact.orderSet();

    public Iterable<ImList<PropertyDrawEntityOrPivotColumn>> getNFPivotColumnsListIt(Version version) {
        return pivotColumns.getNFListIt(version);
    }

    public ImList<ImList<PropertyDrawEntityOrPivotColumn>> getPivotColumnsList() {
        return pivotColumns.getList();
    }

    public Iterable<ImList<PropertyDrawEntityOrPivotColumn>> getNFPivotRowsListIt(Version version) {
        return pivotRows.getNFListIt(version);
    }

    public ImList<ImList<PropertyDrawEntityOrPivotColumn>> getPivotRowsList() {
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
        for (ImList<PropertyDrawEntityOrPivotColumn> pivotRow : getPivotRowsList()) {
            for (PropertyDrawEntityOrPivotColumn entry : pivotRow) {
                if (entry instanceof PropertyDrawEntity) {
                    mGroupProps.add((PropertyDrawEntity) entry);
                }
            }
        }
        for (ImList<PropertyDrawEntityOrPivotColumn> pivotColumn : getPivotColumnsList()) {
            for (PropertyDrawEntityOrPivotColumn entry : pivotColumn) {
                if (entry instanceof PropertyDrawEntity) {
                    mGroupProps.add((PropertyDrawEntity) entry);
                }
            }
        }
        return mGroupProps.immutable().group(key -> key.getToDraw(this));
    }
    @IdentityLazy
    public ImMap<GroupObjectEntity, ImSet<PropertyDrawEntity>> getPivotMeasureProps() {
        return getPivotMeasuresList().toOrderSet().getSet().group(key -> key.getToDraw(this));
    }
    @IdentityLazy
    public ImSet<PropertyDrawEntity> getUserPrefsHiddenProperties() {
        return getPropertyDrawsList().getSet().filterFn(property -> property.isHide() || property.isRemove());
    }

    public NFProperty<Boolean> localAsync = NFFact.property();

    public void setLocalAsync(boolean localAsync, Version version) {
        this.localAsync.set(localAsync, version);
    }
    public boolean getLocalAsync() {
        return localAsync.get() != null;
    }

    public NFProperty<PropertyObjectEntity> reportPathProp = NFFact.property();

    public PropertyObjectEntity<?> getReportPathProp() {
        return reportPathProp.get();
    }

    public FormEntity(boolean interactive, String canonicalName, Version version, DebugInfo.DebugPoint debugPoint) {
        this(new DefaultIDGenerator(), interactive, canonicalName, version, debugPoint);
    }

    @Override
    protected String getDefaultSIDPrefix() {
        return "form";
    }

    public FormEntity(IDGenerator idGenerator, boolean interactive, String canonicalName, Version version, DebugInfo.DebugPoint debugPoint) {
        super(idGenerator, canonicalName, debugPoint);

        genID = idGenerator;

        this.canonicalName = canonicalName; // needed for isNamed in initDefaultProps

        if (interactive) {
            view = new DefaultFormView(this, version);

            initDefaultProps(version);

            ((DefaultFormView) view).initDefaultProps(version);

            initDefaultEvents(version);
        }
    }

    public void initDefaultProps(Version version) {
        BaseLogicsModule baseLM = ThreadLocalContext.getBaseLM();

        editActionPropertyDraw = addPropertyDraw(baseLM.getFormEditReport(), version);
        editActionPropertyDraw.setAddParent(this, (Function<FormEntity, PropertyDrawEntity>) formEntity -> formEntity.editActionPropertyDraw);
        refreshActionPropertyDraw = addPropertyDraw(baseLM.getFormRefresh(), version);
        refreshActionPropertyDraw.setAddParent(this, (Function<FormEntity, PropertyDrawEntity>) formEntity -> formEntity.refreshActionPropertyDraw);
        applyActionPropertyDraw = addPropertyDraw(baseLM.getFormApply(), version);
        applyActionPropertyDraw.setAddParent(this, (Function<FormEntity, PropertyDrawEntity>) formEntity -> formEntity.applyActionPropertyDraw);
        cancelActionPropertyDraw = addPropertyDraw(baseLM.getFormCancel(), version);
        cancelActionPropertyDraw.setAddParent(this, (Function<FormEntity, PropertyDrawEntity>) formEntity -> formEntity.cancelActionPropertyDraw);
        okActionPropertyDraw = addPropertyDraw(baseLM.getFormOk(), version);
        okActionPropertyDraw.setAddParent(this, (Function<FormEntity, PropertyDrawEntity>) formEntity -> formEntity.okActionPropertyDraw);
        closeActionPropertyDraw = addPropertyDraw(baseLM.getFormClose(), version);
        closeActionPropertyDraw.setAddParent(this, (Function<FormEntity, PropertyDrawEntity>) formEntity -> formEntity.closeActionPropertyDraw);
        dropActionPropertyDraw = addPropertyDraw(baseLM.getFormDrop(), version);
        dropActionPropertyDraw.setAddParent(this, (Function<FormEntity, PropertyDrawEntity>) formEntity -> formEntity.dropActionPropertyDraw);

        if(isNamed()) {
            shareActionPropertyDraw = addPropertyDraw(baseLM.getFormShare(), version);
            shareActionPropertyDraw.setAddParent(this, (Function<FormEntity, PropertyDrawEntity>) formEntity -> formEntity.shareActionPropertyDraw);
            customizeActionPropertyDraw = addPropertyDraw(baseLM.getFormCustomize(), version);
            customizeActionPropertyDraw.setAddParent(this, (Function<FormEntity, PropertyDrawEntity>) formEntity -> formEntity.customizeActionPropertyDraw);
            customizeActionPropertyDraw.setPropertyExtra((PropertyObjectEntity<?>) baseLM.getFormCustomizeBackground().createObjectEntity(SetFact.EMPTYORDER()), PropertyDrawExtraType.BACKGROUND, version);
        }

        logMessagePropertyDraw = addPropertyDraw(baseLM.getLogMessage(), version);
        logMessagePropertyDraw.setPropertyExtra(addPropertyObject(externalShowIf), PropertyDrawExtraType.SHOWIF, version);
        logMessagePropertyDraw.setAddParent(this, (Function<FormEntity, PropertyDrawEntity>) formEntity -> formEntity.logMessagePropertyDraw);
    }

    private void initDefaultEvents(Version version) {
        //        ??? here we can also ActionObjectEntity in eventActions eliminate ???
        BaseLogicsModule baseLM = ThreadLocalContext.getBaseLM();

        addActionsOnEvent(FormEventType.AFTERAPPLY, version, new ActionObjectEntity<>(baseLM.getFormApplied()));
        addActionsOnEvent(FormEventType.QUERYOK, version, new ActionObjectEntity<>(baseLM.getFormOk()));
        addActionsOnEvent(FormEventType.QUERYCLOSE, version, new ActionObjectEntity<>(baseLM.getFormClose()));
    }

    public Map<FormEvent, AsyncEventExec> getAsyncExecMap(FormInstanceContext context) {
        Map<FormEvent, AsyncEventExec> asyncExecMap = new HashMap<>();

        for(EventAction eventAction : eventActions.getListIt()) {
            FormServerEvent formEvent = eventAction.event;
            FormEvent formClientEvent = FormServerEvent.getEventObject(formEvent);
            if(formClientEvent != null) {
                AsyncEventExec asyncEventExec = getAsyncEventExec(formEvent, context);
                if (asyncEventExec != null) {
                    asyncExecMap.put(formClientEvent, asyncEventExec);
                }
            }
        }
        return asyncExecMap;
    }

    // form events
    public AsyncEventExec getAsyncEventExec(FormServerEvent formEvent, FormInstanceContext context) {
        AsyncEventExec asyncEventExec = null;
        for(ActionObjectEntity<?> eventAction : getEventActionsListIt(formEvent))
            asyncEventExec = eventAction.getAsyncEventExec(context, null, null, null,  false);
        if (asyncEventExec == null && formEvent instanceof FormServerScheduler) // important to have all form schedulers on the client
            asyncEventExec = AsyncNoWaitExec.instance;
        return asyncEventExec;
    }

    private void initDefaultGroupElements(GroupObjectEntity group, Version version) {
        if(group.getNFViewType(version).isList() && !group.isInTree()) {
            BaseLogicsModule baseLM = ThreadLocalContext.getBaseLM();

            PropertyDrawEntity propertyDraw = addPropertyDraw(baseLM.count, version);
            group.count = propertyDraw;
            propertyDraw.setAddParent(group, (Function<GroupObjectEntity, PropertyDrawEntity>) g -> g.count);
            propertyDraw.setSID("COUNT(" + group.getSID() + ")");
            propertyDraw.setToDraw(group, this, version);
            propertyDraw.setIntegrationSID(PropertyDrawEntity.NOEXTID, version); // we want to exclude this property from all integrations / apis / reports (use only in interactive view)
            propertyDraw.setPropertyExtra(addPropertyObject(baseLM.addJProp(baseLM.isPivot, new LP(group.getNFListViewType(version)))), PropertyDrawExtraType.SHOWIF, version);
            propertyDraw.setIgnoreHasHeaders(true, version);
        }
    }

    public void finalizeInit(Version version) {
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
        FormView richDesign = view;
        if (richDesign != null)
            richDesign.addRegularFilterGroup(group, version);
    }
    
    public void addRegularFilter(RegularFilterGroupEntity filterGroup, RegularFilterEntity filter, boolean isDefault, Version version) {
        filterGroup.addFilter(filter, isDefault, version);

        FormView richDesign = view;
        if (richDesign != null)
            richDesign.addRegularFilter(filterGroup, filter, version);
    }

    public IDGenerator genID;

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

    public GroupObjectEntity getNFGroupObject(String sID, Version version) {
        for (GroupObjectEntity group : getNFGroupsIt(version)) {
            if (group.getSID().equals(sID)) {
                return group;
            }
        }
        return null;
    }

    public GroupObjectEntity getNFGroupObject(String sID, Version version, GroupObjectEntity except) {
        for (GroupObjectEntity group : getNFGroupsIt(version)) {
            if (group != except && group.getSID().equals(sID)) {
                return group;
            }
        }
        return null;
    }

    public GroupObjectEntity getNFGroupObject(String sID, Version version, boolean allowRead) {
        for (GroupObjectEntity group : getNFGroupsIt(version, allowRead)) {
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
            ObjectEntity object = group.getObject(sid);
            if(object != null)
                return object;
        }
        return null;
    }

    public ObjectEntity getNFObject(String sid, Version version) {
        for (ObjectEntity object : getNFObjectsIt(version))
            if (object.getSID().equals(sid)) {
                return object;
            }
        return null;
    }

    public ObjectEntity getNFObject(String sid, Version version, ObjectEntity except) {
        for (ObjectEntity object : getNFObjectsIt(version))
            if (object != except && object.getSID().equals(sid)) {
                return object;
            }
        return null;
    }

    public TreeGroupEntity getNFTreeGroupObject(String sid, Version version, TreeGroupEntity except) {
        for (TreeGroupEntity object : getNFTreeGroupsIt(version))
            if (object != except && object.getSID().equals(sid)) {
                return object;
            }
        return null;
    }

    public List<String> getNFObjectsNamesAndClasses(List<ValueClass> classes, Version version) {
        List<String> names = new ArrayList<>();
        classes.clear();

        for (ObjectEntity object : getNFObjectsIt(version)) {
            names.add(object.getSID());
            classes.add(object.baseClass);
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

    public RegularFilterGroupEntity getRegularFilterGroup(String sid) {
        for (RegularFilterGroupEntity filterGroup : getRegularFilterGroupsIt()) {
            if (filterGroup.getSID().equals(sid)) {
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
    public <P extends PropertyInterface> ImMap<GroupObjectEntity, ImSet<ContextFilterEntity<?, P, ObjectEntity>>> getGroupContextFilters(ImSet<ContextFilterEntity<?, P, ObjectEntity>> filters, ImSet<GroupObjectEntity> excludeGroupObjects) {
        return filters.group(key -> {
            GroupObjectEntity applyObject = getApplyObject(key.getObjects(), excludeGroupObjects);
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

    public <P extends PropertyInterface> Pair<InputFilterEntity<?, P>, ImOrderMap<InputOrderEntity<?, P>, Boolean>> getInputFilterAndOrderEntities(ObjectEntity object, ImSet<ContextFilterEntity<?, P, ObjectEntity>> contextFilters, ImRevMap<ObjectEntity, P> mapObjects) {
        assert object.baseClass instanceof CustomClass;
        GroupObjectEntity groupObject = object.groupTo;
        assert groupObject.getObjects().size() == 1;
        assert !mapObjects.containsKey(object);

        ImSet<FilterEntity> filters = getGroupFixedFilters(SetFact.EMPTY(), true).get(groupObject);
        if(filters == null)
            filters = SetFact.EMPTY();
        ImSet<? extends ContextFilterEntity<?, P, ObjectEntity>> contextFilterEntities = filters.mapSetValues((FilterEntity filterEntity) -> ((FilterEntity<?>) filterEntity).getContext());

        ImSet<ContextFilterEntity<?, P, ObjectEntity>> contextGroupFilters = getGroupContextFilters(contextFilters, SetFact.EMPTY()).get(groupObject);
        if(contextGroupFilters == null)
            contextGroupFilters = SetFact.EMPTY();

        ImOrderMap<OrderEntity, Boolean> orders = getGroupOrdersList(SetFact.EMPTY()).get(groupObject);
        if(orders == null)
            orders = MapFact.EMPTYORDER();

        InputFilterEntity<?, P> inputFilter = groupObject.getInputFilterEntity(SetFact.addExclSet(contextFilterEntities, contextGroupFilters), mapObjects);

        MOrderExclMap<InputOrderEntity<?, P>, Boolean> mInputOrders = MapFact.mOrderExclMapMax(orders.size());
        for(int i = 0, size = orders.size(); i < size ; i++) {
            OrderEntity<?, ?> key = orders.getKey(i);
            InputOrderEntity<?, P> inputOrder = key.getInputOrderEntity(object, mapObjects);
            if(inputOrder != null)
                mInputOrders.exclAdd(inputOrder, orders.getValue(i));
        }
        ImOrderMap<InputOrderEntity<?, P>, Boolean> inputOrders = mInputOrders.immutableOrder();

        return new Pair<>(inputFilter, inputOrders);
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
    public ImMap<GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> getAllGroupProperties() {
        return ((ImOrderSet<PropertyDrawEntity>)getPropertyDrawsList()).groupOrder(key -> {
            GroupObjectEntity applyObject = key.getToDraw(FormEntity.this);
            return applyObject == null ? GroupObjectEntity.NULL : applyObject;
        });
    }

    @IdentityLazy
    public ImSet<GroupObjectEntity> getAllGroupColumns() {
        return getPropertyDrawsList().getCol().mapMergeSetSetValues(propertyDraw -> propertyDraw.getColumnGroupObjects().getSet());
    }

    public ImOrderSet<PropertyDrawEntity> getStaticPropertyDrawsList() {
        return ((ImOrderSet<PropertyDrawEntity>)getPropertyDrawsList()).filterOrder(element -> element.isStaticProperty() && element.getIntegrationSID() != null && element != logMessagePropertyDraw);
    }

    // assumes that there is an equals check for a PropertyObjectEntity
    // here may be some context scoped cache should
    @IdentityLazy
    public <X extends PropertyInterface, T extends PropertyInterface> ImList<PropertyDrawEntity> findChangedProperties(OrderEntity<?, ?> changeProp, boolean toNull) {
        MList<PropertyDrawEntity> mProps = null;
        FormInstanceContext context = FormInstanceContext.CACHE(this);
        for(PropertyDrawEntity property : getPropertyDrawsList()) {
            PropertyObjectEntity<T> valueProperty;
            if(property.isProperty(context)) {
                if ((valueProperty = property.getAssertCellProperty(context)).mapping.valuesSet().containsAll(changeProp.getObjects()) &&
                        (!(changeProp instanceof PropertyMapImplement) || Property.depends(valueProperty.property, ((PropertyMapImplement<?, ?>) changeProp).property)) && // optimization
                        valueProperty.property.isChangedWhen(toNull, changeProp.getImplement(valueProperty.mapping.reverse()))) {
                    if (mProps == null)
                        mProps = ListFact.mList();
                    mProps.add(property);
                }
            }
        }
        if(mProps != null)
            return mProps.immutableList();
        return null;
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
                result.put(groups.getKey(i).getIntegrationSIDValue(), groups.getValue(i).serialize());
            return result;
        }
    }

    public MetaExternal getMetaExternal(FormInstanceContext context) {
        final ImMap<GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> groupProperties = getAllGroupProperties();

        return new MetaExternal(getGroups().mapValues((GroupObjectEntity group) -> {
            ImOrderSet<PropertyDrawEntity> properties = groupProperties.get(group);
            if(properties == null)
                properties = SetFact.EMPTYORDER();

            return new GroupMetaExternal(properties.getSet().mapValues((PropertyDrawEntity property) ->  {
                    AsyncEventExec asyncEventExec = ((PropertyDrawEntity<?, ?>) property).getAsyncEventExec(context, CHANGE, true);
                    Boolean newDelete = asyncEventExec instanceof AsyncAddRemove ? ((AsyncAddRemove) asyncEventExec).add : null;
                    return new PropMetaExternal(ThreadLocalContext.localize(property.getCaption()), property.isProperty(context) ? property.getExternalType(context).getJSONType() : "action", newDelete);
                }));
        }));
    }

    @IdentityLazy
    public ImMap<GroupObjectEntity, ImOrderMap<OrderEntity, Boolean>> getGroupOrdersList(final ImSet<GroupObjectEntity> excludeGroupObjects) {
        return BaseUtils.immutableCast(getDefaultOrdersList().mapOrderKeyValues((Function<PropertyDrawEntity, OrderEntity>) PropertyDrawEntity::getOrder, value -> value).mergeOrder(getFixedOrdersList()).groupOrder(key -> {
            GroupObjectEntity groupObject = key.getApplyObject(FormEntity.this, excludeGroupObjects);
            if(groupObject == null)
                return GroupObjectEntity.NULL;
            return groupObject;
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
    public boolean hasNoChange(FormChangeFlowType type) {
        for (PropertyDrawEntity property : getPropertyDrawsIt()) {
            // in theory it is possible to support securityPolicy, but in this case we have to drag it through hasFlow + do some complex caching
            ActionObjectEntity<?> eventAction = property.getCheckedEventAction(CHANGE, FormInstanceContext.CACHE(this));
            if (eventAction != null && eventAction.property.hasFlow(type) && !eventAction.property.endsWithApplyAndNoChangesAfterBreaksBefore(type))
                return false;
        }

        return true;
    }

    public TreeGroupEntity addTreeGroupObject(TreeGroupEntity treeGroup, Version version) {
        treeGroups.add(treeGroup, version);

        FormView richDesign = view;
        if (richDesign != null)
            richDesign.addTreeGroup(treeGroup, version);

        return treeGroup;
    }

    public void moveTreeGroupObject(TreeGroupEntity tree, ComplexLocation<GroupObjectEntity> location, Version version) {
        ImOrderSet<GroupObjectEntity> groups = tree.getGroups();
        for(GroupObjectEntity groupObject : location.isReverseList() ? groups.reverseOrder() : groups)
            this.groups.add(groupObject, location, version);

        if (view != null)
            view.moveTreeGroup(tree.view, location, version);

        updatePropertyDraws(version);
    }

    public void addGroupObject(GroupObjectEntity group, Version version) {
        groups.add(group, ComplexLocation.DEFAULT(), version);
        if (view != null)
            view.addGroupObject(group, version);

        initDefaultGroupElements(group, version);

        group.fillGroupChanges(version);
    }

    public void moveGroupObject(GroupObjectEntity group, ComplexLocation<GroupObjectEntity> location, Version version) {
        groups.add(group, location, version);
        if (view != null)
            view.moveGroupObject(group.view, location, version);

        updatePropertyDraws(version);
    }

    public void addObject(ObjectEntity object, Version version) {
        objects.add(object, version);
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

    public PropertyDrawEntity addPropertyDraw(LAP property, Version version) {
        return addPropertyDraw(property, version, SetFact.EMPTYORDER());
    }

    public <P extends PropertyInterface> PropertyDrawEntity addPropertyDraw(LAP<P, ?> property, Version version, ImOrderSet<ObjectEntity> objects) {
        return addPropertyDraw(property.createObjectEntity(objects), null, property.listInterfaces, version);
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

    // auto form constructors + initDefault elements
    public <P extends PropertyInterface, I extends PropertyInterface> PropertyDrawEntity<P, ?> addPropertyDraw(ActionOrPropertyObjectEntity<P, ?, ?> propertyImplement, Pair<ActionOrProperty, List<String>> inherited,
                                                                                                               ImOrderSet<P> interfaces, Version version) {
        return addPropertyDraw(propertyImplement, interfaces, inherited, false, version, null, null);
    }
    public <P extends PropertyInterface, I extends PropertyInterface> PropertyDrawEntity<P, ?> addPropertyDraw(ActionOrPropertyObjectEntity<P, ?, ?> propertyImplement,
                                                                                                               ImOrderSet<P> interfaces, Pair<ActionOrProperty, List<String>> inherited, boolean extend, Version version, DebugInfo.DebugPoint debugPoint, String alias) {

        ActionOrProperty inheritedProperty = inherited != null ? inherited.first : propertyImplement.property;

        String propertySID;
        if(alias != null) {
            propertySID = alias;
        } else if (inheritedProperty.isNamed() && interfaces != null) {
            propertySID = PropertyDrawEntity.createSID(inheritedProperty.getName(), inherited != null ? inherited.second : PropertyDrawEntity.getMapping(propertyImplement, interfaces));
        } else {
            propertySID = "propertyDraw" + version.getOrder() + propertyDraws.size(version);
        }

        PropertyDrawEntity<P, ?> propertyDraw;
        if(extend)
            return (PropertyDrawEntity<P, ?>) getNFPropertyDraw(propertySID, version);
        else {
            propertyDraw = new PropertyDrawEntity<>(genID, propertySID, propertyImplement, inheritedProperty, debugPoint);
            propertyDraws.add(propertyDraw, ComplexLocation.DEFAULT(), version);
            if (view != null)
                view.addPropertyDraw(propertyDraw, version);

            propertyDraw.proceedDefaultDraw(this, version);
        }

        return propertyDraw;
    }

    public void movePropertyDraw(PropertyDrawEntity propertyDraw, ComplexLocation<PropertyDrawEntity> location, Version version) {
        propertyDraws.move(propertyDraw, location, version);
        if (view != null)
            view.movePropertyDraw(propertyDraw.view, location.map(view::get), version);
    }

    public void updatePropertyDraws(Version version) {
        for(PropertyDrawEntity property : getNFPropertyDrawsIt(version))
            updatePropertyDraw(property, version);
    }
    public void updatePropertyDraw(PropertyDrawEntity propertyDraw, Version version) {
        if (view != null)
            view.updatePropertyDrawContainer(propertyDraw.view, version);
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

    public PropertyDrawEntity<?, ?> getPropertyDraw(int iID) {
        for (PropertyDrawEntity propertyDraw : getPropertyDrawsIt()) {
            if (propertyDraw.getID() == iID) {
                return propertyDraw;
            }
        }

        return null;
    }

    public PropertyDrawEntity<?, ?> getPropertyDraw(String sid) {
        for (PropertyDrawEntity propertyDraw : getPropertyDrawsIt()) {
            if (propertyDraw.getSID().equals(sid)) {
                return propertyDraw;
            }
        }

        return null;
    }

    public PropertyDrawEntity<?, ?> getPropertyDrawIntegration(String sID) {
        for (PropertyDrawEntity propertyDraw : getPropertyDrawsIt()) {
            if (BaseUtils.nullEquals(propertyDraw.getIntegrationSID(), sID)) {
                return propertyDraw;
            }
        }

        return null;
    }

    public PropertyDrawEntity<?, ?> getNFPropertyDraw(String sid, Version version) {
        return getNFPropertyDraw(sid, version, null);
    }
    public PropertyDrawEntity<?, ?> getNFPropertyDraw(String sid, Version version, PropertyDrawEntity except) {
        if (sid == null) {
            return null;
        }
        for (PropertyDrawEntity propertyDraw : getNFPropertyDrawsIt(version)) {
            if (sid.equals(propertyDraw.getSID()) && propertyDraw != except) {
                return propertyDraw;
            }
        }

        return null;
    }

    public boolean noClasses() {
        return false;
    }

    public PropertyDrawEntity<?, ?> getNFPropertyDraw(String name, List<String> mapping, Version version) {
        return getNFPropertyDraw(PropertyDrawEntity.createSID(name, mapping), version);
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

    public FormView view;

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
        ImOrderSet<PropertyDrawEntity> properties = getAllGroupProperties().get(group);
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

    public void addActionsOnEvent(FormServerEvent eventObject, Version version, ActionObjectEntity<?> action) {
        eventActions.add(new EventAction(eventObject, action), version);
    }

    public void removeActionsOnEvent(FormServerEvent eventObject, Version version) {
        eventActions.removeAll(eventAction -> eventAction.event.equals(eventObject), version);
    }

    public ComponentView getDrawComponent(PropertyDrawEntity<?, ?> property) {
        FormView formView = view;
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

    @IdentityLazy
    public boolean isCalendarCompletePeriod(GroupObjectEntity entity) {
        return getField(entity, "dateTo", "dateTimeTo") != null;
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
            if (property.isList(this) && property.getPropertyExtra(PropertyDrawExtraType.FOOTER) != null)
                return true;
        }
        return false;
    }

    private boolean finalizedChanges;

    public void finalizeAndPreread() { // need to preread to fill FILTER + ORDER
        finalizeAroundInit();
        prereadEventActions();
    }
    public void finalizeAroundInit() {
        // we need this synchronization since forms finalization first marks modules, and only then reads all unnamed forms (so form can be finalized twice)
        // unlike properties finalization it seems that here we can solve finalization problem another way (by adding synchronized to the addAutoFormEntity, getAllModuleForms methods)
        // however a) it won't be that pretty either b) the used approach is similar to the one used for properties finalization
        if (!finalizedChanges) {
            synchronized (this) { // in theory there can be separate lock, but for now there is no need for this
                if (!finalizedChanges) {
                    finalizeChanges();
                    finalizedChanges = true;
                }
            }
        }
    }

    private void finalizeChanges() {

        for(GroupObjectEntity group : getGroupsIt()) {
            if(group.getListViewTypeValue().isMap() && !isMap(group)) {
                throw new RuntimeException(getCreationPath() + " none of required MAP propertyDraws found (longitude, latitude or polygon)");
            }
        }

        for(GroupObjectEntity group : getGroupsIt()) {
            if(group.getListViewTypeValue().isCalendar()) {
                if (!isCalendarDate(group) && !isCalendarDateTime(group))
                    throw new RuntimeException(getCreationPath() + " none of required CALENDAR propertyDraws found (date, dateFrom, dateTime or dateTimeFrom)");
                if (isCalendarPeriod(group) && !isCalendarCompletePeriod(group)) // If dateFrom/dateTimeFrom are added to the form, but dateTo/dateTimeTo are not added, an error occurs when setting viewFilters
                    throw new RuntimeException(getCreationPath() + " none of required CALENDAR period propertyDraws found (dateTo or dateTimeTo)");
            }
        }

        checkInternalClientAction();

        groups.finalizeChanges();
        objects.finalizeChanges();
        treeGroups.finalizeChanges();
        propertyDraws.finalizeChanges();
        fixedFilters.finalizeChanges();
        eventActions.finalizeChanges();
        defaultOrders.finalizeChanges();
        fixedOrders.finalizeChanges();
        
        hintsIncrementTable.finalizeChanges();
        hintsNoUpdate.finalizeChanges();
        
        for(RegularFilterGroupEntity regularFilterGroup : getRegularFilterGroupsIt())
            regularFilterGroup.finalizeAroundInit();

        finalizeDesignAroundInit();
    }

    protected void finalizeDesignAroundInit() {
        view.finalizeAroundInit();
    }

    public void prereadAutoIcons(ConnectionContext context) {
        for(PropertyDrawEntity property : getPropertyDrawsIt())
            property.getImage(context);

        view.prereadAutoIcons(context);
    }

    @IdentityLazy
    public boolean hasHeaders(GroupObjectEntity entity) {
        for (PropertyDrawEntity property : getProperties(entity))
            if (property.isList(this) && !property.isIgnoreHasHeaders() && property.getDrawCaption() != null)
                return true;
        return false;
    }

    private void checkInternalClientAction() {
        Version version = Version.current();
        Iterable<ActionObjectEntity<?>> eventActionsNFList = getNFEventActionsListIt(FormEventType.INIT, version);
        if (eventActionsNFList != null) {
            for (ActionObjectEntity<?> actionObjectEntity : eventActionsNFList) {
                for (ActionMapImplement<?, ?> actionMapImplement : actionObjectEntity.property.getList()) {
                    if (actionMapImplement.hasFlow(ChangeFlowType.INTERNALASYNC)) {
                        ActionMapImplement<?, PropertyInterface> internalClientAction = PropertyFact
                                .createJoinAction(new InternalClientAction(ListFact.EMPTY(), ListFact.EMPTY(), true),
                                        PropertyFact.createStatic(LocalizedString.create("empty.js", false), StringClass.text));
                        addActionsOnEvent(FormEventType.INIT, version, new ActionObjectEntity<>(internalClientAction.action, MapFact.EMPTYREV()));
                        return;
                    }
                }
            }
        }
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getName() {
        if(isNamed())
            return CanonicalNameUtils.getName(canonicalName);
        return null;
    }

    private String integrationSID;
    
    public String getIntegrationSID() {
        return integrationSID != null ? integrationSID : getName();
    }

    public void setIntegrationSID(String integrationSID) {
        this.integrationSID = integrationSID;
    }

    public String getCreationPath() {
        return debugPoint != null ? debugPoint.toString() : null;
    }

    public String getPath() {
        return debugPoint != null ? debugPoint.path : null;
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
        for(PropertyDrawEntity<?, ?> property : getPropertyDrawsIt())
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
    public ImSet<ComponentView> getPropertyComponents() {
        MExclSet<ComponentView> mComponents = SetFact.mExclSet();
        view.mainContainer.fillPropertyComponents(mComponents);
        return mComponents.immutable();
    }

    @IdentityLazy
    public ImSet<ComponentView> getBaseComponents() {
        MExclSet<ComponentView> mContainers = SetFact.mExclSet();
        view.mainContainer.fillBaseComponents(mContainers, false);
        return mContainers.immutable();
    }

    public LocalizedString getCaption() {
        return view.mainContainer.getCaption();
    }

    public AppServerImage getImage(ConnectionContext context) {
        return view.mainContainer.getImage(view, context);
    }

    public String getLocalizedCaption() {
        return ThreadLocalContext.localize(getCaption());
    }

    public void setEditType(PropertyDrawEntity property, PropertyEditType editType, Version version) {
        property.setEditType(editType, version);
    }
    
    public void addUserFilter(PropertyDrawEntity property, Version version) {
        FormView richDesign = view;
        if(richDesign !=null)
            richDesign.addFilter(property, version);
    }

    public void addDefaultOrder(PropertyDrawEntity property, boolean descending, Version version) {
        defaultOrders.add(property, descending, version);

        if(view != null)
            view.addDefaultOrder(property, descending, version);
    }

    public void addDefaultOrderFirst(PropertyDrawEntity property, boolean descending, Version version) {
        defaultOrders.addFirst(property, descending, version);

        if(view != null)
            view.addDefaultOrderFirst(property, descending, version);
    }

    public void addPivotColumn(PropertyDrawEntityOrPivotColumn column, Version version) {
        pivotColumns.add(ListFact.singleton(column), version);
    }

    public void addPivotColumns(List<List<PropertyDrawEntityOrPivotColumn>> columns, Version version) {
        for(List<PropertyDrawEntityOrPivotColumn> column : columns) {
            ImList<PropertyDrawEntityOrPivotColumn> columnList = ListFact.fromJavaList(column);
            pivotColumns.add(columnList, version);
            addPivotColumnView(columnList, version);
        }
    }

    public void addPivotRow(PropertyDrawEntityOrPivotColumn row, Version version) {
        pivotRows.add(ListFact.singleton(row), version);
    }

    public void addPivotRows(List<List<PropertyDrawEntityOrPivotColumn>> rows, Version version) {
        for(List<PropertyDrawEntityOrPivotColumn> row : rows) {
            ImList<PropertyDrawEntityOrPivotColumn> rowList = ListFact.fromJavaList(row);
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

    private void addPivotColumnView(ImList<PropertyDrawEntityOrPivotColumn> column, Version version) {
        FormView richDesign = view;
        if(richDesign !=null)
            richDesign.addPivotColumn(column, version);
    }
    private void addPivotRowView(ImList<PropertyDrawEntityOrPivotColumn> column, Version version) {
        FormView richDesign = view;
        if(richDesign !=null)
            richDesign.addPivotRow(column, version);
    }
    private void addPivotMeasureView(PropertyDrawEntity column, Version version) {
        FormView richDesign = view;
        if(richDesign !=null)
            richDesign.addPivotMeasure(column, version);
    }

    public void setNeedVerticalScroll(boolean scroll) {
        for (GroupObjectEntity entity : getGroupsIt()) {
            view.get(entity).needVerticalScroll = scroll;
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
        String caption = getLocalizedCaption();
        if (caption != null) {
            result += " '" + caption + "'";
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
    public FormEntity getStaticForm(BusinessLogics BL) {
        return this;
    }

    Map<String, Integer> formsCount = new HashMap<>();
    @Override
    public Pair<FormEntity, ImRevMap<ObjectEntity, ObjectEntity>> getForm(BusinessLogics BL, DataSession session, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues) throws SQLException, SQLHandledException {
        if(isNamed()) {
            String name = getName();
            String canonicalName = getCanonicalName();
            String extendCode = session != null ? (String) BL.systemEventsLM.extendCode.read(session, new DataObject(canonicalName)) : null;
            if (extendCode != null) {
                try {
                    String script = "FORM " + name + " EXTEND FORM " + canonicalName + ";\n" + extendCode + ";\nrun{}";
                    Pair<LA, EvalScriptingLogicsModule> evalResult = BL.LM.evaluateRun(script, Collections.emptySet(), false);

                    FormEntity copyForm = evalResult.second.findForm(name);
                    copyForm.customizeForm = this;
                    copyForm.customizeLM = evalResult.second;

                    return new Pair<>(copyForm, getObjects().mapRevKeys((ObjectEntity obj) -> copyForm.getExEntity(obj)));
                } catch (ScriptingErrorLog.SemanticErrorException e) {
                    throw Throwables.propagate(e);
                }
            }
        }
        return new Pair<>(this, getObjects().toRevMap());
    }

    public void prereadEventActions() {
        // not sure if we need this, because the platform at first shows optimistic list, so we won't have much benefits from this
//        MSet<Property> mAsyncInitPropertyChanges = SetFact.mSet();
        prereadEventActions((action, property) -> {
//            if(property != null) {
//                AsyncMapEventExec<?> asyncEventExec = action.property.getAsyncEventExec(property.optimisticAsync);
//                if(asyncEventExec instanceof AsyncMapInput) {
//                    InputListEntity<?, ?> list = ((AsyncMapInput<?>) asyncEventExec).list;
//                    if(list != null)
//                        mAsyncInitPropertyChanges.add(list.getProperty());
//                }
//            }
        }); // need this to generate default event actions (which will generate auto forms, and for example fill GroupObjectEntity.FILTER props, what is important to do before form is used)
//        asyncInitPropertyChanges = mAsyncInitPropertyChanges.immutable();
    }

    private void prereadEventActions(BiConsumer<ActionObjectEntity<?>, PropertyDrawEntity<?, ?>> consumer) {
        FormInstanceContext context = getGlobalContext();
        for(PropertyDrawEntity<?, ?> propertyDraw : getPropertyDrawsIt()) {
            for(String changeEvent : propertyDraw.getAllPropertyEventActions(context)) {
                ActionObjectEntity<?> editAction = propertyDraw.getCheckedEventAction(changeEvent, context);
                if (editAction != null)
                    consumer.accept(editAction, propertyDraw);
            }
        }
        for(EventAction eventAction : getEventActionsIt()) {
            consumer.accept(eventAction.action, null);
        }
    }

    @IdentityStrongLazy
    public <X extends PropertyInterface> ActionObjectEntity<?> getShareAction() {
        ImOrderSet<ObjectEntity> objects = getObjects().toOrderSet();
        String objectsString = objects.toString((i, object) -> object.getSID() + "=$" + (i + 1) + " NULL", ",");
        ImList<Type> objectsTypes = objects.mapListValues(ObjectEntity::getType);
        String script = "NEWSESSION SHOW " + getCanonicalName() + (objectsString.isEmpty() ? "" : " OBJECTS " + objectsString) + ";";

        BaseLogicsModule lm = ThreadLocalContext.getBaseLM();
        SystemEventsLogicsModule systemEventsLM = ThreadLocalContext.getSystemEventsLM();

        LP<?> targetProp = lm.getRequestedValueProperty(StringClass.text);

        ImRevMap<ObjectEntity, X> mapObjects = objects.mapOrderRevValues((int ID) -> (X) new PropertyInterface(ID));
        ImOrderSet<X> listInterfaces = objects.mapOrder(mapObjects);

        CallHTTPAction genUrlAction = new ExternalLSFAction(objectsTypes, ListFact.singleton(targetProp), true, true);

        // EXTERNAL LSF '' EVAL ACTION TO target()
        // shareAction(target())
        ActionMapImplement<?, X> shareAction = PropertyFact.createListAction(listInterfaces.getSet(),
                PropertyFact.createJoinAction(genUrlAction.getActionImplement(lm.addCProp(StringClass.text, LocalizedString.create("", false)).<X>getImplement(),
                        ListFact.add(PropertyFact.createStatic(LocalizedString.create(script, false), StringClass.text), BaseUtils.<ImOrderSet<PropertyInterfaceImplement<X>>>immutableCast(listInterfaces)))),
                PropertyFact.createJoinAction(systemEventsLM.shareAction.action, targetProp.<X>getImplement()));

        return shareAction.mapObjects(mapObjects.reverse());
    }


    private FormInstanceContext context;
    @ManualLazy
    public FormInstanceContext getGlobalContext() {
        if(context == null)
            context = new FormInstanceContext(this, view, null, false, false, false, false, false, null, null);
        return context;
    }

    @Override
    public FormSelector<ObjectEntity> merge(FormSelector formSelector) {
        if(!(formSelector instanceof FormEntity))
            return null;
        
        if(BaseUtils.hashEquals(this, formSelector))
            return this;
        
        return null;
    }

    public FormEntity(FormEntity src, ObjectMapping mapping) {
        super(src, mapping);

        customizeLM = src.customizeLM; // strictly for form customizing, not sure that needed at all, because customized form is customized once

        integrationSID = src.integrationSID;

        view = mapping.get(src.view);

        editActionPropertyDraw = mapping.get(src.editActionPropertyDraw);
        refreshActionPropertyDraw = mapping.get(src.refreshActionPropertyDraw);
        applyActionPropertyDraw = mapping.get(src.applyActionPropertyDraw);
        cancelActionPropertyDraw = mapping.get(src.cancelActionPropertyDraw);
        okActionPropertyDraw = mapping.get(src.okActionPropertyDraw);
        closeActionPropertyDraw = mapping.get(src.closeActionPropertyDraw);
        dropActionPropertyDraw = mapping.get(src.dropActionPropertyDraw);

        shareActionPropertyDraw = mapping.get(src.shareActionPropertyDraw);
        customizeActionPropertyDraw = mapping.get(src.customizeActionPropertyDraw);

        logMessagePropertyDraw = mapping.get(src.logMessagePropertyDraw);
    }

    @Override
    public void extend(FormEntity src, ObjectMapping mapping) {
        super.extend(src, mapping);

        mapping.sets(localAsync, src.localAsync);

        mapping.set(reportPathProp, src.reportPathProp);
    }

    public void add(FormEntity src, ObjectMapping mapping) {
        super.add(src, mapping);

        mapping.adds(hintsIncrementTable, src.hintsIncrementTable);
        mapping.adds(hintsNoUpdate, src.hintsNoUpdate);

        mapping.add(eventActions, src.eventActions);
        mapping.add(objects, src.objects);
        mapping.add(groups, src.groups);
        mapping.add(treeGroups, src.treeGroups);
        mapping.add(propertyDraws, src.propertyDraws);
        mapping.add(fixedFilters, src.fixedFilters);
        mapping.add(regularFilterGroups, src.regularFilterGroups);
        mapping.add(defaultOrders, src.defaultOrders);
        mapping.add(fixedOrders, src.fixedOrders);
        mapping.addl(pivotColumns, src.pivotColumns);
        mapping.addl(pivotRows, src.pivotColumns);
        mapping.add(pivotMeasures, src.pivotMeasures);

        forms.add(new Pair<>(src, mapping));
    }

    @Override
    public FormEntity copy(ObjectMapping mapping) {
        return new FormEntity(this, mapping);
    }

    public abstract static class ExProp<T, This extends ExProp<T, This>> implements MappingInterface<This> {
        public final NFProperty<Boolean> used = NFFact.property();
        public final ExSupplier<T> supplier;

        interface ExSupplier<T> {
            T getNF();

            T get();
        }

        static class LazySupplier<T> implements ExSupplier<T> {

            private final Supplier<T> delegate;
            private volatile T value;

            public LazySupplier(Supplier<T> delegate) {
                this.delegate = delegate;
            }

            public T getNF() {
                T result = value;
                if (result == null) {
                    synchronized (this) {
                        result = value;
                        if (result == null) {
                            result = delegate.get();
                            value = result;
                        }
                    }
                }
                return result;
            }

            public T get() {
                return value;
            }
        }
        static class MapSupplier<T> implements ExSupplier<T> {
            private final ExSupplier<T> supplier;
            private final Function<T, T> mapping;

            public MapSupplier(ExSupplier supplier, Function<T, T> mapping) {
                this.supplier = supplier;
                this.mapping = mapping;
            }

            @Override
            public T getNF() {
                return mapping.apply(supplier.getNF());
            }

            @Override
            public T get() {
                return mapping.apply(supplier.get());
            }
        }

        public ExProp(Supplier<T> supplier) {
            this.supplier = new LazySupplier<>(supplier);
        }

        public ExProp(This exProp, Function<T, T> mapping, ObjectMapping objectMapping) {
            objectMapping.sets(used, exProp.used);
            this.supplier = new MapSupplier<>(exProp.supplier, mapping);
        }

        public T getNF(Version version) {
            used.set(true, version);
            return supplier.getNF();
        }

        public T get() {
            return used.get() != null ? supplier.get() : null;
        }
    }
    public abstract static class ExMapProp<T extends MappingInterface<T>, This extends ExMapProp<T, This>> extends ExProp<T, This> {

        public ExMapProp(Supplier<T> supplier) {
            super(supplier);
        }

        public ExMapProp(This exProp, ObjectMapping mapping) {
            super(exProp, mapping::get, mapping);
        }
    }
    public static class ExProperty extends ExProp<Property<?>, ExProperty> {

        public ExProperty(Supplier<Property<?>> supplier) {
            super(supplier);
        }

        public ExProperty(ExProperty src, ObjectMapping mapping) {
            super(src, p -> p, mapping);
        }

        @Override
        public ExProperty get(ObjectMapping mapping) {
            return new ExProperty(this, mapping);
        }
    }

    private final List<Pair<FormEntity, ObjectMapping>> forms = Collections.synchronizedList(new ArrayList<>());

    public GroupObjectEntity getExEntity(GroupObjectEntity entity) {
        if(getGroups().contains(entity))
            return entity;

        for(Pair<FormEntity, ObjectMapping> form : forms) {
            GroupObjectEntity exEntity = form.first.getExEntity(entity);
            if(exEntity != null)
                return form.second.getFinal(exEntity);
        }
        return null;
    }

    public PropertyDrawEntity getExEntity(PropertyDrawEntity entity) {
        if(getPropertyDrawsList().contains(entity))
            return entity;

        for(Pair<FormEntity, ObjectMapping> form : forms) {
            PropertyDrawEntity exEntity = form.first.getExEntity(entity);
            if(exEntity != null)
                return form.second.getFinal(exEntity);
        }
        return null;
    }

    public ComponentView getExEntity(ComponentView entity) {
        if(view.getComponents().contains(entity))
            return entity;

        for(Pair<FormEntity, ObjectMapping> form : forms) {
            ComponentView exEntity = form.first.getExEntity(entity);
            if(exEntity != null)
                return form.second.getFinal(exEntity);
        }
        return null;
    }

    public RegularFilterGroupEntity getExEntity(RegularFilterGroupEntity entity) {
        if(getRegularFilterGroupsList().contains(entity))
            return entity;

        for(Pair<FormEntity, ObjectMapping> form : forms) {
            RegularFilterGroupEntity exEntity = form.first.getExEntity(entity);
            if(exEntity != null)
                return form.second.getFinal(exEntity);
        }
        return null;
    }

    public ObjectEntity getExEntity(ObjectEntity entity) {
        if(getObjects().contains(entity))
            return entity;

        for(Pair<FormEntity, ObjectMapping> form : forms) {
            ObjectEntity exEntity = form.first.getExEntity(entity);
            if(exEntity != null)
                return form.second.getFinal(exEntity);
        }
        return null;
    }

    public boolean isOrHasExEntity(FormEntity entity) {
        if(equals(entity))
            return true;

        for(Pair<FormEntity, ObjectMapping> form : forms)
            if(form.first.isOrHasExEntity(entity))
                return true;

        return false;
    }

    public FormEntity customizeForm;
    public FormEntity getCustomizeForm() {
        return nvl(customizeForm, this);
    }

}