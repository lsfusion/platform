package lsfusion.server.form.instance;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKey;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.interop.*;
import lsfusion.interop.action.ConfirmClientAction;
import lsfusion.interop.action.EditNotPerformedClientAction;
import lsfusion.interop.action.HideFormClientAction;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.interop.form.ColorPreferences;
import lsfusion.interop.form.ColumnUserPreferences;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.GroupObjectUserPreferences;
import lsfusion.server.ServerLoggers;
import lsfusion.server.auth.ChangePropertySecurityPolicy;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.classes.*;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.form.entity.*;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.entity.filter.NotFilterEntity;
import lsfusion.server.form.entity.filter.NotNullFilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterGroupEntity;
import lsfusion.server.form.instance.PropertyDrawInstance.ShowIfReaderInstance;
import lsfusion.server.form.instance.filter.FilterInstance;
import lsfusion.server.form.instance.filter.RegularFilterGroupInstance;
import lsfusion.server.form.instance.filter.RegularFilterInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.form.view.ComponentView;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.logics.*;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.MaxChangeProperty;
import lsfusion.server.logics.property.derived.OnChangeProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.profiler.ProfiledObject;
import lsfusion.server.session.*;
import lsfusion.server.stack.ParamMessage;
import lsfusion.server.stack.StackMessage;
import lsfusion.server.stack.ThisMessage;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

import static lsfusion.base.BaseUtils.deserializeObject;
import static lsfusion.base.BaseUtils.systemLogger;
import static lsfusion.interop.ClassViewType.GRID;
import static lsfusion.interop.ClassViewType.HIDE;
import static lsfusion.interop.Order.*;
import static lsfusion.interop.form.ServerResponse.*;
import static lsfusion.server.form.instance.GroupObjectInstance.*;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который
//  разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class FormInstance<T extends BusinessLogics<T>> extends ExecutionEnvironment implements ReallyChanged, ProfiledObject {

    private final static GetKey<CalcPropertyObjectInstance<?>, PropertyReaderInstance> GET_PROPERTY_OBJECT_FROM_READER =
            new GetKey<CalcPropertyObjectInstance<?>, PropertyReaderInstance>() {
                @Override
                public CalcPropertyObjectInstance<?> getMapValue(PropertyReaderInstance key) {
                    return key.getPropertyObjectInstance();
                }
            };

    private final GetKey<CalcPropertyObjectInstance<?>, ContainerView> GET_CONTAINER_SHOWIF =
            new GetKey<CalcPropertyObjectInstance<?>, ContainerView>() {
                @Override
                public CalcPropertyObjectInstance<?> getMapValue(ContainerView key) {
                    return containerShowIfs.get(key);
                }
            };


    public final LogicsInstance logicsInstance;

    public final T BL;

    public final FormEntity<T> entity;

    public final InstanceFactory instanceFactory;

    public final SecurityPolicy securityPolicy;

    private final ImMap<ObjectEntity, ? extends ObjectValue> mapObjects;

    private final ImOrderSet<GroupObjectInstance> groups;

    private final ImSet<PullChangeProperty> pullProps;

    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    public final ImList<PropertyDrawInstance> properties;
    
    public final ImMap<ContainerView, CalcPropertyObjectInstance<?>> containerShowIfs;

    // "закэшированная" проверка присутствия в интерфейсе, отличается от кэша тем что по сути функция от mutable объекта
    protected Map<PropertyDrawInstance, Boolean> isInInterface = new HashMap<>();
    protected Map<PropertyDrawInstance, Boolean> isShown = new HashMap<>();
    protected Map<ContainerView, Boolean> isContainerShown = new HashMap<>();

    public final PropertyDrawEntity initFilterPropertyDraw;

    private final boolean checkOnOk;

    private final boolean isModal;

    public boolean isModal() {
        return isModal;
    }

    private final FormSessionScope sessionScope;

    private final boolean showDrop;
    
    private final Locale locale;

    private boolean interactive = true; // важно для assertion'а в endApply

    private ImSet<ObjectInstance> objects;

    private boolean readOnly = false;

    public FormInstance(FormEntity<T> entity, LogicsInstance logicsInstance, DataSession session, SecurityPolicy securityPolicy,
                        FocusListener<T> focusListener, CustomClassListener classListener,
                        PropertyObjectInterfaceInstance computer, DataObject connection,
                        ImMap<ObjectEntity, ? extends ObjectValue> mapObjects,
                        ExecutionStack stack, boolean isModal, boolean isAdd, FormSessionScope sessionScope, boolean checkOnOk,
                        boolean showDrop, boolean interactive,
                        ImSet<FilterEntity> contextFilters,
                        PropertyDrawEntity initFilterPropertyDraw,
                        ImSet<PullChangeProperty> pullProps, boolean readOnly, Locale locale) throws SQLException, SQLHandledException {
        this(entity, logicsInstance, session, securityPolicy, focusListener, classListener, computer, connection, mapObjects, stack, isModal, isAdd, sessionScope, checkOnOk, showDrop, interactive, false, contextFilters, initFilterPropertyDraw, pullProps, readOnly, locale);
    }

    public FormInstance(FormEntity<T> entity, LogicsInstance logicsInstance, DataSession session, SecurityPolicy securityPolicy,
                        FocusListener<T> focusListener, CustomClassListener classListener,
                        PropertyObjectInterfaceInstance computer, DataObject connection,
                        ImMap<ObjectEntity, ? extends ObjectValue> mapObjects,
                        ExecutionStack stack,
                        boolean isModal, boolean isAdd, FormSessionScope sessionScope, boolean checkOnOk,
                        boolean showDrop, boolean interactive, boolean isDialog,
                        ImSet<FilterEntity> contextFilters,
                        PropertyDrawEntity initFilterPropertyDraw,
                        ImSet<PullChangeProperty> pullProps,
                        boolean readOnly, Locale locale) throws SQLException, SQLHandledException {
        this.sessionScope = sessionScope;
        this.isModal = isModal;
        this.checkOnOk = checkOnOk;
        this.showDrop = showDrop;

        this.session = session;
        this.entity = entity;
        this.logicsInstance = logicsInstance;
        this.BL = (T) logicsInstance.getBusinessLogics();
        this.securityPolicy = securityPolicy;

        this.initFilterPropertyDraw = initFilterPropertyDraw;

        this.pullProps = pullProps;

        this.readOnly = readOnly;

        this.locale = locale;
        
        instanceFactory = new InstanceFactory(computer, connection);

        this.weakFocusListener = new WeakReference<>(focusListener);
        this.weakClassListener = new WeakReference<>(classListener);

        groups = entity.getGroupsList().mapOrderSetValues(new GetValue<GroupObjectInstance, GroupObjectEntity>() {
            public GroupObjectInstance getMapValue(GroupObjectEntity value) {
                return instanceFactory.getInstance(value);
            }
        });
        ImOrderSet<GroupObjectInstance> groupObjects = getOrderGroups();
        for (int i = 0, size = groupObjects.size(); i < size; i++) {
            GroupObjectInstance groupObject = groupObjects.get(i);
            groupObject.order = i;
            groupObject.setClassListener(classListener);
        }

        for (TreeGroupEntity treeGroup : entity.getTreeGroupsIt()) {
            instanceFactory.getInstance(treeGroup); // чтобы зарегить ссылки
        }

        ImList<PropertyDrawEntity<?>> propertyDraws = entity.getPropertyDrawsList();
        MList<PropertyDrawInstance> mProperties = ListFact.mListMax(propertyDraws.size());
        for (PropertyDrawEntity<?> propertyDrawEntity : propertyDraws)
            if (this.securityPolicy.property.view.checkPermission(propertyDrawEntity.propertyObject.property)) {
                PropertyDrawInstance propertyDrawInstance = instanceFactory.getInstance(propertyDrawEntity);
                if (propertyDrawInstance.toDraw == null) // для Instance'ов проставляем не null, так как в runtime'е порядок меняться не будет
                    propertyDrawInstance.toDraw = instanceFactory.getInstance(propertyDrawEntity.getToDraw(entity));
                mProperties.add(propertyDrawInstance);
            }
        properties = mProperties.immutableList();

        MExclMap<ContainerView, CalcPropertyObjectInstance<?>> mContainerShowIfs = MapFact.mExclMap();
        fillContainerShowIfs(mContainerShowIfs, entity.getRichDesign().mainContainer);
        containerShowIfs = mContainerShowIfs.immutable();

        ImSet<FilterEntity> allFixedFilters = entity.getFixedFilters();
        if (contextFilters != null)
            allFixedFilters = allFixedFilters.merge(contextFilters);
        ImMap<GroupObjectInstance, ImSet<FilterInstance>> fixedFilters = allFixedFilters.mapSetValues(new GetValue<FilterInstance, FilterEntity>() {
            public FilterInstance getMapValue(FilterEntity value) {
                return value.getInstance(instanceFactory);
            }
        }).group(new BaseUtils.Group<GroupObjectInstance, FilterInstance>() {
            public GroupObjectInstance group(FilterInstance key) {
                return key.getApplyObject();
            }
        });
        for (int i = 0, size = fixedFilters.size(); i < size; i++)
            fixedFilters.getKey(i).fixedFilters = fixedFilters.getValue(i);


        for (RegularFilterGroupEntity filterGroupEntity : entity.getRegularFilterGroupsList()) {
            regularFilterGroups.add(instanceFactory.getInstance(filterGroupEntity));
        }

        ImMap<GroupObjectInstance, ImOrderMap<OrderInstance, Boolean>> fixedOrders = entity.getFixedOrdersList().mapOrderKeys(new GetValue<OrderInstance, OrderEntity<?>>() {
            public OrderInstance getMapValue(OrderEntity<?> value) {
                return value.getInstance(instanceFactory);
            }
        }).groupOrder(new BaseUtils.Group<GroupObjectInstance, OrderInstance>() {
            public GroupObjectInstance group(OrderInstance key) {
                return key.getApplyObject();
            }
        });
        for (int i = 0, size = fixedOrders.size(); i < size; i++)
            fixedOrders.getKey(i).fixedOrders = fixedOrders.getValue(i);


        for (GroupObjectInstance groupObject : groupObjects) {
            UpdateType updateType = groupObject.getUpdateType();
            if (updateType == UpdateType.FIRST || updateType == UpdateType.LAST) {
                groupObject.seek(updateType == UpdateType.LAST);
            } else {
                assert updateType == UpdateType.PREV;
                for (ObjectInstance object : groupObject.objects) {
                    // ставим на объекты из cache'а
                    if (object.getBaseClass() instanceof CustomClass && classListener != null) {
                        CustomClass cacheClass = (CustomClass) object.getBaseClass();
                        Integer objectID = classListener.getObject(cacheClass);
                        if (objectID != null) {
                            groupObject.addSeek(object, session.getDataObject(cacheClass, objectID), false);
                        }
                    }
                }
            }
        }

        for (int i = 0, size = mapObjects.size(); i < size; i++) {
            forceChangeObject(instanceFactory.getInstance(mapObjects.getKey(i)), mapObjects.getValue(i));
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
                toDraw.changeOrder((CalcPropertyObjectInstance) property.propertyObject, wasOrder.contains(toDraw) ? ADD : REPLACE);
                if (!ascending) {
                    toDraw.changeOrder((CalcPropertyObjectInstance) property.propertyObject, DIR);
                }
                wasOrder.add(toDraw);
            }
        }

        applyFilters();

        this.session.registerForm(this);

        environmentIncrement = createEnvironmentIncrement(isModal, isDialog, isAdd, sessionScope.isManageSession(), entity.isReadOnly(), showDrop);

        if (!interactive) {
            endApply(stack);
            this.mapObjects = mapObjects;
        } else {
            this.mapObjects = null;
        }

        this.interactive = interactive; // обязательно в конце чтобы assertion с endApply не рушить

        fireOnInit(stack);

        ServerLoggers.remoteLifeLog("FORM OPEN : " + this);
    }

    private void fillContainerShowIfs(MExclMap<ContainerView, CalcPropertyObjectInstance<?>> mContainerShowIfs, ContainerView container) {
        if (container.showIf != null) {
            mContainerShowIfs.exclAdd(container, instanceFactory.getInstance(container.showIf));
        }
        for (ComponentView component : container.getChildrenIt()) {
            if (component instanceof ContainerView) {
                fillContainerShowIfs(mContainerShowIfs, (ContainerView) component);
            }
        }
    }

    private static IncrementChangeProps createEnvironmentIncrement(boolean isModal, boolean isDialog, boolean isAdd, boolean manageSession, boolean isReadOnly, boolean showDrop) {
        IncrementChangeProps environment = new IncrementChangeProps();
        environment.add(FormEntity.isModal, PropertyChange.<ClassPropertyInterface>STATIC(isModal));
        environment.add(FormEntity.isDialog, PropertyChange.<ClassPropertyInterface>STATIC(isDialog));
        environment.add(FormEntity.isAdd, PropertyChange.<ClassPropertyInterface>STATIC(isAdd));
        environment.add(FormEntity.manageSession, PropertyChange.<ClassPropertyInterface>STATIC(manageSession));
        environment.add(FormEntity.isReadOnly, PropertyChange.<ClassPropertyInterface>STATIC(isReadOnly));
        environment.add(FormEntity.showDrop, PropertyChange.<ClassPropertyInterface>STATIC(showDrop));
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

            ObjectValue formValue = BL.reflectionLM.navigatorElementCanonicalName.readClasses(session, new DataObject(entity.getCanonicalName(), StringClass.get(50)));
            if (formValue.isNull())
                return null;
            DataObject formObject = (DataObject) formValue;

            KeyExpr propertyDrawExpr = new KeyExpr("propertyDraw");

            Integer userId = (Integer) BL.authenticationLM.currentUser.read(session);
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

    public ColorPreferences loadColorPreferences() {       
        try {
            Color selectedRowBackground = (Color) BL.LM.overrideSelectedRowBackgroundColor.read(session);
            Color selectedRowBorder = (Color) BL.LM.overrideSelectedRowBorderColor.read(session);
            Color selectedCellBackground = (Color) BL.LM.overrideSelectedCellBackgroundColor.read(session);
            Color focusedCellBackground = (Color) BL.LM.overrideFocusedCellBackgroundColor.read(session);
            Color focusedCellBorder = (Color) BL.LM.overrideFocusedCellBorderColor.read(session);
            return new ColorPreferences(selectedRowBackground, selectedRowBorder, selectedCellBackground, 
                    focusedCellBackground, focusedCellBorder);
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
    }
    
    public void readPreferencesValues(ImMap<String, Object> values, List<GroupObjectUserPreferences> goPreferences, boolean general) {
        String prefix = general ? "general" : "user";
        String propertyDrawSID = values.get("propertySID").toString().trim();
        Integer groupObjectPropertyDraw = (Integer) values.get("groupObject");

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

    public String saveUserPreferences(ExecutionStack stack, GroupObjectUserPreferences preferences, boolean forAllUsers, boolean completeOverride) {
        if (!entity.isNamed()) {
            return null;
        }

        try (DataSession dataSession = session.createSession()) {
            List<DataObject> userObjectList = completeOverride ? readUserObjectList() : null;

            DataObject userObject = dataSession.getDataObject(BL.authenticationLM.user, BL.authenticationLM.currentUser.read(dataSession));
            for (Map.Entry<String, ColumnUserPreferences> entry : preferences.getColumnUserPreferences().entrySet()) {
                ObjectValue propertyDrawObjectValue = BL.reflectionLM.propertyDrawSIDNavigatorElementNamePropertyDraw.readClasses(
                        dataSession,
                        new DataObject(entity.getCanonicalName(), StringClass.get(false, false, 50)),
                        new DataObject(entry.getKey(), StringClass.get(false, false, 100)));
                if (propertyDrawObjectValue instanceof DataObject) {
                    DataObject propertyDrawObject = (DataObject) propertyDrawObjectValue;
                    ColumnUserPreferences columnPreferences = entry.getValue();
                    Integer idShow = columnPreferences.userHide == null ? null : BL.reflectionLM.propertyDrawShowStatus.getObjectID(columnPreferences.userHide ? "Hide" : "Show");
                    if(completeOverride) {
                        for(DataObject user : userObjectList) {
                            BL.reflectionLM.showPropertyDrawCustomUser.change(idShow, dataSession, propertyDrawObject, user);
                            BL.reflectionLM.columnCaptionPropertyDrawCustomUser.change(columnPreferences.userCaption, dataSession, propertyDrawObject, user);
                            BL.reflectionLM.columnPatternPropertyDrawCustomUser.change(columnPreferences.userPattern, dataSession, propertyDrawObject, user);
                            BL.reflectionLM.columnWidthPropertyDrawCustomUser.change(columnPreferences.userWidth, dataSession, propertyDrawObject, user);
                            BL.reflectionLM.columnOrderPropertyDrawCustomUser.change(columnPreferences.userOrder, dataSession, propertyDrawObject, user);
                            BL.reflectionLM.columnSortPropertyDrawCustomUser.change(columnPreferences.userSort, dataSession, propertyDrawObject, user);
                            BL.reflectionLM.columnAscendingSortPropertyDrawCustomUser.change(columnPreferences.userAscendingSort, dataSession, propertyDrawObject, user);
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
                        BL.reflectionLM.showPropertyDrawCustomUser.change(idShow, dataSession, propertyDrawObject, userObject);
                        BL.reflectionLM.columnCaptionPropertyDrawCustomUser.change(columnPreferences.userCaption, dataSession, propertyDrawObject, userObject);
                        BL.reflectionLM.columnPatternPropertyDrawCustomUser.change(columnPreferences.userPattern, dataSession, propertyDrawObject, userObject);
                        BL.reflectionLM.columnWidthPropertyDrawCustomUser.change(columnPreferences.userWidth, dataSession, propertyDrawObject, userObject);
                        BL.reflectionLM.columnOrderPropertyDrawCustomUser.change(columnPreferences.userOrder, dataSession, propertyDrawObject, userObject);
                        BL.reflectionLM.columnSortPropertyDrawCustomUser.change(columnPreferences.userSort, dataSession, propertyDrawObject, userObject);
                        BL.reflectionLM.columnAscendingSortPropertyDrawCustomUser.change(columnPreferences.userAscendingSort, dataSession, propertyDrawObject, userObject);
                    }
                } else {
                    throw new RuntimeException("Объект " + entry.getKey() + " (" + entity.getCanonicalName() + ") не найден");
                }
            }
            DataObject groupObjectObject = (DataObject) BL.reflectionLM.groupObjectSIDNavigatorElementNameGroupObject.readClasses(dataSession, new DataObject(preferences.groupObjectSID, StringClass.get(50)), new DataObject(entity.getCanonicalName(), StringClass.get(50)));
            if(completeOverride) {
                for(DataObject user : userObjectList) {
                    BL.reflectionLM.hasUserPreferencesGroupObjectCustomUser.change(preferences.hasUserPreferences ? true : null, dataSession, groupObjectObject, user);
                    BL.reflectionLM.fontSizeGroupObjectCustomUser.change(preferences.fontInfo.fontSize != -1 ? preferences.fontInfo.fontSize : null, dataSession, groupObjectObject, user);
                    BL.reflectionLM.pageSizeGroupObjectCustomUser.change(preferences.pageSize, dataSession, groupObjectObject, user);
                    BL.reflectionLM.headerHeightGroupObjectCustomUser.change(preferences.headerHeight, dataSession, groupObjectObject, user);
                    BL.reflectionLM.isFontBoldGroupObjectCustomUser.change(preferences.fontInfo.isBold() ? true : null, dataSession, groupObjectObject, user);
                    BL.reflectionLM.isFontItalicGroupObjectCustomUser.change(preferences.fontInfo.isItalic() ? true : null, dataSession, groupObjectObject, user);
                }
            }
            if (forAllUsers) {
                BL.reflectionLM.hasUserPreferencesGroupObject.change(preferences.hasUserPreferences ? true : null, dataSession, groupObjectObject);
                BL.reflectionLM.fontSizeGroupObject.change(preferences.fontInfo.fontSize != -1 ? preferences.fontInfo.fontSize : null, dataSession, groupObjectObject);
                BL.reflectionLM.pageSizeGroupObject.change(preferences.pageSize, dataSession, groupObjectObject);
                BL.reflectionLM.headerHeightGroupObject.change(preferences.headerHeight, dataSession, groupObjectObject);
                BL.reflectionLM.isFontBoldGroupObject.change(preferences.fontInfo.isBold() ? true : null, dataSession, groupObjectObject);
                BL.reflectionLM.isFontItalicGroupObject.change(preferences.fontInfo.isItalic() ? true : null, dataSession, groupObjectObject);
            } else if (!completeOverride){
                BL.reflectionLM.hasUserPreferencesGroupObjectCustomUser.change(preferences.hasUserPreferences ? true : null, dataSession, groupObjectObject, userObject);
                BL.reflectionLM.fontSizeGroupObjectCustomUser.change(preferences.fontInfo.fontSize != -1 ? preferences.fontInfo.fontSize : null, dataSession, groupObjectObject, userObject);
                BL.reflectionLM.pageSizeGroupObjectCustomUser.change(preferences.pageSize, dataSession, groupObjectObject, userObject);
                BL.reflectionLM.headerHeightGroupObjectCustomUser.change(preferences.headerHeight, dataSession, groupObjectObject, userObject);
                BL.reflectionLM.isFontBoldGroupObjectCustomUser.change(preferences.fontInfo.isBold() ? true : null, dataSession, groupObjectObject, userObject);
                BL.reflectionLM.isFontItalicGroupObjectCustomUser.change(preferences.fontInfo.isItalic() ? true : null, dataSession, groupObjectObject, userObject);
            }
            return dataSession.applyMessage(BL, stack);
        } catch (SQLException | SQLHandledException e) {
            throw Throwables.propagate(e);
        }
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

    public boolean areObjectsFound() {
        assert !interactive;
        for (int i = 0, size = mapObjects.size(); i < size; i++)
            if (!instanceFactory.getInstance(mapObjects.getKey(i)).getObjectValue().equals(mapObjects.getValue(i)))
                return false;
        return true;
    }

    protected FunctionSet<CalcProperty> getNoHints() {
        FunctionSet<CalcProperty> result = entity.getNoHints();
        if (pullProps == null)
            return result;

        return BaseUtils.merge(result, new FunctionSet<CalcProperty>() {
            public boolean contains(CalcProperty element) {
                for (PullChangeProperty pullProp : pullProps)
                    if (pullProp.isChangeBetween(element))
                        return true;
                return false;
            }

            public boolean isEmpty() {
                return false;
            }

            public boolean isFull() {
                return false;
            }
        });
    }

    public CustomClass getCustomClass(int classID) {
        return BL.LM.baseClass.findClassID(classID);
    }

    public final DataSession session;

    private final WeakReference<FocusListener<T>> weakFocusListener;

    public FocusListener<T> getFocusListener() {
        return weakFocusListener.get();
    }
    
    public LogInfo getLogInfo() {
        FocusListener<T> focusListener = getFocusListener();
        if(focusListener != null)
            return focusListener.getLogInfo();

        return LogInfo.system; 
    }

    private final WeakReference<CustomClassListener> weakClassListener;

    public CustomClassListener getClassListener() {
        return weakClassListener.get();
    }

    public QueryEnvironment getQueryEnv() {
        return session.env;
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

    public PropertyDrawInstance getPropertyDraw(Property<?> property, ObjectInstance object) {
        for (PropertyDrawInstance propertyDraw : properties)
            if (property.equals(propertyDraw.propertyObject.property) && propertyDraw.propertyObject.mapping.containsValue(object))
                return propertyDraw;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(Property<?> property, GroupObjectInstance group) {
        for (PropertyDrawInstance propertyDraw : properties)
            if (property.equals(propertyDraw.propertyObject.property) && (group == null || group.equals(propertyDraw.toDraw)))
                return propertyDraw;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(Property<?> property) {
        return getPropertyDraw(property, (GroupObjectInstance) null);
    }

    public PropertyDrawInstance getPropertyDraw(LP property) {
        return getPropertyDraw(property.property);
    }

    public PropertyDrawInstance getPropertyDraw(LP property, ObjectInstance object) {
        return getPropertyDraw(property.property, object);
    }

    public PropertyDrawInstance getPropertyDraw(LP property, GroupObjectInstance group) {
        return getPropertyDraw(property.property, group);
    }

    // ----------------------------------- Навигация ----------------------------------------- //

    public void changeGroupObject(GroupObjectInstance group, Scroll changeType) throws SQLException {
        switch (changeType) {
            case HOME:
                group.seek(false);
                break;
            case END:
                group.seek(true);
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

    public void expandGroupObject(GroupObjectInstance group, ImMap<ObjectInstance, DataObject> value) throws SQLException, SQLHandledException {
        if (group.expandTable == null)
            group.expandTable = group.createKeyTable();
        group.expandTable.modifyRecord(session.sql, value, Modify.MODIFY, session.getOwner());
        group.updated |= UPDATED_EXPANDS;
    }

    public void collapseGroupObject(GroupObjectInstance group, ImMap<ObjectInstance, DataObject> value) throws SQLException, SQLHandledException {
        if (group.expandTable != null) {
            group.expandTable.modifyRecord(session.sql, value, Modify.DELETE, session.getOwner());
            group.updated |= UPDATED_EXPANDS;
        }
    }

    public void expandCurrentGroupObject(ValueClass cls) throws SQLException, SQLHandledException {
        for (ObjectInstance object : getObjects()) {
            if (object.getBaseClass().isCompatibleParent(cls))
                expandCurrentGroupObject(object);
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
                        expandGroupObject(group, value);
                    } else {
                        expandGroupObject(group.getUpTreeGroup(), value);
                    }
                }
                if (group.equals(groupObject)) {
                    break;
                }
            }
        }
    }

    public void changeClassView(GroupObjectInstance group, ClassViewType newClassView) {
        if (group.entity.isAllowedClassView(newClassView)) {
            group.curClassView = newClassView;
            group.updated = group.updated | UPDATED_CLASSVIEW;
        }
    }

    // сстандартные фильтры
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
        return setFilters.filterFn(new SFunctionSet<FilterInstance>() {
            public boolean contains(FilterInstance filter) {
                return FilterInstance.ignoreInInterface || filter.isInInterface(groupTo);
            }
        }).equals(groupTo.filters);
    }

    public <P extends PropertyInterface> DataObject addFormObject(CustomObjectInstance object, ConcreteCustomClass cls, DataObject pushed, ExecutionStack stack) throws SQLException, SQLHandledException {
        DataObject dataObject = session.addObject(cls, pushed);

        // резолвим все фильтры
        assert checkFilters(object.groupTo);
        for (FilterInstance filter : object.groupTo.filters)
            filter.resolveAdd(this, object, dataObject, stack);

        for (LP lp : BL.getNamedProperties()) {
            if (lp instanceof LCP) {
                LCP<P> lcp = (LCP<P>) lp;
                CalcProperty<P> property = lcp.property;
                if (property.autoset) {
                    ValueClass interfaceClass = property.getInterfaceClasses(ClassType.autoSetPolicy).singleValue();
                    ValueClass valueClass = property.getValueClass(ClassType.autoSetPolicy);
                    if (valueClass instanceof CustomClass && interfaceClass instanceof CustomClass &&
                            cls.isChild((CustomClass) interfaceClass)) { // в общем то для оптимизации
                        Integer obj = getClassListener().getObject((CustomClass) valueClass);
                        if (obj != null)
                            property.change(MapFact.singleton(property.interfaces.single(), dataObject), this, obj);
                    }
                }
            }
        }

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

            if (securityPolicy.cls.edit.change.checkPermission(object.currentClass)) {
                object.changeClass(session, dataObject, cls);
                dataChanged = true;
            }
        } else
            session.changeClass(objectInstance, dataObject, cls);
    }

    public void executeEditAction(PropertyDrawInstance property, String editActionSID, ImMap<ObjectInstance, DataObject> keys, ExecutionStack stack) throws SQLException, SQLHandledException {
        executeEditAction(property, editActionSID, keys, null, null, false, stack);
    }
    
    @LogTime
    @ThisMessage
    public void executeEditAction(PropertyDrawInstance property, String editActionSID, ImMap<ObjectInstance, DataObject> keys, ObjectValue pushChange, DataObject pushAdd, boolean pushConfirm, ExecutionStack stack) throws SQLException, SQLHandledException {
        ActionPropertyObjectInstance editAction = property.getEditAction(editActionSID, instanceFactory, entity);

        if (property.propertyReadOnly != null && property.propertyReadOnly.getRemappedPropertyObject(keys).read(this) != null && editAction != null && ((ActionProperty) editAction.property).checkReadOnly) {
            ThreadLocalContext.delayUserInteraction(EditNotPerformedClientAction.instance);
            return;
        }

        if (!checkEditActionPermission(editAction, editActionSID, property)) {
            ThreadLocalContext.delayUserInteraction(EditNotPerformedClientAction.instance);
            return;
        }

        if (readOnly && !checkReadOnlyPermission(editAction, property)) {
            ThreadLocalContext.delayUserInteraction(EditNotPerformedClientAction.instance);
            return;
        }

        if (editActionSID.equals(CHANGE) || editActionSID.equals(GROUP_CHANGE)) { //ask confirm logics...
            PropertyDrawEntity propertyDraw = property.getEntity();
            if (!pushConfirm && propertyDraw.askConfirm) {
                int result = (Integer) ThreadLocalContext.requestUserInteraction(new ConfirmClientAction("lsFusion",
                        entity.getRichDesign().get(propertyDraw).getAskConfirmMessage()));
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
        }
        editAction.getRemappedPropertyObject(keys).execute(this, stack, pushChange, pushAdd, property);
    }
    
    private boolean checkEditActionPermission(ActionPropertyObjectInstance editAction, String editActionSID, PropertyDrawInstance property) {
        ChangePropertySecurityPolicy securityPolicy = this.securityPolicy.property.change;
        if (editAction != null) {
            if (property.isSelector()) {
                return true;
            } else if (securityPolicy.checkPermission(editAction.property)) {
                if (property.hasContextMenuBinding(editActionSID) || securityPolicy.checkPermission(property.propertyObject.property)) {
                    return true;
                }
            }
        }
        return false;  
    }

    private boolean checkReadOnlyPermission(ActionPropertyObjectInstance editAction, PropertyDrawInstance property) {
         ChangePropertySecurityPolicy securityPolicy = logicsInstance.getSecurityManager().readOnlyPolicy.property.change;
        return editAction != null && securityPolicy.checkPermission(editAction.property) && securityPolicy.checkPermission(property.propertyObject.property);
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

    public void pasteMulticellValue(Map<PropertyDrawInstance, ImOrderMap<ImMap<ObjectInstance, DataObject>, Object>> cellsValues, ExecutionStack stack) throws SQLException, IOException, SQLHandledException {
        for (Entry<PropertyDrawInstance, ImOrderMap<ImMap<ObjectInstance, DataObject>, Object>> e : cellsValues.entrySet()) { // бежим по ячейкам
            PropertyDrawInstance property = e.getKey();
            executePasteAction(property, null, e.getValue(), stack);
        }
    }

    private void executePasteAction(PropertyDrawInstance<?> property, ImMap<ObjectInstance, DataObject> columnKey, ImOrderMap<ImMap<ObjectInstance, DataObject>, Object> pasteRows, ExecutionStack stack) throws SQLException, SQLHandledException {
        if (!pasteRows.isEmpty()) {
            for (int i = 0, size = pasteRows.size(); i < size; i++) {
                ImMap<ObjectInstance, DataObject> key = pasteRows.getKey(i);
                if (columnKey != null) {
                    key = key.addExcl(columnKey);
                }

                Object oValue = pasteRows.getValue(i);
                ObjectValue value = NullValue.instance;
                if (oValue != null) {
                    DataClass changeType = property.entity.getWYSRequestInputType(entity);
                    if (changeType != null) {
                        value = session.getObjectValue(changeType, oValue);
                    }
                }
                executeEditAction(property, CHANGE_WYS, key, value, null, true, stack);
            }
        }
    }

    public int countRecords(int groupObjectID) throws SQLException, SQLHandledException {
        GroupObjectInstance group = getGroupObjectInstance(groupObjectID);
        Expr expr = GroupExpr.create(MapFact.<Object, Expr>EMPTY(), new ValueExpr(1, IntegerClass.instance), group.getWhere(group.getMapKeys(), getModifier()), GroupType.SUM, MapFact.<Object, Expr>EMPTY());
        QueryBuilder<Object, Object> query = new QueryBuilder<>(MapFact.<Object, KeyExpr>EMPTYREV());
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
        return MapFact.override(mapKeys, columnKeys.mapValues(new GetValue<Expr, DataObject>() { // замещение с добавлением
            public Expr getMapValue(DataObject value) {
                return value.getExpr();
            }
        }));
    }

    public Object calculateSum(PropertyDrawInstance propertyDraw, ImMap<ObjectInstance, DataObject> columnKeys) throws SQLException, SQLHandledException {
        GroupObjectInstance groupObject = propertyDraw.toDraw;

        ImRevMap<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();

        ImMap<ObjectInstance, Expr> keys = overrideColumnKeys(mapKeys, columnKeys);

        Expr expr = GroupExpr.create(MapFact.<Object, Expr>EMPTY(), propertyDraw.getDrawInstance().getExpr(keys, getModifier()), groupObject.getWhere(mapKeys, getModifier()), GroupType.SUM, MapFact.<Object, Expr>EMPTY());

        QueryBuilder<Object, String> query = new QueryBuilder<>(MapFact.<Object, KeyExpr>EMPTYREV());
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
        Expr exprQuant = GroupExpr.create(exprMap, new ValueExpr(1, IntegerClass.instance), groupObject.getWhere(mapKeys, getModifier()), GroupType.SUM, keyExprMap);
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
                groupType = GroupType.MAX;

                property = toMax.getKey(i - separator);
                currentList = toMax.getValue(i - separator);

                if (property.getType() instanceof FileClass) {
                    groupType = GroupType.ANY;
                }
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
        
        ObjectValue groupObjectObjectValue = BL.reflectionLM.groupObjectSIDNavigatorElementNameGroupObject.readClasses(session, new DataObject(groupObjectSID, StringClass.get(50)), new DataObject(entity.getCanonicalName(), StringClass.get(50)));
        
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
                    grouping = new FormGrouping((String) values.get("groupingSID"), groupObjectSID, (Boolean) values.get("itemQuantity"), new ArrayList<FormGrouping.PropertyGrouping>());
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
            ObjectValue groupObjectObjectValue = BL.reflectionLM.groupObjectSIDNavigatorElementNameGroupObject.readClasses(dataSession, new DataObject(grouping.groupObjectSID, StringClass.get(50)), new DataObject(entity.getCanonicalName(), StringClass.get(50)));
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
                    dataSession.apply(BL, stack);
                    return;
                }
            } else {
                assert grouping.propertyGroupings != null;
                groupingObject = dataSession.addObject((ConcreteCustomClass) BL.reflectionLM.findClass("FormGrouping"));
                BL.reflectionLM.groupObjectFormGrouping.change(groupObjectObject.getValue(), dataSession, groupingObject);
                BL.reflectionLM.nameFormGrouping.change(grouping.name, dataSession, groupingObject);
            }
            assert grouping.propertyGroupings != null;
            BL.reflectionLM.itemQuantityFormGrouping.change(grouping.showItemQuantity, dataSession, groupingObject);

            for (FormGrouping.PropertyGrouping propGrouping : grouping.propertyGroupings) {
                ObjectValue propertyDrawObjectValue = BL.reflectionLM.propertyDrawSIDNavigatorElementNamePropertyDraw.readClasses(dataSession,
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
            dataSession.apply(BL, stack);
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

    public boolean apply(BusinessLogics BL, ExecutionStack stack, UserInteraction interaction, ImOrderSet<ActionPropertyValueImplement> applyActions, FunctionSet<SessionDataProperty> keepProperties, FormInstance formInstance) throws SQLException, SQLHandledException {
        assert formInstance == null || this == formInstance;

        fireOnBeforeApply(stack);

        boolean succeeded = session.apply(BL, this, stack, interaction, applyActions, keepProperties);

        if (!succeeded)
            return false;

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
        FocusListener<T> focusListener = getFocusListener();
        if (focusListener != null)
            focusListener.gainedFocus(this);
        session.form.changeCurrentForm(BL.getDbManager().getFormObject(entity.getCanonicalName(), stack));
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
    
    public void forceChangeObject(ValueClass cls, ObjectValue value) throws SQLException, SQLHandledException {

        for (ObjectInstance object : getObjects()) {
            if (object.getBaseClass().isCompatibleParent(cls))
                forceChangeObject(object, value);
        }
    }

    private boolean hasEventActions() {
        ImMap<Object, ImList<ActionPropertyObjectEntity<?>>> eventActions = entity.getEventActions();
        for(ImList<ActionPropertyObjectEntity<?>> list : eventActions.valueIt())
            if(list.size() > 0)
                return true;
        return false;
    }
    
    public void seekObject(ObjectInstance object, ObjectValue value) throws SQLException, SQLHandledException {
        seekObject(object, value, false);
    }
    
    // todo : временная затычка
    public void seekObject(ObjectInstance object, ObjectValue value, boolean last) throws SQLException, SQLHandledException {

        if (hasEventActions()) { // дебилизм конечно но пока так
            forceChangeObject(object, value, last);
        } else {
            object.groupTo.addSeek(object, value, last);
        }
    }

    public void changeObject(ObjectInstance object, ObjectValue objectValue) throws SQLException, SQLHandledException {
        seekObject(object, objectValue);
//        fireObjectChanged(object); // запускаем все Action'ы, которые следят за этим объектом
    }

    // кэш на изменение
    protected Set<CalcPropertyObjectInstance> isReallyChanged = new HashSet<>();
    public boolean containsChange(CalcPropertyObjectInstance instance) {
        return isReallyChanged.contains(instance);
    }
    public void addChange(CalcPropertyObjectInstance instance) {
        isReallyChanged.add(instance);
    }

    // проверки видимости (для оптимизации pageframe'ов)
    protected Set<PropertyReaderInstance> pendingHidden = SetFact.mAddRemoveSet();

    private ComponentView getDrawComponent(PropertyDrawInstance<?> property, boolean grid) {
        return entity.getDrawComponent(property.entity, grid);
    }

    private boolean isTabHidden(PropertyDrawInstance<?> property, boolean grid) {
        ComponentView drawComponent = getDrawComponent(property, grid);
        assert !isNoTabHidden(drawComponent); // так как если бы был null не попалы бы в newIsShown в readShowIfs
        ComponentView drawTabContainer = drawComponent.getTabContainer();
        return drawTabContainer != null && isTabHidden(drawTabContainer); // первая проверка - cheat / оптимизация
    }

    private boolean isHidden(GroupObjectInstance group) {
        FormEntity.ComponentUpSet containers = entity.getDrawLocalHideableContainers(group.entity);
        if (containers == null) // cheat / оптимизация, иначе пришлось бы в isHidden и еще в нескольких местах явную проверку на null
            return false;
        for (ComponentView component : containers.it())
            if (!isLocalHidden(component))
                return false;
        // для случая, когда группа FORCE PANEL, в группе только свойства, зависящие от ключей, и находятся не в первом табе.
        // наличие ключа влияет на видимость этих свойств, которая в свою очередь влияет на видимость таба.
        // неполное решение, т.к. возможны ещё более сложные случаи
        if (group.isNull() && group.entity.isForcedPanel()) {
            return false;
        }
        return true;
    }

    private boolean isNoTabHidden(ComponentView component) { // design or showf
        return isDesignHidden(component) || isShowIfHidden(component);
    }

    private boolean isLocalHidden(ComponentView component) { // showif or tab
        assert !isDesignHidden(component);
        assert (component instanceof ContainerView && ((ContainerView)component).showIf != null) || component.getContainer().isTabbedPane();
        return isShowIfHidden(component) || isTabHidden(component);
    }

    private boolean isDesignHidden(ComponentView component) { // global
        return entity.isDesignHidden(component);
    }

    private boolean isShowIfHidden(ComponentView component) { // local
        assert !isDesignHidden(component);

        if(containerShowIfs.isEmpty()) // оптимизация
            return false;

        ContainerView parent = component.getContainer();

        while (parent != null) {
            Boolean shown = isContainerShown.get(parent);

            if (shown != null && !shown) {
                return true;
            }

            parent = parent.getContainer();
        }

        return false;
    }

    private boolean isTabHidden(ComponentView component) { // sublocal
        assert !isNoTabHidden(component);
        ContainerView parent = component.getContainer();
        assert parent.isTabbedPane(); // tabbed

        ComponentView visible = visibleTabs.get(parent);
        ImList<ComponentView> siblings = parent.getChildrenList();
        if (visible == null && siblings.size() > 0) // аналогичные проверки на клиентах, чтобы при init'е не вызывать
            visible = siblings.get(0);
        if (!component.equals(visible))
            return true;

        ComponentView tabContainer = parent.getTabContainer();
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

    boolean refresh = true;

    private boolean classUpdated(Updated updated, GroupObjectInstance groupObject) {
        return updated.classUpdated(SetFact.singleton(groupObject));
    }

    private boolean objectUpdated(Updated updated, GroupObjectInstance groupObject) {
        return updated.objectUpdated(SetFact.singleton(groupObject));
    }

    private boolean objectUpdated(Updated updated, ImSet<GroupObjectInstance> groupObjects) {
        return updated.objectUpdated(groupObjects);
    }

    private boolean propertyUpdated(CalcPropertyObjectInstance updated, ImSet<GroupObjectInstance> groupObjects, ChangedData changedProps, boolean hidden) throws SQLException, SQLHandledException {
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

    private void updateDrawProps(
            MExclMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties,
            ImSet<GroupObjectInstance> keyGroupObjects,
            ImOrderSet<PropertyReaderInstance> propertySet) throws SQLException, SQLHandledException {
        queryPropertyObjectValues(propertySet, properties, keyGroupObjects, GET_PROPERTY_OBJECT_FROM_READER);
    }

    @StackMessage("message.form.update.props")
    @ThisMessage
    private <T> void queryPropertyObjectValues(
            @ParamMessage ImOrderSet<T> keysSet,
            MExclMap<T, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> valuesMap,
            ImSet<GroupObjectInstance> keyGroupObjects,
            GetKey<CalcPropertyObjectInstance<?>, T> getPropertyObject
    ) throws SQLException, SQLHandledException {
        
        QueryBuilder<ObjectInstance, T> selectProps = new QueryBuilder<>(GroupObjectInstance.getObjects(getUpTreeGroups(keyGroupObjects)));
        for (GroupObjectInstance keyGroup : keyGroupObjects) {
            selectProps.and(keyGroup.keyTable.getWhere(selectProps.getMapExprs()));
        }

        for (T key : keysSet) {
            selectProps.addProperty(
                    key,
                    getPropertyObject.getMapValue(key).getExpr(selectProps.getMapExprs(), getModifier(), this)
            );
        }

        ImMap<ImMap<ObjectInstance, DataObject>, ImMap<T, ObjectValue>> queryResult = selectProps.executeClasses(this, BL.LM.baseClass).getMap();
        for (final T key : keysSet) {
            valuesMap.exclAdd(key,
                    queryResult.mapValues(new GetValue<ObjectValue, ImMap<T, ObjectValue>>() {
                        public ObjectValue getMapValue(ImMap<T, ObjectValue> value) {
                            return value.get(key);
                        }
                    }));
        }
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

    @StackMessage("message.form.end.apply")
    @LogTime
    @ThisMessage
    @AssertSynchronized
    public FormChanges endApply(ExecutionStack stack) throws SQLException, SQLHandledException {

        assert interactive;
        checkNavigatorDeactivated();

        final MFormChanges result = new MFormChanges();

        QueryEnvironment queryEnv = getQueryEnv();

        // если изменились данные, применяем изменения
        Result<ChangedData> mChangedProps = new Result<>(ChangedData.EMPTY);  // так как могут еще измениться свойства созданные при помощи операторов форм
        updateData(mChangedProps, stack);

        GroupObjectValue updateGroupObject = null; // так как текущий groupObject идет относительно treeGroup, а не group
        for (GroupObjectInstance group : getOrderGroups()) {
            try {
                ImMap<ObjectInstance, DataObject> selectObjects = group.updateKeys(session.sql, queryEnv, getModifier(), environmentIncrement, this, BL.LM.baseClass, isHidden(group), refresh, result, mChangedProps, this);
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

        updateData(mChangedProps, stack); // повторная проверка для VIEW свойств

        fillChangedDrawProps(result, mChangedProps.result);

        // сбрасываем все пометки
        for (GroupObjectInstance group : getGroups()) {
            for (ObjectInstance object : group.objects)
                object.updated = 0;
            group.updated = 0;
        }
        refresh = false;

//        result.out(this);

        return result.immutable();
    }

    private void checkNavigatorDeactivated() {
        CustomClassListener classListener = getClassListener();
        ServerLoggers.assertLog(classListener == null || !classListener.isDeactivated(), "NAVIGATOR DEACTIVATED " + BaseUtils.nullToString(classListener));
    }

    private ImMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> readShowIfs(
            Map<PropertyDrawInstance, Boolean> newIsShown,
            Map<PropertyDrawInstance, ImSet<GroupObjectInstance>> rowGrids,
            Map<PropertyDrawInstance, ImSet<GroupObjectInstance>> rowColumnGrids,
            ChangedData changedProps) throws SQLException, SQLHandledException {

        updateContainersShowIfs(changedProps);

        MAddSet<ComponentView> hiddenButDefinitelyShownSet = SetFact.mAddSet(); // не ComponentDownSet для оптимизации
        MAddExclMap<PropertyReaderInstance, ComponentView> hiddenNotSureShown = MapFact.mAddExclMap();
        final MOrderExclMap<PropertyReaderInstance, ImSet<GroupObjectInstance>> mShowIfs = MapFact.mOrderExclMap();
        for (PropertyDrawInstance drawProperty : properties) {
            ClassViewType curClassView = drawProperty.getCurClassView();
            if (curClassView == HIDE) continue;

            ClassViewType forceViewType = drawProperty.getForceViewType();
            if (forceViewType != null && forceViewType == HIDE) continue;

            ImSet<GroupObjectInstance> propRowColumnGrids = drawProperty.getColumnGroupObjectsInGridView();
            ImSet<GroupObjectInstance> propRowGrids;
            Boolean newInInterface = null;
            if (curClassView == GRID && (forceViewType == null || forceViewType == GRID) &&
                    drawProperty.propertyObject.isInInterface(propRowGrids = propRowColumnGrids.addExcl(drawProperty.toDraw), forceViewType != null)) {
                // в grid'е
                newInInterface = true;
            } else if (drawProperty.propertyObject.isInInterface(propRowGrids = propRowColumnGrids, false)) {
                // в панели
                newInInterface = false;
            }

            ComponentView drawComponent = null;

            if(newInInterface != null) {
                drawComponent = getDrawComponent(drawProperty, newInInterface);
                if (isNoTabHidden(drawComponent)) { // hidden, но без учета tab, для него отдельная оптимизация, чтобы не переобновляться при переключении "туда-назад",  связан с assertion'ом в FormInstance.isHidden
                    newInInterface = null;
                }
            }

            rowGrids.put(drawProperty, propRowGrids);
            rowColumnGrids.put(drawProperty, propRowColumnGrids);
            newIsShown.put(drawProperty, newInInterface);

            Boolean oldInInterface = isInInterface.put(drawProperty, newInInterface);
            if (newInInterface != null) { // если показывается
                ComponentView tabContainer = drawComponent.getTabContainer(); // у tab container'а по сравнению с containerShowIfs есть разница, так как они оптимизированы на изменение видимости без перезапроса данных
                boolean hidden = tabContainer != null && isTabHidden(tabContainer);
                boolean isDefinitelyShown = drawProperty.propertyShowIf == null;
                if (!isDefinitelyShown) {
                    ShowIfReaderInstance showIfReader = drawProperty.showIfReader;
                    boolean read = refresh
                                   || !newInInterface.equals(oldInInterface) // если изменилось представление
                                   || (!hidden && pendingHidden.contains(showIfReader)) // если стал видим, но не читался
                                   || groupUpdated(drawProperty.getColumnGroupObjects(), UPDATED_CLASSVIEW) // изменились группы в колонки (так как отбираются только GRID)
                                   || propertyUpdated(drawProperty.propertyShowIf, propRowColumnGrids, changedProps, hidden); //изменился propertyShowIf
                    if (read) {
                        mShowIfs.exclAdd(showIfReader, propRowColumnGrids);
                        if(hidden)
                            hiddenNotSureShown.exclAdd(showIfReader, tabContainer);
                    } else {
                        //т.е. не поменялся ни inInterface, ни showIf, достаем из кэша
                        Boolean isPropShown = isShown.get(drawProperty);
                        newIsShown.put(drawProperty, isPropShown);
                        isDefinitelyShown = isPropShown != null;
                    }
                }
                if(hidden && isDefinitelyShown) // помечаем component'ы которые точно показываются
                    hiddenButDefinitelyShownSet.add(tabContainer);
            }
        }
        ImOrderMap<PropertyReaderInstance, ImSet<GroupObjectInstance>> showIfs = mShowIfs.immutableOrder();

        if(hiddenNotSureShown.size() > 0) { // оптимизация
            FormEntity.ComponentDownSet hiddenButDefinitelyShown = FormEntity.ComponentDownSet.create(hiddenButDefinitelyShownSet);

            MOrderFilterMap<PropertyReaderInstance, ImSet<GroupObjectInstance>> mRestShowIfs = MapFact.mOrderFilter(showIfs);
            for (int i = 0, size = showIfs.size(); i < size; i++) {
                PropertyReaderInstance property = showIfs.getKey(i);
                ComponentView component = hiddenNotSureShown.get(property);
                if (component != null && hiddenButDefinitelyShown.containsAll(component)) // те которые попали в hiddenButDefinitelyShown - добавляем в pendingHidden, исключаем из ShowIf'а
                    pendingHidden.add(property);
                else { // исключаем из pendingHidden, оставляем в map'е
                    pendingHidden.remove(property);
                    mRestShowIfs.keep(property, showIfs.getValue(i));
                }
            }
            showIfs = MapFact.imOrderFilter(mRestShowIfs, showIfs);
        }

        MExclMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> showIfValues = MapFact.mExclMap();
        ImOrderMap<ImSet<GroupObjectInstance>, ImOrderSet<PropertyReaderInstance>> changedShowIfs = showIfs.groupOrderValues();
        for (int i = 0, size = changedShowIfs.size(); i < size; i++) {
            updateDrawProps(showIfValues, changedShowIfs.getKey(i), changedShowIfs.getValue(i));
        }

        ImMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> immShowIfs = showIfValues.immutable();
        for (int i = 0, size = immShowIfs.size(); i < size; ++i) {
            ShowIfReaderInstance key = (ShowIfReaderInstance) immShowIfs.getKey(i);
            ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue> values = immShowIfs.getValue(i);

            boolean allNull = true;
            for (ObjectValue val : values.valueIt()) {
                if (val.getValue() != null) {
                    allNull = false;
                    break;
                }
            }

            if (allNull) {
                newIsShown.remove(key.getPropertyDraw());
            }
        }

        return immShowIfs;
    }

    private void updateContainersShowIfs(ChangedData changedProps) throws SQLException, SQLHandledException {
        if (!containerShowIfs.isEmpty()) {
            ImSet<GroupObjectInstance> groupObjectKeys = SetFact.EMPTY();
            
            MOrderExclSet<ContainerView> changedContainersShowIfs = SetFact.mOrderExclSet();
            
            int containerShowIfCount = containerShowIfs.size();
            for (int i = 0; i < containerShowIfCount; ++i) {
                ContainerView container = containerShowIfs.getKey(i);
                CalcPropertyObjectInstance<?> showIf = containerShowIfs.getValue(i);
                if (propertyUpdated(showIf, groupObjectKeys, changedProps, false)) {
                    changedContainersShowIfs.exclAdd(container);
                }
            }
            
            MExclMap<ContainerView, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> containerShowIfValues = MapFact.mExclMap();
            queryPropertyObjectValues(changedContainersShowIfs.immutableOrder(), containerShowIfValues, groupObjectKeys, GET_CONTAINER_SHOWIF);
            ImMap<ContainerView, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> immContainerShowIfs = containerShowIfValues.immutable();

            int containerShowIfsCount = immContainerShowIfs.size();
            for (int i = 0; i < containerShowIfsCount; ++i) {
                ContainerView container = immContainerShowIfs.getKey(i);
                ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue> value = immContainerShowIfs.getValue(i);
                isContainerShown.put(container, value.getValue(0).getValue() != null);
            }
        }
    }

    private void fillChangedDrawProps(MFormChanges result, ChangedData changedProps) throws SQLException, SQLHandledException {
        //1е чтение - читаем showIfs
        HashMap<PropertyDrawInstance, Boolean> newIsShown = new HashMap<>();
        HashMap<PropertyDrawInstance, ImSet<GroupObjectInstance>> rowGrids = new HashMap<>();
        Map<PropertyDrawInstance, ImSet<GroupObjectInstance>> rowColumnGrids = new HashMap<>();

        ImMap<PropertyReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> showIfValues =
                readShowIfs(newIsShown, rowGrids, rowColumnGrids, changedProps);

        MOrderExclMap<PropertyReaderInstance, ImSet<GroupObjectInstance>> mReadProperties = MapFact.mOrderExclMap();

        for (PropertyDrawInstance drawProperty : properties) {
            Boolean newPropIsShown = newIsShown.get(drawProperty);
            Boolean oldPropIsShown = isShown.put(drawProperty, newPropIsShown);

            if (newPropIsShown != null) { // hidden проверка внутри чтобы вкладки если что уходили
                boolean read = refresh || !newPropIsShown.equals(oldPropIsShown) // если изменилось представление
                        || groupUpdated(drawProperty.getColumnGroupObjects(), UPDATED_CLASSVIEW); // изменились группы в колонки (так как отбираются только GRID)

                ImSet<GroupObjectInstance> propRowGrids = rowGrids.get(drawProperty);
                ImSet<GroupObjectInstance> propRowColumnGrids = rowColumnGrids.get(drawProperty);

                boolean hidden = isTabHidden(drawProperty, newPropIsShown);

                // расширенный fillChangedReader, но есть часть специфики, поэтому дублируется
                if (read || (!hidden && pendingHidden.contains(drawProperty)) || propertyUpdated(drawProperty.getDrawInstance(), propRowGrids, changedProps, hidden)) {
                    if (hidden) { // если спрятан
                        if (read) { // все равно надо отослать клиенту, так как влияет на наличие вкладки, но с "hidden" значениями
                            mReadProperties.exclAdd(drawProperty.hiddenReader, propRowGrids);
                            if (!newPropIsShown) // говорим клиенту, что свойство в панели
                                result.panelProperties.exclAdd(drawProperty);
                        }
                        pendingHidden.add(drawProperty); // помечаем что когда станет видимым надо будет обновить
                    } else {
                        mReadProperties.exclAdd(drawProperty, propRowGrids);
                        if (!newPropIsShown) // говорим клиенту что свойство в панели
                            result.panelProperties.exclAdd(drawProperty);
                        pendingHidden.remove(drawProperty);
                    }
                }

                if (showIfValues.containsKey(drawProperty.showIfReader)) {
                    //используем значения showIf, зачитанные при 1м шаге
                    result.properties.exclAdd(drawProperty.showIfReader, showIfValues.get(drawProperty.showIfReader));
                } else {
                    // hidden = false, чтобы читать showIf всегда, т.к. влияет на видимость, а соответственно на наличие вкладки
                    // (с hidden'ом избыточный функционал, но небольшой, поэтому все же используем fillChangedReader)
                    // непонятно зачем эта строка была нужна
//                    fillChangedReader(drawProperty.propertyShowIf, drawProperty.showIfReader, propRowColumnGrids, false, read, mReadProperties, changedProps);
                }

                fillChangedReader(drawProperty.propertyCaption, drawProperty.captionReader, propRowColumnGrids, hidden, read, mReadProperties, changedProps);

                fillChangedReader(drawProperty.propertyFooter, drawProperty.footerReader, propRowColumnGrids, hidden, read, mReadProperties, changedProps);

                fillChangedReader(drawProperty.propertyReadOnly, drawProperty.readOnlyReader, propRowGrids, hidden, read, mReadProperties, changedProps);

                fillChangedReader(drawProperty.propertyBackground, drawProperty.backgroundReader, propRowGrids, hidden, read, mReadProperties, changedProps);

                fillChangedReader(drawProperty.propertyForeground, drawProperty.foregroundReader, propRowGrids, hidden, read, mReadProperties, changedProps);
            } else if (oldPropIsShown != null) {
                // говорим клиенту что свойство надо удалить
                result.dropProperties.exclAdd(drawProperty);
            }
        }

        for (GroupObjectInstance group : getGroups()) {
            boolean groupHidden = isHidden(group);
            if (group.propertyBackground != null) {
                ImSet<GroupObjectInstance> gridGroups = (group.curClassView == GRID ? SetFact.singleton(group) : SetFact.<GroupObjectInstance>EMPTY());
                if (refresh || (group.updated & UPDATED_CLASSVIEW) != 0 || propertyUpdated(group.propertyBackground, gridGroups, changedProps, groupHidden))
                    mReadProperties.exclAdd(group.rowBackgroundReader, gridGroups);
            }
            if (group.propertyForeground != null) {
                ImSet<GroupObjectInstance> gridGroups = (group.curClassView == GRID ? SetFact.singleton(group) : SetFact.<GroupObjectInstance>EMPTY());
                if (refresh || (group.updated & UPDATED_CLASSVIEW) != 0 || propertyUpdated(group.propertyForeground, gridGroups, changedProps, groupHidden))
                    mReadProperties.exclAdd(group.rowForegroundReader, gridGroups);
            }
        }

        ImOrderMap<ImSet<GroupObjectInstance>, ImOrderSet<PropertyReaderInstance>> changedDrawProps = mReadProperties.immutableOrder().groupOrderValues();
        for (int i = 0, size = changedDrawProps.size(); i < size; i++) {
            updateDrawProps(result.properties, changedDrawProps.getKey(i), changedDrawProps.getValue(i));
        }
    }

    private void fillChangedReader(CalcPropertyObjectInstance<?> drawProperty, PropertyReaderInstance propertyReader,
                                   ImSet<GroupObjectInstance> columnGroupGrids, boolean hidden, boolean read,
                                   MOrderExclMap<PropertyReaderInstance, ImSet<GroupObjectInstance>> readProperties,
                                   ChangedData changedProps) throws SQLException, SQLHandledException {
        if (drawProperty != null && (read || (!hidden && pendingHidden.contains(propertyReader)) || propertyUpdated(drawProperty, columnGroupGrids, changedProps, hidden))) {
            if (hidden)
                pendingHidden.add(propertyReader);
            else {
                readProperties.exclAdd(propertyReader, columnGroupGrids);
                pendingHidden.remove(propertyReader);
            }
        }
    }

    // возвращает какие объекты на форме показываются
    private Set<GroupObjectInstance> getPropertyGroups() {
        Set<GroupObjectInstance> reportObjects = new HashSet<>();
        for (GroupObjectInstance group : getGroups())
            if (group.curClassView != HIDE)
                reportObjects.add(group);

        return reportObjects;
    }

    public FormData getFormData(int orderTop) throws SQLException, SQLHandledException {
        return getFormData(getCalcProperties(), getGroups(), orderTop);
    }

    public ImSet<PropertyDrawInstance> getCalcProperties() {
        return properties.toOrderSet().getSet().filterFn(new SFunctionSet<PropertyDrawInstance>() {
                public boolean contains(PropertyDrawInstance property) {
                    return property.propertyObject instanceof CalcPropertyObjectInstance;
                }
            });
    }

    public FormData getFormData(Collection<PropertyDrawInstance> propertyDraws, Set<GroupObjectInstance> classGroups) throws SQLException, SQLHandledException {
        return getFormData(ListFact.fromJavaCol(propertyDraws).toSet(), SetFact.fromJavaSet(classGroups), 0);
    }

    // считывает все данные с формы
    public FormData getFormData(ImSet<PropertyDrawInstance> propertyDraws, ImSet<GroupObjectInstance> classGroups, int orderTop) throws SQLException, SQLHandledException {

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

                if (group.curClassView == ClassViewType.PANEL) {
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

    public <P extends PropertyInterface, F extends PropertyInterface> ImSet<FilterEntity> getEditFixedFilters(ClassFormEntity<T> editForm, CalcPropertyValueImplement<P> implement, GroupObjectInstance selectionGroupObject, Result<ImSet<PullChangeProperty>> pullProps) {
        return getContextFilters(editForm.object, implement, selectionGroupObject, pullProps);
    }

    // pullProps чтобы запретить hint'ить
    public <P extends PropertyInterface, F extends PropertyInterface> ImSet<FilterEntity> getContextFilters(ObjectEntity filterObject, CalcPropertyValueImplement<P> propValues, GroupObjectInstance selectionGroupObject, Result<ImSet<PullChangeProperty>> pullProps) {
        CalcProperty<P> implementProperty = propValues.property;

        MSet<FilterEntity> mFixedFilters = SetFact.mSet();
        MSet<PullChangeProperty> mPullProps = SetFact.mSet();
        for (MaxChangeProperty<?, P> constrainedProperty : implementProperty.getMaxChangeProperties(BL.getCheckConstrainedProperties(implementProperty))) {
            mPullProps.add(constrainedProperty);
            mFixedFilters.add(new NotFilterEntity(new NotNullFilterEntity<>(
                    constrainedProperty.getPropertyObjectEntity(propValues.mapping, filterObject))));
        }

        for (FilterEntity filterEntity : entity.getFixedFilters()) {
            FilterInstance filter = filterEntity.getInstance(instanceFactory);
            if (filter.getApplyObject() == selectionGroupObject) {
                for (CalcPropertyValueImplement<?> filterImplement : filter.getResolveChangeProperties(implementProperty)) {
                    OnChangeProperty<F, P> onChangeProperty = (OnChangeProperty<F, P>) ((CalcProperty) filterImplement.property).getOnChangeProperty((CalcProperty) propValues.property);
                    mPullProps.add(onChangeProperty);
                    mFixedFilters.add(new NotNullFilterEntity<>(
                            onChangeProperty.getPropertyObjectEntity((ImMap<F, DataObject>) filterImplement.mapping, propValues.mapping, filterObject)));
                }
            }
        }
        if (pullProps != null) {
            pullProps.set(mPullProps.immutable());
        }
        return mFixedFilters.immutable();
    }

    public <P extends PropertyInterface, F extends PropertyInterface> ImSet<FilterEntity> getObjectFixedFilters(ClassFormEntity<T> editForm, GroupObjectInstance selectionGroupObject) {
        MSet<FilterEntity> mFixedFilters = SetFact.mSet();
        ObjectEntity object = editForm.object;
        for (FilterEntity filterEntity : entity.getFixedFilters()) {
            FilterInstance filter = filterEntity.getInstance(instanceFactory);
            if (filter.getApplyObject() == selectionGroupObject) { // берем фильтры из этой группы
                for (ObjectEntity filterObject : filterEntity.getObjects()) {
                    //добавляем фильтр только, если есть хотя бы один объект который не будет заменён на константу
                    if (filterObject.baseClass == object.baseClass) {
                        mFixedFilters.add(filterEntity.getRemappedFilter(filterObject, object, instanceFactory));
                        break;
                    }
                }
            }
        }
        return mFixedFilters.immutable();
    }

    public Object read(CalcPropertyObjectInstance<?> property) throws SQLException, SQLHandledException {
        return property.read(this);
    }

    public DialogRequest createObjectDialogRequest(final CustomClass objectClass, final ExecutionStack stack) throws SQLException {
        return new DialogRequestAdapter() {
            @Override
            public FormInstance doCreateDialog() throws SQLException, SQLHandledException {
                ClassFormEntity<T> classForm = objectClass.getDialogForm(BL.LM);
                dialogObject = classForm.object;
                return createDialogInstance(classForm.form, dialogObject, NullValue.instance, null, null, null, stack);
            }
        };
    }

    public DialogRequest createObjectEditorDialogRequest(final CalcPropertyValueImplement propertyValues, final ExecutionStack stack) throws SQLException {
        return new DialogRequestAdapter() {
            @Override
            protected FormInstance doCreateDialog() throws SQLException, SQLHandledException {
                CustomClass objectClass = propertyValues.getDialogClass(session);
                ClassFormEntity<T> classForm = objectClass.getEditForm(BL.LM);

                ObjectValue currentObject = propertyValues.readClasses(FormInstance.this);
//                if (currentObject == null && objectClass instanceof ConcreteCustomClass) {
//                    currentObject = addObject((ConcreteCustomClass)objectClass).object;
//                }

                dialogObject = classForm.object;
                return currentObject == null ? null : createDialogInstance(classForm.form, dialogObject, currentObject, null, null, null, stack);
            }
        };
    }

    public DialogRequest createChangeEditorDialogRequest(final CalcPropertyValueImplement propertyValues,
                                                         final GroupObjectInstance groupObject,
                                                         final CalcProperty filterProperty,
                                                         final ExecutionStack stack) throws SQLException {
        return new DialogRequestAdapter() {
            @Override
            protected FormInstance doCreateDialog() throws SQLException, SQLHandledException {
                ClassFormEntity<T> formEntity = propertyValues.getDialogClass(session).getDialogForm(BL.LM);
                Result<ImSet<PullChangeProperty>> pullProps = new Result<>();
                ImSet<FilterEntity> additionalFilters = getEditFixedFilters(formEntity, propertyValues, groupObject, pullProps);

                dialogObject = formEntity.object;

                PropertyDrawEntity initFilterPropertyDraw = filterProperty == null ? null : formEntity.form.getPropertyDraw(filterProperty, dialogObject);

                return createDialogInstance(formEntity.form, dialogObject, propertyValues.readClasses(FormInstance.this), additionalFilters, initFilterPropertyDraw, pullProps.result, stack);
            }
        };
    }

    public DialogRequest createChangeObjectDialogRequest(final CustomClass dialogClass,
                                                         final ObjectValue dialogValue,
                                                         final GroupObjectInstance groupObject,
                                                         final CalcProperty filterProperty,
                                                         final ExecutionStack stack) throws SQLException {
        return new DialogRequestAdapter() {
            @Override
            protected FormInstance doCreateDialog() throws SQLException, SQLHandledException {
                ClassFormEntity<T> formEntity = dialogClass.getDialogForm(BL.LM);
                ImSet<FilterEntity> additionalFilters = getObjectFixedFilters(formEntity, groupObject);

                dialogObject = formEntity.object;

                PropertyDrawEntity initFilterPropertyDraw = filterProperty == null ? null : formEntity.form.getPropertyDraw(filterProperty, dialogObject);

                return createDialogInstance(formEntity.form, dialogObject, dialogValue, additionalFilters, initFilterPropertyDraw, SetFact.<PullChangeProperty>EMPTY(), stack);
            }
        };
    }

    // вызов из обработчиков по умолчанию AggChange, DefaultChange, ChangeReadObject
    private FormInstance createDialogInstance(FormEntity<T> entity, ObjectEntity dialogEntity, ObjectValue dialogValue,
                                              ImSet<FilterEntity> additionalFilters, PropertyDrawEntity initFilterPropertyDraw,
                                              ImSet<PullChangeProperty> pullProps, ExecutionStack outerStack) throws SQLException, SQLHandledException {
        return new FormInstance(entity, this.logicsInstance,
                                this.session, this.securityPolicy,
                                getFocusListener(), getClassListener(),
                                instanceFactory.computer, instanceFactory.connection,
                                MapFact.singleton(dialogEntity, dialogValue),
                                outerStack,
                                true, false, FormSessionScope.OLDSESSION, false, true, true, true,
                                additionalFilters, initFilterPropertyDraw, pullProps, false, locale);
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

    public void fireOnCancel(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireEvent(FormEventType.CANCEL, stack);
    }

    public void fireOnOk() throws SQLException, SQLHandledException {
        formResult = FormCloseType.OK;
    }
    public ImOrderSet<ActionPropertyValueImplement> getEventsOnOk() throws SQLException, SQLHandledException {
        return getEvents(FormEventType.OK);
    }

    public void fireOnClose(ExecutionStack stack) throws SQLException, SQLHandledException {
        formResult = FormCloseType.CLOSE;
        fireEvent(FormEventType.CLOSE, stack);
    }

    public void fireOnDrop(ExecutionStack stack) throws SQLException, SQLHandledException {
        formResult = FormCloseType.DROP;
        fireEvent(FormEventType.DROP, stack);
    }

    public void fireQueryOk(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireEvent(FormEventType.QUERYOK, stack);
    }

    public void fireQueryClose(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireEvent(FormEventType.QUERYCLOSE, stack);
    }

    private void fireEvent(Object eventObject, ExecutionStack stack) throws SQLException, SQLHandledException {
        for(ActionPropertyValueImplement event : getEvents(eventObject))
            event.execute(this, stack);
    }

    private ImOrderSet<ActionPropertyValueImplement> getEvents(Object eventObject) {
        MOrderExclSet<ActionPropertyValueImplement> mResult = SetFact.mOrderExclSet();
        Iterable<ActionPropertyObjectEntity<?>> actionsOnEvent = entity.getEventActionsListIt(eventObject);
        if (actionsOnEvent != null) {
            for (ActionPropertyObjectEntity<?> autoAction : actionsOnEvent) {
                ActionPropertyObjectInstance<? extends PropertyInterface> autoInstance = instanceFactory.getInstance(autoAction);
                if (autoInstance.isInInterface(null) && securityPolicy.property.change.checkPermission(autoAction.property)) { // для проверки null'ов и политики безопасности
                    mResult.exclAdd(autoInstance.getValueImplement());
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

    private SessionModifier createModifier() {
        FunctionSet<CalcProperty> noHints = getNoHints();
        return new OverrideSessionModifier(environmentIncrement, noHints, noHints, entity.getHintsIncrementTable(), entity.getHintsNoUpdate(), session.getModifier());
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

    public boolean isInTransaction() {
        return false;
    }

    // close делать не надо, так как по умолчанию добавляется обработчик события formClose
    public void formQueryClose(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireQueryClose(stack);
    }

    // close делать не надо, так как по умолчанию добавляется обработчик события formOk
    public void formQueryOk(ExecutionStack stack) throws SQLException, SQLHandledException {
        fireQueryOk(stack);
    }

    public void formApply(ExecutionContext context) throws SQLException, SQLHandledException {
        if(apply(BL, context))
            environmentIncrement.add(FormEntity.isAdd, PropertyChange.<ClassPropertyInterface>STATIC(false));
    }

    public void formCancel(ExecutionContext context) throws SQLException, SQLHandledException {
        int result = (Integer) context.requestUserInteraction(new ConfirmClientAction("lsFusion", ThreadLocalContext.localize("{form.do.you.really.want.to.undo.changes}")));
        if (result == JOptionPane.YES_OPTION) {
            cancel(context.stack);
        }
    }

    public void formClose(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        if (sessionScope.isManageSession() && session.isStoredDataChanged()) {
            int result = (Integer) context.requestUserInteraction(new ConfirmClientAction("lsFusion", ThreadLocalContext.localize("{form.do.you.really.want.to.close.form}")));
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        fireOnClose(context.stack);
        formHide(context);
    }

    private void formHide(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        ServerLoggers.remoteLifeLog("FORM HIDE : " + this);
        context.delayUserInteraction(new HideFormClientAction());
        // здесь не делаем close, так как нет RemoteForm + надо делать closeLater, так как могут остаться еще запросы к форме которые возможно надо обработать, так что это делается prepareRemoteChangesResponse
    }

    public void formDrop(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        fireOnDrop(context.stack);
        formHide(context);
    }

    public void formOk(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        if (checkOnOk) {
            if (!checkApply(context.stack, context)) {
                return;
            }
        }

        fireOnOk();

        if (sessionScope.isManageSession() && !apply(BL, context, getEventsOnOk())) {
            return;
        }
        formHide(context);
    }

    public void formRefresh() throws SQLException, SQLHandledException {
        refreshData();
    }

    @Override
    public String toString() {
        return "FORM["+System.identityHashCode(this) + " - " + entity.getSID()+","+getClassListener()+"]";
    }
}
