package lsfusion.server.logics.form.interactive.instance;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.interop.action.*;
import lsfusion.interop.form.UpdateMode;
import lsfusion.interop.form.WindowFormType;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.interop.form.event.FormEventType;
import lsfusion.interop.form.event.FormScheduler;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.object.table.grid.user.design.ColumnUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;
import lsfusion.interop.form.object.table.grid.user.toolbar.FormGrouping;
import lsfusion.interop.form.order.Scroll;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.controller.stack.ParamMessage;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.stack.ThisMessage;
import lsfusion.server.base.controller.thread.AssertSynchronized;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.formula.FormulaUnionExpr;
import lsfusion.server.data.expr.formula.StringOverrideFormulaImpl;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.SubQueryExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.expr.where.classes.data.MatchWhere;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.sql.lambda.SQLFunction;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.controller.stack.SameThreadExecutionStack;
import lsfusion.server.logics.action.implement.ActionValueImplement;
import lsfusion.server.logics.action.interactive.UserInteraction;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.increment.IncrementChangeProps;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.change.modifier.OverridePropSourceSessionModifier;
import lsfusion.server.logics.action.session.change.modifier.SessionModifier;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.integral.DoubleClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.ConcreteObjectClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.FormCloseType;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.action.async.AsyncInput;
import lsfusion.server.logics.form.interactive.action.async.AsyncEventExec;
import lsfusion.server.logics.form.interactive.action.async.PushAsyncInput;
import lsfusion.server.logics.form.interactive.action.async.PushAsyncResult;
import lsfusion.server.logics.form.interactive.action.input.InputContext;
import lsfusion.server.logics.form.interactive.action.input.InputValueList;
import lsfusion.server.logics.form.interactive.changed.*;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.instance.design.BaseComponentViewInstance;
import lsfusion.server.logics.form.interactive.instance.design.ContainerViewInstance;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.filter.NotNullFilterInstance;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterGroupInstance;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterInstance;
import lsfusion.server.logics.form.interactive.instance.object.*;
import lsfusion.server.logics.form.interactive.instance.order.OrderInstance;
import lsfusion.server.logics.form.interactive.instance.property.*;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.interactive.property.Async;
import lsfusion.server.logics.form.interactive.property.AsyncMode;
import lsfusion.server.logics.form.interactive.property.GroupObjectProp;
import lsfusion.server.logics.form.interactive.property.PropertyAsync;
import lsfusion.server.logics.form.stat.print.FormReportManager;
import lsfusion.server.logics.form.stat.print.StaticFormReportManager;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.filter.FilterEntityInstance;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.admin.log.LogInfo;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.profiler.ProfiledObject;

import javax.swing.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static lsfusion.base.BaseUtils.deserializeObject;
import static lsfusion.base.BaseUtils.systemLogger;
import static lsfusion.interop.action.ServerResponse.CHANGE;
import static lsfusion.interop.action.ServerResponse.INPUT;
import static lsfusion.interop.form.order.user.Order.*;
import static lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance.*;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который
//  разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class FormInstance extends ExecutionEnvironment implements ReallyChanged, ProfiledObject, AutoCloseable {

    private final Function<ComponentView, PropertyObjectInstance<?>> GET_COMPONENT_SHOWIF =
            new Function<ComponentView, PropertyObjectInstance<?>>() {
                @Override
                public PropertyObjectInstance<?> apply(ComponentView key) {
                    return instanceFactory.getInstance(key.showIf);
                }
            };

    public final LogicsInstance logicsInstance;

    public final BusinessLogics BL;

    public final FormEntity entity;

    public final InstanceFactory instanceFactory;

    public final SecurityPolicy securityPolicy;

    private final ImOrderSet<GroupObjectInstance> groups;

    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    public final ImList<PropertyDrawInstance<?>> properties;

    public final ImSet<ObjectEntity> inputObjects;

    // "закэшированная" проверка присутствия в интерфейсе, отличается от кэша тем что по сути функция от mutable объекта
    protected Set<PropertyDrawInstance> isShown = new HashSet<>();
    protected Set<PropertyDrawInstance> isStaticShown = new HashSet<>();
    protected Set<ComponentView> isComponentHidden = new HashSet<>();
    protected Set<ComponentView> isBaseComponentHidden = new HashSet<>();
    private static <T> boolean addShownHidden(Set<T> isShownHidden, T property, boolean shownHidden) {
        if(shownHidden)
            return !isShownHidden.add(property);
        return isShownHidden.remove(property);
    }

    private final boolean checkOnOk;

    private final boolean isSync;
    private final boolean isModal;
    private final boolean isEditing;

    public boolean isSync() {
        return isSync;
    }
    public boolean isModal() {
        return isModal;
    }

    private final boolean manageSession;

    private final boolean showDrop;
    
    private final Locale locale;

    private boolean interactive = true; // важно для assertion'а в endApply

    private ImSet<ObjectInstance> objects;

    public boolean local = false; // временный хак для resolve'а, так как modifier очищается синхронно, а форма нет, можно было бы в транзакцию перенести, но там подмену modifier'а (resolveModifier) так не встроишь

    public FormInstance(FormEntity entity, LogicsInstance logicsInstance, ImSet<ObjectEntity> inputObjects, DataSession session, SecurityPolicy securityPolicy,
                        FocusListener focusListener, CustomClassListener classListener,
                        ImMap<ObjectEntity, ? extends ObjectValue> mapObjects,
                        ExecutionStack stack,
                        boolean isSync, Boolean noCancel, ManageSessionType manageSession, boolean checkOnOk,
                        boolean showDrop, boolean interactive, WindowFormType type,
                        boolean isExternal, ImSet<ContextFilterInstance> contextFilters,
                        boolean showReadOnly, Locale locale) throws SQLException, SQLHandledException {
        this.isSync = isSync;
        this.isModal = type.isModal();
        this.isEditing = type.isEditing();
        this.checkOnOk = checkOnOk;
        this.showDrop = showDrop;

        this.session = session;
        this.entity = entity;
        this.logicsInstance = logicsInstance;
        this.BL = logicsInstance.getBusinessLogics();
        this.inputObjects = inputObjects;

        if(showReadOnly)
            securityPolicy = securityPolicy.add(logicsInstance.getSecurityManager().getReadOnlySecurityPolicy(session));
        this.securityPolicy = securityPolicy;

        this.locale = locale;
        
        instanceFactory = new InstanceFactory();

        this.weakFocusListener = new WeakReference<>(focusListener);
        this.weakClassListener = new WeakReference<>(classListener);

        groups = entity.getGroupsList().mapOrderSetValues(instanceFactory::getInstance);
        ImOrderSet<GroupObjectInstance> groupObjects = getOrderGroups();

        ImMap<GroupObjectEntity, ImSet<PropertyDrawEntity>> groupPivotProps = null;
        ImMap<GroupObjectEntity, ImSet<PropertyDrawEntity>> groupMeasureProps = null;
        for (int i = 0, size = groupObjects.size(); i < size; i++) {
            GroupObjectInstance groupObject = groupObjects.get(i);
            GroupObjectEntity groupEntity = groupObject.entity;

            PropertyDrawEntity calendarDateProperty = entity.getField(groupEntity, "date", "dateFrom", "dateTime", "dateTimeFrom");
            if (calendarDateProperty != null)
                groupObject.setCalendarDateProperty(instanceFactory.getInstance(calendarDateProperty));

            groupObject.order = i;
            groupObject.setClassListener(classListener);
            if(groupObject.pageSize == null)
                groupObject.pageSize = entity.hasNoProperties(groupEntity) ? (entity.usedAsGroupColumn(groupEntity) ? 0 : 1) : Settings.get().getPageSizeDefaultValue();

            if(groupObject.viewType.isList()) {
                // should correspond RemoteForm.changeMode in general
                ListViewType listViewType = groupEntity.listViewType;

                if(listViewType != ListViewType.GRID) {
                    if (listViewType == ListViewType.PIVOT) {
                        if(groupEntity.asyncInit) // will wait for first changeGroupMode
                            groupObject.setUpdateMode(UpdateMode.MANUAL);
                        else {
                            // should correspond RemoteForm.changeMode method (block with changeGroupMode)
                            if (groupPivotProps == null) {
                                groupPivotProps = entity.getPivotGroupProps();
                                groupMeasureProps = entity.getPivotMeasureProps();
                            }
                            Function<PropertyDrawEntity, GroupColumn> propToColumn = prop -> new GroupColumn(instanceFactory.getInstance(prop), MapFact.EMPTY());

                            ImSet<GroupColumn> pivotColumns = SetFact.EMPTY();
                            ImSet<PropertyDrawEntity> pivotProps = groupPivotProps.get(groupEntity);
                            if (pivotProps != null)
                                pivotColumns = pivotProps.mapSetValues(propToColumn);

                            ImSet<GroupColumn> measureColumns = SetFact.EMPTY();
                            ImSet<PropertyDrawEntity> measureProps = groupMeasureProps.get(groupEntity);
                            if (measureProps != null)
                                measureColumns = measureProps.mapSetValues(propToColumn);

                            groupObject.changeGroupMode(GroupMode.create(pivotColumns, measureColumns, groupEntity.pivotOptions.getAggregation(), instanceFactory));
                        }
                    }
                    changePageSize(groupObject, listViewType == ListViewType.CALENDAR ? 10 : 1000); // GStateTableView.getDefaultPageSize
                }

                changeListViewType(groupObject, listViewType);
            }
        }

        for (TreeGroupEntity treeGroup : entity.getTreeGroupsIt()) {
            instanceFactory.getInstance(treeGroup); // чтобы зарегить ссылки
        }

        ImOrderSet<PropertyDrawEntity> propertyDraws = (ImOrderSet<PropertyDrawEntity>) entity.getPropertyDrawsList();
        MList<PropertyDrawInstance<?>> mProperties = ListFact.mListMax(propertyDraws.size());
        for (PropertyDrawEntity<?> propertyDrawEntity : propertyDraws)
            if (securityPolicy.checkPropertyViewPermission(propertyDrawEntity.getSecurityProperty())) {
                PropertyDrawInstance propertyDrawInstance = instanceFactory.getInstance(propertyDrawEntity);
                if (propertyDrawInstance.toDraw == null)
                    propertyDrawInstance.toDraw = instanceFactory.getInstance(propertyDrawEntity.getToDraw(entity));
                mProperties.add(propertyDrawInstance);
            }
        properties = mProperties.immutableList();

        ImSet<FilterEntityInstance> allFixedFilters = BaseUtils.immutableCast(entity.getFixedFilters());
        if (contextFilters != null)
            allFixedFilters = allFixedFilters.addExcl(contextFilters);
        ImMap<GroupObjectInstance, ImSet<FilterInstance>> fixedFilters = allFixedFilters.mapSetValues(value -> value.getInstance(instanceFactory)).group(key -> key.getApplyObject());
        for (int i = 0, size = fixedFilters.size(); i < size; i++)
            fixedFilters.getKey(i).fixedFilters = fixedFilters.getValue(i);
        for (GroupObjectInstance groupObject : groupObjects)
            groupObject.classFilter = new NotNullFilterInstance<>(FilterInstance.getPropertyObjectInstance(IsClassProperty.getProperty(getGridClasses(groupObject.objects))));

        for (RegularFilterGroupEntity filterGroupEntity : entity.getRegularFilterGroupsList()) {
            regularFilterGroups.add(instanceFactory.getInstance(filterGroupEntity));
        }

        for(Property property : entity.asyncInitPropertyChanges)
            asyncPropertyChanges.put(property, HasChanges.NULL);

        ImMap<GroupObjectInstance, ImOrderMap<OrderInstance, Boolean>> fixedOrders = entity.getFixedOrdersList().mapOrderKeys((Function<OrderEntity<?>, OrderInstance>) value -> value.getInstance(instanceFactory)).groupOrder(new BaseUtils.Group<GroupObjectInstance, OrderInstance>() {
            public GroupObjectInstance group(OrderInstance key) {
                return key.getApplyObject();
            }
        });
        for (int i = 0, size = fixedOrders.size(); i < size; i++)
            fixedOrders.getKey(i).fixedOrders = fixedOrders.getValue(i);

        MExclMap<ObjectEntity, ObjectValue> mSeekCachedObjects = MapFact.mExclMap();
        for (GroupObjectInstance groupObject : groupObjects) {
            UpdateType updateType = groupObject.getUpdateType();
            if (updateType != UpdateType.PREV) {
                groupObject.seek(updateType);
            } else {
                for (ObjectInstance object : groupObject.objects) {
                    // ставим на объекты из cache'а
                    if (object.getBaseClass() instanceof CustomClass && classListener != null) {
                        CustomClass cacheClass = (CustomClass) object.getBaseClass();
                        Long objectID = classListener.getObject(cacheClass);
                        mSeekCachedObjects.exclAdd(object.entity, session.getObjectValue(cacheClass, objectID));
                    }
                }
            }
        }
        mapObjects = MapFact.override(mSeekCachedObjects.immutable(), mapObjects);
        for (int i = 0, size = mapObjects.size(); i < size; i++)
            seekObject(instanceFactory.getInstance(mapObjects.getKey(i)), mapObjects.getValue(i));

        //устанавливаем фильтры и порядки по умолчанию...
        for (RegularFilterGroupInstance filterGroup : regularFilterGroups) {
            int defaultInd = filterGroup.entity.getDefault();
            if (defaultInd >= 0 && defaultInd < filterGroup.filters.size()) {
                setRegularFilter(filterGroup, filterGroup.filters.get(defaultInd));
            }
        }

        Set<GroupObjectInstance> wasOrder = new HashSet<>();
        ImOrderMap<PropertyDrawEntity<?>, Boolean> defaultOrders = entity.getDefaultOrdersList();
        for (int i=0,size=defaultOrders.size();i<size;i++) {
            PropertyDrawInstance property = instanceFactory.getInstance(defaultOrders.getKey(i));
            GroupObjectInstance toDraw = property.toDraw;
            Boolean ascending = defaultOrders.getValue(i);

            if(toDraw != null) {
                OrderInstance order = property.getOrder();
                toDraw.changeOrder(order, wasOrder.contains(toDraw) ? ADD : REPLACE);
                if (!ascending) {
                    toDraw.changeOrder(order, DIR);
                }
                wasOrder.add(toDraw);
            }
        }

        this.session.registerForm(this);
        
        boolean adjNoCancel, adjManageSession;
        if(interactive) {
            int prevOwners = updateSessionOwner(true, stack);

            if(manageSession == ManageSessionType.AUTO)
                adjManageSession = heuristicManageSession(entity, showReadOnly, prevOwners, session.isNested());
            else
                adjManageSession = manageSession.isManageSession();

            if(noCancel == null)
                adjNoCancel = heuristicNoCancel(mapObjects);
            else
                adjNoCancel = noCancel;
        } else { // deprecated ветка, в будущем должна уйти
            adjManageSession = false;
            adjNoCancel = false; // temp
        }

        this.manageSession = adjManageSession;
        environmentIncrement = createEnvironmentIncrement(isSync || adjManageSession, type, isExternal, adjNoCancel, adjManageSession, showDrop);

        MExclMap<SessionDataProperty, Pair<GroupObjectInstance, GroupObjectProp>> mEnvironmentIncrementSources = MapFact.mExclMap();
        for (GroupObjectInstance groupObject : groupObjects) {
            ImMap<GroupObjectProp, PropertyRevImplement<ClassPropertyInterface, ObjectInstance>> props = groupObject.props;
            for(int i = 0, size = props.size(); i<size; i++)
                mEnvironmentIncrementSources.exclAdd((SessionDataProperty) props.getValue(i).property, new Pair<>(groupObject, props.getKey(i)));
        }
        environmentIncrementSources = mEnvironmentIncrementSources.immutable();
        
        if (!interactive) // deprecated ветка, в будущем должна уйти
            getChanges(stack);
        
        processComponent(entity.getRichDesign().getMainContainer());

        this.interactive = interactive; // обязательно в конце чтобы assertion с endApply не рушить

        fireOnInit(stack);

        ServerLoggers.remoteLifeLog("FORM OPEN : " + this);
    }

    public void changeListViewType(GroupObjectInstance groupObject, ListViewType listViewType) throws SQLException, SQLHandledException {
        groupObject.changeListViewType(this, BL.LM.listViewType, listViewType);
    }

    public static class DiffForm {
        public final String type;
        public final FormEntity entity;
        public final String stackString;
        public final Boolean explicit;
        public final Boolean heur;

        public DiffForm(String type, FormEntity entity, String stackString, Boolean explicit, Boolean heur) {
            this.type = type;
            this.entity = entity;
            this.stackString = stackString;
            this.explicit = explicit;
            this.heur = heur;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DiffForm diffForm = (DiffForm) o;
            return Objects.equal(type, diffForm.type) &&
                    Objects.equal(entity, diffForm.entity) &&
                    Objects.equal(stackString, diffForm.stackString) &&
                    Objects.equal(explicit, diffForm.explicit) &&
                    Objects.equal(heur, diffForm.heur);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(type, entity, stackString, explicit, heur);
        }
    }

    private boolean heuristicManageSession(FormEntity entity, boolean showReadOnly, int prevOwners, boolean isNested) {
        return prevOwners <= 0 && !showReadOnly && (!entity.hasNoChange() || isNested);
    }

    private boolean heuristicNoCancel(ImMap<ObjectEntity, ? extends ObjectValue> mapObjects) {
        for(int i=0,size = mapObjects.size();i<size;i++) {
            ObjectEntity object = mapObjects.getKey(i);
            ObjectValue value = mapObjects.getValue(i);

            if (object.groupTo.isPanel() && value instanceof DataObject && !((DataObject) value).objectClass.inSet(object.baseClass.getUpSet()))
                return true;
        }
        return false;
    }

    private static IncrementChangeProps createEnvironmentIncrement(boolean showOk, WindowFormType type, boolean isExternal, boolean isAdd, boolean manageSession, boolean showDrop) throws SQLException, SQLHandledException {
        IncrementChangeProps environment = new IncrementChangeProps();
        environment.add(FormEntity.showOk, PropertyChange.STATIC(showOk));
        environment.add(FormEntity.isDocked, PropertyChange.STATIC(type == WindowFormType.DOCKED));
        environment.add(FormEntity.isEditing, PropertyChange.STATIC(type.isEditing()));
        environment.add(FormEntity.isAdd, PropertyChange.STATIC(isAdd));
        environment.add(FormEntity.manageSession, PropertyChange.STATIC(manageSession));
        environment.add(FormEntity.isExternal, PropertyChange.STATIC(isExternal));
        environment.add(FormEntity.showDrop, PropertyChange.STATIC(showDrop));
        return environment;
    }

    public ImSet<GroupObjectInstance> getGroups() {
        return groups.getSet();
    }
    
    private void processComponent(ComponentView component) throws SQLException, SQLHandledException {
        if (component instanceof ContainerView) {
            ContainerView container = (ContainerView) component;
            
            if (container.collapsed) {
                collapseContainer(container);
            }
            
            for (ComponentView childComponent : container.getChildrenIt()) {
                processComponent(childComponent);
            }
        }
        
        if (component.activated) {
            activateTab(component);
        }
    } 

    public ImOrderSet<GroupObjectInstance> getOrderGroups() {
        return groups;
    }

    public FormUserPreferences loadUserPreferences() {
        if (!entity.isNamed()) {
            return null;
        }
        
        List<GroupObjectUserPreferences> goUserPreferences = new ArrayList<>();
        List<GroupObjectUserPreferences> goGeneralPreferences = new ArrayList<>();
        try {

            ObjectValue formValue = BL.reflectionLM.formByCanonicalName.readClasses(session, new DataObject(entity.getCanonicalName(), StringClass.get(100)));
            if (formValue.isNull())
                return null;
            DataObject formObject = (DataObject) formValue;

            KeyExpr propertyDrawExpr = new KeyExpr("propertyDraw");

            Long userId = (Long) BL.authenticationLM.currentUser.read(session);
            DataObject currentUser = session.getDataObject(BL.authenticationLM.user, userId);

            Expr customUserExpr = currentUser.getExpr();

            ImRevMap<String, KeyExpr> newKeys = MapFact.singletonRev("propertyDraw", propertyDrawExpr);

            QueryBuilder<String, String> query = new QueryBuilder<>(newKeys);
            Expr groupObjectPropertyDrawExpr = BL.reflectionLM.groupObjectPropertyDraw.getExpr(propertyDrawExpr);
            
            query.addProperty("propertySID", BL.reflectionLM.sidPropertyDraw.getExpr(propertyDrawExpr));
            query.addProperty("groupObject", groupObjectPropertyDrawExpr);
            query.addProperty("groupObjectSID", BL.reflectionLM.sidGroupObject.getExpr(groupObjectPropertyDrawExpr));

            query.addProperty("generalShowPropertyName", BL.reflectionLM.nameShowPropertyDraw.getExpr(propertyDrawExpr));
            query.addProperty("generalCaption", BL.reflectionLM.columnCaptionPropertyDraw.getExpr(propertyDrawExpr));
            query.addProperty("generalPattern", BL.reflectionLM.columnPatternPropertyDraw.getExpr(propertyDrawExpr));
            query.addProperty("generalWidth", BL.reflectionLM.columnWidthPropertyDraw.getExpr(propertyDrawExpr));
            query.addProperty("generalOrder", BL.reflectionLM.columnOrderPropertyDraw.getExpr(propertyDrawExpr));
            query.addProperty("generalSort", BL.reflectionLM.columnSortPropertyDraw.getExpr(propertyDrawExpr));
            query.addProperty("generalAscendingSort", BL.reflectionLM.columnAscendingSortPropertyDraw.getExpr(propertyDrawExpr));
            query.addProperty("generalHasUserPreferences", BL.reflectionLM.hasUserPreferencesGroupObject.getExpr(groupObjectPropertyDrawExpr));
            query.addProperty("generalFontSize", BL.reflectionLM.fontSizeGroupObject.getExpr(groupObjectPropertyDrawExpr));
            query.addProperty("generalPageSize", BL.reflectionLM.pageSizeGroupObject.getExpr(groupObjectPropertyDrawExpr));
            query.addProperty("generalHeaderHeight", BL.reflectionLM.headerHeightGroupObject.getExpr(groupObjectPropertyDrawExpr));
            query.addProperty("generalIsFontBold", BL.reflectionLM.isFontBoldGroupObject.getExpr(groupObjectPropertyDrawExpr));
            query.addProperty("generalIsFontItalic", BL.reflectionLM.isFontItalicGroupObject.getExpr(groupObjectPropertyDrawExpr));

            query.addProperty("userShowPropertyName", BL.reflectionLM.nameShowPropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.addProperty("userCaption", BL.reflectionLM.columnCaptionPropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.addProperty("userPattern", BL.reflectionLM.columnPatternPropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.addProperty("userWidth", BL.reflectionLM.columnWidthPropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.addProperty("userOrder", BL.reflectionLM.columnOrderPropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.addProperty("userSort", BL.reflectionLM.columnSortPropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.addProperty("userAscendingSort", BL.reflectionLM.columnAscendingSortPropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.addProperty("userHasUserPreferences", BL.reflectionLM.hasUserPreferencesGroupObjectCustomUser.getExpr(groupObjectPropertyDrawExpr, customUserExpr));
            query.addProperty("userFontSize", BL.reflectionLM.fontSizeGroupObjectCustomUser.getExpr(groupObjectPropertyDrawExpr, customUserExpr));
            query.addProperty("userPageSize", BL.reflectionLM.pageSizeGroupObjectCustomUser.getExpr(groupObjectPropertyDrawExpr, customUserExpr));
            query.addProperty("userHeaderHeight", BL.reflectionLM.headerHeightGroupObjectCustomUser.getExpr(groupObjectPropertyDrawExpr, customUserExpr));
            query.addProperty("userIsFontBold", BL.reflectionLM.isFontBoldGroupObjectCustomUser.getExpr(groupObjectPropertyDrawExpr, customUserExpr));
            query.addProperty("userIsFontItalic", BL.reflectionLM.isFontItalicGroupObjectCustomUser.getExpr(groupObjectPropertyDrawExpr, customUserExpr));

            query.and(BL.reflectionLM.formPropertyDraw.getExpr(propertyDrawExpr).compare(formObject.getExpr(), Compare.EQUALS));
            query.and(BL.reflectionLM.hasUserPreferencesOverrideGroupObjectCustomUser.getExpr(groupObjectPropertyDrawExpr, customUserExpr).getWhere());

            ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> result = query.execute(this);

            for (ImMap<String, Object> values : result.valueIt()) {
                readPreferencesValues(values, goGeneralPreferences, true);
                readPreferencesValues(values, goUserPreferences, false);
            }
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }

        return new FormUserPreferences(goGeneralPreferences, goUserPreferences);
    }
    
    private ImSet<PropertyDrawInstance> userPrefsHiddenProperties = SetFact.EMPTY();
    
    public void refreshUPHiddenProperties(String groupObjectSID, String[] hiddenSids) {
        GroupObjectInstance go = getGroupObjectInstance(groupObjectSID);
        List<String> hiddenSidsList = new ArrayList<>(Arrays.asList(hiddenSids));
        
        Set<PropertyDrawInstance> hiddenProps = new HashSet<>(userPrefsHiddenProperties.toJavaSet()); // removing from singleton is not supported
        
        for (PropertyDrawInstance property : userPrefsHiddenProperties) {
            if (property.toDraw == go) {
                if (!hiddenSidsList.contains(property.getSID())) {
                    hiddenProps.remove(property);        
                } else {
                    hiddenSidsList.remove(property.getSID());
                }
            }
        }

        for (String sid : hiddenSidsList) {
            PropertyDrawInstance prop = getPropertyDraw(sid);
            if (prop != null) {
                hiddenProps.add(prop);
            }
        }
        
        userPrefsHiddenProperties = SetFact.fromJavaSet(hiddenProps);
    }
    
    public void readPreferencesValues(ImMap<String, Object> values, List<GroupObjectUserPreferences> goPreferences, boolean general) {
        String prefix = general ? "general" : "user";
        String propertyDrawSID = values.get("propertySID").toString().trim();
        Long groupObjectPropertyDraw = (Long) values.get("groupObject");

        if (groupObjectPropertyDraw != null) {
            String groupObjectSID = (String) values.get("groupObjectSID");

            String hide = (String) values.get(prefix + "ShowPropertyName");
            Boolean needToHide = hide == null ? null : hide.trim().endsWith("Hide");
            String caption = (String) values.get(prefix + "Caption");
            String pattern = (String) values.get(prefix + "Pattern");
            Integer width = (Integer) values.get(prefix + "Width");
            Integer order = (Integer) values.get(prefix + "Order");
            Integer sort = (Integer) values.get(prefix + "Sort");
            Boolean userAscendingSort = (Boolean) values.get(prefix + "AscendingSort");
            ColumnUserPreferences columnPrefs = new ColumnUserPreferences(needToHide, caption, pattern, width, order, sort, userAscendingSort != null ? userAscendingSort : (sort != null ? false : null));

            Integer pageSize = (Integer) values.get(prefix + "PageSize");
            Integer headerHeight = (Integer) values.get(prefix + "HeaderHeight");

            Object hasPreferences = values.get(prefix + "HasUserPreferences");
            Integer fontSize = (Integer) values.get(prefix + "FontSize");
            boolean isFontBold = values.get(prefix + "IsFontBold") != null;
            boolean isFontItalic = values.get(prefix + "IsFontItalic") != null;

            PropertyDrawInstance property = getPropertyDraw(propertyDrawSID);
            if(property == null) {
                //ServerLoggers.assertLog(false, "LoadUserPreferences property not found: " + propertyDrawSID);
            } else {
                if (userPrefsHiddenProperties.contains(property)) {
                    if (hasPreferences != null && (needToHide == null || !needToHide)) {
                        userPrefsHiddenProperties = userPrefsHiddenProperties.removeIncl(property);
                    }
                } else if (hasPreferences != null && needToHide != null && needToHide) {
                    userPrefsHiddenProperties = userPrefsHiddenProperties.addExcl(property);
                }
            }
            
            
            boolean prefsFound = false;
            for (GroupObjectUserPreferences groupObjectPreferences : goPreferences) {
                if (groupObjectPreferences.groupObjectSID.equals(groupObjectSID.trim())) {
                    groupObjectPreferences.getColumnUserPreferences().put(propertyDrawSID, columnPrefs);
                    if (!groupObjectPreferences.hasUserPreferences)
                        groupObjectPreferences.hasUserPreferences = hasPreferences != null;
                    if (groupObjectPreferences.fontInfo == null)
                        groupObjectPreferences.fontInfo = new FontInfo(null, fontSize, isFontBold, isFontItalic);
                    prefsFound = true;
                }
            }
            if (!prefsFound) {
                Map preferencesMap = new HashMap<>();
                preferencesMap.put(propertyDrawSID, columnPrefs);
                goPreferences.add(new GroupObjectUserPreferences(preferencesMap,
                        groupObjectSID.trim(),
                        new FontInfo(null, fontSize == null ? 0 : fontSize, isFontBold, isFontItalic),
                        pageSize, headerHeight, hasPreferences != null));
            }
        }    
    }

    public void saveUserPreferences(ExecutionStack stack, GroupObjectUserPreferences preferences, boolean forAllUsers, boolean completeOverride) {
        if (!entity.isNamed()) {
            return;
        }

        try (DataSession dataSession = session.createSession()) {
            List<DataObject> userObjectList = completeOverride ? readUserObjectList() : null;

            DataObject userObject = (!forAllUsers && !completeOverride) ? (DataObject) BL.authenticationLM.currentUser.readClasses(dataSession) : null;
            for (Map.Entry<String, ColumnUserPreferences> entry : preferences.getColumnUserPreferences().entrySet()) {
                ObjectValue propertyDrawObjectValue = BL.reflectionLM.propertyDrawByFormNameAndPropertyDrawSid.readClasses(
                        dataSession,
                        new DataObject(entity.getCanonicalName(), StringClass.get(false, false, 100)),
                        new DataObject(entry.getKey(), StringClass.get(false, false, 100)));
                if (propertyDrawObjectValue instanceof DataObject) {
                    DataObject propertyDrawObject = (DataObject) propertyDrawObjectValue;
                    ColumnUserPreferences columnPreferences = entry.getValue();
                    Long idShow = columnPreferences.userHide == null ? null : BL.reflectionLM.propertyDrawShowStatus.getObjectID(columnPreferences.userHide ? "Hide" : "Show");
                    if (completeOverride) {
                        for (DataObject user : userObjectList) {
                            changeUserColumnPreferences(columnPreferences, dataSession, idShow, propertyDrawObject, user);
                        }
                    }
                    if (forAllUsers) {
                        BL.reflectionLM.showPropertyDraw.change(idShow, dataSession, propertyDrawObject);
                        BL.reflectionLM.columnCaptionPropertyDraw.change(columnPreferences.userCaption, dataSession, propertyDrawObject);
                        BL.reflectionLM.columnPatternPropertyDraw.change(columnPreferences.userPattern, dataSession, propertyDrawObject);
                        BL.reflectionLM.columnWidthPropertyDraw.change(columnPreferences.userWidth, dataSession, propertyDrawObject);
                        BL.reflectionLM.columnOrderPropertyDraw.change(columnPreferences.userOrder, dataSession, propertyDrawObject);
                        BL.reflectionLM.columnSortPropertyDraw.change(columnPreferences.userSort, dataSession, propertyDrawObject);
                        BL.reflectionLM.columnAscendingSortPropertyDraw.change(columnPreferences.userAscendingSort, dataSession, propertyDrawObject);
                    } else if (!completeOverride) {
                        changeUserColumnPreferences(columnPreferences, dataSession, idShow, propertyDrawObject, userObject);
                    }
                } else {
                    throw new RuntimeException("Объект " + entry.getKey() + " (" + entity.getCanonicalName() + ") не найден");
                }
            }
            DataObject groupObjectObject = (DataObject) BL.reflectionLM.groupObjectSIDFormNameGroupObject.readClasses(dataSession, new DataObject(preferences.groupObjectSID, StringClass.get(100)), new DataObject(entity.getCanonicalName(), StringClass.get(100)));
            if (completeOverride) {
                for (DataObject user : userObjectList) {
                    changeUserGOPreferences(preferences, dataSession, groupObjectObject, user);
                }
            }
            if (forAllUsers) {
                BL.reflectionLM.hasUserPreferencesGroupObject.change(preferences.hasUserPreferences ? true : null, dataSession, groupObjectObject);
                BL.reflectionLM.fontSizeGroupObject.change(preferences.fontInfo.fontSize != -1 ? preferences.fontInfo.fontSize : null, dataSession, groupObjectObject);
                BL.reflectionLM.pageSizeGroupObject.change(preferences.pageSize, dataSession, groupObjectObject);
                BL.reflectionLM.headerHeightGroupObject.change(preferences.headerHeight, dataSession, groupObjectObject);
                BL.reflectionLM.isFontBoldGroupObject.change(preferences.fontInfo.isBold() ? true : null, dataSession, groupObjectObject);
                BL.reflectionLM.isFontItalicGroupObject.change(preferences.fontInfo.isItalic() ? true : null, dataSession, groupObjectObject);
            } else if (!completeOverride) {
                changeUserGOPreferences(preferences, dataSession, groupObjectObject, userObject);
            }
            
            dataSession.applyException(BL, stack);
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }
    
    private void changeUserColumnPreferences(ColumnUserPreferences columnPreferences, DataSession dataSession, Long idShow, DataObject propertyDrawObject, DataObject user) throws SQLException, SQLHandledException {
        BL.reflectionLM.showPropertyDrawCustomUser.change(idShow, dataSession, propertyDrawObject, user);
        BL.reflectionLM.columnCaptionPropertyDrawCustomUser.change(columnPreferences.userCaption, dataSession, propertyDrawObject, user);
        BL.reflectionLM.columnPatternPropertyDrawCustomUser.change(columnPreferences.userPattern, dataSession, propertyDrawObject, user);
        BL.reflectionLM.columnWidthPropertyDrawCustomUser.change(columnPreferences.userWidth, dataSession, propertyDrawObject, user);
        BL.reflectionLM.columnOrderPropertyDrawCustomUser.change(columnPreferences.userOrder, dataSession, propertyDrawObject, user);
        BL.reflectionLM.columnSortPropertyDrawCustomUser.change(columnPreferences.userSort, dataSession, propertyDrawObject, user);
        BL.reflectionLM.columnAscendingSortPropertyDrawCustomUser.change(columnPreferences.userAscendingSort, dataSession, propertyDrawObject, user);    
    }
    
    private void changeUserGOPreferences(GroupObjectUserPreferences preferences, DataSession dataSession, DataObject groupObject, DataObject user) throws SQLException, SQLHandledException {
        BL.reflectionLM.hasUserPreferencesGroupObjectCustomUser.change(preferences.hasUserPreferences ? true : null, dataSession, groupObject, user);
        BL.reflectionLM.fontSizeGroupObjectCustomUser.change(preferences.fontInfo.fontSize != -1 ? preferences.fontInfo.fontSize : null, dataSession, groupObject, user);
        BL.reflectionLM.pageSizeGroupObjectCustomUser.change(preferences.pageSize, dataSession, groupObject, user);
        BL.reflectionLM.headerHeightGroupObjectCustomUser.change(preferences.headerHeight, dataSession, groupObject, user);
        BL.reflectionLM.isFontBoldGroupObjectCustomUser.change(preferences.fontInfo.isBold() ? true : null, dataSession, groupObject, user);
        BL.reflectionLM.isFontItalicGroupObjectCustomUser.change(preferences.fontInfo.isItalic() ? true : null, dataSession, groupObject, user);
    }
    

    private List<DataObject> readUserObjectList() throws SQLException, SQLHandledException {
        List<DataObject> userObjectList = new ArrayList<>();
        KeyExpr customUserExpr = new KeyExpr("customUser");
        ImRevMap<String, KeyExpr> keys = MapFact.singletonRev("customUser", customUserExpr);
        QueryBuilder<String, String> query = new QueryBuilder<>(keys);
        query.and(BL.authenticationLM.loginCustomUser.getExpr(customUserExpr).getWhere());
        ImOrderMap<ImMap<String, DataObject>, ImMap<String, ObjectValue>> queryResult = query.executeClasses(this);
        for (ImMap<String, DataObject> values : queryResult.keyIt()) {
            userObjectList.add(values.get("customUser"));
        }
        return userObjectList;
    }

    public CustomClass getCustomClass(long classID) {
        return BL.LM.baseClass.findClassID(classID);
    }

    public final DataSession session;

    private final WeakReference<FocusListener> weakFocusListener;

    public FocusListener getFocusListener() {
        return weakFocusListener.get();
    }
    
    public LogInfo getLogInfo() {
        FocusListener focusListener = getFocusListener();
        if(focusListener != null)
            return focusListener.getLogInfo();

        return LogInfo.system; 
    }

    private final WeakReference<CustomClassListener> weakClassListener;

    public CustomClassListener getClassListener() {
        return weakClassListener.get();
    }

    @ManualLazy
    public ImSet<ObjectInstance> getObjects() {
        if (objects == null)
            objects = GroupObjectInstance.getObjects(getGroups());
        return objects;
    }

    // ----------------------------------- Поиск объектов по ID ------------------------------ //
    public GroupObjectInstance getGroupObjectInstance(int groupID) {
        for (GroupObjectInstance groupObject : getGroups())
            if (groupObject.getID() == groupID)
                return groupObject;
        return null;
    }

    public ObjectInstance getObjectInstance(int objectID) {
        for (ObjectInstance object : getObjects())
            if (object.getID() == objectID)
                return object;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(int propertyID) {
        for (PropertyDrawInstance property : properties)
            if (property.getID() == propertyID)
                return property;
        return null;
    }

    public RegularFilterGroupInstance getRegularFilterGroup(int groupID) {
        for (RegularFilterGroupInstance filterGroup : regularFilterGroups)
            if (filterGroup.getID() == groupID)
                return filterGroup;
        return null;
    }

    public GroupObjectInstance getGroupObjectInstance(String sid) {
        for (GroupObjectInstance groupObject : getGroups())
            if (groupObject.getSID().equals(sid))
                return groupObject;
        return null;
    }

    public GroupObjectInstance getGroupObjectInstanceIntegration(String sid) {
        for (GroupObjectInstance groupObject : getGroups())
            if (groupObject.getIntegrationSID().equals(sid))
                return groupObject;
        return null;
    }
    
    public PropertyDrawInstance getPropertyDraw(String sid) {
        for (PropertyDrawInstance property : properties)
            if (property.getSID().equals(sid))
                return property;
        return null;
    }

    public PropertyDrawInstance getPropertyDrawIntegration(String groupSID, String sid) {
        for (PropertyDrawInstance property : properties)
            if (((groupSID == null && property.toDraw == null) || (groupSID != null && property.toDraw != null && property.toDraw.getIntegrationSID().equals(groupSID)))
                    && sid.equals(property.getIntegrationSID()))
                return property;
        return null;
    }

    // ----------------------------------- Навигация ----------------------------------------- //

    public void changeGroupObject(GroupObjectInstance group, Scroll changeType) {
        switch (changeType) {
            case HOME:
                group.seek(UpdateType.FIRST);
                break;
            case END:
                group.seek(UpdateType.LAST);
                break;
        }
    }

    public void changeGroupObject(ImSet<ObjectInstance> objects, ExecutionStack stack) throws SQLException, SQLHandledException {
        for (ObjectInstance objectInstance : objects) {
            if ((objectInstance.updated & UPDATED_OBJECT) != 0) {
                fireObjectChanged(objectInstance, stack);
            }
        }
    }

    public void expandCurrentGroupObject(ObjectInstance object) throws SQLException, SQLHandledException {
        GroupObjectInstance groupObject = object.groupTo;
        if (groupObject != null && groupObject.isInTree()) {
            for (GroupObjectInstance group : getOrderGroups()) {
                ImOrderSet<GroupObjectInstance> upGroups = group.getOrderUpTreeGroups();
                MExclMap<ObjectInstance, DataObject> mValue = MapFact.mExclMap();
                int upObjects = 0;
                if (group.parent != null) {
                    ImMap<ObjectInstance, DataObject> goValue = group.getGroupObjectValue();
                    upObjects += goValue.size();
                    mValue.exclAddAll(goValue);
                } else {
                    for (GroupObjectInstance goi : upGroups) {
                        if (goi != null && !goi.equals(group)) {
                            upObjects += goi.objects.size();
                            mValue.exclAddAll(goi.getGroupObjectValue());
                        }
                    }
                }
                ImMap<ObjectInstance, DataObject> value = mValue.immutable();
                if (!value.isEmpty() && value.size() == upObjects) { // проверка на то, что в каждом из верхних groupObject выбран какой-то объект
                    if (group.parent != null) {
                        group.expandCollapseDown(this, value, true);
                    } else {
                        group.getUpTreeGroup().expandCollapseDown(this, value, true);
                    }
                }
                if (group.equals(groupObject)) {
                    break;
                }
            }
        }
    }

    public List<RegularFilterGroupInstance> regularFilterGroups = new ArrayList<>();
    private Map<RegularFilterGroupInstance, RegularFilterInstance> regularFilterValues = new HashMap<>();

    public void setRegularFilter(RegularFilterGroupInstance filterGroup, int filterId) {
        setRegularFilter(filterGroup, filterGroup.getFilter(filterId));
    }

    private void setRegularFilter(RegularFilterGroupInstance filterGroup, RegularFilterInstance filter) {
        RegularFilterInstance prevFilter = regularFilterValues.get(filterGroup);
        if (prevFilter != null)
            prevFilter.filter.getApplyObject().removeRegularFilter(prevFilter.filter);

        if (filter == null) {
            regularFilterValues.remove(filterGroup);
        } else {
            regularFilterValues.put(filterGroup, filter);
            filter.filter.getApplyObject().addRegularFilter(filter.filter);
        }
    }

    // -------------------------------------- Изменение данных ----------------------------------- //

    // пометка что изменились данные
    public boolean dataChanged = true;

    public <P extends PropertyInterface> DataObject addFormObject(CustomObjectInstance object, ConcreteCustomClass cls, DataObject pushed, ExecutionStack stack) throws SQLException, SQLHandledException {
        DataObject dataObject = session.addObjectAutoSet(cls, pushed, BL, getClassListener());

        for (FilterInstance filter : object.groupTo.getFilters(true))
            filter.resolveAdd(this, object, dataObject, stack);

        expandCurrentGroupObject(object);

        // todo : теоретически надо переделывать
        // нужно менять текущий объект, иначе не будет работать ImportFromExcelActionProperty
        seekObject(object, dataObject);

        // меняем вид, если при добавлении может получиться, что фильтр не выполнится, нужно как-то проверить в общем случае
//      changeClassView(object.groupTo, ClassViewType.PANEL);

        dataChanged = true;

        return dataObject;
    }

    public void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject dataObject, ConcreteObjectClass cls) throws SQLException, SQLHandledException {
        if (objectInstance instanceof CustomObjectInstance) {
            CustomObjectInstance object = (CustomObjectInstance) objectInstance;

            object.changeClass(session, dataObject, cls);
            dataChanged = true;
        } else
            session.changeClass(objectInstance, dataObject, cls);
    }

    @ThisMessage
    public void executeEventAction(final PropertyDrawInstance<?> property, String eventActionSID, final ImMap<ObjectInstance, ? extends ObjectValue> keys, boolean externalChange, Function<AsyncEventExec, PushAsyncResult> asyncResult, final ExecutionStack stack) throws SQLException, SQLHandledException {
        SQLCallable<Boolean> checkReadOnly = property.propertyReadOnly != null ? () -> property.propertyReadOnly.getRemappedPropertyObject(keys).read(FormInstance.this) != null : null;
        ActionObjectInstance<?> eventAction = property.getEventAction(eventActionSID, this, checkReadOnly, securityPolicy);
        if(eventAction == null) {
            ThreadLocalContext.delayUserInteraction(EditNotPerformedClientAction.instance);
            return;
        }

        PushAsyncResult result = null;
        if(asyncResult != null) {
            AsyncEventExec asyncEventExec = property.getEntity().getAsyncEventExec(entity, securityPolicy, eventActionSID, externalChange);
            if(asyncEventExec != null) { // in case of paste can be null
                result = asyncResult.apply(asyncEventExec);
                if(result == null) // in case of paste can be null
                    return;
            } else
                return;
        } else {
            if (ServerResponse.isChangeEvent(CHANGE)) { //ask confirm logics... it is assumed that async logics checks confirm logics itself
                PropertyDrawEntity propertyDraw = property.getEntity();
                if (propertyDraw.askConfirm) {
                    int confirmResult = (Integer) ThreadLocalContext.requestUserInteraction(new ConfirmClientAction("lsFusion",
                            entity.getRichDesign().get(propertyDraw).getAskConfirmMessage()));
                    if (confirmResult != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
            }
        }

        final ActionObjectInstance remappedEventAction = eventAction.getRemappedPropertyObject(keys);
        remappedEventAction.execute(FormInstance.this, stack, result, property, FormInstance.this);
    }

    public void pasteExternalTable(List<PropertyDrawInstance> properties, List<ImMap<ObjectInstance, DataObject>> columnKeys, List<List<byte[]>> values, ExecutionStack stack) throws SQLException, IOException, SQLHandledException {
        GroupObjectInstance groupObject = properties.get(0).toDraw;
        ImOrderSet<ImMap<ObjectInstance, DataObject>> executeList = groupObject.seekObjects(session.sql, getQueryEnv(), getModifier(), BL.LM.baseClass, values.size()).keyOrderSet();

        //создание объектов
        int availableQuantity = executeList.size();
        if (availableQuantity < values.size()) {
            executeList = executeList.addOrderExcl(groupObject.createObjects(session, this, values.size() - availableQuantity, stack));
        }

        for (int i = 0; i < properties.size(); i++) {
            PropertyDrawInstance property = properties.get(i);

            ImOrderValueMap<ImMap<ObjectInstance, DataObject>, Object> mvPasteRows = executeList.mapItOrderValues();
            for (int j = 0; j < executeList.size(); j++) {
                Object value = deserializeObject(values.get(j).get(i));
                mvPasteRows.mapValue(j, value);
            }

            executePasteAction(property, columnKeys.get(i), mvPasteRows.immutableValueOrder(), stack);
        }
    }

    public void pasteMulticellValue(Map<PropertyDrawInstance, ImOrderMap<ImMap<ObjectInstance, DataObject>, Object>> cellsValues, ExecutionStack stack) throws SQLException, SQLHandledException {
        for (Entry<PropertyDrawInstance, ImOrderMap<ImMap<ObjectInstance, DataObject>, Object>> e : cellsValues.entrySet()) { // бежим по ячейкам
            PropertyDrawInstance property = e.getKey();
            executePasteAction(property, null, e.getValue(), stack);
        }
    }

    private void executePasteAction(PropertyDrawInstance<?> property, ImMap<ObjectInstance, DataObject> columnKey, ImOrderMap<ImMap<ObjectInstance, DataObject>, Object> pasteRows, ExecutionStack stack) throws SQLException, SQLHandledException {
        if (!pasteRows.isEmpty()) {
            for (int i = 0, size = pasteRows.size(); i < size; i++) {
                ImMap<ObjectInstance, DataObject> key = pasteRows.getKey(i);
                Object value = pasteRows.getValue(i);
                if (columnKey != null) {
                    key = key.addExcl(columnKey);
                }
                executeEventAction(property, CHANGE, key, true, asyncChange -> asyncChange instanceof AsyncInput ? new PushAsyncInput(ObjectValue.getValue(value, ((AsyncInput) asyncChange).changeType)) : null, stack);
            }
        }
    }

    public int countRecords(int groupObjectID) throws SQLException, SQLHandledException {
        GroupObjectInstance group = getGroupObjectInstance(groupObjectID);
        Expr expr = GroupExpr.create(MapFact.EMPTY(), ValueExpr.COUNT, group.getWhere(group.getMapKeys(), getModifier()), GroupType.SUM, MapFact.EMPTY());
        QueryBuilder<Object, Object> query = new QueryBuilder<>(MapFact.EMPTYREV());
        query.addProperty("quant", expr);
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(this);
        Integer quantity = (Integer) result.getValue(0).get("quant");
        if (quantity != null) {
            return quantity;
        } else {
            return 0;
        }
    }

    private ImMap<ObjectInstance, Expr> overrideColumnKeys(ImRevMap<ObjectInstance, KeyExpr> mapKeys, ImMap<ObjectInstance, ? extends ObjectValue> columnKeys) {
        // замещение с добавлением
        return MapFact.override(mapKeys, columnKeys.mapValues(ObjectValue::getExpr));
    }

    public Object calculateSum(PropertyDrawInstance propertyDraw, ImMap<ObjectInstance, ? extends ObjectValue> columnKeys) throws SQLException, SQLHandledException {
        GroupObjectInstance groupObject = propertyDraw.toDraw;

        ImRevMap<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();

        ImMap<ObjectInstance, Expr> keys = overrideColumnKeys(mapKeys, columnKeys);

        Expr expr = GroupExpr.create(MapFact.EMPTY(), propertyDraw.getDrawInstance().getExpr(keys, getModifier()), groupObject.getWhere(mapKeys, getModifier()), GroupType.SUM, MapFact.EMPTY());

        QueryBuilder<Object, String> query = new QueryBuilder<>(MapFact.EMPTYREV());
        query.addProperty("sum", expr);
        ImOrderMap<ImMap<Object, Object>, ImMap<String, Object>> result = query.execute(this);
        Object sum = result.getValue(0).get("sum");
        return sum == null ? 0 : sum;
    }

    private enum HasChanges {
        HAS, NO, NULL;

        public static HasChanges toHasChanges(Boolean b) {
            return b != null ? (b ? HAS : NO) : NULL;
        }

        public Boolean toBoolean() {
            switch (this) {
                case HAS:
                    return true;
                case NO:
                    return false;
            }
            return null;
        }

    }

    // later it would be better if this map is automatically cleaned / filled some way (see proceedAllEventActions in FormEntity.finalizaeAroundInit)
    private Map<Property, HasChanges> asyncPropertyChanges = new ConcurrentHashMap<>();
    // thread-safe
    private void updateAsyncPropertyChanges() throws SQLException, SQLHandledException {
        for(Property property : asyncPropertyChanges.keySet())
            updateAsyncPropertyChanges(property);
    }
    // thread-safe
    private <P extends PropertyInterface> boolean updateAsyncPropertyChanges(Property property) throws SQLException, SQLHandledException {
        // something more efficient like isReallyChange could be used (passing inputvaluelist), but so far it doesn't seem to be reall necessary
        boolean hasChanges = property.hasChanges(getModifier());
        asyncPropertyChanges.put(property, HasChanges.toHasChanges(hasChanges));
        return hasChanges;
    }
    // true - has, false - has not, null - not sure, not thread-safe
    private Boolean hasOptimisticAsyncChanges(Property property) {
        if(asyncPropertyChanges.containsKey(property))
            return asyncPropertyChanges.get(property).toBoolean();

        asyncPropertyChanges.put(property, HasChanges.NULL);
        return null;
    }

    public static <P extends PropertyInterface> PropertyAsync<P>[] getAsyncValues(InputValueList<P> list, DataSession session, Modifier modifier, String value, AsyncMode asyncMode) throws SQLException, SQLHandledException {
        Settings settings = Settings.get();
        int neededCount = settings.getAsyncValuesNeededCount();
        double extraReadCoeff = settings.getAsyncValuesExtraReadCoeff();
        double statDegree = settings.getStatDegree();
        int maxLimitRead = settings.getAsyncValuesMaxReadCount();

        double estDistinctRate = (asyncMode == AsyncMode.VALUES ? list.getDistinctStat().getCount() : 1) * extraReadCoeff;

        Pair<ImRevMap<P, KeyExpr>, Expr> listExprKeys = getListExpr(list, modifier);
//        t(o) = list AND list match request
        Expr listExpr = listExprKeys.second.and(listExprKeys.second.compare(new DataObject(value), Compare.MATCH));

        int estNeededRead = (int) BaseUtils.min(((double)neededCount * estDistinctRate), maxLimitRead);
        while(estNeededRead <= maxLimitRead) {
            // t(o) = LIMIT estX BY t(o)
            int ceilEstNeedRead = new Stat((long)estNeededRead, true).getCount(); // we use "degreed" value instead of actual value, to have more granular caches
            Pair<PropertyAsync<P>[], Integer> result = getAsyncValues(session, SubQueryExpr.create(listExpr, false, ceilEstNeedRead), listExprKeys.first, asyncMode, neededCount, value);
            PropertyAsync<P>[] resultValues = result.first;
            int count = result.second;

            if(resultValues.length >= neededCount || // found it
                    count < estNeededRead) // or we've read all the records, no need to read more
                return resultValues;

            // continue reading - we've read estNeededRead records and got resultValues.length, so we'll increase estNeededRead on that ratio (but not less than coeff)
            estNeededRead = (int) (estNeededRead * BaseUtils.max((double)neededCount * extraReadCoeff / (double)resultValues.length, statDegree));
        }

        return getAsyncValues(session, listExpr, listExprKeys.first, asyncMode, neededCount, value).first;
    }

    private static <P extends PropertyInterface, Q> Pair<PropertyAsync<P>[], Integer> getAsyncValues(DataSession session, Expr listBaseExpr, ImRevMap<P, KeyExpr> baseKeys, AsyncMode asyncMode, int neededCount, String value) throws SQLException, SQLHandledException {
        // z = GROUP SUM 1 BY t(o)
        Expr countExpr;
        Expr listExpr;
        Where listWhere;
        ImRevMap<Q, KeyExpr> groupListKeys;
        boolean readObjects = asyncMode == AsyncMode.OBJECTS;
        if(readObjects) {
            groupListKeys = (ImRevMap<Q, KeyExpr>) baseKeys;
            countExpr = ValueExpr.COUNT;
            listWhere = listBaseExpr.getWhere();
            listExpr = listBaseExpr;
        } else {
            KeyExpr listKeyExpr = new KeyExpr("list");
            Q key = (Q) "key";
            groupListKeys = MapFact.singletonRev(key, listKeyExpr);
            countExpr = GroupExpr.create(MapFact.singleton(key, listBaseExpr), ValueExpr.COUNT, GroupType.SUM, groupListKeys);
            listWhere = countExpr.getWhere();
            listExpr = listKeyExpr;
        }

        // SELECT t, higlh(t) WHERE z(t) ORDER BY rank(t) LIMIR act
        String language = Settings.get().getFilterMatchLanguage();
        SQLSyntax syntax = session.sql.syntax;
        String match = "'" + value + "'";

        MExclMap<String, Expr> mProps = MapFact.mExclMapMax(4);
        mProps.exclAdd("highlight", value.isEmpty() ? listExpr : FormulaExpr.createCustomFormula(MatchWhere.getHighlight(syntax, "prm1", match, language), StringClass.text, listExpr));
        mProps.exclAdd("rank", value.isEmpty() ? ValueExpr.COUNT : FormulaExpr.createCustomFormula(MatchWhere.getRank(syntax, "prm1", match, language), DoubleClass.instance, listExpr));
        mProps.exclAdd("count", countExpr);
        if(readObjects)
            mProps.exclAdd("raw", listExpr);

        ImOrderMap<ImMap<Q, DataObject>, ImMap<String, ObjectValue>> result = new Query<>(groupListKeys, mProps.immutable(), listWhere).executeClasses(session, MapFact.toOrderMap("rank", true, "count", true), neededCount);

        PropertyAsync<P>[] resultValues = new PropertyAsync[result.size()];
        int count = 0;
        for(int i = 0, size = result.size(); i < size; i++) {
            ImMap<String, ObjectValue> values = result.getValue(i);
            count += (Integer)values.get("count").getValue();
            resultValues[i] = new PropertyAsync<P>((String)values.get("highlight").getValue(),
                    readObjects ? (String)values.get("raw").getValue() : (String)result.getKey(i).singleValue().getValue(),
                    readObjects ? (ImMap<P, DataObject>)result.getKey(i) : null);
        }

        return new Pair<>(resultValues, count);
    }

    public static <P extends PropertyInterface> ObjectValue getAsyncKey(InputValueList<P> list, DataSession session, Modifier modifier, ObjectValue value) throws SQLException, SQLHandledException {
        Pair<ImRevMap<P, KeyExpr>, Expr> listExprKeys = getListExpr(list, modifier);
        ImSet<ImMap<P, DataObject>> keys =
                new Query<>(listExprKeys.first, listExprKeys.second, "value", listExprKeys.second.compare(value.getExpr(), Compare.EQUALS))
                .executeClasses(session, 1)
                .keys();
        if(keys.isEmpty())
            return NullValue.instance;
        return keys.get(0).get(list.singleInterface());
    }

    private static <P extends PropertyInterface> Pair<ImRevMap<P, KeyExpr>, Expr> getListExpr(InputValueList<P> list, Modifier modifier) throws SQLException, SQLHandledException {
        ImRevMap<P, KeyExpr> innerKeys = KeyExpr.getMapKeys(list.property.interfaces.removeIncl(list.mapValues.keys()));
        ImMap<P, Expr> innerExprs = MapFact.addExcl(innerKeys, DataObject.getMapExprs(list.mapValues));

        return new Pair<>(innerKeys, list.property.getExpr(innerExprs, modifier));
    }

    private static <P extends PropertyInterface> Async[] convertPropertyAsyncs(ImRevMap<P, ObjectInstance> mapObjects, PropertyAsync<P>[] propAsyncs) {
        Async[] result = new Async[propAsyncs.length];
        for(int i=0;i<propAsyncs.length;i++) {
            PropertyAsync<P> propAsync = propAsyncs[i];
            result[i] = new Async(propAsync.displayString, propAsync.rawString, propAsync.key != null ? mapObjects.crossJoin(propAsync.key) : null);
        }
        return result;
    }
    public <P extends PropertyInterface, X extends PropertyInterface> Async[] getAsyncValues(PropertyDrawInstance<P> propertyDraw, ImMap<ObjectInstance, ? extends ObjectValue> keys, String actionSID, String value, Boolean optimistic, Supplier<Boolean> optimisticRun) throws SQLException, SQLHandledException {
        InputValueList<X> listProperty;
        ImRevMap<X, ObjectInstance> mapObjects;
        AsyncMode asyncMode;
        boolean needRecheck = false;
        if (actionSID.equals(INPUT)) {
            assert optimistic;
            InputContext<X> inputContext = ThreadLocalContext.lockInputContext();
            try {
                if (inputContext == null) // recheck
                    return new Async[] {Async.CANCELED};

                listProperty = inputContext.list;
                mapObjects = null;
                asyncMode = inputContext.strict ? AsyncMode.OBJECTVALUES : AsyncMode.VALUES;
                if (!inputContext.newSession && listProperty.property.hasChanges(inputContext.modifier))
                    return convertPropertyAsyncs(mapObjects, getAsyncValues(inputContext.list, inputContext.session, inputContext.modifier, value, asyncMode));
            } finally {
                ThreadLocalContext.unlockInputContext();
            }
        } else {
            PropertyDrawInstance.AsyncValueList<X> valueList = (PropertyDrawInstance.AsyncValueList<X>) propertyDraw.getAsyncValueList(actionSID, this, keys);
            if(valueList == null)
                return new Async[] {Async.CANCELED};

            listProperty = valueList.list;
            mapObjects = valueList.mapObjects;
            asyncMode = valueList.asyncMode;
            if(!valueList.newSession) { // ! new session
                if (optimistic) {
                    Boolean hasOptimisticChanges = hasOptimisticAsyncChanges(listProperty.property);
                    if (hasOptimisticChanges == null) // not sure yet
                        needRecheck = true;
                    else if (hasOptimisticChanges)
                        return null; // switching to pessimistic mode
                } else {
                    if (updateAsyncPropertyChanges(listProperty.property)) // recheck changes since we're in a thread-safe mode
                        return convertPropertyAsyncs(mapObjects, getAsyncValues(listProperty, getSession(), getModifier(), value, asyncMode));
                }
            }
        }

        // we need synchronized recheck to synchronously set the cancelable thread and check that this is the last request
        // this way we'll guarantee that there is always one global form request running (single local form request is guaranteed by regular local requests synchronization)
        if(!optimisticRun.get())
            return new Async[] {Async.CANCELED};

        Async[] result = convertPropertyAsyncs(mapObjects, logicsInstance.getDbManager().getAsyncValues(listProperty, value, asyncMode));
        if(needRecheck) // not sure yet, resending RECHECK
            result = BaseUtils.addElement(result, Async.RECHECK, Async[]::new);
        return result;

    }

    private static String getSID(PropertyDrawInstance property, int index) {
        return property.getID() + "_" + index;
    }

    public Map<List<Object>, List<Object>> groupData(ImOrderMap<PropertyDrawInstance, ImList<ImMap<ObjectInstance, DataObject>>> toGroup,
                                                     ImOrderMap<Object, ImList<ImMap<ObjectInstance, DataObject>>> toSum,
                                                     ImOrderMap<PropertyDrawInstance, ImList<ImMap<ObjectInstance, DataObject>>> toMax, boolean onlyNotNull) throws SQLException, SQLHandledException {
        GroupObjectInstance groupObject = toGroup.getKey(0).toDraw;
        ImRevMap<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();

        MRevMap<String, KeyExpr> mKeyExprMap = MapFact.mRevMap();
        MExclMap<String, Expr> mExprMap = MapFact.mExclMap();
        for (PropertyDrawInstance property : toGroup.keyIt()) {
            int i = 0;
            for (ImMap<ObjectInstance, DataObject> columnKeys : toGroup.get(property)) {
                i++;
                ImMap<ObjectInstance, Expr> keys = overrideColumnKeys(mapKeys, columnKeys);
                String propertyKey = getSID(property, i);
                mKeyExprMap.revAdd(propertyKey, new KeyExpr("expr"));
                Expr propExpr = property.getDrawInstance().getExpr(keys, getModifier());
                // override to support NULLs
                Expr expr = FormulaUnionExpr.create(StringOverrideFormulaImpl.instance, ListFact.toList(propExpr, ValueExpr.IMPOSSIBLESTRING));
                mExprMap.exclAdd(propertyKey, expr);
            }
        }
        ImRevMap<String, KeyExpr> keyExprMap = mKeyExprMap.immutableRev();
        ImMap<String, Expr> exprMap = mExprMap.immutable();

        QueryBuilder<String, String> query = new QueryBuilder<>(keyExprMap);
        Expr exprQuant = GroupExpr.create(exprMap, ValueExpr.COUNT, groupObject.getWhere(mapKeys, getModifier()), GroupType.SUM, keyExprMap);
        query.and(exprQuant.getWhere());

        int separator = toSum.size();
        int idIndex = 0;
        for (int i = 0; i < toSum.size() + toMax.size(); i++) {
            PropertyDrawInstance property;
            ImList<ImMap<ObjectInstance, DataObject>> currentList;
            GroupType groupType;
            if (i < separator) {
                groupType = GroupType.SUM;

                Object sumObject = toSum.getKey(i);
                if (!(sumObject instanceof PropertyDrawInstance)) {
                    query.addProperty("quant", exprQuant);
                    continue;
                }

                property = (PropertyDrawInstance) sumObject;
                currentList = toSum.getValue(i);
            } else {
                property = toMax.getKey(i - separator);
                currentList = toMax.getValue(i - separator);

                groupType = GroupType.MAXCHECK(property.getType());
            }
            for (ImMap<ObjectInstance, DataObject> columnKeys : currentList) {
                idIndex++;
                ImMap<ObjectInstance, Expr> keys = overrideColumnKeys(mapKeys, columnKeys);
                Expr expr = GroupExpr.create(exprMap, property.getDrawInstance().getExpr(keys, getModifier()), groupObject.getWhere(mapKeys, getModifier()), groupType, keyExprMap);
                query.addProperty(getSID(property, idIndex), expr);
                if (onlyNotNull) {
                    query.and(expr.getWhere());
                }
            }
        }

        // to get real key values, not converted to String
        for (PropertyDrawInstance property : toGroup.keyIt()) {
            int i = 0;
            for (ImMap<ObjectInstance, DataObject> columnKeys : toGroup.get(property)) {
                i++;
                Expr expr = property.getDrawInstance().getExpr(overrideColumnKeys(mapKeys, columnKeys), getModifier());
                Expr gexpr = GroupExpr.create(exprMap, expr, groupObject.getWhere(mapKeys, getModifier()), GroupType.ANY, keyExprMap);
                query.addProperty(getSID(property, i) + "_key", gexpr);
            }
        }

        Map<List<Object>, List<Object>> resultMap = new OrderedMap<>();
        ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> result = query.execute(this);
        for (int j = 0, size = result.size(); j < size; j++) {
            ImMap<String, Object> oneValue = result.getValue(j);

            List<Object> groupList = new ArrayList<>();
            List<Object> sumList = new ArrayList<>();

            for (PropertyDrawInstance propertyDraw : toGroup.keyIt()) {
                for (int i = 1; i <= toGroup.get(propertyDraw).size(); i++) {
                    groupList.add(oneValue.get(getSID(propertyDraw, i) + "_key"));
                }
            }
            int index = 1;
            for (int k = 0, sizeK = toSum.size(); k < sizeK; k++) {
                Object propertyDraw = toSum.getKey(k);
                if (propertyDraw instanceof PropertyDrawInstance) {
                    for (int i = 1, sizeI = toSum.getValue(k).size(); i <= sizeI; i++) {
                        sumList.add(oneValue.get(getSID(((PropertyDrawInstance) propertyDraw), index)));
                        index++;
                    }
                } else
                    sumList.add(oneValue.get("quant"));
            }
            for (int k = 0, sizeK = toMax.size(); k < sizeK; k++) {
                PropertyDrawInstance propertyDraw = toMax.getKey(k);
                for (int i = 1, sizeI = toMax.getValue(k).size(); i <= sizeI; i++) {
                    sumList.add(oneValue.get(getSID(propertyDraw, index)));
                    index++;
                }
            }
            resultMap.put(groupList, sumList);
        }
        return resultMap;
    }

    public List<FormGrouping> readGroupings(String groupObjectSID) throws SQLException, SQLHandledException {
        if (!entity.isNamed()) {
            return null;
        }
        
        Map<String, FormGrouping> groupings = new LinkedHashMap<>();
        
        ObjectValue groupObjectObjectValue = BL.reflectionLM.groupObjectSIDFormNameGroupObject.readClasses(session, new DataObject(groupObjectSID, StringClass.get(100)), new DataObject(entity.getCanonicalName(), StringClass.get(100)));
        
        if (groupObjectObjectValue instanceof DataObject) {
            KeyExpr propertyDrawExpr = new KeyExpr("propertyDraw");

            KeyExpr formGroupingExpr = new KeyExpr("formGrouping");

            ImRevMap<String, KeyExpr> newKeys = MapFact.toRevMap("formGrouping", formGroupingExpr, "propertyDraw", propertyDrawExpr);

            QueryBuilder<String, String> query = new QueryBuilder<>(newKeys);

            query.addProperty("groupingSID", BL.reflectionLM.nameFormGrouping.getExpr(formGroupingExpr));
            query.addProperty("itemQuantity", BL.reflectionLM.itemQuantityFormGrouping.getExpr(formGroupingExpr));
            query.addProperty("propertySID", BL.reflectionLM.sidPropertyDraw.getExpr(propertyDrawExpr));
            query.addProperty("groupOrder", BL.reflectionLM.groupOrderFormGroupingPropertyDraw.getExpr(formGroupingExpr, propertyDrawExpr));
            query.addProperty("sum", BL.reflectionLM.sumFormGroupingPropertyDraw.getExpr(formGroupingExpr, propertyDrawExpr));
            query.addProperty("max", BL.reflectionLM.maxFormGroupingPropertyDraw.getExpr(formGroupingExpr, propertyDrawExpr));
            query.addProperty("pivot", BL.reflectionLM.pivotFormGroupingPropertyDraw.getExpr(formGroupingExpr, propertyDrawExpr));
            
            Expr goExpr = ((DataObject) groupObjectObjectValue).getExpr();
            query.and(BL.reflectionLM.groupObjectFormGrouping.getExpr(formGroupingExpr).compare(goExpr, Compare.EQUALS));
            query.and(BL.reflectionLM.groupObjectPropertyDraw.getExpr(propertyDrawExpr).compare(goExpr, Compare.EQUALS));

            ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> queryResult = query.execute(this);

            for (ImMap<String, Object> values : queryResult.valueIt()) {
                String groupingSID = (String) values.get("groupingSID");
                FormGrouping grouping = groupings.get(groupingSID);
                if (grouping == null) {
                    grouping = new FormGrouping((String) values.get("groupingSID"), groupObjectSID, (Boolean) values.get("itemQuantity"), new ArrayList<>());
                    groupings.put(groupingSID, grouping);
                }
                grouping.propertyGroupings.add(grouping.new PropertyGrouping((String) values.get("propertySID"), (Integer) values.get("groupOrder"), (Boolean) values.get("sum"), (Boolean) values.get("max"), (Boolean) values.get("pivot")));
            }
        }
        return new ArrayList<>(groupings.values());
    }

    public void saveGrouping(FormGrouping grouping, ExecutionStack stack) throws SQLException, ScriptingErrorLog.SemanticErrorException, SQLHandledException {
        if (!entity.isNamed()) {
            return;
        }
        
        try (DataSession dataSession = session.createSession()) {
            ObjectValue groupObjectObjectValue = BL.reflectionLM.groupObjectSIDFormNameGroupObject.readClasses(dataSession, new DataObject(grouping.groupObjectSID, StringClass.get(100)), new DataObject(entity.getCanonicalName(), StringClass.get(100)));
            if (!(groupObjectObjectValue instanceof DataObject)) {
                throw new RuntimeException("Объект " + grouping.groupObjectSID + " (" + entity.getCanonicalName() + ") не найден");
            }
            DataObject groupObjectObject = (DataObject) groupObjectObjectValue;
            ObjectValue groupingObjectValue = BL.reflectionLM.formGroupingNameFormGroupingGroupObject.readClasses(dataSession, new DataObject(grouping.name, StringClass.get(100)), groupObjectObject);
            DataObject groupingObject;
            if (groupingObjectValue instanceof DataObject) {
                groupingObject = (DataObject) groupingObjectValue;

                if (grouping.propertyGroupings == null) { // признак удаления группировки
                    dataSession.changeClass(groupingObject, null);
                    dataSession.applyException(BL, stack);
                    return;
                }
            } else {
                assert grouping.propertyGroupings != null;
                groupingObject = dataSession.addObject((ConcreteCustomClass) BL.reflectionLM.findClass("FormGrouping"));
                BL.reflectionLM.groupObjectFormGrouping.change(groupObjectObject, dataSession, groupingObject);
                BL.reflectionLM.nameFormGrouping.change(grouping.name, dataSession, groupingObject);
            }
            assert grouping.propertyGroupings != null;
            BL.reflectionLM.itemQuantityFormGrouping.change(grouping.showItemQuantity, dataSession, groupingObject);

            for (FormGrouping.PropertyGrouping propGrouping : grouping.propertyGroupings) {
                ObjectValue propertyDrawObjectValue = BL.reflectionLM.propertyDrawByFormNameAndPropertyDrawSid.readClasses(dataSession,
                        new DataObject(entity.getCanonicalName(), StringClass.get(false, false, 100)),
                        new DataObject(propGrouping.propertySID, StringClass.get(false, false, 100)));
                if (propertyDrawObjectValue instanceof DataObject) {
                    DataObject propertyDrawObject = (DataObject) propertyDrawObjectValue;
                    BL.reflectionLM.groupOrderFormGroupingPropertyDraw.change(propGrouping.groupingOrder, dataSession, groupingObject, propertyDrawObject);
                    BL.reflectionLM.sumFormGroupingPropertyDraw.change(propGrouping.sum, dataSession, groupingObject, propertyDrawObject);
                    BL.reflectionLM.maxFormGroupingPropertyDraw.change(propGrouping.max, dataSession, groupingObject, propertyDrawObject);
                    BL.reflectionLM.pivotFormGroupingPropertyDraw.change(propGrouping.pivot, dataSession, groupingObject, propertyDrawObject);
                } else {
                    throw new RuntimeException("Свойство " + propGrouping.propertySID + " (" + entity.getCanonicalName() + ") не найдено");
                }
            }
            dataSession.applyException(BL, stack);
        }
    }

    // Обновление данных
    public void refreshData() throws SQLException, SQLHandledException {

        for (ObjectInstance object : getObjects())
            if (object instanceof CustomObjectInstance)
                ((CustomObjectInstance) object).refreshValueClass(session);
        refresh = true;
        dataChanged = session.hasChanges();
    }

    public boolean checkApply(ExecutionStack stack, UserInteraction interaction) throws SQLException, SQLHandledException {
        return session.check(BL, this, stack, interaction);
    }

    private class FormStack extends SameThreadExecutionStack {

        public FormStack(ExecutionStack upStack) {
            super(upStack);
        }

        protected DataSession getSession() {
            return session;
        }

        @Override
        public void updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
            for (GroupObjectInstance group : getGroups())
                group.updateExpandClasses(session);
            super.updateCurrentClasses(session);
        }
    }
    
    // formApply "injected" in every apply
    public boolean apply(BusinessLogics BL, ExecutionStack stack, UserInteraction interaction, ImOrderSet<ActionValueImplement> applyActions, FunctionSet<SessionDataProperty> keepProperties, ExecutionEnvironment sessionEventFormEnv, Result<String> applyMessage) throws SQLException, SQLHandledException {
        assert sessionEventFormEnv == null || this == sessionEventFormEnv;

        stack = new FormStack(stack);

        BL.LM.dropBeforeCanceled(this);
        fireOnBeforeApply(stack);        
        if(BL.LM.isBeforeCanceled(this))
            return false;

        if (!session.apply(BL, stack, interaction, applyActions.mergeOrder(getEventsOnApply()), keepProperties, this, applyMessage))
            return false;

        fireOnAfterApply(stack);

        environmentIncrement.add(FormEntity.isAdd, PropertyChange.STATIC(false));
        
        refreshData();
        dataChanged = true;

        return true;
    }

    @Override
    public void cancel(ExecutionStack stack, FunctionSet<SessionDataProperty> keep) throws SQLException, SQLHandledException {
        if(session.cancelSession(keep)) {
            // пробежим по всем объектам
            for (ObjectInstance object : getObjects())
                if (object instanceof CustomObjectInstance)
                    ((CustomObjectInstance) object).updateCurrentClass(session);
            fireOnCancel(stack);

            dataChanged = true;
        }
    }

    // ------------------ Через эти методы сообщает верхним объектам об изменениях ------------------- //

    // В дальнейшем наверное надо будет переделать на Listener'ы...
    protected void objectChanged(ConcreteCustomClass cls, Integer objectID) {
    }

    public void changePageSize(GroupObjectInstance groupObject, Integer pageSize) {
        groupObject.setPageSize(pageSize);
    }

    public void gainedFocus(ExecutionStack stack) {
        dataChanged = true;
        FocusListener focusListener = getFocusListener();
        if (focusListener != null)
            focusListener.gainedFocus(this);

        String formCanonicalName = entity.getCanonicalName();
        if (session.prevFormCanonicalName == null || !session.prevFormCanonicalName.equals(formCanonicalName)) {
            session.form.changeCurrentForm(formCanonicalName);
            session.prevFormCanonicalName = formCanonicalName;
        }
    }

    private int updateSessionOwner(boolean set, ExecutionStack stack) throws SQLException, SQLHandledException {
        ExecutionEnvironment env = getSession();
        LP<?> sessionOwners = BL.LM.sessionOwners;
        int prevOwners = BaseUtils.nvl((Integer) sessionOwners.read(env), 0);
        int newOwners = prevOwners + (set ? 1 : -1);
        sessionOwners.change(newOwners == 0 ? null : newOwners, env);
        return prevOwners;
    }

    // сейчас закрытие формы асинхронно (для экономии round trip'а), для записи же скажем sessionOwner'а нужна синхронная работа сессии
    // для этого можно делать это либо при отсылке hide'а формы на сервере (но тогда owner может сброситься чуть раньше чем надо)
    // или в контексте вызова, но тогда в случае немодальной формы, sessionOwner не сбрасывается, то есть мы полагаемся на то что сессия сразу же закроется (де-факто так и будет, но мало ли)
    // в будущем если все же вернемся к синхронизации закрытия возможно проблема уйдет
    private static boolean useCallerSyncOnClose = false;
    public void syncLikelyOnClose(boolean call, ExecutionStack stack) throws SQLException, SQLHandledException {
        if(call == useCallerSyncOnClose) {
            updateSessionOwner(false, stack);

            for(SessionModifier modifier : modifiers.values()) // нужен для того чтобы очистить views раньше и не синхронизировать тогда clean и eventChange
                modifier.cleanViews();
        }
    }

    @Override
    protected void onClose(Object o) throws SQLException {
        assert o == null;

        ServerLoggers.remoteLifeLog("FORM CLOSE : " + this);

        session.unregisterForm(this);
    }


    // --------------------------------------------------------------------------------------- //
    // --------------------- Общение в обратную сторону с ClientForm ------------------------- //
    // --------------------------------------------------------------------------------------- //

    public ConcreteCustomClass getObjectClass(ObjectInstance object) {

        if (!(object instanceof CustomObjectInstance))
            return null;

        return ((CustomObjectInstance) object).currentClass;
    }

    @Deprecated
    public void forceChangeObject(ObjectInstance object, ObjectValue value) throws SQLException, SQLHandledException {
        seekObject(object, value);
    }

    public void seekObject(ObjectInstance object, ObjectValue value) throws SQLException, SQLHandledException {
        changeObjectValue(object, value);

        if(value instanceof DataObject) // ignoring null values, it's important when we're seeking with NULL option (since we want to set null object if all objects are NULL)
            object.groupTo.addSeek(object, value, false);
    }

    public void changeObjectValue(ObjectInstance object, ObjectValue value) throws SQLException, SQLHandledException {
//        if (object instanceof DataObjectInstance && !(value instanceof DataObject))
//            object.changeValue(session, ((DataObjectInstance) object).getBaseClass().getDefaultObjectValue());
//        else
        object.changeValue(session, value);
    }

    private boolean hasEventActions() {
        ImMap<Object, ImList<ActionObjectEntity<?>>> eventActions = entity.getEventActions();
        for(ImList<ActionObjectEntity<?>> list : eventActions.valueIt())
            if(list.size() > 0)
                return true;
        return false;
    }

    // explicit SEEK with explicit updateType
    public void seekObjects(GroupObjectInstance group, ImMap<ObjectInstance, ObjectValue> objectInstances, UpdateType type) throws SQLException, SQLHandledException {
        if(group == null)
            group = objectInstances.getKey(0).groupTo;
        if (group == null) //if seek objects from another form
            return;
        if(type == null)
            type = group.getUpdateType();
        // assert that all objects are from this group
        group.seek(type);
        for (int i = 0; i < objectInstances.size(); ++i)
            seekObject(objectInstances.getKey(i), objectInstances.getValue(i));
    }

    private ImList<ComponentView> userActivateTabs = ListFact.EMPTY();
    // программный activate tab
    public void activateTab(ComponentView view) throws SQLException, SQLHandledException {
        setTabVisible(view.getContainer(), view);
        
        userActivateTabs = userActivateTabs.addList(view);
    }

    private ImList<PropertyDrawInstance> userActivateProps = ListFact.EMPTY(); 
    // программный activate property
    public void activateProperty(PropertyDrawEntity view) {
        userActivateProps = userActivateProps.addList(instanceFactory.getInstance(view));
    }
    
    private ImList<ContainerView> userCollapseContainers = ListFact.EMPTY();
    public void collapseContainer(ContainerView container) throws SQLException, SQLHandledException {
        setContainerCollapsed(container, true);
        userCollapseContainers = userCollapseContainers.addList(container);
    } 
    
    private ImList<ContainerView> userExpandContainers = ListFact.EMPTY();
    public void expandContainer(ContainerView container) throws SQLException, SQLHandledException {
        setContainerCollapsed(container, false);
        userExpandContainers = userExpandContainers.addList(container);
    }

    // кэш на изменение
    protected Set<PropertyObjectInstance> isReallyChanged = new HashSet<>();
    public boolean containsChange(PropertyObjectInstance instance) {
        return isReallyChanged.contains(instance);
    }
    public void addChange(PropertyObjectInstance instance) {
        isReallyChanged.add(instance);
    }

    protected Set<PropertyReaderInstance> pendingRead = SetFact.mAddRemoveSet();

    private ComponentView getDrawComponent(PropertyDrawInstance<?> property) {
        return entity.getDrawComponent(property.entity);
    }

    private boolean isUserHidden(PropertyDrawInstance<?> property) {
        ComponentView drawComponent = getDrawComponent(property);
        assert !isNoUserHidden(drawComponent); // так как если бы был null не попалы бы в newIsShown в readShowIfs
        return isUserHidden(drawComponent);
    }

    private boolean isHidden(ComponentView component) { // is Tab or showIfHidden or designHidden
        if(component.isMain()) // form container
            return false;

        if(isStaticHidden(component))
            return true;

        // if this is a tab / collapsible container - use it's parent, since we want it's caption to be updated too (however maybe later we'll have to distinguish caption from other attributes)
        ComponentView dynamicHidableContainer = component.getDynamicHidableContainer();
        if(dynamicHidableContainer == component && component.isUserHidable())
            dynamicHidableContainer = component.getHiddenContainer().getDynamicHidableContainer();

        return dynamicHidableContainer != null && isDynamicHidden(dynamicHidableContainer);
    }

    private boolean isHidden(GroupObjectInstance group) { // is Tab or showIfHidden or designHidden
        FormEntity.ComponentUpSet containers = entity.getDrawDynamicHideableContainers(group.entity);
        if (containers == null) // cheat / оптимизация, иначе пришлось бы в isHidden и еще в нескольких местах явную проверку на null
            return false;
        for (ComponentView component : containers.it())
            if (!isDynamicHidden(component))
                return false;
        // для случая, когда группа PANEL, в группе только свойства, зависящие от ключей, и находятся не в первом табе.
        // наличие ключа влияет на видимость этих свойств, которая в свою очередь влияет на видимость таба.
        // неполное решение, т.к. возможны ещё более сложные случаи
        if (group.isNull() && group.entity.isPanel()) {
            return false;
        }
        return true;
    }

    private boolean isNoUserHidden(ComponentView component) { // design or showf
        return isStaticHidden(component) || isShowIfHidden(component);
    }

    private boolean isDynamicHidden(ComponentView component) { // showif or tab
        assert !isStaticHidden(component);
        assert component.isDynamicHidable();
        return isShowIfHidden(component) || isUserHidden(component);
    }

    private boolean isStaticHidden(ComponentView component) { // static
        return entity.isDesignHidden(component);
    }

    private boolean isShowIfHidden(ComponentView component) { // dynamic
        assert !isStaticHidden(component);

        ComponentView showIfHidableContainer = component.getShowIfHidableContainer();
        if(showIfHidableContainer == null)
            return false;
        assert !isStaticHidden(showIfHidableContainer);
        assert showIfHidableContainer.isShowIfHidable();

        if(isComponentHidden.contains(showIfHidableContainer))
            return true;

        return isShowIfHidden(showIfHidableContainer.getHiddenContainer());
    }

    private boolean isUserHidden(ComponentView component) {
        ComponentView userHidableContainer = component.getUserHidableContainer();
        if(userHidableContainer == null)
            return false;
        assert !isNoUserHidden(userHidableContainer);
        assert userHidableContainer.isUserHidable();

        ComponentView container = userHidableContainer.getHiddenContainer();
        if(container instanceof ContainerView && ((ContainerView) container).isTabbed()) {
            ComponentView visible = visibleTabs.get((ContainerView)container);
            ImList<ComponentView> siblings = ((ContainerView) container).getChildrenList();
            if (visible == null && siblings.size() > 0) // аналогичные проверки на клиентах, чтобы при init'е не вызывать
                visible = siblings.get(0);
            if (!userHidableContainer.equals(visible))
                return true;
        } else {
            assert ((ContainerView) userHidableContainer).isCollapsible();
            if(collapsedContainers.contains((ContainerView) userHidableContainer))
                return true;
        }

        return isUserHidden(userHidableContainer.getHiddenContainer());
    }

    protected Map<ContainerView, ComponentView> visibleTabs = new HashMap<>();
    protected Set<ContainerView> collapsedContainers = new HashSet<>();

    public void setTabVisible(ContainerView view, ComponentView page) throws SQLException, SQLHandledException {
        assert view.isTabbed();
        updateActiveTabProperty(page);
        visibleTabs.put(view, page);
    }
    
    public void setContainerCollapsed(ContainerView container, boolean collapsed) throws SQLException, SQLHandledException {
        if (collapsed) {
            collapsedContainers.add(container);
        } else {
            collapsedContainers.remove(container);
        }
    }

    private void updateActiveTabProperty(ComponentView page) throws SQLException, SQLHandledException {
        ComponentView prevActiveTab = visibleTabs.get(page.getLayoutParamContainer());
        if(prevActiveTab != null) {
            prevActiveTab.updateActiveTabProperty(session, null);
        }
        page.updateActiveTabProperty(session, true);
    }

    public ImOrderSet<PropertyDrawEntity> getPropertyEntitiesShownInGroup(final GroupObjectInstance group) {
        return properties.filterList(property -> {
            return isShown.contains(property) && property.isProperty() && property.isList() && property.toDraw == group; // toDraw and not getApplyObject to get WYSIWYG
        }).toOrderExclSet().mapOrderSetValues(value -> ((PropertyDrawInstance<?>)value).entity);
    }

    public ImOrderSet<PropertyDrawEntity> getVisibleProperties(final GroupObjectInstance groupObject, ImOrderSet<PropertyDrawEntity> properties, FormUserPreferences preferences) {

        final GroupObjectUserPreferences groupPreferences = getGroupPreferences(groupObject, preferences);
        if (groupPreferences == null)
            return properties;

        return properties.filterOrder(property -> {
            // to check hide we need FormView, now we don't have it and it is not that important
            ColumnUserPreferences propertyPreferences = getPropertyPreferences(instanceFactory.getInstance(property), groupPreferences);
            if (propertyPreferences == null)
                return true;
            
            if(propertyPreferences.userHide != null && propertyPreferences.userHide)
                return false;
            
            return propertyPreferences.userOrder != null;
        });
    }

    // should be the same as GridTable.getOrderedVisibleProperties 
    public ImOrderSet<PropertyDrawEntity> getOrderedVisibleProperties(GroupObjectInstance groupObject, ImOrderSet<PropertyDrawEntity> properties, FormUserPreferences preferences) {
        // first part is skipped because props have been already filtered in getVisibleProperties
        GroupObjectUserPreferences groupPreferences = getGroupPreferences(groupObject, preferences);
        if (groupPreferences == null)
            return properties;

        List<PropertyDrawEntity> list = new ArrayList<>(properties.toJavaList());
        list.sort(getUserOrderComparator(properties, groupPreferences));
        return SetFact.fromJavaOrderSet(list);
    }

    private Comparator<PropertyDrawEntity> getUserOrderComparator(ImOrderSet<PropertyDrawEntity> baseOrder, final GroupObjectUserPreferences groupPreferences) {
        final ImMap<PropertyDrawEntity, Integer> userOrders = baseOrder.getSet().mapValues((PropertyDrawEntity value) -> getUserOrder(instanceFactory.getInstance(value), groupPreferences));
        return (c1, c2) -> {
            Integer order1 = userOrders.get(c1);
            Integer order2 = userOrders.get(c2);
            if (order1 == null)
                return order2 == null ? 0 : 1;
            else
                return order2 == null ? -1 : (order1 - order2);
        };
    }

    private Integer getUserOrder(PropertyDrawInstance property, GroupObjectUserPreferences groupPreferences) {
        ColumnUserPreferences propertyPreferences = getPropertyPreferences(property, groupPreferences);
        if (propertyPreferences == null)
            return null;

        return propertyPreferences.userOrder;
    }

    public FontInfo getUserFont(GroupObjectInstance group, FormUserPreferences preferences) {
        GroupObjectUserPreferences groupPreferences = getGroupPreferences(group, preferences);
        if (groupPreferences == null) 
            return null;

        return groupPreferences.fontInfo;
    }

    private GroupObjectUserPreferences getGroupPreferences(GroupObjectInstance group, FormUserPreferences preferences) {
        if (group == null || preferences == null) return null;

        GroupObjectUserPreferences groupPreferences = preferences.getUsedPreferences(group.getSID());
        if (groupPreferences == null) 
            return null;
        return groupPreferences;
    }

    public Integer getUserWidth(PropertyDrawInstance instance, FormUserPreferences preferences) {
        GroupObjectUserPreferences groupPreferences = getGroupPreferences(instance.toDraw, preferences);
        if (groupPreferences == null)
            return null;

        ColumnUserPreferences propertyPreferences = getPropertyPreferences(instance, groupPreferences);
        if (propertyPreferences == null) 
            return null;

        return propertyPreferences.userWidth;
    }

    private ColumnUserPreferences getPropertyPreferences(PropertyDrawInstance instance, GroupObjectUserPreferences groupPreferences) {
        ColumnUserPreferences propertyPreferences = groupPreferences.getColumnUserPreferences().get(instance.getSID());
        if(propertyPreferences == null) 
            return null;

        return propertyPreferences;
    }

    private boolean refresh = true;

    private boolean classUpdated(Updated updated, GroupObjectInstance groupObject) {
        return updated.classUpdated(SetFact.singleton(groupObject));
    }

    private boolean objectUpdated(Updated updated, GroupObjectInstance groupObject) {
        return updated.objectUpdated(SetFact.singleton(groupObject));
    }

    private boolean objectUpdated(Updated updated, ImSet<GroupObjectInstance> groupObjects) {
        return updated.objectUpdated(groupObjects);
    }

    private boolean propertyUpdated(PropertyObjectInstance<?> updated, ImSet<GroupObjectInstance> groupObjects, ChangedData changedProps, boolean hidden) throws SQLException, SQLHandledException {
        return objectUpdated(updated, groupObjects)
                || groupUpdated(groupObjects, UPDATED_KEYS)
                || dataUpdated(updated, changedProps, hidden, groupObjects);
    }

    private boolean groupUpdated(ImSet<GroupObjectInstance> groupObjects, int flags) {
        for (GroupObjectInstance groupObject : groupObjects)
            if ((groupObject.updated & flags) != 0)
                return true;
        return false;
    }

    private boolean dataUpdated(Updated updated, ChangedData changedProps, boolean hidden, ImSet<GroupObjectInstance> groupObjects) throws SQLException, SQLHandledException {
        return updated.dataUpdated(changedProps, this, getModifier(), hidden, groupObjects);
    }

    private void applyOrders() {
        for (GroupObjectInstance group : getGroups())
            group.orders = group.getSetOrders();
    }

    @Override
    public Object getProfiledObject() {
        return entity;
    }

    private static class GroupObjectValue {
        private GroupObjectInstance group;
        private ImMap<ObjectInstance, DataObject> value;

        private GroupObjectValue(GroupObjectInstance group, ImMap<ObjectInstance, DataObject> value) {
            this.group = group;
            this.value = value;
        }
    }

    private <R extends PropertyReaderInstance> void updateDrawProps(
            MExclMap<R, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties,
            ImSet<GroupObjectInstance> keyGroupObjects,
            ImSet<R> propertySet) throws SQLException, SQLHandledException {
        queryPropertyObjectValues(propertySet, properties, keyGroupObjects, PropertyReaderInstance::getPropertyObjectInstance);
    }

    private <T> Expr groupExpr(SQLFunction<PropertyObjectInstance<?>, Expr> getExpr, T key, Where groupModeWhere, ImMap<Object, Expr> groupModeExprs, ImSet<GroupMode> groupModes) throws SQLException, SQLHandledException {
        if(key instanceof AggrReaderInstance) {
            for(GroupMode groupMode : groupModes) {
                Expr transformedExpr = groupMode.transformExpr(getExpr, (AggrReaderInstance) key, groupModeWhere, groupModeExprs);
                if(transformedExpr != null)
                    return transformedExpr;
            }
        }
        return Expr.NULL();
    }
    private <T> Expr listExpr(SQLFunction<PropertyObjectInstance<?>, Expr> getExpr, T key, Function<T, PropertyObjectInstance<?>> getPropertyObject) throws SQLException, SQLHandledException {
        return getExpr.apply(getPropertyObject.apply(key));
    }

    @StackMessage("{message.form.update.props}")
    private <T> void queryPropertyObjectValues(
            @ParamMessage ImSet<T> keysSet,
            MExclMap<T, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> valuesMap,
            ImSet<GroupObjectInstance> keyGroupObjects,
            Function<T, PropertyObjectInstance<?>> getPropertyObject
    ) throws SQLException, SQLHandledException {

        ImRevMap<ObjectInstance, KeyExpr> mapKeys = KeyExpr.getMapKeys(GroupObjectInstance.getObjects(getUpTreeGroups(keyGroupObjects)));
        Modifier modifier = getModifier();

        Where groupModeWhere = null;
        ImMap<Object, Expr> groupModeExprs = MapFact.EMPTY();
        ImSet<GroupMode> groupModes = SetFact.EMPTY();
        ImSet<ObjectInstance> groupModeKeys = SetFact.EMPTY();

        Where groupWhere = Where.TRUE();
        for (GroupObjectInstance keyGroup : keyGroupObjects) {
            Where where = keyGroup.keyTable.getWhere(mapKeys); // call mapExprs to optimize if where is anded
            groupWhere = groupWhere.and(where);

            if(keyGroup.groupMode != null) {
                assert !keyGroup.isInTree();
                Where activeGroupWhere = keyGroup.getWhere(mapKeys, modifier);

                groupModes = groupModes.addExcl(keyGroup.groupMode);
                groupModeExprs = groupModeExprs.addExcl(keyGroup.getGroupExprs(mapKeys.filter(keyGroup.objects), modifier, null)); // because active group mode is usually one, so we will not do some extra optimization
                groupModeWhere = groupModeWhere != null ? groupModeWhere.and(activeGroupWhere) : activeGroupWhere;
                groupModeKeys = groupModeKeys.addExcl(keyGroup.objects);
            }
        }

        QueryBuilder<ObjectInstance, T> selectProps = new QueryBuilder<>(mapKeys, groupWhere);
        ImMap<ObjectInstance, ? extends Expr> mapExprs;

        if(groupModeWhere != null) {
            groupModeExprs = groupModeExprs.addExcl(mapKeys.removeIncl(groupModeKeys));
            mapExprs = mapKeys; // we don't need groupwhere in it, so will "disable" this optimization
        } else
            mapExprs = selectProps.getMapExprs();

        for (T key : keysSet) {
            SQLFunction<PropertyObjectInstance<?>, Expr> getExpr = value -> value.getExpr(mapExprs, modifier, this);
            Expr expr;

            if(groupModeWhere != null)
                expr = groupExpr(getExpr, key, groupModeWhere, groupModeExprs, groupModes);
            else
                expr = listExpr(getExpr, key, getPropertyObject);

            selectProps.addProperty(key, expr);
        }

        ImMap<ImMap<ObjectInstance, DataObject>, ImMap<T, ObjectValue>> queryResult = selectProps.executeClasses(this, BL.LM.baseClass).getMap();
        for (final T key : keysSet)
            valuesMap.exclAdd(key, queryResult.mapValues(value -> value.get(key)));
    }

    private void updateData(Result<ChangedData> mChangedProps, ExecutionStack stack, boolean forceLocalEvents, List<ClientAction> resultActions) throws SQLException, SQLHandledException {
        mChangedProps.set(mChangedProps.result.merge(session.updateExternal(this)));

        if (dataChanged) {
            if(forceLocalEvents || !entity.localAsync) {
                session.executeSessionEvents(this, stack);
            } else {
                resultActions.add(new AsyncGetRemoteChangesClientAction(true));
            }

            updateAsyncPropertyChanges();
            ChangedData update = session.update(this);
            if(update.wasRestart) // очищаем кэш при рестарте
                isReallyChanged.clear();
            mChangedProps.set(mChangedProps.result.merge(update));
            if(forceLocalEvents || !entity.localAsync) {
                dataChanged = false;
            }
        }

    }

    public FormChanges getChanges(ExecutionStack stack) throws SQLException, SQLHandledException {
        return getChanges(stack, false, new ArrayList<>());
    }

    @StackMessage("{message.form.end.apply}")
    @ThisMessage
    @AssertSynchronized
    public FormChanges getChanges(ExecutionStack stack, boolean forceLocalEvents, List<ClientAction> resultActions) throws SQLException, SQLHandledException {

        checkNavigatorDeactivated();

        final MFormChanges result = new MFormChanges();

        QueryEnvironment queryEnv = getQueryEnv();

        // если изменились данные, применяем изменения
        Result<ChangedData> mChangedProps = new Result<>(ChangedData.EMPTY);  // так как могут еще измениться свойства созданные при помощи операторов форм
        updateData(mChangedProps, stack, forceLocalEvents, resultActions);

        MSet<PropertyDrawInstance> mChangedDrawProps = SetFact.mSet();

        fillChangedObjects(result, stack, queryEnv, mChangedProps, mChangedDrawProps);

        updateData(mChangedProps, stack, forceLocalEvents, resultActions); // повторная проверка для VIEW свойств

        fillChangedDrawProps(result, mChangedDrawProps.immutable().merge(forcePropertyDrawUpdates), mChangedProps.result);
        
        result.activateTabs.addAll(userActivateTabs);
        result.activateProps.addAll(userActivateProps);
        
        result.collapseContainers.addAll(userCollapseContainers);
        result.expandContainers.addAll(userExpandContainers);

        result.needConfirm = needConfirm();

        // сбрасываем все пометки
        userActivateTabs = ListFact.EMPTY();
        userActivateProps = ListFact.EMPTY();
        userCollapseContainers = ListFact.EMPTY();
        userExpandContainers = ListFact.EMPTY();
        for (GroupObjectInstance group : getGroups()) {
            for (ObjectInstance object : group.objects)
                object.updated = 0;
            group.updated = 0;
        }
        forcePropertyDrawUpdates = SetFact.EMPTY();
        refresh = false;

//        result.out(this);
//        try {
//            Thread.sleep(4000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        return result.immutable();
    }

    @StackMessage("{message.getting.changed.objects}")
    public void fillChangedObjects(MFormChanges result, ExecutionStack stack, QueryEnvironment queryEnv, Result<ChangedData> mChangedProps, MSet<PropertyDrawInstance> mChangedDrawProps) throws SQLException, SQLHandledException {
        GroupObjectValue updateGroupObject = null; // так как текущий groupObject идет относительно treeGroup, а не group
        for (GroupObjectInstance group : getOrderGroups()) {
            try {
                ImMap<ObjectInstance, DataObject> selectObjects = group.updateKeys(session.sql, queryEnv, getModifier(), environmentIncrement, this, BL.LM.baseClass, isHidden(group), refresh, result, mChangedDrawProps, mChangedProps, this);
                if (selectObjects != null) // то есть нужно изменять объект
                    updateGroupObject = new GroupObjectValue(group, selectObjects);

                if (group.getDownTreeGroups().size() == 0 && updateGroupObject != null) { // так как в tree группе currentObject друг на друга никак не влияют, то можно и нужно делать updateGroupObject в конце
                    updateGroupObject.group.update(session, result, this, updateGroupObject.value, stack);
                    updateGroupObject = null;
                }
            } catch (EmptyStackException e) {
                systemLogger.error("OBJECTS : " + group.toString() + " FORM " + entity.toString());
                throw Throwables.propagate(e);
            }
        }
    }

    private void checkNavigatorDeactivated() {
        CustomClassListener classListener = getClassListener();
        ServerLoggers.assertLog(classListener == null || !classListener.isDeactivated(), "NAVIGATOR DEACTIVATED " + BaseUtils.nullToString(classListener));
    }

    @StackMessage("{message.getting.visible.properties}")
    private Set<PropertyDrawInstance> readShowIfs(ChangedData changedProps, MFormChanges result) throws SQLException, SQLHandledException {

        updateContainersShowIfs(changedProps);

        updateBaseComponentsShowIfs(result);

        return updatePropertiesShowIfs(changedProps, result);
    }

    private Set<PropertyDrawInstance> updatePropertiesShowIfs(ChangedData changedProps, MFormChanges result) throws SQLException, SQLHandledException {
        Set<PropertyDrawInstance> newShown = new HashSet<>();

        MAddSet<ComponentView> hiddenButDefinitelyShownSet = SetFact.mAddSet(); // не ComponentDownSet для оптимизации
        MAddExclMap<PropertyReaderInstance, ComponentView> hiddenNotSureShown = MapFact.mAddExclMap();
        final MExclMap<PropertyDrawInstance.ShowIfReaderInstance, ImSet<GroupObjectInstance>> mShowIfs = MapFact.mExclMap();
        for (PropertyDrawInstance<?> drawProperty : properties) {
            ImSet<GroupObjectInstance> propRowGrids = drawProperty.getGroupObjectsInGrid();
            ComponentView drawComponent = getDrawComponent(drawProperty);

            // in theory this part can be optimized to check if (objects, or user preference, hidden was update) but probably it will be premature optimization
            boolean newStaticShown = isPropertyStaticShown(drawComponent, drawProperty, propRowGrids);
            boolean oldStaticShown = addShownHidden(isStaticShown, drawProperty, newStaticShown);

            if (newStaticShown) {
                newShown.add(drawProperty);

                boolean hidden = isUserHidden(drawProperty);
                ComponentView userHidableContainer = drawComponent.getUserHidableContainer(); // у tab container'а по сравнению с containerShowIfs есть разница, так как они оптимизированы на изменение видимости без перезапроса данных

                boolean isDefinitelyShown = drawProperty.propertyShowIf == null;
                if (!isDefinitelyShown) {
                    ImSet<GroupObjectInstance> propRowColumnGrids = drawProperty.getColumnGroupObjectsInGrid();
                    PropertyDrawInstance.ShowIfReaderInstance showIfReader = drawProperty.showIfReader;
                    GroupObjectInstance toDraw = drawProperty.toDraw;
                    boolean read = refresh // this check is pretty equivalent to fillChangedReader
                                   || !oldStaticShown
                                   || (toDraw != null && toDraw.toRefresh())
                                   || (!hidden && pendingRead.contains(showIfReader))
                                   || propertyUpdated(drawProperty.propertyShowIf, propRowColumnGrids, changedProps, hidden);
                    if (read) {
                        mShowIfs.exclAdd(showIfReader, propRowColumnGrids);
                        if(hidden)
                            hiddenNotSureShown.exclAdd(showIfReader, userHidableContainer);
                    } else {
                        // nor static / nor dynamic visibility changed, reading from cache
                        boolean oldShown = isShown.contains(drawProperty);
                        if(!oldShown)
                            newShown.remove(drawProperty);
                        isDefinitelyShown = oldShown;
                    }
                }
                if(hidden && isDefinitelyShown) // помечаем component'ы которые точно показываются
                    hiddenButDefinitelyShownSet.add(userHidableContainer);
            }
        }
        ImMap<PropertyDrawInstance.ShowIfReaderInstance, ImSet<GroupObjectInstance>> showIfs = mShowIfs.immutable();

        if(hiddenNotSureShown.size() > 0) { // optimization
            FormEntity.ComponentDownSet hiddenButDefinitelyShown = FormEntity.ComponentDownSet.create(hiddenButDefinitelyShownSet);

            MFilterMap<PropertyDrawInstance.ShowIfReaderInstance, ImSet<GroupObjectInstance>> mRestShowIfs = MapFact.mFilter(showIfs);
            for (int i = 0, size = showIfs.size(); i < size; i++) {
                PropertyDrawInstance.ShowIfReaderInstance showIf = showIfs.getKey(i);
                ComponentView component = hiddenNotSureShown.get(showIf);
                if (component != null && hiddenButDefinitelyShown.containsAll(component)) // те которые попали в hiddenButDefinitelyShown - добавляем в pendingHidden, исключаем из ShowIf'а
                    pendingRead.add(showIf);
                else { // исключаем из pendingHidden, оставляем в map'е
                    pendingRead.remove(showIf);
                    mRestShowIfs.keep(showIf, showIfs.getValue(i));
                }
            }
            showIfs = MapFact.imFilter(mRestShowIfs, showIfs);
        } else {
            pendingRead.removeAll(showIfs.keys().toJavaCol());
        }

        MExclMap<PropertyDrawInstance.ShowIfReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> mShowIfValues = MapFact.mExclMap();
        ImMap<ImSet<GroupObjectInstance>, ImSet<PropertyDrawInstance.ShowIfReaderInstance>> changedShowIfs = showIfs.groupValues();
        for (int i = 0, size = changedShowIfs.size(); i < size; i++) {
            updateDrawProps(mShowIfValues, changedShowIfs.getKey(i), changedShowIfs.getValue(i));
        }

        ImMap<PropertyDrawInstance.ShowIfReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> showIfValues = mShowIfValues.immutable();
        for (int i = 0, size = showIfValues.size(); i < size; ++i) {
            PropertyDrawInstance.ShowIfReaderInstance key = showIfValues.getKey(i);
            ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue> values = showIfValues.getValue(i);

            boolean allNull = true;
            for (ObjectValue val : values.valueIt()) {
                if (val.getValue() != null) {
                    allNull = false;
                    break;
                }
            }

            if (allNull) {
                newShown.remove(key.getPropertyDraw());
            } else {
                assert newShown.contains(key.getPropertyDraw());
                result.properties.exclAdd(key, values);
            }
        }

        return newShown;
    }

    private boolean isPropertyStaticShown(ComponentView drawComponent, PropertyDrawInstance drawProperty, ImSet<GroupObjectInstance> propRowColumnGrids) {
        if(!drawProperty.isInInterface(propRowColumnGrids, true) && !drawProperty.getEntity().ignoreIsInInterfaceCheck) { // don't show property if it is always null
            return false;
        }

        if (isNoUserHidden(drawComponent)) { // hidden, но без учета tab, для него отдельная оптимизация, чтобы не переобновляться при переключении "туда-назад",  связан с assertion'ом в FormInstance.isHidden
            return false;
        }

        if (userPrefsHiddenProperties.contains(drawProperty) && drawProperty.isList()) { // панель показывается всегда
            return false;
        }

        return true;
    }

    private void updateContainersShowIfs(ChangedData changedProps) throws SQLException, SQLHandledException {
        ImSet<ComponentView> changed = entity.getPropertyComponents().<SQLException, SQLHandledException>filterFnEx(
                key -> key.showIf != null && (refresh || propertyUpdated(instanceFactory.getInstance(key.showIf), SetFact.EMPTY(), changedProps, false)));

        if(changed.isEmpty()) // optimization
            return;

        MExclMap<ComponentView, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> mChangedValues = MapFact.mExclMap();
        queryPropertyObjectValues(changed, mChangedValues, SetFact.EMPTY(), GET_COMPONENT_SHOWIF);
        ImMap<ComponentView, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> changedValues = mChangedValues.immutable();

        for (int i = 0, size = changedValues.size() ; i < size; i++) {
            addShownHidden(isComponentHidden, changedValues.getKey(i), changedValues.getValue(i).getValue(0).getValue() == null);
        }
    }

    private void updateBaseComponentsShowIfs(MFormChanges result) {
        ImSet<ComponentView> changed = entity.getBaseComponents();

        if(changed.isEmpty()) // optimization
            return;

        for (int i = 0; i < changed.size(); i++) {
            ComponentView component = changed.get(i);
            boolean newIsHidden = isNoUserHidden(component);
            boolean oldIsHidden = addShownHidden(isBaseComponentHidden, component, newIsHidden);

            if(newIsHidden != oldIsHidden) {
                BaseComponentViewInstance componentInstance = instanceFactory.getInstance(component);
                ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue> values = MapFact.singleton(MapFact.EMPTY(), newIsHidden ? new DataObject(true) : NullValue.instance);
                result.properties.exclAdd(componentInstance.showIfReader, values);
            }
        }
    }

    private ImSet<PropertyDrawInstance> forcePropertyDrawUpdates = SetFact.EMPTY();
    public void forcePropertyDrawUpdate(PropertyDrawInstance propertyDraw) {
        forcePropertyDrawUpdates = forcePropertyDrawUpdates.merge(propertyDraw);
    }

    private boolean propertyUpdated(PropertyReaderInstance propertyDraw, GroupObjectInstance toDraw, PropertyObjectInstance propertyObjectInstance, ImSet<GroupObjectInstance> groupObjects, ImSet<PropertyDrawInstance> changedDrawProps, ChangedData changedProps, boolean hidden) throws SQLException, SQLHandledException {
        if (propertyDraw instanceof PropertyDrawInstance && changedDrawProps.contains((PropertyDrawInstance) propertyDraw))
            return true;

        // since we return null for all not needed props they are not updated (except when keys are updated and we need to resend nulls)
        if(toDraw != null && toDraw.groupMode != null && !(propertyDraw instanceof PropertyDrawInstance && (toDraw.groupMode.need((PropertyDrawInstance) propertyDraw) || groupUpdated(groupObjects, UPDATED_KEYS))))
            return false;

        return propertyUpdated(propertyObjectInstance, groupObjects, changedProps, hidden);
    }

    @StackMessage("{message.getting.changed.property.values}")
    private void fillChangedDrawProps(MFormChanges result, ImSet<PropertyDrawInstance> changedDrawProps, ChangedData changedProps) throws SQLException, SQLHandledException {
        Set<PropertyDrawInstance> newShown = readShowIfs(changedProps, result);

        ImMap<PropertyReaderInstance, ImSet<GroupObjectInstance>> readProperties = getChangedDrawProps(newShown, changedDrawProps, changedProps, result);

        ImMap<ImSet<GroupObjectInstance>, ImSet<PropertyReaderInstance>> groupReadProps = readProperties.groupValues();
        for (int i = 0, size = groupReadProps.size(); i < size; i++) {
            updateDrawProps(result.properties, groupReadProps.getKey(i), groupReadProps.getValue(i));
        }
    }

    @StackMessage("{message.getting.changed.properties}")
    private ImMap<PropertyReaderInstance, ImSet<GroupObjectInstance>> getChangedDrawProps(Set<PropertyDrawInstance> newShown, ImSet<PropertyDrawInstance> changedDrawProps, ChangedData changedProps, MFormChanges result) throws SQLException, SQLHandledException {
        MExclMap<PropertyReaderInstance, ImSet<GroupObjectInstance>> mReadProperties = MapFact.mExclMap();

        for (PropertyDrawInstance<?> drawProperty : properties) {
            boolean newPropIsShown = newShown.contains(drawProperty);
            boolean oldPropIsShown = addShownHidden(isShown, drawProperty, newPropIsShown);

            if (newPropIsShown) {
                GroupObjectInstance toDraw = drawProperty.toDraw;
                boolean update = toDraw == null || !drawProperty.isList() || toDraw.toUpdate();
                boolean updateCaption = update || (drawProperty.isList() && toDraw.listViewType.isPivot() && toDraw.toRefresh()); // we want to update captions when switching to pivot to avoid some unnecessary effects (blinking when default property captions are shown, especially when there are group-to-columns) since pivot really relies on caption
                boolean hidden = isUserHidden(drawProperty);

                ImSet<GroupObjectInstance> propRowGrids = drawProperty.getGroupObjectsInGrid();
                ImSet<GroupObjectInstance> propRowColumnGrids = drawProperty.getColumnGroupObjectsInGrid();

                fillChangedReader(drawProperty, toDraw, result, propRowGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
                fillChangedReader(drawProperty.captionReader, toDraw, result, propRowColumnGrids, hidden, updateCaption, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
                fillChangedReader(drawProperty.footerReader, toDraw, result, propRowColumnGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
                fillChangedReader(drawProperty.readOnlyReader, toDraw, result, propRowGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
                fillChangedReader(drawProperty.backgroundReader, toDraw, result, propRowGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
                fillChangedReader(drawProperty.foregroundReader, toDraw, result, propRowGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
                fillChangedReader(drawProperty.imageReader, toDraw, result, propRowGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
                for(PropertyDrawInstance<?>.LastReaderInstance aggrLastReader : drawProperty.aggrLastReaders)
                    fillChangedReader(aggrLastReader, toDraw, result, propRowGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
            } else if (oldPropIsShown) {
                result.dropProperties.exclAdd(drawProperty);
            }
        }

        for (GroupObjectInstance group : getGroups()) {
            boolean hidden = isHidden(group);
            boolean update = group.toUpdate();

            ImSet<GroupObjectInstance> gridGroups = (group.viewType.isList() ? SetFact.singleton(group) : SetFact.EMPTY());

            fillChangedReader(group.rowBackgroundReader, group, result, gridGroups, hidden, update, true, mReadProperties, changedDrawProps, changedProps);
            fillChangedReader(group.rowForegroundReader, group, result, gridGroups, hidden, update, true, mReadProperties, changedDrawProps, changedProps);
            fillChangedReader(group.customOptionsReader, null, result, gridGroups, hidden, update, true, mReadProperties, changedDrawProps, changedProps);
        }

        for (ComponentView component : entity.getPropertyComponents()) {
            if(component instanceof ContainerView) {
                ContainerView container = (ContainerView) component;

                boolean hidden = isHidden(container);
                boolean update = true;

                ImSet<GroupObjectInstance> gridGroups = SetFact.EMPTY();

                ContainerViewInstance containerInstance = instanceFactory.getInstance(container);
                fillChangedReader(containerInstance.captionReader, null, result, gridGroups, hidden, update, true, mReadProperties, changedDrawProps, changedProps);
                fillChangedReader(containerInstance.customDesignReader, null, result, gridGroups, hidden, update, true, mReadProperties, changedDrawProps, changedProps);
            }
        }
        return mReadProperties.immutable();
    }

    private void fillChangedReader(PropertyReaderInstance propertyReader, GroupObjectInstance toDraw, MFormChanges result, ImSet<GroupObjectInstance> columnGroupGrids, boolean hidden, boolean update, boolean wasShown, MExclMap<PropertyReaderInstance, ImSet<GroupObjectInstance>> readProperties, ImSet<PropertyDrawInstance> changedDrawProps, ChangedData changedProps) throws SQLException, SQLHandledException {
        PropertyObjectInstance<?> drawProperty = propertyReader.getPropertyObjectInstance();
        if(drawProperty == null)
            return;

        boolean needed = !hidden && update;

        boolean read = refresh || !wasShown || (toDraw != null && toDraw.toRefresh()) || propertyUpdated(propertyReader, toDraw, drawProperty, columnGroupGrids, changedDrawProps, changedProps, !needed);

        if(needed)
            read = pendingRead.remove(propertyReader) || read;
        else {
            if(read) {
                pendingRead.add(propertyReader);

                if(propertyReader instanceof PropertyDrawInstance) // we need to send property to the client anyway, because tab requires that information to be shown (or not) + group mode requires
                    readProperties.exclAdd(((PropertyDrawInstance<?>)propertyReader).hiddenReader, columnGroupGrids);

                read = false;
            }
        }

        if(toDraw != null && (read || pendingRead.contains(propertyReader))) {
            toDraw.checkPending(result, () -> {
                if (!hidden && !update)
                    toDraw.pendingUpdateProps.add(propertyReader);
                else
                    toDraw.pendingUpdateProps.remove(propertyReader);
            });
        }

        if(read)
            readProperties.exclAdd(propertyReader, columnGroupGrids);
    }

    @Deprecated
    public FormData getFormData(int orderTop) throws SQLException, SQLHandledException {
        return getFormData(getProperties(), getGroups(), orderTop);
    }

    public ImSet<PropertyDrawInstance<?>> getProperties() {
        return properties.toOrderSet().getSet().filterFn(PropertyDrawInstance::isProperty);
    }

    // считывает все данные с формы
    @Deprecated
    public FormData getFormData(ImSet<PropertyDrawInstance<?>> propertyDraws, ImSet<GroupObjectInstance> classGroups, int orderTop) throws SQLException, SQLHandledException {

        checkNavigatorDeactivated();

        applyOrders();

        // пока сделаем тупо получаем один большой запрос

        QueryBuilder<ObjectInstance, Object> query = new QueryBuilder<>(GroupObjectInstance.getObjects(classGroups));
        MOrderMap<Object, Boolean> mQueryOrders = MapFact.mOrderMap();

        for (GroupObjectInstance group : getGroups()) {

            if (classGroups.contains(group)) {

                // не фиксированные ключи
                query.and(group.getWhere(query.getMapExprs(), getModifier()));

                // закинем Order'ы
                for (int i = 0, size = group.orders.size(); i < size; i++) {
                    Object orderObject = new Object();
                    query.addProperty(orderObject, group.orders.getKey(i).getExpr(query.getMapExprs(), getModifier()));
                    mQueryOrders.add(orderObject, group.orders.getValue(i));
                }

                for (ObjectInstance object : group.objects) {
                    query.addProperty(object, object.getExpr(query.getMapExprs(), getModifier()));
                    mQueryOrders.add(object, false);
                }

                if (group.viewType.isPanel()) {
                    for (ObjectInstance object : group.objects) {
                        query.and(object.getExpr(query.getMapExprs(), getModifier()).compare(object.getObjectValue().getExpr(), Compare.EQUALS));
                    }
                }
            }
        }

        for (PropertyDrawInstance<?> property : propertyDraws)
            query.addProperty(property, property.getDrawInstance().getExpr(query.getMapExprs(), getModifier()));

        ImOrderMap<ImMap<ObjectInstance, Object>, ImMap<Object, Object>> resultSelect = query.execute(this, mQueryOrders.immutableOrder(), orderTop);

        Set<Integer> notEmptyValues = new HashSet<>();
        LinkedHashMap<ImMap<ObjectInstance, Object>, ImMap<PropertyDrawInstance, Object>> result = new LinkedHashMap<>();
        MOrderExclMap<ImMap<ObjectInstance, Object>, ImMap<PropertyDrawInstance, Object>> mResult = MapFact.mOrderExclMap(resultSelect.size());
        for (int i = 0, size = resultSelect.size(); i < size; i++) {
            ImMap<ObjectInstance, Object> resultKey = resultSelect.getKey(i);
            ImMap<Object, Object> resultValue = resultSelect.getValue(i);

            MExclMap<ObjectInstance, Object> mGroupValue = MapFact.mExclMap();
            for (GroupObjectInstance group : getGroups())
                for (ObjectInstance object : group.objects)
                    if (classGroups.contains(group))
                        mGroupValue.exclAdd(object, resultKey.get(object));
                    else
                        mGroupValue.exclAdd(object, object.getObjectValue().getValue());
            ImMap<PropertyDrawInstance, Object> values = resultValue.filterIncl(propertyDraws);
            for(int j = 0; j < values.size(); j++) {
                if(values.getValue(j) != null)
                    notEmptyValues.add(j);
            }
            result.put(mGroupValue.immutable(), resultValue.filterIncl(propertyDraws));
        }
        for(Entry<ImMap<ObjectInstance, Object>, ImMap<PropertyDrawInstance, Object>> entry : result.entrySet()) {
            ImMap<PropertyDrawInstance, Object> sourceValues = entry.getValue();
            ImMap<PropertyDrawInstance, Object> targetValues = MapFact.EMPTY();
            for(int j = 0; j < sourceValues.size(); j++) {
                if(notEmptyValues.contains(j))
                    targetValues = targetValues.addExcl(sourceValues.getKey(j), sourceValues.getValue(j));
            }
            mResult.exclAdd(entry.getKey(), targetValues);    
        }

        return new FormData(mResult.immutableOrder());
    }

    public Object read(PropertyObjectInstance<?> property) throws SQLException, SQLHandledException {
        return property.read(this);
    }

    // ---------------------------------------- Events ----------------------------------------

    public void fireObjectChanged(ObjectInstance object, ExecutionStack stack) throws SQLException, SQLHandledException {
        fireEvent(object.entity, stack);
    }

    public void fireOnInit(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireEvent(FormEventType.INIT, stack);
    }

    public void fireOnBeforeApply(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireEvent(FormEventType.BEFOREAPPLY, stack);
    }

    public void fireOnAfterApply(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireEvent(FormEventType.AFTERAPPLY, stack);
    }

    public void fireOnBeforeOk(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireEvent(FormEventType.BEFOREOK, stack);
    }

    public void fireOnAfterOk(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireEvent(FormEventType.AFTEROK, stack);
        formResult = FormCloseType.OK;
    }
    
    public void fireOnOk(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireEvent(FormEventType.OK, stack);
    }

    public void fireOnCancel(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireEvent(FormEventType.CANCEL, stack);
    }

    public ImOrderSet<ActionValueImplement> getEventsOnOk() {
        return getEvents(FormEventType.OK);
    }
    public ImOrderSet<ActionValueImplement> getEventsOnApply() {
        return getEvents(FormEventType.APPLY);
    }

    public void fireOnClose(ExecutionStack stack) throws SQLException, SQLHandledException {
        formResult = FormCloseType.CLOSE;
        fireEvent(FormEventType.CLOSE, stack);
    }

    public void fireOnDrop(ExecutionStack stack) throws SQLException, SQLHandledException {
        formResult = FormCloseType.DROP;
        fireEvent(FormEventType.DROP, stack);
    }

    public void fireQueryClose(ExecutionStack stack, boolean ok) throws SQLException, SQLHandledException {
        fireEvent(ok ? FormEventType.QUERYOK : FormEventType.QUERYCLOSE, stack);
    }

    public void fireFormSchedulerEvent(ExecutionStack stack, FormScheduler formScheduler) throws SQLException, SQLHandledException {
        fireEvent(formScheduler, stack);
    }

    private void fireEvent(Object eventObject, ExecutionStack stack) throws SQLException, SQLHandledException {
        for(ActionValueImplement event : getEvents(eventObject))
            event.execute(this, stack);
    }

    private ImOrderSet<ActionValueImplement> getEvents(Object eventObject) {
        MOrderExclSet<ActionValueImplement> mResult = SetFact.mOrderExclSet();
        Iterable<ActionObjectEntity<?>> actionsOnEvent = entity.getEventActionsListIt(eventObject);
        if (actionsOnEvent != null) {
            for (ActionObjectEntity<?> autoAction : actionsOnEvent) {
                ActionObjectInstance<? extends PropertyInterface> autoInstance = instanceFactory.getInstance(autoAction);
                if (securityPolicy.checkPropertyChangePermission(autoAction.property, autoAction.property)) { // для проверки null'ов и политики безопасности
                    mResult.exclAdd(autoInstance.getValueImplement(this));
                }
            }
        }
        return mResult.immutableOrder();
    }

    private FormCloseType formResult = FormCloseType.DROP;

    public FormCloseType getFormResult() {
        return formResult;
    }

    public DataSession getSession() {
        return session;
    }

    public Locale getLocale() {
        return locale;
    }
    
    private final IncrementChangeProps environmentIncrement;
    private final ImMap<SessionDataProperty, Pair<GroupObjectInstance, GroupObjectProp>> environmentIncrementSources;

    public class FormModifier extends OverridePropSourceSessionModifier<SessionDataProperty> {

        public FormModifier(String debugInfo, IncrementChangeProps overrideChange, FunctionSet<Property> forceDisableHintIncrement, FunctionSet<Property> forceDisableNoUpdate, FunctionSet<Property> forceHintIncrement, FunctionSet<Property> forceNoUpdate, SessionModifier modifier) {
            super(debugInfo, overrideChange, forceDisableHintIncrement, forceDisableNoUpdate, forceHintIncrement, forceNoUpdate, modifier);
        }

        @Override
        protected ImSet<Property> getSourceProperties(SessionDataProperty property) {
            Pair<GroupObjectInstance, GroupObjectProp> source = environmentIncrementSources.get(property);
            if(source == null)
                return SetFact.EMPTY();
            ImSet<Property> result = source.first.getUsedEnvironmentIncrementProps(source.second);
            if(result == null)
                return SetFact.EMPTY();
            return result;
        }

            // нужно не в транзакции, так как если откатится, у ведомления начнут приходить не целостными из restart и это может привести к странному поведению
            // поэтому по-хорошему надо делать явное обновление в restart (как updateSessionEventNotChangedOld), но пока делать не будем, а просто не будем update'ить в транзакции
//            @Override
//            protected boolean noUpdateInTransaction() {
//                return false;
//            }

        // recursion guard, just like in notifySourceChange (however this guard is needed optimization if property is really complex and thus uses a lot of prereads / materialized changes)
        private ImSet<Pair<GroupObjectInstance, GroupObjectProp>> updateChangesRecursionGuard = SetFact.EMPTY();

        public void updateEnvironmentIncrementProp(Pair<GroupObjectInstance, GroupObjectProp> source, IncrementChangeProps environmentIncrement, Result<ChangedData> changedProps, final ReallyChanged reallyChanged, boolean propsChanged, boolean dataChanged) throws SQLException, SQLHandledException {
            if(!updateChangesRecursionGuard.contains(source)) {
                ImSet<Pair<GroupObjectInstance, GroupObjectProp>> prevRecursionGuard = updateChangesRecursionGuard;
                updateChangesRecursionGuard = updateChangesRecursionGuard.addExcl(source);
                try {
                    source.first.updateEnvironmentIncrementProp(environmentIncrement, this, changedProps, reallyChanged, source.second, propsChanged, dataChanged, FormInstance.this::isHidden);
                } finally {
                    updateChangesRecursionGuard = prevRecursionGuard;
                }
            }
        }

        @Override
        protected void updateSource(SessionDataProperty property, boolean dataChanged, boolean forceUpdate) throws SQLException, SQLHandledException {
            if(!getSQL().isInTransaction()) { // если в транзакции предполагается что все обновится само (в форме - refresh будет)
                Pair<GroupObjectInstance, GroupObjectProp> source = environmentIncrementSources.get(property);
                updateEnvironmentIncrementProp(source, environmentIncrement, null, FormInstance.this, false, dataChanged);
            } else
                ServerLoggers.exinfoLog("FAILED TO UPDATE SOURCE IN TRANSACTION " + property);
        }
    }

    private FormModifier createModifier() {
        return new FormModifier(toString(), environmentIncrement, SetFact.EMPTY(), SetFact.EMPTY(), entity.getHintsIncrementTable(), entity.getHintsNoUpdate(), session.getModifier());
    }

    public Map<SessionModifier, FormModifier> modifiers = new HashMap<>();

    @ManualLazy
    public FormModifier getModifier() {
        SessionModifier sessionModifier = session.getModifier();
        FormModifier modifier = modifiers.get(sessionModifier);
        if (modifier == null) {
            modifier = createModifier();
            modifiers.put(sessionModifier, modifier);
        }
        return modifier;
    }

    public FormInstance getFormInstance() {
        return this;
    }

    // close делать не надо, так как по умолчанию добавляется обработчик события formClose
    public void formQueryClose(ExecutionStack stack, boolean ok) throws SQLException, SQLHandledException {
        fireQueryClose(stack, ok);
    }

    public void formCancel(ExecutionContext context) throws SQLException, SQLHandledException {
        int result = (Integer) context.requestUserInteraction(new ConfirmClientAction("lsFusion", ThreadLocalContext.localize("{form.do.you.really.want.to.undo.changes}")));
        if (result == JOptionPane.YES_OPTION) {
            cancel(context.stack);
        }
    }

    private boolean needConfirm() {
        return manageSession && session.isStoredDataChanged() && !isEditing;
    }

    public void formClose(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        if (!context.isPushedConfirmedClose() && needConfirm()) {
            int result = (Integer) context.requestUserInteraction(new ConfirmClientAction("lsFusion", ThreadLocalContext.localize("{form.do.you.really.want.to.close.form}")));
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        fireOnClose(context.stack);
        formHide(context);
    }

    private void formHide(ExecutionContext context) {
        ServerLoggers.remoteLifeLog("FORM HIDE : " + this);
        context.delayUserInteraction(new HideFormClientAction(Settings.get().getCloseConfirmedDelay(), Settings.get().getCloseNotConfirmedDelay()));
        // здесь не делаем close, так как нет RemoteForm + надо делать closeLater, так как могут остаться еще запросы к форме которые возможно надо обработать, так что это делается prepareRemoteChangesResponse
    }

    public void formDrop(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        fireOnDrop(context.stack);
        formHide(context);
    }

    public void formOk(ExecutionContext context) throws SQLException, SQLHandledException {
        assert context.getEnv() == this;
        
        if (checkOnOk) {
            if (!checkApply(context.stack, context)) {
                return;
            }
        }

        BL.LM.dropBeforeCanceled(this);
        fireOnBeforeOk(context.stack);
        if(BL.LM.isBeforeCanceled(this))
            return;

        if (manageSession) {
            if (!context.apply(getEventsOnOk())) {
                return;
            }
        } else
            fireOnOk(context.stack);

        fireOnAfterOk(context.stack);

        formHide(context);
    }

    public void formRefresh() throws SQLException, SQLHandledException {
        if(!Settings.get().isDisableExternalAndForceClearHints())
            session.refresh();

        refreshData();
    }

    @Override
    public String toString() {
        return "FORM["+System.identityHashCode(this) + " - " + entity.getSID()+","+getClassListener()+"]";
    }

    @Override
    public void close() throws SQLException { // в общем случае пытается закрыть, а не закрывает объект
        explicitClose();
    }

    public List<ReportPath> getCustomReportPathList() throws SQLException, SQLHandledException {
        FormReportManager newFormManager = new StaticFormReportManager(entity, MapFact.EMPTY(), null, SetFact.EMPTY()); // можно теоретически interactiveFormManager использовать, но он в RemoteForm, а переносить его сюда, не хочется создавать такую зависимость 
        return newFormManager.getCustomReportPathList(FormPrintType.PRINT);
    }

    public static List<ReportPath> saveAndGetCustomReportPathList(FormEntity formEntity, boolean recreate) throws SQLException, SQLHandledException {
        FormReportManager newFormManager = new StaticFormReportManager(formEntity, MapFact.EMPTY(), null, SetFact.EMPTY());
        return newFormManager.saveAndGetCustomReportPathList(FormPrintType.PRINT, recreate);
    }
}
