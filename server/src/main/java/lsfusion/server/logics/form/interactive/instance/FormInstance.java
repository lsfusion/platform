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
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.interop.action.*;
import lsfusion.interop.form.design.FontInfo;
import lsfusion.interop.form.event.FormEventType;
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
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.sql.lambda.SQLFunction;
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
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.ConcreteObjectClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.FormCloseType;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.changed.*;
import lsfusion.server.logics.form.interactive.controller.init.InstanceFactory;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.dialogedit.ClassFormEntity;
import lsfusion.server.logics.form.interactive.dialogedit.DialogRequest;
import lsfusion.server.logics.form.interactive.dialogedit.DialogRequestAdapter;
import lsfusion.server.logics.form.interactive.instance.design.ContainerViewInstance;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterGroupInstance;
import lsfusion.server.logics.form.interactive.instance.filter.RegularFilterInstance;
import lsfusion.server.logics.form.interactive.instance.object.*;
import lsfusion.server.logics.form.interactive.instance.order.OrderInstance;
import lsfusion.server.logics.form.interactive.instance.property.*;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.interactive.property.GroupObjectProp;
import lsfusion.server.logics.form.interactive.property.checked.PullChangeProperty;
import lsfusion.server.logics.form.stat.print.FormReportManager;
import lsfusion.server.logics.form.stat.print.StaticFormReportManager;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.filter.FilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.order.OrderEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.admin.log.LogInfo;
import lsfusion.server.physics.admin.log.LogTime;
import lsfusion.server.physics.admin.log.ServerLoggers;
import lsfusion.server.physics.admin.profiler.ProfiledObject;

import javax.swing.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

import static lsfusion.base.BaseUtils.deserializeObject;
import static lsfusion.base.BaseUtils.systemLogger;
import static lsfusion.interop.action.ServerResponse.*;
import static lsfusion.interop.form.order.user.Order.*;
import static lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance.*;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который
//  разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class FormInstance extends ExecutionEnvironment implements ReallyChanged, ProfiledObject, AutoCloseable {

    private final static Function<PropertyReaderInstance, PropertyObjectInstance<?>> GET_PROPERTY_OBJECT_FROM_READER =
            PropertyReaderInstance::getPropertyObjectInstance;

    private final Function<ContainerView, PropertyObjectInstance<?>> GET_CONTAINER_SHOWIF =
            new Function<ContainerView, PropertyObjectInstance<?>>() {
                @Override
                public PropertyObjectInstance<?> apply(ContainerView key) {
                    return instanceFactory.getInstance(key.showIf);
                }
            };


    public final LogicsInstance logicsInstance;

    public final BusinessLogics BL;

    public final FormEntity entity;

    public final InstanceFactory instanceFactory;

    public final SecurityPolicy securityPolicy;

    private final ImOrderSet<GroupObjectInstance> groups;

    private final ImSet<PullChangeProperty> pullProps;

    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    public final ImList<PropertyDrawInstance<?>> properties;


    // "закэшированная" проверка присутствия в интерфейсе, отличается от кэша тем что по сути функция от mutable объекта
    protected Set<PropertyDrawInstance> isShown = new HashSet<>();
    protected Set<PropertyDrawInstance> isStaticShown = new HashSet<>();
    protected Set<ContainerView> isContainerHidden = new HashSet<>();
    private static <T> boolean addShownHidden(Set<T> isShownHidden, T property, boolean shownHidden) {
        if(shownHidden)
            return !isShownHidden.add(property);
        return isShownHidden.remove(property);
    }

    private final boolean checkOnOk;

    private final boolean isSync;
    private final boolean isFloat;

    public boolean isSync() {
        return isSync;
    }
    public boolean isFloat() {
        return isFloat;
    }

    private final boolean manageSession;

    private final boolean showDrop;
    
    private final Locale locale;

    private boolean interactive = true; // важно для assertion'а в endApply

    private ImSet<ObjectInstance> objects;

    public boolean local = false; // временный хак для resolve'а, так как modifier очищается синхронно, а форма нет, можно было бы в транзакцию перенести, но там подмену modifier'а (resolveModifier) так не встроишь

    public FormInstance(FormEntity entity, LogicsInstance logicsInstance, DataSession session, SecurityPolicy securityPolicy,
                        FocusListener focusListener, CustomClassListener classListener,
                        ImMap<ObjectEntity, ? extends ObjectValue> mapObjects,
                        ExecutionStack stack,
                        boolean isSync, Boolean noCancel, ManageSessionType manageSession, boolean checkOnOk,
                        boolean showDrop, boolean interactive, boolean isFloat,
                        boolean isExternal, ImSet<ContextFilterInstance> contextFilters,
                        boolean showReadOnly, Locale locale) throws SQLException, SQLHandledException {
        this.isSync = isSync;
        this.isFloat = isFloat;
        this.checkOnOk = checkOnOk;
        this.showDrop = showDrop;

        this.session = session;
        this.entity = entity;
        this.logicsInstance = logicsInstance;
        this.BL = logicsInstance.getBusinessLogics();

        this.securityPolicy = securityPolicy;

        this.locale = locale;
        
        instanceFactory = new InstanceFactory();

        this.weakFocusListener = new WeakReference<>(focusListener);
        this.weakClassListener = new WeakReference<>(classListener);

        groups = entity.getGroupsList().mapOrderSetValues(instanceFactory::getInstance);
        ImOrderSet<GroupObjectInstance> groupObjects = getOrderGroups();
        for (int i = 0, size = groupObjects.size(); i < size; i++) {
            GroupObjectInstance groupObject = groupObjects.get(i);
            groupObject.order = i;
            groupObject.setClassListener(classListener);
            if(groupObject.pageSize == null)
                groupObject.pageSize = entity.hasNoProperties(groupObject.entity) ? 0 : Settings.get().getPageSizeDefaultValue();
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

        ImSet<FilterInstance> allFixedFilters = entity.getFixedFilters().mapSetValues(value -> value.getInstance(instanceFactory));
        if (contextFilters != null) {
            allFixedFilters = allFixedFilters.addExcl(contextFilters.mapSetValues(value -> value.getFilter(instanceFactory)));
            pullProps = ContextFilterInstance.getPullProps(contextFilters);
        } else 
            pullProps = SetFact.EMPTY();
            
        ImMap<GroupObjectInstance, ImSet<FilterInstance>> fixedFilters = allFixedFilters.group(new BaseUtils.Group<GroupObjectInstance, FilterInstance>() {
            public GroupObjectInstance group(FilterInstance key) {
                return key.getApplyObject();
            }
        });
        for (int i = 0, size = fixedFilters.size(); i < size; i++)
            fixedFilters.getKey(i).fixedFilters = fixedFilters.getValue(i);


        for (RegularFilterGroupEntity filterGroupEntity : entity.getRegularFilterGroupsList()) {
            regularFilterGroups.add(instanceFactory.getInstance(filterGroupEntity));
        }

        ImMap<GroupObjectInstance, ImOrderMap<OrderInstance, Boolean>> fixedOrders = entity.getFixedOrdersList().mapOrderKeys((Function<OrderEntity<?>, OrderInstance>) value -> value.getInstance(instanceFactory)).groupOrder(new BaseUtils.Group<GroupObjectInstance, OrderInstance>() {
            public GroupObjectInstance group(OrderInstance key) {
                return key.getApplyObject();
            }
        });
        for (int i = 0, size = fixedOrders.size(); i < size; i++)
            fixedOrders.getKey(i).fixedOrders = fixedOrders.getValue(i);


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
                        if (objectID != null) {
                            groupObject.addSeek(object, session.getDataObject(cacheClass, objectID), false);
                        }
                    }
                }
            }

            if(groupObject.viewType.isGrid())
                changeListViewType(groupObject, ListViewType.GRID);
        }

        for (int i = 0, size = mapObjects.size(); i < size; i++) {
            ObjectValue value = mapObjects.getValue(i);
            if(value instanceof DataObject) {
                forceChangeObject(instanceFactory.getInstance(mapObjects.getKey(i)), value);
            }
        }

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

        applyFilters();

        this.session.registerForm(this);
        
        boolean adjNoCancel, adjManageSession;
        if(interactive) {
            int prevOwners = updateSessionOwner(true, stack);

            if(manageSession == ManageSessionType.AUTO) // если нет других собственников и не readonly 
                adjManageSession = heuristicManageSession(entity, showReadOnly, prevOwners); // по идее при showreadonly редактирование все равно могут включить политикой безопасности, но при определении manageSession не будем на это обращать внимание
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
        environmentIncrement = createEnvironmentIncrement(isSync || adjManageSession, isFloat, isExternal, adjNoCancel, adjManageSession, showDrop);

        MExclMap<SessionDataProperty, Pair<GroupObjectInstance, GroupObjectProp>> mEnvironmentIncrementSources = MapFact.mExclMap();
        for (GroupObjectInstance groupObject : groupObjects) {
            ImMap<GroupObjectProp, PropertyRevImplement<ClassPropertyInterface, ObjectInstance>> props = groupObject.props;
            for(int i = 0, size = props.size(); i<size; i++)
                mEnvironmentIncrementSources.exclAdd((SessionDataProperty) props.getValue(i).property, new Pair<>(groupObject, props.getKey(i)));
        }
        environmentIncrementSources = mEnvironmentIncrementSources.immutable();
        
        if (!interactive) // deprecated ветка, в будущем должна уйти
            getChanges(stack);

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

    private boolean heuristicManageSession(FormEntity entity, boolean showReadOnly, int prevOwners) {
        return prevOwners <= 0 && !showReadOnly && !entity.hasNoChange();
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

    private static IncrementChangeProps createEnvironmentIncrement(boolean showOk, boolean isFloat, boolean isExternal, boolean isAdd, boolean manageSession, boolean showDrop) throws SQLException, SQLHandledException {
        IncrementChangeProps environment = new IncrementChangeProps();
        environment.add(FormEntity.showOk, PropertyChange.STATIC(showOk));
        environment.add(FormEntity.isFloat, PropertyChange.STATIC(isFloat));
        environment.add(FormEntity.isAdd, PropertyChange.STATIC(isAdd));
        environment.add(FormEntity.manageSession, PropertyChange.STATIC(manageSession));
        environment.add(FormEntity.isExternal, PropertyChange.STATIC(isExternal));
        environment.add(FormEntity.showDrop, PropertyChange.STATIC(showDrop));
        return environment;
    }

    public ImSet<GroupObjectInstance> getGroups() {
        return groups.getSet();
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

            ObjectValue formValue = BL.reflectionLM.formByCanonicalName.readClasses(session, new DataObject(entity.getCanonicalName(), StringClass.get(50)));
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
                        new DataObject(entity.getCanonicalName(), StringClass.get(false, false, 50)),
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
            DataObject groupObjectObject = (DataObject) BL.reflectionLM.groupObjectSIDFormNameGroupObject.readClasses(dataSession, new DataObject(preferences.groupObjectSID, StringClass.get(50)), new DataObject(entity.getCanonicalName(), StringClass.get(50)));
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

    protected FunctionSet<Property> getNoHints() {
        if (pullProps == null)
            return SetFact.EMPTY();

        return (SFunctionSet<Property>) element -> {
            for (PullChangeProperty pullProp : pullProps)
                if (pullProp.isChangeBetween(element))
                    return true;
            return false;
        };
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

    // временно
    private boolean checkFilters(final GroupObjectInstance groupTo) {
        ImSet<FilterInstance> setFilters = groupTo.getSetFilters();
        return setFilters.equals(groupTo.filters);
    }

    public <P extends PropertyInterface> DataObject addFormObject(CustomObjectInstance object, ConcreteCustomClass cls, DataObject pushed, ExecutionStack stack) throws SQLException, SQLHandledException {
        DataObject dataObject = session.addObjectAutoSet(cls, pushed, BL, getClassListener());

        // резолвим все фильтры
        assert checkFilters(object.groupTo);
        for (FilterInstance filter : object.groupTo.filters)
            filter.resolveAdd(this, object, dataObject, stack);

        expandCurrentGroupObject(object);

        // todo : теоретически надо переделывать
        // нужно менять текущий объект, иначе не будет работать ImportFromExcelActionProperty
        forceChangeObject(object, dataObject);

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

    public void executeEventAction(PropertyDrawInstance property, String eventActionSID, ImMap<ObjectInstance, DataObject> keys, ExecutionStack stack) throws SQLException, SQLHandledException {
        executeEventAction(property, eventActionSID, keys, null, null, null, false, stack);
    }
    
    @LogTime
    @ThisMessage
    public void executeEventAction(final PropertyDrawInstance<?> property, String eventActionSID, final ImMap<ObjectInstance, DataObject> keys, final ObjectValue pushChange, DataClass pushChangeType, final DataObject pushAdd, boolean pushConfirm, final ExecutionStack stack) throws SQLException, SQLHandledException {
        SQLCallable<Boolean> checkReadOnly = property.propertyReadOnly != null ? () -> property.propertyReadOnly.getRemappedPropertyObject(keys).read(FormInstance.this) != null : null;
        ActionObjectInstance<?> eventAction = property.getEventAction(eventActionSID, this, checkReadOnly, securityPolicy);
        if(eventAction == null) {
            ThreadLocalContext.delayUserInteraction(EditNotPerformedClientAction.instance);
            return;
        }

        if (eventActionSID.equals(CHANGE) || eventActionSID.equals(GROUP_CHANGE)) { //ask confirm logics...
            PropertyDrawEntity propertyDraw = property.getEntity();
            if (!pushConfirm && propertyDraw.askConfirm) {
                int result = (Integer) ThreadLocalContext.requestUserInteraction(new ConfirmClientAction("lsFusion",
                        entity.getRichDesign().get(propertyDraw).getAskConfirmMessage()));
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
        }
        final ActionObjectInstance remappedEventAction = eventAction.getRemappedPropertyObject(keys);
        BL.LM.pushRequestedValue(pushChange, pushChangeType, this, () -> remappedEventAction.execute(FormInstance.this, stack, pushAdd, property, FormInstance.this));
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
            DataClass changeType = property.entity.getWYSRequestInputType(entity, securityPolicy);
            if (changeType != null) {
                for (int i = 0, size = pasteRows.size(); i < size; i++) {
                    ImMap<ObjectInstance, DataObject> key = pasteRows.getKey(i);
                    if (columnKey != null) {
                        key = key.addExcl(columnKey);
                    }
    
                    ObjectValue value = NullValue.instance;
                    Object oValue = pasteRows.getValue(i);
                    if (oValue != null) {
                        value = session.getObjectValue(changeType, oValue);
                    }
                    executeEventAction(property, CHANGE_WYS, key, value, changeType, null, true, stack);
                }
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

    private ImMap<ObjectInstance, Expr> overrideColumnKeys(ImRevMap<ObjectInstance, KeyExpr> mapKeys, ImMap<ObjectInstance, DataObject> columnKeys) {
        // замещение с добавлением
        return MapFact.override(mapKeys, columnKeys.mapValues((Function<DataObject, Expr>) DataObject::getExpr));
    }

    public Object calculateSum(PropertyDrawInstance propertyDraw, ImMap<ObjectInstance, DataObject> columnKeys) throws SQLException, SQLHandledException {
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
                mExprMap.exclAdd(propertyKey, property.getDrawInstance().getExpr(keys, getModifier()));
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

        Map<List<Object>, List<Object>> resultMap = new OrderedMap<>();
        ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> result = query.execute(this);
        for (int j = 0, size = result.size(); j < size; j++) {
            ImMap<String, Object> one = result.getKey(j);
            ImMap<String, Object> oneValue = result.getValue(j);

            List<Object> groupList = new ArrayList<>();
            List<Object> sumList = new ArrayList<>();

            for (PropertyDrawInstance propertyDraw : toGroup.keyIt()) {
                for (int i = 1; i <= toGroup.get(propertyDraw).size(); i++) {
                    groupList.add(one.get(getSID(propertyDraw,i)));
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
        
        ObjectValue groupObjectObjectValue = BL.reflectionLM.groupObjectSIDFormNameGroupObject.readClasses(session, new DataObject(groupObjectSID, StringClass.get(50)), new DataObject(entity.getCanonicalName(), StringClass.get(50)));
        
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
            ObjectValue groupObjectObjectValue = BL.reflectionLM.groupObjectSIDFormNameGroupObject.readClasses(dataSession, new DataObject(grouping.groupObjectSID, StringClass.get(50)), new DataObject(entity.getCanonicalName(), StringClass.get(50)));
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
                        new DataObject(entity.getCanonicalName(), StringClass.get(false, false, 50)),
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

        boolean succeeded = session.apply(BL, stack, interaction, applyActions.mergeOrder(getEventsOnApply()), keepProperties, this, applyMessage);
        
        if (!succeeded)
            return false;

        environmentIncrement.add(FormEntity.isAdd, PropertyChange.STATIC(false));
        
        refreshData(); // нужно перечитать ключи в таблицах, и т.п.
        fireOnAfterApply(stack);

        dataChanged = true; // временно пока applyChanges синхронен, для того чтобы пересылался факт изменения данных

        LogMessageClientAction message = new LogMessageClientAction(ThreadLocalContext.localize("{form.instance.changes.saved}"), false);
        if(interaction!=null)
            interaction.delayUserInteraction(message);
        else
            ThreadLocalContext.delayUserInteraction(message);
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

    public void forceChangeObject(ObjectInstance object, ObjectValue value) throws SQLException, SQLHandledException {
        forceChangeObject(object, value, false);
    }

    public void forceChangeObject(ObjectInstance object, ObjectValue value, boolean last) throws SQLException, SQLHandledException {
        if (object instanceof DataObjectInstance && !(value instanceof DataObject))
            object.changeValue(session, ((DataObjectInstance) object).getBaseClass().getDefaultObjectValue());
        else
            object.changeValue(session, value);

        object.groupTo.addSeek(object, value, last);
    }

    private boolean hasEventActions() {
        ImMap<Object, ImList<ActionObjectEntity<?>>> eventActions = entity.getEventActions();
        for(ImList<ActionObjectEntity<?>> list : eventActions.valueIt())
            if(list.size() > 0)
                return true;
        return false;
    }

    // todo : временная затычка
    public void seekObject(ObjectInstance object, ObjectValue value, UpdateType updateType) throws SQLException, SQLHandledException {
        boolean last = updateType == UpdateType.LAST;
        if (hasEventActions()) { // дебилизм конечно но пока так
            forceChangeObject(object, value, last);
        } else {
            object.groupTo.addSeek(object, value, last);
        }
    }
    
    private ImList<ComponentView> userActivateTabs = ListFact.EMPTY(); 
    // программный activate tab
    public void activateTab(ComponentView view) throws SQLException, SQLHandledException {
        setTabVisible(view.getTabbedContainer(), view);
        
        userActivateTabs = userActivateTabs.addList(view);
    }

    private ImList<PropertyDrawInstance> userActivateProps = ListFact.EMPTY(); 
    // программный activate property
    public void activateProperty(PropertyDrawEntity view) {
        userActivateProps = userActivateProps.addList(instanceFactory.getInstance(view));
    }

    public void changeObject(ObjectInstance object, ObjectValue objectValue) throws SQLException, SQLHandledException {
        UpdateType updateType = object.groupTo.getUpdateType();
        if(updateType == UpdateType.NULL)
            object.groupTo.seek(updateType);
        else
            seekObject(object, objectValue, updateType);
        //        fireObjectChanged(object); // запускаем все Action'ы, которые следят за этим объектом
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

    private boolean isTabHidden(PropertyDrawInstance<?> property) {
        ComponentView drawComponent = getDrawComponent(property);
        assert !isNoTabHidden(drawComponent); // так как если бы был null не попалы бы в newIsShown в readShowIfs
        ComponentView drawTabContainer = drawComponent.getTabHiddenContainer();
        return drawTabContainer != null && isTabHidden(drawTabContainer); // первая проверка - cheat / оптимизация
    }

    private boolean isHidden(ContainerView container) { // is Tab or showIfHidden or designHidden
        if(container.main) // form container
            return false;

        if(isDesignHidden(container))
            return true;

        ComponentView localHideableContainer = container.getLocalHideableContainer();
        if(localHideableContainer == container) // if this is a tab - use it's parent, since we want it's caption to be updated too (however maybe later we'll have to distinguish caption from other attributes)
            localHideableContainer = container.getHiddenContainer().getLocalHideableContainer();

        return localHideableContainer != null && isLocalHidden(localHideableContainer);
    }

    private boolean isHidden(GroupObjectInstance group) { // is Tab or showIfHidden or designHidden
        FormEntity.ComponentUpSet containers = entity.getDrawLocalHideableContainers(group.entity);
        if (containers == null) // cheat / оптимизация, иначе пришлось бы в isHidden и еще в нескольких местах явную проверку на null
            return false;
        for (ComponentView component : containers.it())
            if (!isLocalHidden(component))
                return false;
        // для случая, когда группа PANEL, в группе только свойства, зависящие от ключей, и находятся не в первом табе.
        // наличие ключа влияет на видимость этих свойств, которая в свою очередь влияет на видимость таба.
        // неполное решение, т.к. возможны ещё более сложные случаи
        if (group.isNull() && group.entity.isPanel()) {
            return false;
        }
        return true;
    }

    private boolean isNoTabHidden(ComponentView component) { // design or showf
        return isDesignHidden(component) || isShowIfHidden(component);
    }

    private boolean isLocalHidden(ComponentView component) { // showif or tab
        assert !isDesignHidden(component);
        assert (component instanceof ContainerView && ((ContainerView)component).showIf != null) || component.getTabbedContainer() != null;
        if (isShowIfHidden(component)) 
            return true;

        ComponentView tabContainer = component.getTabHiddenContainer();
        return tabContainer != null && isTabHidden(tabContainer);
    }

    private boolean isDesignHidden(ComponentView component) { // global
        return entity.isDesignHidden(component);
    }

    private boolean isShowIfHidden(ComponentView component) { // local
        assert !isDesignHidden(component);

        if(isContainerHidden.isEmpty()) // optimization
            return false;

        ComponentView parent = component.getHiddenContainer();

        while (parent != null) {
            boolean hidden = parent instanceof ContainerView && (((ContainerView) parent).showIf != null && isContainerHidden.contains(parent));

            if (hidden) {
                return true;
            }

            parent = parent.getHiddenContainer();
        }

        return false;
    }

    private boolean isTabHidden(ComponentView component) { // sublocal
        assert !isNoTabHidden(component);
        ContainerView parent = component.getTabbedContainer();

        ComponentView visible = visibleTabs.get(parent);
        ImList<ComponentView> siblings = parent.getChildrenList();
        if (visible == null && siblings.size() > 0) // аналогичные проверки на клиентах, чтобы при init'е не вызывать
            visible = siblings.get(0);
        if (!component.equals(visible))
            return true;

        ComponentView tabContainer = parent.getTabHiddenContainer();
        return tabContainer != null && isTabHidden(tabContainer);
    }

    protected Map<ContainerView, ComponentView> visibleTabs = new HashMap<>();

    public void setTabVisible(ContainerView view, ComponentView page) throws SQLException, SQLHandledException {
        assert view.isTabbedPane();
        updateActiveTabProperty(page);
        visibleTabs.put(view, page);
    }

    private void updateActiveTabProperty(ComponentView page) throws SQLException, SQLHandledException {
        for(ComponentView tab : visibleTabs.values()) {
            tab.updateActiveTabProperty(session, null);
        }
        page.updateActiveTabProperty(session, true);
    }

    public ImOrderSet<PropertyDrawEntity> getPropertyEntitiesShownInGroup(final GroupObjectInstance group) {
        return properties.filterList(property -> {
            return isStaticShown.contains(property) && property.isProperty() && property.toDraw == group; // toDraw and not getApplyObject to get WYSIWYG
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

    void applyFilters() {
        for (GroupObjectInstance group : getGroups())
            group.filters = group.getSetFilters();
    }

    void applyOrders() {
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
    @ThisMessage
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

    private void updateData(Result<ChangedData> mChangedProps, ExecutionStack stack) throws SQLException, SQLHandledException {
        mChangedProps.set(mChangedProps.result.merge(session.updateExternal(this)));

        if (dataChanged) {
            session.executeSessionEvents(this, stack);
            
            ChangedData update = session.update(this);
            if(update.wasRestart) // очищаем кэш при рестарте
                isReallyChanged.clear();
            mChangedProps.set(mChangedProps.result.merge(update));
            dataChanged = false;
        }

    }

    @StackMessage("{message.form.end.apply}")
    @LogTime
    @ThisMessage
    @AssertSynchronized
    public FormChanges getChanges(ExecutionStack stack) throws SQLException, SQLHandledException {

        checkNavigatorDeactivated();

        final MFormChanges result = new MFormChanges();

        QueryEnvironment queryEnv = getQueryEnv();

        // если изменились данные, применяем изменения
        Result<ChangedData> mChangedProps = new Result<>(ChangedData.EMPTY);  // так как могут еще измениться свойства созданные при помощи операторов форм
        updateData(mChangedProps, stack);

        MSet<PropertyDrawInstance> mChangedDrawProps = SetFact.mSet();
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
        ImSet<PropertyDrawInstance> changedDrawProps = mChangedDrawProps.immutable().merge(forcePropertyDrawUpdates);

        updateData(mChangedProps, stack); // повторная проверка для VIEW свойств

        fillChangedDrawProps(result, changedDrawProps, mChangedProps.result);
        
        result.activateTabs.addAll(userActivateTabs);
        result.activateProps.addAll(userActivateProps);

        // сбрасываем все пометки
        userActivateTabs = ListFact.EMPTY();
        userActivateProps = ListFact.EMPTY();
        for (GroupObjectInstance group : getGroups()) {
            for (ObjectInstance object : group.objects)
                object.updated = 0;
            group.updated = 0;
        }
        forcePropertyDrawUpdates = SetFact.EMPTY();
        refresh = false;

//        result.out(this);

        return result.immutable();
    }

    private void checkNavigatorDeactivated() {
        CustomClassListener classListener = getClassListener();
        ServerLoggers.assertLog(classListener == null || !classListener.isDeactivated(), "NAVIGATOR DEACTIVATED " + BaseUtils.nullToString(classListener));
    }

    private Set<PropertyDrawInstance> readShowIfs(ChangedData changedProps, MFormChanges result) throws SQLException, SQLHandledException {

        Set<PropertyDrawInstance> newShown = new HashSet<>();

        updateContainersShowIfs(changedProps);

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

                ComponentView tabContainer = drawComponent.getTabHiddenContainer(); // у tab container'а по сравнению с containerShowIfs есть разница, так как они оптимизированы на изменение видимости без перезапроса данных
                boolean hidden = tabContainer != null && isTabHidden(tabContainer);
                boolean isDefinitelyShown = drawProperty.propertyShowIf == null;
                if (!isDefinitelyShown) {
                    ImSet<GroupObjectInstance> propRowColumnGrids = drawProperty.getColumnGroupObjectsInGrid();
                    PropertyDrawInstance.ShowIfReaderInstance showIfReader = drawProperty.showIfReader;
                    boolean read = refresh // this check is pretty equivalent to fillChangedReader
                                   || !oldStaticShown
                                   || (!hidden && pendingRead.contains(showIfReader))
                                   || propertyUpdated(drawProperty.propertyShowIf, propRowColumnGrids, changedProps, hidden);
                    if (read) {
                        mShowIfs.exclAdd(showIfReader, propRowColumnGrids);
                        if(hidden)
                            hiddenNotSureShown.exclAdd(showIfReader, tabContainer);
                    } else {
                        // nor static / nor dynamic visibility changed, reading from cache
                        boolean oldShown = isShown.contains(drawProperty);
                        if(!oldShown)
                            newShown.remove(drawProperty);
                        isDefinitelyShown = oldShown;
                    }
                }
                if(hidden && isDefinitelyShown) // помечаем component'ы которые точно показываются
                    hiddenButDefinitelyShownSet.add(tabContainer);
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
        if(!drawProperty.isInInterface(propRowColumnGrids, true)) { // don't show property if it is always null
            return false;
        }

        if (isNoTabHidden(drawComponent)) { // hidden, но без учета tab, для него отдельная оптимизация, чтобы не переобновляться при переключении "туда-назад",  связан с assertion'ом в FormInstance.isHidden
            return false;
        }

        if (userPrefsHiddenProperties.contains(drawProperty) && drawProperty.isList()) { // панель показывается всегда
            return false;
        }

        return true;
    }

    private void updateContainersShowIfs(ChangedData changedProps) throws SQLException, SQLHandledException {
        ImSet<ContainerView> changed = entity.getPropertyContainers().<SQLException, SQLHandledException>filterFnEx(
                key -> key.showIf != null && (refresh || propertyUpdated(instanceFactory.getInstance(key.showIf), SetFact.EMPTY(), changedProps, false)));

        if(changed.isEmpty()) // optimization
            return;

        MExclMap<ContainerView, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> mChangedValues = MapFact.mExclMap();
        queryPropertyObjectValues(changed, mChangedValues, SetFact.EMPTY(), GET_CONTAINER_SHOWIF);
        ImMap<ContainerView, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> changedValues = mChangedValues.immutable();

        for (int i = 0, size = changedValues.size() ; i < size; i++)
            addShownHidden(isContainerHidden, changedValues.getKey(i), changedValues.getValue(i).getValue(0).getValue() == null);
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

    private void fillChangedDrawProps(MFormChanges result, ImSet<PropertyDrawInstance> changedDrawProps, ChangedData changedProps) throws SQLException, SQLHandledException {
        Set<PropertyDrawInstance> newShown = readShowIfs(changedProps, result);

        MExclMap<PropertyReaderInstance, ImSet<GroupObjectInstance>> mReadProperties = MapFact.mExclMap();

        for (PropertyDrawInstance<?> drawProperty : properties) {
            boolean newPropIsShown = newShown.contains(drawProperty);
            boolean oldPropIsShown = addShownHidden(isShown, drawProperty, newPropIsShown);

            if (newPropIsShown) {
                boolean update = drawProperty.toDraw == null || !drawProperty.isList() || drawProperty.toDraw.toUpdate();
                boolean hidden = isTabHidden(drawProperty);

                ImSet<GroupObjectInstance> propRowGrids = drawProperty.getGroupObjectsInGrid();
                ImSet<GroupObjectInstance> propRowColumnGrids = drawProperty.getColumnGroupObjectsInGrid();

                fillChangedReader(drawProperty, drawProperty.toDraw, result, propRowGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
                fillChangedReader(drawProperty.captionReader, drawProperty.toDraw, result, propRowColumnGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
                fillChangedReader(drawProperty.footerReader, drawProperty.toDraw, result, propRowColumnGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
                fillChangedReader(drawProperty.readOnlyReader, drawProperty.toDraw, result, propRowGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
                fillChangedReader(drawProperty.backgroundReader, drawProperty.toDraw, result, propRowGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
                fillChangedReader(drawProperty.foregroundReader, drawProperty.toDraw, result, propRowGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
                for(PropertyDrawInstance<?>.LastReaderInstance aggrLastReader : drawProperty.aggrLastReaders)
                    fillChangedReader(aggrLastReader, drawProperty.toDraw, result, propRowGrids, hidden, update, oldPropIsShown, mReadProperties, changedDrawProps, changedProps);
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
        }

        for (ContainerView container : entity.getPropertyContainers()) {
            boolean hidden = isHidden(container);
            boolean update = true;

            ImSet<GroupObjectInstance> gridGroups = SetFact.EMPTY();

            ContainerViewInstance containerInstance = instanceFactory.getInstance(container);
            fillChangedReader(containerInstance.captionReader, null, result, gridGroups, hidden, update, true, mReadProperties, changedDrawProps, changedProps);
        }

        ImMap<ImSet<GroupObjectInstance>, ImSet<PropertyReaderInstance>> groupReadProps = mReadProperties.immutable().groupValues();
        for (int i = 0, size = groupReadProps.size(); i < size; i++) {
            updateDrawProps(result.properties, groupReadProps.getKey(i), groupReadProps.getValue(i));
        }
    }

    private void fillChangedReader(PropertyReaderInstance propertyReader, GroupObjectInstance toDraw, MFormChanges result, ImSet<GroupObjectInstance> columnGroupGrids, boolean hidden, boolean update, boolean wasShown, MExclMap<PropertyReaderInstance, ImSet<GroupObjectInstance>> readProperties, ImSet<PropertyDrawInstance> changedDrawProps, ChangedData changedProps) throws SQLException, SQLHandledException {
        PropertyObjectInstance<?> drawProperty = propertyReader.getPropertyObjectInstance();
        if(drawProperty == null)
            return;

        boolean needed = !hidden && update;

        boolean read = refresh || !wasShown || (toDraw != null && toDraw.toRefresh()) || propertyUpdated(propertyReader, toDraw, drawProperty, columnGroupGrids, changedDrawProps, changedProps, needed);

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

        applyFilters();
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

    public ImSet<ContextFilterInstance> getObjectFixedFilters(ClassFormEntity editForm, GroupObjectInstance selectionGroupObject) {
        MSet<ContextFilterInstance> mFixedFilters = SetFact.mSet();
        ObjectEntity object = editForm.object;
        for (FilterEntity<?> filterEntity : entity.getFixedFilters()) {
            FilterInstance filter = filterEntity.getInstance(instanceFactory);
            if (filter.getApplyObject() == selectionGroupObject) { // берем фильтры из этой группы
                for (ObjectEntity filterObject : filterEntity.getObjects()) {
                    //добавляем фильтр только, если есть хотя бы один объект который не будет заменён на константу
                    if (filterObject.baseClass == object.baseClass) {
                        mFixedFilters.add(filterEntity.getRemappedContextFilter(filterObject, object, instanceFactory));
                        break;
                    }
                }
            }
        }
        return mFixedFilters.immutable();
    }

    public Object read(PropertyObjectInstance<?> property) throws SQLException, SQLHandledException {
        return property.read(this);
    }

    public DialogRequest createObjectDialogRequest(final CustomClass objectClass, final ExecutionStack stack) {
        return new DialogRequestAdapter() {
            @Override
            public FormInstance doCreateDialog() throws SQLException, SQLHandledException {
                ClassFormEntity classForm = objectClass.getDialogForm(BL.LM);
                dialogObject = classForm.object;
                return createDialogInstance(classForm.form, dialogObject, NullValue.instance, null, stack);
            }
        };
    }

    public DialogRequest createChangeObjectDialogRequest(final CustomClass dialogClass, final ObjectValue dialogValue, final GroupObjectInstance groupObject, final ExecutionStack stack) {
        return new DialogRequestAdapter() {
            @Override
            protected FormInstance doCreateDialog() throws SQLException, SQLHandledException {
                ClassFormEntity formEntity = dialogClass.getDialogForm(BL.LM);
                ImSet<ContextFilterInstance> additionalFilters = getObjectFixedFilters(formEntity, groupObject);
                
                dialogObject = formEntity.object;

                return createDialogInstance(formEntity.form, dialogObject, dialogValue, additionalFilters, stack);
            }
        };
    }

    // вызов из обработчиков по умолчанию AggChange, DefaultChange, ChangeReadObject
    private FormInstance createDialogInstance(FormEntity entity, ObjectEntity dialogEntity, ObjectValue dialogValue, ImSet<ContextFilterInstance> additionalFilters, ExecutionStack outerStack) throws SQLException, SQLHandledException {
        return new FormInstance(entity, this.logicsInstance,
                                this.session, securityPolicy,
                                getFocusListener(), getClassListener(),
                                MapFact.singleton(dialogEntity, dialogValue),
                                outerStack,
                                true, FormEntity.DEFAULT_NOCANCEL, ManageSessionType.AUTO, false, true, true, true,
                                false, additionalFilters, false, locale);
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

    public void fireQueryClose(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireEvent(FormEventType.QUERYCLOSE, stack);
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
                if (securityPolicy.checkPropertyChangePermission(autoAction.property)) { // для проверки null'ов и политики безопасности
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

    private SessionModifier createModifier() {
        FunctionSet<Property> noHints = getNoHints();
        return new OverridePropSourceSessionModifier<SessionDataProperty>(toString(), environmentIncrement, noHints, noHints, entity.getHintsIncrementTable(), entity.getHintsNoUpdate(), session.getModifier()) {
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

            @Override
            protected void updateSource(SessionDataProperty property, boolean dataChanged, boolean forceUpdate) throws SQLException, SQLHandledException {
                if(!getSQL().isInTransaction()) { // если в транзакции предполагается что все обновится само (в форме - refresh будет)
                    Pair<GroupObjectInstance, GroupObjectProp> source = environmentIncrementSources.get(property);
                    source.first.updateEnvironmentIncrementProp(environmentIncrement, this, null, FormInstance.this, source.second, false, dataChanged);
                } else
                    ServerLoggers.exinfoLog("FAILED TO UPDATE SOURCE IN TRANSACTION " + property);
            }
        };
    }

    public Map<SessionModifier, SessionModifier> modifiers = new HashMap<>();

    @ManualLazy
    public Modifier getModifier() {
        SessionModifier sessionModifier = session.getModifier();
        SessionModifier modifier = modifiers.get(sessionModifier);
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
    public void formQueryClose(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireQueryClose(stack);
    }

    public void formCancel(ExecutionContext context) throws SQLException, SQLHandledException {
        int result = (Integer) context.requestUserInteraction(new ConfirmClientAction("lsFusion", ThreadLocalContext.localize("{form.do.you.really.want.to.undo.changes}")));
        if (result == JOptionPane.YES_OPTION) {
            cancel(context.stack);
        }
    }

    public void formClose(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        if (manageSession && session.isStoredDataChanged()) {
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
            if (!context.apply(getEventsOnOk(), SetFact.EMPTY())) {
                return;
            }
        } else
            fireOnOk(context.stack);

        fireOnAfterOk(context.stack);

        formHide(context);
    }

    public void formRefresh() throws SQLException, SQLHandledException {
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
