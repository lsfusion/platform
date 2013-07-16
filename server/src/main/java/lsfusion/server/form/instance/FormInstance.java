package lsfusion.server.form.instance;

import com.google.common.base.Throwables;
import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.Compare;
import lsfusion.interop.FormEventType;
import lsfusion.interop.Scroll;
import lsfusion.interop.action.ConfirmClientAction;
import lsfusion.interop.action.EditNotPerformedClientAction;
import lsfusion.interop.action.HideFormClientAction;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.interop.form.ColumnUserPreferences;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.GroupObjectUserPreferences;
import lsfusion.interop.form.ServerResponse;
import lsfusion.interop.form.layout.ContainerType;
import lsfusion.server.Message;
import lsfusion.server.ParamMessage;
import lsfusion.server.Settings;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.classes.*;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.LogTime;
import lsfusion.server.data.Modify;
import lsfusion.server.data.QueryEnvironment;
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
import lsfusion.server.form.view.ComponentView;
import lsfusion.server.form.view.ContainerView;
import lsfusion.server.logics.*;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.flow.FlowResult;
import lsfusion.server.logics.property.derived.MaxChangeProperty;
import lsfusion.server.logics.property.derived.OnChangeProperty;
import lsfusion.server.session.*;

import javax.swing.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

import static lsfusion.base.BaseUtils.deserializeObject;
import static lsfusion.interop.ClassViewType.GRID;
import static lsfusion.interop.ClassViewType.HIDE;
import static lsfusion.interop.Order.*;
import static lsfusion.interop.form.ServerResponse.CHANGE_WYS;
import static lsfusion.server.form.instance.GroupObjectInstance.*;
import static lsfusion.server.logics.ServerResourceBundle.getString;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который
//  разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class FormInstance<T extends BusinessLogics<T>> extends ExecutionEnvironment {

    public final LogicsInstance logicsInstance;

    public final T BL;

    public final FormEntity<T> entity;

    public final InstanceFactory instanceFactory;

    public final SecurityPolicy securityPolicy;

    private ImMap<ObjectEntity, ? extends ObjectValue> mapObjects = null;
    private final ImOrderSet<GroupObjectInstance> groups;
    public ImSet<GroupObjectInstance> getGroups() {
        return groups.getSet();
    }
    public ImOrderSet<GroupObjectInstance> getOrderGroups() {
        return groups;
    }

    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    public final ImList<PropertyDrawInstance> properties;

    private ImSet<ObjectInstance> objects;

    public final boolean checkOnOk;

    public final boolean isModal;

    public final boolean manageSession;

    public final boolean showDrop;

    private boolean interactive = true; // важно для assertion'а в endApply

    // для импорта конструктор, объекты пустые
    public FormInstance(FormEntity<T> entity, LogicsInstance logicsInstance, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer, DataObject connection) throws SQLException {
        this(entity, logicsInstance, session, securityPolicy, focusListener, classListener, computer, connection, MapFact.<ObjectEntity, ObjectValue>EMPTY(), false, true, false, false, false, null, null);
    }

    private final ImSet<PullChangeProperty> pullProps;

    public FormInstance(FormEntity<T> entity, LogicsInstance logicsInstance, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer, DataObject connection, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, boolean isModal, boolean manageSession, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<FilterEntity> contextFilters, ImSet<PullChangeProperty> pullProps) throws SQLException {
        this.manageSession = manageSession;
        this.isModal = isModal;
        this.checkOnOk = checkOnOk;
        this.showDrop = showDrop;

        this.session = session;
        this.entity = entity;
        this.logicsInstance = logicsInstance;
        this.BL = (T) logicsInstance.getBusinessLogics();
        this.securityPolicy = securityPolicy;

        this.pullProps = pullProps;

        instanceFactory = new InstanceFactory(computer, connection);

        this.weakFocusListener = new WeakReference<FocusListener<T>>(focusListener);
        this.weakClassListener = new WeakReference<CustomClassListener>(classListener);

        groups = SetFact.fromJavaOrderSet(entity.groups).mapOrderSetValues(new GetValue<GroupObjectInstance, GroupObjectEntity>() {
            public GroupObjectInstance getMapValue(GroupObjectEntity value) {
                return instanceFactory.getInstance(value);
            }});
        ImOrderSet<GroupObjectInstance> groupObjects = getOrderGroups();
        for (int i = 0, size = groupObjects.size() ; i < size; i++) {
            GroupObjectInstance groupObject = groupObjects.get(i);
            groupObject.order = i;
            groupObject.setClassListener(classListener);
        }

        for (TreeGroupEntity treeGroup : entity.treeGroups) {
            instanceFactory.getInstance(treeGroup); // чтобы зарегить ссылки
        }

        MList<PropertyDrawInstance> mProperties = ListFact.mListMax(entity.propertyDraws.size());
        for (PropertyDrawEntity<?> propertyDrawEntity : entity.propertyDraws)
            if (this.securityPolicy.property.view.checkPermission(propertyDrawEntity.propertyObject.property)) {
                PropertyDrawInstance propertyDrawInstance = instanceFactory.getInstance(propertyDrawEntity);
                if (propertyDrawInstance.toDraw == null) // для Instance'ов проставляем не null, так как в runtime'е порядок меняться не будет
                    propertyDrawInstance.toDraw = instanceFactory.getInstance(propertyDrawEntity.getToDraw(entity));
                mProperties.add(propertyDrawInstance);
            }
        properties = mProperties.immutableList();

        ImSet<FilterEntity> allFixedFilters = SetFact.fromJavaSet(entity.fixedFilters);
        if(contextFilters !=null)
            allFixedFilters = allFixedFilters.merge(contextFilters);
        ImMap<GroupObjectInstance, ImSet<FilterInstance>> fixedFilters = allFixedFilters.mapSetValues(new GetValue<FilterInstance, FilterEntity>() {
            public FilterInstance getMapValue(FilterEntity value) {
                return value.getInstance(instanceFactory);
            }}).group(new BaseUtils.Group<GroupObjectInstance, FilterInstance>() {
            public GroupObjectInstance group(FilterInstance key) {
                return key.getApplyObject();
            }
        });
        for(int i=0,size=fixedFilters.size();i<size;i++)
            fixedFilters.getKey(i).fixedFilters = fixedFilters.getValue(i);


        for (RegularFilterGroupEntity filterGroupEntity : entity.regularFilterGroups) {
            regularFilterGroups.add(instanceFactory.getInstance(filterGroupEntity));
        }

        ImMap<GroupObjectInstance, ImOrderMap<OrderInstance, Boolean>> fixedOrders = MapFact.fromJavaOrderMap(entity.fixedOrders).mapOrderKeys(new GetValue<OrderInstance, OrderEntity<?>>() {
            public OrderInstance getMapValue(OrderEntity<?> value) {
                return value.getInstance(instanceFactory);
            }}).groupOrder(new BaseUtils.Group<GroupObjectInstance, OrderInstance>() {
            public GroupObjectInstance group(OrderInstance key) {
                return key.getApplyObject();
            }
        });
        for(int i=0,size=fixedOrders.size();i<size;i++)
            fixedOrders.getKey(i).fixedOrders = fixedOrders.getValue(i);

        // в первую очередь ставим на объекты из cache'а
        if (classListener != null) {
            for (GroupObjectInstance groupObject : groupObjects) {
                for (ObjectInstance object : groupObject.objects)
                    if (object.getBaseClass() instanceof CustomClass) {
                        CustomClass cacheClass = (CustomClass) object.getBaseClass();
                        Integer objectID = classListener.getObject(cacheClass);
                        if (objectID != null)
                            groupObject.addSeek(object, session.getDataObject(cacheClass, objectID), false);
                    }
            }
        }

        for (int i=0,size=mapObjects.size();i<size;i++) {
            ObjectInstance instance = instanceFactory.getInstance(mapObjects.getKey(i));
            instance.groupTo.addSeek(instance, mapObjects.getValue(i), false);
        }

        //устанавливаем фильтры и порядки по умолчанию...
        for (RegularFilterGroupInstance filterGroup : regularFilterGroups) {
            int defaultInd = filterGroup.entity.defaultFilterIndex;
            if (defaultInd >= 0 && defaultInd < filterGroup.filters.size()) {
                setRegularFilter(filterGroup, filterGroup.filters.get(defaultInd));
            }
        }

        Set<GroupObjectInstance> wasOrder = new HashSet<GroupObjectInstance>();
        for (Entry<PropertyDrawEntity<?>, Boolean> entry : entity.defaultOrders.entrySet()) {
            PropertyDrawInstance property = instanceFactory.getInstance(entry.getKey());
            GroupObjectInstance toDraw = property.toDraw;
            Boolean ascending = entry.getValue();

            toDraw.changeOrder((CalcPropertyObjectInstance) property.propertyObject, wasOrder.contains(toDraw) ? ADD : REPLACE);
            if (!ascending) {
                toDraw.changeOrder((CalcPropertyObjectInstance) property.propertyObject, DIR);
            }
            wasOrder.add(toDraw);
        }

        applyFilters();

        this.session.registerForm(this);

        environmentIncrement = createEnvironmentIncrement(isModal, this instanceof DialogInstance, manageSession, entity.isReadOnly(), showDrop);

        if (!interactive) {
            endApply();
            this.mapObjects = mapObjects;
        }

        this.interactive = interactive; // обязательно в конце чтобы assertion с endApply не рушить

        fireOnInit();
    }

    private static IncrementChangeProps createEnvironmentIncrement(boolean isModal, boolean isDialog, boolean manageSession, boolean isReadOnly, boolean showDrop) {
        IncrementChangeProps environment = new IncrementChangeProps();
        environment.add(FormEntity.isModal, PropertyChange.<ClassPropertyInterface>STATIC(isModal));
        environment.add(FormEntity.isDialog, PropertyChange.<ClassPropertyInterface>STATIC(isDialog));
        environment.add(FormEntity.manageSession, PropertyChange.<ClassPropertyInterface>STATIC(manageSession));
        environment.add(FormEntity.isReadOnly, PropertyChange.<ClassPropertyInterface>STATIC(isReadOnly));
        environment.add(FormEntity.showDrop, PropertyChange.<ClassPropertyInterface>STATIC(showDrop));
        return environment;
    }

    public FormUserPreferences loadUserPreferences() {
        List<GroupObjectUserPreferences> preferences = new ArrayList<GroupObjectUserPreferences>();
        try {

            ObjectValue formValue = BL.reflectionLM.navigatorElementSID.readClasses(session, new DataObject(entity.getSID(), StringClass.get(50)));
            if (formValue.isNull())
                return null;
            DataObject formObject = (DataObject) formValue;

            KeyExpr propertyDrawExpr = new KeyExpr("propertyDraw");

            Integer userId = (Integer) BL.authenticationLM.currentUser.read(session);
            DataObject currentUser = session.getDataObject(BL.authenticationLM.user, userId);

            Expr customUserExpr = currentUser.getExpr();

            ImRevMap<String, KeyExpr> newKeys = MapFact.singletonRev("propertyDraw", propertyDrawExpr);

            QueryBuilder<String, String> query = new QueryBuilder<String, String>(newKeys);
            query.addProperty("sidPropertyDraw", BL.reflectionLM.sidPropertyDraw.getExpr(propertyDrawExpr));
            query.addProperty("nameShowOverridePropertyDrawCustomUser", BL.reflectionLM.nameShowOverridePropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.addProperty("columnWidthOverridePropertyDrawCustomUser", BL.reflectionLM.columnWidthOverridePropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.addProperty("columnOrderOverridePropertyDrawCustomUser", BL.reflectionLM.columnOrderOverridePropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.addProperty("columnSortOverridePropertyDrawCustomUser", BL.reflectionLM.columnSortOverridePropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.addProperty("columnAscendingSortOverridePropertyDrawCustomUser", BL.reflectionLM.columnAscendingSortOverridePropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            Expr groupObjectPropertyDrawExpr = BL.reflectionLM.groupObjectPropertyDraw.getExpr(propertyDrawExpr);
            query.addProperty("groupObjectPropertyDraw", groupObjectPropertyDrawExpr);
            query.addProperty("sIDGroupObjectPropertyDraw", BL.reflectionLM.sidGroupObject.getExpr(groupObjectPropertyDrawExpr));
            query.addProperty("hasUserPreferencesOverrideGroupObjectCustomUser", BL.reflectionLM.hasUserPreferencesOverrideGroupObjectCustomUser.getExpr(groupObjectPropertyDrawExpr, customUserExpr));

            query.and(BL.reflectionLM.formPropertyDraw.getExpr(propertyDrawExpr).compare(formObject.getExpr(), Compare.EQUALS));

            ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> result = query.execute(this);

            for (ImMap<String, Object> values : result.valueIt()) {
                String propertyDrawSID = values.get("sidPropertyDraw").toString().trim();
                Boolean needToHide = null;
                Object hide = values.get("nameShowOverridePropertyDrawCustomUser");
                if (hide != null) {
                    if (getString("logics.property.draw.hide").equals(hide.toString().trim()))
                        needToHide = true;
                    else if (getString("logics.property.draw.show").equals(hide.toString().trim()))
                        needToHide = false;
                }
                Integer width = (Integer) values.get("columnWidthOverridePropertyDrawCustomUser");
                Integer order = (Integer) values.get("columnOrderOverridePropertyDrawCustomUser");
                Integer sort = (Integer) values.get("columnSortOverridePropertyDrawCustomUser");
                Boolean ascendingSort = (Boolean) values.get("columnAscendingSortOverridePropertyDrawCustomUser");
                Integer groupObjectPropertyDraw = (Integer) values.get("groupObjectPropertyDraw");
                if (groupObjectPropertyDraw != null) {
                    String groupObjectSID = (String) values.get("sIDGroupObjectPropertyDraw");
                    ColumnUserPreferences pref = new ColumnUserPreferences(needToHide, width, order, sort, ascendingSort != null ? ascendingSort : (sort != null ? false : null));
                    boolean found = false;
                    Object hasUserPreferences = values.get("hasUserPreferencesOverrideGroupObjectCustomUser");
                    for (GroupObjectUserPreferences groupObjectPreferences : preferences) {
                        if (groupObjectPreferences.groupObjectSID.equals(groupObjectSID.trim())) {
                            groupObjectPreferences.getColumnUserPreferences().put(propertyDrawSID, pref);
                            if (!groupObjectPreferences.hasUserPreferences)
                                groupObjectPreferences.hasUserPreferences = hasUserPreferences != null;
                            found = true;
                        }
                    }
                    if (!found) {
                        Map preferencesMap = new HashMap<String, ColumnUserPreferences>();
                        preferencesMap.put(propertyDrawSID, pref);
                        preferences.add(new GroupObjectUserPreferences(preferencesMap, groupObjectSID.trim(), hasUserPreferences != null));
                    }
                }
            }
        } catch (SQLException e) {
            Throwables.propagate(e);
        }
        return new FormUserPreferences(preferences);
    }

    public void saveUserPreferences(FormUserPreferences preferences, Boolean forAllUsers) {
        try {
            DataSession dataSession = session.createSession();
            DataObject userObject = dataSession.getDataObject(BL.authenticationLM.user, BL.authenticationLM.currentUser.read(dataSession));
            for (GroupObjectUserPreferences groupObjectPreferences : preferences.getGroupObjectUserPreferencesList()) {
                for (Map.Entry<String, ColumnUserPreferences> entry : groupObjectPreferences.getColumnUserPreferences().entrySet()) {
                    ObjectValue propertyDrawObjectValue = BL.reflectionLM.propertyDrawSIDNavigatorElementSIDPropertyDraw.readClasses(dataSession, new DataObject(entity.getSID(), StringClass.get(50)), new DataObject(entry.getKey(), StringClass.get(50)));
                    if (propertyDrawObjectValue instanceof DataObject) {
                        DataObject propertyDrawObject = (DataObject) propertyDrawObjectValue;
                        Integer idShow = null;
                        ColumnUserPreferences columnPreferences = entry.getValue();
                        if (columnPreferences.isNeedToHide() != null) {
                            idShow = columnPreferences.isNeedToHide() ? BL.reflectionLM.propertyDrawShowStatus.getObjectID("Hide") : BL.reflectionLM.propertyDrawShowStatus.getObjectID("Show");
                        }
                        if (forAllUsers) {
                            BL.reflectionLM.showPropertyDraw.change(idShow, dataSession, propertyDrawObject, userObject);
                            BL.reflectionLM.columnWidthPropertyDraw.change(columnPreferences.getWidthUser(), dataSession, propertyDrawObject);
                            BL.reflectionLM.columnOrderPropertyDraw.change(columnPreferences.getOrderUser(), dataSession, propertyDrawObject);
                            BL.reflectionLM.columnSortPropertyDraw.change(columnPreferences.getSortUser(), dataSession, propertyDrawObject, userObject);
                            BL.reflectionLM.columnAscendingSortPropertyDraw.change(columnPreferences.getAscendingSortUser(), dataSession, propertyDrawObject, userObject);
                        }
                        BL.reflectionLM.showPropertyDrawCustomUser.change(idShow, dataSession, propertyDrawObject, userObject);
                        BL.reflectionLM.columnWidthPropertyDrawCustomUser.change(columnPreferences.getWidthUser(), dataSession, propertyDrawObject, userObject);
                        BL.reflectionLM.columnOrderPropertyDrawCustomUser.change(columnPreferences.getOrderUser(), dataSession, propertyDrawObject, userObject);
                        BL.reflectionLM.columnSortPropertyDrawCustomUser.change(columnPreferences.getSortUser(), dataSession, propertyDrawObject, userObject);
                        BL.reflectionLM.columnAscendingSortPropertyDrawCustomUser.change(columnPreferences.getAscendingSortUser(), dataSession, propertyDrawObject, userObject);
                    } else {
                        throw new RuntimeException("Объект " + entry.getKey() + " (" + entity.getSID() + ") не найден");
                    }
                }
                DataObject groupObjectObject = (DataObject) BL.reflectionLM.groupObjectSIDGroupObjectSIDNavigatorElementGroupObject.readClasses(dataSession, new DataObject(groupObjectPreferences.groupObjectSID, StringClass.get(50)), new DataObject(entity.getSID(), StringClass.get(50)));
                BL.reflectionLM.hasUserPreferencesGroupObjectCustomUser.change(groupObjectPreferences.hasUserPreferences ? true : null, dataSession, groupObjectObject, userObject);
                if (forAllUsers) {
                    BL.reflectionLM.hasUserPreferencesGroupObject.change(groupObjectPreferences.hasUserPreferences ? true : null, dataSession, groupObjectObject);
                }
            }
            dataSession.apply(BL);
        } catch (SQLException e) {
            Throwables.propagate(e);
        }
    }

    public boolean areObjectsFound() {
        assert !interactive;
        for (int i=0,size=mapObjects.size();i<size;i++)
            if (!instanceFactory.getInstance(mapObjects.getKey(i)).getObjectValue().equals(mapObjects.getValue(i)))
                return false;
        return true;
    }

    protected FunctionSet<CalcProperty> getNoHints() {
        FunctionSet<CalcProperty> result = entity.getNoHints();
        if(pullProps==null)
            return result;

        return BaseUtils.merge(result, new FunctionSet<CalcProperty>() {
            public boolean contains(CalcProperty element) {
                for(PullChangeProperty pullProp : pullProps)
                    if(pullProp.isChangeBetween(element))
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

    public void  changeGroupObject(GroupObjectInstance group, ImMap<ObjectInstance, DataObject> values) throws SQLException {
        ImMap<ObjectInstance, DataObject> oldValues = group.getGroupObjectValue();
        for (ObjectInstance objectInstance : oldValues.keyIt()) {
            if (!BaseUtils.nullEquals(oldValues.get(objectInstance), values.get(objectInstance))) {
                fireObjectChanged(objectInstance);
            }
        }
    }

    public void expandGroupObject(GroupObjectInstance group, ImMap<ObjectInstance, DataObject> value) throws SQLException {
        if(group.expandTable==null)
            group.expandTable = group.createKeyTable();
        group.expandTable.modifyRecord(session.sql, value, Modify.MODIFY);
        group.updated |= UPDATED_EXPANDS;
    }

    public void collapseGroupObject(GroupObjectInstance group, ImMap<ObjectInstance, DataObject> value) throws SQLException {
        if(group.expandTable!=null) {
            group.expandTable.modifyRecord(session.sql, value, Modify.DELETE);
            group.updated |= UPDATED_EXPANDS;
        }
    }

    public void expandCurrentGroupObject(ValueClass cls) throws SQLException {
        for (ObjectInstance object : getObjects()) {
            if (object.getBaseClass().isCompatibleParent(cls))
                expandCurrentGroupObject(object);
        }
    }

    public void expandCurrentGroupObject(ObjectInstance object) throws SQLException {
        GroupObjectInstance groupObject = object.groupTo;
        if (groupObject != null && groupObject.isInTree()) {
            if (groupObject.parent != null) {
                // если рекурсивное свойство, то просто раскрываем текущий объект
                ImMap<ObjectInstance, DataObject> value = DataObject.filterDataObjects(groupObject.objects.mapValues(new GetValue<ObjectValue, ObjectInstance>() {
                    public ObjectValue getMapValue(ObjectInstance value) {
                        return value.getObjectValue();
                    }}));
                if (!value.isEmpty())
                    expandGroupObject(groupObject, value);
            } else {
                // раскрываем все верхние groupObject
                for (GroupObjectInstance group : getOrderGroups()) {
                    ImOrderSet<GroupObjectInstance> upGroups = group.getOrderUpTreeGroups();
                    MExclMap<ObjectInstance, DataObject> mValue = MapFact.mExclMap();
                    int upObjects = 0;
                    for (GroupObjectInstance goi : upGroups) {
                        if (goi != null && !goi.equals(group)) {
                            upObjects += goi.objects.size();
                            mValue.exclAddAll(goi.getGroupObjectValue());
                        }
                    }
                    ImMap<ObjectInstance, DataObject> value = mValue.immutable();
                    if (!value.isEmpty() && value.size() == upObjects) // проверка на то, что в каждом из верхних groupObject выбран какой-то объект
                        expandGroupObject(group.getUpTreeGroup(), value);
                    if (group.equals(groupObject))
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
    public List<RegularFilterGroupInstance> regularFilterGroups = new ArrayList<RegularFilterGroupInstance>();
    private Map<RegularFilterGroupInstance, RegularFilterInstance> regularFilterValues = new HashMap<RegularFilterGroupInstance, RegularFilterInstance>();

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
                return !FilterInstance.ignoreInInterface || filter.isInInterface(groupTo);
            }
        }).equals(groupTo.filters);
    }

    public DataObject addFormObject(CustomObjectInstance object, ConcreteCustomClass cls, DataObject pushed) throws SQLException {
        DataObject dataObject = session.addObject(cls, pushed);

        // резолвим все фильтры
        assert checkFilters(object.groupTo);
        for (FilterInstance filter : object.groupTo.filters)
            filter.resolveAdd(this, object, dataObject);

        for (LP lp : BL.LM.lproperties) {
            if (lp instanceof LCP) {
                LCP<?> lcp = (LCP<?>) lp;
                CalcProperty<?> property = lcp.property;
                if (property.autoset) {
                    ValueClass interfaceClass = property.getInterfaceClasses(ClassType.ASSERTFULL).singleValue();
                    ValueClass valueClass = property.getValueClass();
                    if (valueClass instanceof CustomClass && interfaceClass instanceof CustomClass &&
                            cls.isChild((CustomClass) interfaceClass)) { // в общем то для оптимизации
                        Integer obj = getClassListener().getObject((CustomClass) valueClass);
                        if (obj != null)
                            lcp.change(obj, this, dataObject);
                    }
                }
            }
        }

        expandCurrentGroupObject(object);

        // todo : теоретически надо переделывать
        // нужно менять текущий объект, иначе не будет работать ImportFromExcelActionProperty
        object.changeValue(session, dataObject);

        object.groupTo.addSeek(object, dataObject, false);

        // меняем вид, если при добавлении может получиться, что фильтр не выполнится, нужно как-то проверить в общем случае
//      changeClassView(object.groupTo, ClassViewType.PANEL);

        dataChanged = true;

        return dataObject;
    }

    public void changeClass(PropertyObjectInterfaceInstance objectInstance, DataObject dataObject, ConcreteObjectClass cls) throws SQLException {
        if (objectInstance instanceof CustomObjectInstance) {
            CustomObjectInstance object = (CustomObjectInstance) objectInstance;

            if (securityPolicy.cls.edit.change.checkPermission(object.currentClass)) {
                object.changeClass(session, dataObject, cls);
                dataChanged = true;
            }
        } else
            session.changeClass(objectInstance, dataObject, cls);
    }

    public void executeEditAction(PropertyDrawInstance property, String editActionSID, ImMap<ObjectInstance, DataObject> keys) throws SQLException {
        executeEditAction(property, editActionSID, keys, null, null, false);
    }

    public void executeEditAction(PropertyDrawInstance property, String editActionSID, ImMap<ObjectInstance, DataObject> keys, ObjectValue pushChange, DataObject pushAdd, boolean pushConfirm) throws SQLException {
        if (property.propertyReadOnly != null && property.propertyReadOnly.getRemappedPropertyObject(keys).read(this) != null) {
            ThreadLocalContext.delayUserInteraction(EditNotPerformedClientAction.instance);
            return;
        }

        ActionPropertyObjectInstance editAction = property.getEditAction(editActionSID, instanceFactory, entity);
        if (editAction != null && securityPolicy.property.change.checkPermission(editAction.property)) {
            if (editActionSID.equals(ServerResponse.CHANGE) || editActionSID.equals(ServerResponse.GROUP_CHANGE)) { //ask confirm logics...
                PropertyDrawEntity propertyDraw = property.getEntity();
                if (!pushConfirm && propertyDraw.askConfirm) {
                    int result = (Integer) ThreadLocalContext.requestUserInteraction(new ConfirmClientAction("lsFusion",
                            entity.getRichDesign().get(propertyDraw).getAskConfirmMessage()));
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
            }
            editAction.getRemappedPropertyObject(keys).execute(this, pushChange, pushAdd, property);
        } else {
            ThreadLocalContext.delayUserInteraction(EditNotPerformedClientAction.instance);
        }
    }

    public void pasteExternalTable(List<PropertyDrawInstance> properties, List<ImMap<ObjectInstance, DataObject>> columnKeys, List<List<byte[]>> values) throws SQLException, IOException {
        GroupObjectInstance groupObject = properties.get(0).toDraw;
        ImOrderSet<ImMap<ObjectInstance, DataObject>> executeList = groupObject.seekObjects(session.sql, getQueryEnv(), getModifier(), BL.LM.baseClass, values.size()).keyOrderSet();

        //создание объектов
        int availableQuantity = executeList.size();
        if (availableQuantity < values.size()) {
            executeList = executeList.addOrderExcl(groupObject.createObjects(session, this, values.size() - availableQuantity));
        }

        for (int i = 0; i < properties.size(); i++) {
            PropertyDrawInstance property = properties.get(i);

            ImOrderValueMap<ImMap<ObjectInstance, DataObject>, Object> mvPasteRows = executeList.mapItOrderValues();
            for (int j = 0; j < executeList.size(); j++) {
                Object value = deserializeObject(values.get(j).get(i));
                mvPasteRows.mapValue(j, value);
            }

            executePasteAction(property, columnKeys.get(i), mvPasteRows.immutableValueOrder());
        }
    }

    private ImOrderSet<ImMap<ObjectInstance, DataObject>> readObjects(List<Map<Integer, Object>> keyIds) throws SQLException {

        MOrderExclSet<ImMap<ObjectInstance, DataObject>> mResult = SetFact.mOrderExclSet(keyIds.size());
        for(Map<Integer, Object> keyId : keyIds) {
            MExclMap<ObjectInstance, DataObject> mKey = MapFact.mExclMap(keyIds.size());
            for (Entry<Integer, Object> objectId : keyId.entrySet()) {
                ObjectInstance objectInstance = getObjectInstance(objectId.getKey());
                mKey.exclAdd(objectInstance, session.getDataObject(objectInstance.getBaseClass(), objectId.getValue()));
            }
            mResult.exclAdd(mKey.immutable());
        }
        return mResult.immutableOrder();
    }

    public void pasteMulticellValue(Map<Integer, List<Map<Integer, Object>>> cells, Map<Integer, byte[]> values) throws SQLException, IOException {
        for (Integer propertyId : cells.keySet()) { // бежим по ячейкам
            PropertyDrawInstance property = getPropertyDraw(propertyId);
            Object value = deserializeObject(values.get(propertyId));
            executePasteAction(property, null, readObjects(cells.get(propertyId)).toOrderMap(value));
        }
    }

    private void executePasteAction(PropertyDrawInstance<?> property, ImMap<ObjectInstance, DataObject> columnKey, ImOrderMap<ImMap<ObjectInstance, DataObject>, Object> pasteRows) throws SQLException {
        if (!pasteRows.isEmpty()) {
            for (int i = 0, size = pasteRows.size(); i < size; i++) {
                ImMap<ObjectInstance, DataObject> key = pasteRows.getKey(i);
                if (columnKey != null) {
                    key = key.addExcl(columnKey);
                }

                Object oValue = pasteRows.getValue(i);
                ObjectValue value = NullValue.instance;
                if (oValue != null) {
                    DataClass changeType = property.entity.getRequestInputType(CHANGE_WYS, entity);
                    if (changeType != null) {
                        value = session.getObjectValue(changeType, oValue);
                    }
                }
                executeEditAction(property, CHANGE_WYS, key, value, null, true);
            }
        }
    }

    public int countRecords(int groupObjectID) throws SQLException {
        GroupObjectInstance group = getGroupObjectInstance(groupObjectID);
        Expr expr = GroupExpr.create(MapFact.<Object,Expr>EMPTY(), new ValueExpr(1, IntegerClass.instance), group.getWhere(group.getMapKeys(), getModifier()), GroupType.SUM, MapFact.<Object,Expr>EMPTY());
        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(MapFact.<Object, KeyExpr>EMPTYREV());
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
        return MapFact.override(mapKeys, columnKeys.mapKeyValues(new GetValue<Expr, ObjectInstance>() { // замещение с добавлением
            public Expr getMapValue(ObjectInstance value) {
                return value.getExpr();
            }}));
    }
    public Object calculateSum(PropertyDrawInstance propertyDraw, ImMap<ObjectInstance, DataObject> columnKeys) throws SQLException {
        GroupObjectInstance groupObject = propertyDraw.toDraw;

        ImRevMap<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();

        ImMap<ObjectInstance, Expr> keys = overrideColumnKeys(mapKeys, columnKeys);

        Expr expr = GroupExpr.create(MapFact.<Object, Expr>EMPTY(), propertyDraw.getDrawInstance().getExpr(keys, getModifier()), groupObject.getWhere(mapKeys, getModifier()), GroupType.SUM, MapFact.<Object, Expr>EMPTY());

        QueryBuilder<Object, String> query = new QueryBuilder<Object, String>(MapFact.<Object, KeyExpr>EMPTYREV());
        query.addProperty("sum", expr);
        ImOrderMap<ImMap<Object, Object>, ImMap<String, Object>> result = query.execute(this);
        return result.getValue(0).get("sum");
    }

    public Map<List<Object>, List<Object>> groupData(ImMap<PropertyDrawInstance, ImList<ImMap<ObjectInstance, DataObject>>> toGroup,
                                                     ImMap<Object, ImList<ImMap<ObjectInstance, DataObject>>> toSum,
                                                     ImMap<PropertyDrawInstance, ImList<ImMap<ObjectInstance, DataObject>>> toMax, boolean onlyNotNull) throws SQLException {
        GroupObjectInstance groupObject = toGroup.getKey(0).toDraw;
        ImRevMap<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();

        MRevMap<Object, KeyExpr> mKeyExprMap = MapFact.mRevMap();
        MExclMap<Object, Expr> mExprMap = MapFact.mExclMap();
        for (PropertyDrawInstance property : toGroup.keyIt()) {
            int i = 0;
            for (ImMap<ObjectInstance, DataObject> columnKeys : toGroup.get(property)) {
                i++;
                ImMap<ObjectInstance, Expr> keys = overrideColumnKeys(mapKeys, columnKeys);
                mKeyExprMap.revAdd(property.getsID() + i, new KeyExpr("expr"));
                mExprMap.exclAdd(property.getsID() + i, property.getDrawInstance().getExpr(keys, getModifier()));
            }
        }
        ImRevMap<Object, KeyExpr> keyExprMap = mKeyExprMap.immutableRev();
        ImMap<Object, Expr> exprMap = mExprMap.immutable();

        QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keyExprMap);
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
                if(!(sumObject instanceof PropertyDrawInstance)) {
                    query.addProperty("quant", exprQuant);
                    continue;
                }

                property = (PropertyDrawInstance)sumObject;
                currentList = toSum.getValue(i);
            } else {
                groupType = GroupType.MAX;

                property = toMax.getKey(i - separator);
                currentList = toMax.getValue(i - separator);
            }
            for (ImMap<ObjectInstance, DataObject> columnKeys : currentList) {
                idIndex++;
                ImMap<ObjectInstance, Expr> keys = overrideColumnKeys(mapKeys, columnKeys);
                Expr expr = GroupExpr.create(exprMap, property.getDrawInstance().getExpr(keys, getModifier()), groupObject.getWhere(mapKeys, getModifier()), groupType, keyExprMap);
                query.addProperty(property.getsID() + idIndex, expr);
                if (onlyNotNull) {
                    query.and(expr.getWhere());
                }
            }
        }

        Map<List<Object>, List<Object>> resultMap = new OrderedMap<List<Object>, List<Object>>();
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> result = query.execute(this);
        for (int j=0,size=result.size();j<size;j++) {
            ImMap<Object, Object> one = result.getKey(j); ImMap<Object, Object> oneValue = result.getValue(j);

            List<Object> groupList = new ArrayList<Object>();
            List<Object> sumList = new ArrayList<Object>();

            for (PropertyDrawInstance propertyDraw : toGroup.keyIt()) {
                for (int i = 1; i <= toGroup.get(propertyDraw).size(); i++) {
                    groupList.add(one.get(propertyDraw.getsID() + i));
                }
            }
            int index = 1;
            for (int k=0,sizeK=toSum.size();k<sizeK;k++) {
                Object propertyDraw = toSum.getKey(k);
                if (propertyDraw instanceof PropertyDrawInstance) {
                    for (int i = 1, sizeI = toSum.getValue(i).size(); i <= sizeI; i++) {
                        sumList.add(oneValue.get(((PropertyDrawInstance)propertyDraw).getsID() + index));
                        index++;
                    }
                } else
                    sumList.add(oneValue.get("quant"));
            }
            for (int k=0,sizeK=toMax.size();k<sizeK;k++) {
                PropertyDrawInstance propertyDraw = toMax.getKey(k);
                for (int i = 1, sizeI = toMax.getValue(k).size(); i <= sizeI; i++) {
                    sumList.add(oneValue.get(propertyDraw.getsID() + index));
                    index++;
                }
            }
            resultMap.put(groupList, sumList);
        }
        return resultMap;
    }

    // Обновление данных
    public void refreshData() throws SQLException {

        for (ObjectInstance object : getObjects())
            if (object instanceof CustomObjectInstance)
                ((CustomObjectInstance) object).refreshValueClass(session);
        refresh = true;
        dataChanged = session.hasChanges();
    }

    public boolean checkApply() throws SQLException {
        return session.check(BL, this);
    }

    public boolean apply(BusinessLogics BL) throws SQLException {
        if (entity.isSynchronizedApply)
            synchronized (entity) {
                return syncApply();
            }
        else
            return syncApply();
    }

    private boolean syncApply() throws SQLException {
        boolean succeeded = session.apply(BL, this);

        if (!succeeded)
            return false;

        refreshData();
        fireOnApply();

        dataChanged = true; // временно пока applyChanges синхронен, для того чтобы пересылался факт изменения данных

        ThreadLocalContext.delayUserInteraction(new LogMessageClientAction(getString("form.instance.changes.saved"), false));
        return true;
    }

    public void cancel() throws SQLException {
        session.cancel();

        // пробежим по всем объектам
        for (ObjectInstance object : getObjects())
            if (object instanceof CustomObjectInstance)
                ((CustomObjectInstance) object).updateCurrentClass(session);
        fireOnCancel();

        dataChanged = true;
    }

    // ------------------ Через эти методы сообщает верхним объектам об изменениях ------------------- //

    // В дальнейшем наверное надо будет переделать на Listener'ы...
    protected void objectChanged(ConcreteCustomClass cls, Integer objectID) {
    }

    public void changePageSize(GroupObjectInstance groupObject, Integer pageSize) {
        groupObject.setPageSize(pageSize);
    }

    public void gainedFocus() {
        dataChanged = true;
        FocusListener<T> focusListener = getFocusListener();
        if (focusListener != null)
            focusListener.gainedFocus(this);
    }

    private boolean closed = false;

    public void close() throws SQLException {
        closed = true;
        session.unregisterForm(this);
        for (GroupObjectInstance group : getGroups()) {
            if (group.keyTable != null)
                group.keyTable.drop(session.sql);
            if (group.expandTable != null)
                group.expandTable.drop(session.sql);
        }
    }

    // --------------------------------------------------------------------------------------- //
    // --------------------- Общение в обратную сторону с ClientForm ------------------------- //
    // --------------------------------------------------------------------------------------- //

    public ConcreteCustomClass getObjectClass(ObjectInstance object) {

        if (!(object instanceof CustomObjectInstance))
            return null;

        return ((CustomObjectInstance) object).currentClass;
    }

    public FormInstance<T> createForm(FormEntity<T> form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOK, boolean showDrop, boolean interactive, ImSet<FilterEntity> contextFilters, ImSet<PullChangeProperty> pullProps) throws SQLException {
        return new FormInstance<T>(form, logicsInstance,
                sessionScope.isNewSession() ? session.createSession() : session,
                securityPolicy, getFocusListener(), getClassListener(), instanceFactory.computer, instanceFactory.connection, mapObjects, isModal, sessionScope.isManageSession(),
                checkOnOK, showDrop, interactive, contextFilters, pullProps);
    }

    public void forceChangeObject(ObjectInstance object, ObjectValue value) throws SQLException {

        if (object instanceof DataObjectInstance && !(value instanceof DataObject))
            object.changeValue(session, ((DataObjectInstance) object).getBaseClass().getDefaultObjectValue());
        else
            object.changeValue(session, value);

        object.groupTo.addSeek(object, value, false);
    }

    public void forceChangeObject(ValueClass cls, ObjectValue value) throws SQLException {

        for (ObjectInstance object : getObjects()) {
            if (object.getBaseClass().isCompatibleParent(cls))
                forceChangeObject(object, value);
        }
    }

    // todo : временная затычка
    public void seekObject(ObjectInstance object, ObjectValue value) throws SQLException {

        if (entity.eventActions.size() > 0) { // дебилизм конечно но пока так
            forceChangeObject(object, value);
        } else {
            object.groupTo.addSeek(object, value, false);
        }
    }

    public void changeObject(PropertyObjectInterfaceInstance objectInstance, ObjectValue objectValue) throws SQLException {
        if (objectInstance instanceof ObjectInstance) {
            ObjectInstance object = (ObjectInstance) objectInstance;

            seekObject(object, objectValue);
            fireObjectChanged(object); // запускаем все Action'ы, которые следят за этим объектом
        }
    }

    // "закэшированная" проверка присутствия в интерфейсе, отличается от кэша тем что по сути функция от mutable объекта
    protected Map<PropertyDrawInstance, Boolean> isInInterface = new HashMap<PropertyDrawInstance, Boolean>();
    protected Map<PropertyDrawInstance, Boolean> isShown = new HashMap<PropertyDrawInstance, Boolean>();

    // проверки видимости (для оптимизации pageframe'ов)
    protected Set<PropertyReaderInstance> pendingHidden = SetFact.mAddRemoveSet();

    private boolean isHidden(PropertyDrawInstance<?> property, boolean grid) {
        if (Settings.get().isDisableTabbedOptimization())
            return false;

        ComponentView container = entity.getDrawTabContainer(property.entity, grid);
        return container != null && isHidden(container); // первая проверка - cheat / оптимизация
    }

    private boolean isHidden(GroupObjectInstance group) {
        if (Settings.get().isDisableTabbedOptimization())
            return false;

        FormEntity.ComponentSet containers = entity.getDrawTabContainers(group.entity);
        if (containers == null) // cheat / оптимизация, иначе пришлось бы в isHidden и еще в нескольких местах явную проверку на null
            return false;
        for (ComponentView component : containers)
            if (!isHidden(component))
                return false;
        return true;
    }

    private boolean isHidden(ComponentView component) {
        ContainerView parent = component.getContainer();
        assert parent.getType() == ContainerType.TABBED_PANE;

        ComponentView visible = visibleTabs.get(parent);
        if (visible == null && parent.children.size() > 0) // аналогичные проверки на клиентах, чтобы при init'е не вызывать
            visible = parent.children.iterator().next();
        if (!component.equals(visible))
            return true;

        ComponentView tabContainer = parent.getTabContainer();
        return tabContainer != null && isHidden(tabContainer);
    }

    protected Map<ContainerView, ComponentView> visibleTabs = new HashMap<ContainerView, ComponentView>();

    public void setTabVisible(ContainerView view, ComponentView page) {
        assert view.getType() == ContainerType.TABBED_PANE;
        visibleTabs.put(view, page);
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

    private boolean propertyUpdated(CalcPropertyObjectInstance updated, ImSet<GroupObjectInstance> groupObjects, FunctionSet<CalcProperty> changedProps) {
        return dataUpdated(updated, changedProps)
               || groupUpdated(groupObjects, UPDATED_KEYS)
               || objectUpdated(updated, groupObjects);
    }

    private boolean groupUpdated(ImSet<GroupObjectInstance> groupObjects, int flags) {
        for (GroupObjectInstance groupObject : groupObjects)
            if ((groupObject.updated & flags) != 0)
                return true;
        return false;
    }

    private boolean dataUpdated(Updated updated, FunctionSet<CalcProperty> changedProps) {
        return updated.dataUpdated(changedProps);
    }

    void applyFilters() {
        for (GroupObjectInstance group : getGroups())
            group.filters = group.getSetFilters();
    }

    void applyOrders() {
        for (GroupObjectInstance group : getGroups())
            group.orders = group.getSetOrders();
    }

    private static class GroupObjectValue {
        private GroupObjectInstance group;
        private ImMap<ObjectInstance, DataObject> value;

        private GroupObjectValue(GroupObjectInstance group, ImMap<ObjectInstance, DataObject> value) {
            this.group = group;
            this.value = value;
        }
    }

    @Message("message.form.update.props")
    private <T extends PropertyReaderInstance> void updateDrawProps(MExclMap<T, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> properties, ImSet<GroupObjectInstance> keyGroupObjects, @ParamMessage ImOrderSet<T> propertySet) throws SQLException {
        QueryBuilder<ObjectInstance, T> selectProps = new QueryBuilder<ObjectInstance, T>(GroupObjectInstance.getObjects(getUpTreeGroups(keyGroupObjects)));
        for (GroupObjectInstance keyGroup : keyGroupObjects) {
            selectProps.and(keyGroup.keyTable.getWhere(selectProps.getMapExprs()));
        }

        for (T propertyReader : propertySet) {
            selectProps.addProperty(propertyReader, propertyReader.getPropertyObjectInstance().getExpr(selectProps.getMapExprs(), getModifier()));
        }

        ImMap<ImMap<ObjectInstance, DataObject>, ImMap<T, ObjectValue>> queryResult = selectProps.executeClasses(this, BL.LM.baseClass).getMap();
        for (final T propertyReader : propertySet) {
            properties.exclAdd(propertyReader,
                               queryResult.mapValues(new GetValue<ObjectValue, ImMap<T, ObjectValue>>() {
                                   public ObjectValue getMapValue(ImMap<T, ObjectValue> value) {
                                       return value.get(propertyReader);
                                   }
                               }));
        }
    }

    @Message("message.form.end.apply")
    @LogTime
    public FormChanges endApply() throws SQLException {

        assert interactive;

        final MFormChanges result = new MFormChanges();

        if(closed)
            return result.immutable();

        QueryEnvironment queryEnv = getQueryEnv();

        // если изменились данные, применяем изменения
        FunctionSet<CalcProperty> changedProps;
        if (dataChanged) {
            session.executeSessionEvents(this);
            changedProps = CalcProperty.getDependsOnSet(session.update(this));
        } else
            changedProps = SetFact.EMPTY();

        Result<FunctionSet<CalcProperty>> mGroupChangedProps = new Result<FunctionSet<CalcProperty>>(changedProps);  // так как могут еще измениться свойства созданные при помощи операторов форм
        GroupObjectValue updateGroupObject = null; // так как текущий groupObject идет относительно treeGroup, а не group
        for (GroupObjectInstance group : getOrderGroups()) {
            ImMap<ObjectInstance, DataObject> selectObjects = group.updateKeys(session.sql, queryEnv, getModifier(), environmentIncrement, BL.LM.baseClass, isHidden(group), refresh, result, mGroupChangedProps);
            if (selectObjects != null) // то есть нужно изменять объект
                updateGroupObject = new GroupObjectValue(group, selectObjects);

            if (group.getDownTreeGroups().size() == 0 && updateGroupObject != null) { // так как в tree группе currentObject друг на друга никак не влияют, то можно и нужно делать updateGroupObject в конце
                updateGroupObject.group.update(session, result, updateGroupObject.value);
                updateGroupObject = null;
            }
        }
        changedProps = mGroupChangedProps.result;

        fillChangedDrawProps(result, changedProps);

        // сбрасываем все пометки
        for (GroupObjectInstance group : getGroups()) {
            group.userSeeks = null;

            for (ObjectInstance object : group.objects)
                object.updated = 0;
            group.updated = 0;
        }
        refresh = false;
        dataChanged = false;

//        result.out(this);

        return result.immutable();
    }

    private ImMap<ShowIfReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> readShowIfs(Map<PropertyDrawInstance, Boolean> newIsShown, Map<PropertyDrawInstance, ImSet<GroupObjectInstance>> rowGrids, Map<PropertyDrawInstance, ImSet<GroupObjectInstance>> rowColumnGrids, FunctionSet<CalcProperty> changedProps) throws SQLException {
        final MOrderExclMap<ShowIfReaderInstance, ImSet<GroupObjectInstance>> mShowIfs = MapFact.mOrderExclMap();
        for (PropertyDrawInstance<?> drawProperty : properties) {
            ClassViewType curClassView = drawProperty.getCurClassView();
            if (curClassView == HIDE) continue;

            ClassViewType forceViewType = drawProperty.getForceViewType();
            if (forceViewType != null && forceViewType == HIDE) continue;

            ImSet<GroupObjectInstance> propRowColumnGrids = drawProperty.getColumnGroupObjectsInGridView();
            ImSet<GroupObjectInstance> propRowGrids = null;
            Boolean newInInterface = null;
            if (curClassView == GRID && (forceViewType == null || forceViewType == GRID) &&
                drawProperty.propertyObject.isInInterface(propRowGrids = propRowColumnGrids.addExcl(drawProperty.toDraw), forceViewType != null)) {
                // в grid'е
                newInInterface = true;
            } else if (drawProperty.propertyObject.isInInterface(propRowGrids = propRowColumnGrids, false)) {
                // в панели
                newInInterface = false;
            }

            rowGrids.put(drawProperty, propRowGrids);
            rowColumnGrids.put(drawProperty, propRowColumnGrids);
            newIsShown.put(drawProperty, newInInterface);

            Boolean oldInInterface = isInInterface.get(drawProperty);
            if (newInInterface != null && drawProperty.propertyShowIf != null) {
                boolean read = refresh || !newInInterface.equals(oldInInterface) // если изменилось представление
                               || groupUpdated(drawProperty.getColumnGroupObjects(), UPDATED_CLASSVIEW); // изменились группы в колонки (так как отбираются только GRID)
                if (read || propertyUpdated(drawProperty.propertyShowIf, propRowColumnGrids, changedProps)) {
                    mShowIfs.exclAdd(drawProperty.showIfReader, propRowColumnGrids);
                } else {
                    //т.е. не поменялся ни inInterface, ни showIf
                    newIsShown.put(drawProperty, isShown.get(drawProperty));
                }
            }
        }

        MExclMap<ShowIfReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> showIfValues = MapFact.mExclMap();
        ImOrderMap<ImSet<GroupObjectInstance>, ImOrderSet<ShowIfReaderInstance>> changedShowIfs = mShowIfs.immutableOrder().groupOrderValues();
        for (int i = 0, size = changedShowIfs.size(); i < size; i++) {
            updateDrawProps(showIfValues, changedShowIfs.getKey(i), changedShowIfs.getValue(i));
        }

        ImMap<ShowIfReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> immShowIfs = showIfValues.immutable();
        for (int i = 0, size = immShowIfs.size(); i < size; ++i) {
            ShowIfReaderInstance key = immShowIfs.getKey(i);
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

    private void fillChangedDrawProps(MFormChanges result, FunctionSet<CalcProperty> changedProps) throws SQLException {
        //1е чтение - читаем showIfs
        HashMap<PropertyDrawInstance, Boolean> newIsShown = new HashMap<PropertyDrawInstance, Boolean>();
        HashMap<PropertyDrawInstance, ImSet<GroupObjectInstance>> rowGrids = new HashMap<PropertyDrawInstance, ImSet<GroupObjectInstance>>();
        Map<PropertyDrawInstance, ImSet<GroupObjectInstance>> rowColumnGrids = new HashMap<PropertyDrawInstance, ImSet<GroupObjectInstance>>();

        ImMap<ShowIfReaderInstance, ImMap<ImMap<ObjectInstance, DataObject>, ObjectValue>> showIfValues = readShowIfs(newIsShown, rowGrids, rowColumnGrids, changedProps);

        MOrderExclMap<PropertyReaderInstance, ImSet<GroupObjectInstance>> mReadProperties = MapFact.mOrderExclMap();

        for (PropertyDrawInstance<?> drawProperty : properties) {
            Boolean newPropIsShown = newIsShown.get(drawProperty);
            Boolean oldPropIsShown = isShown.put(drawProperty, newPropIsShown);

            if (newPropIsShown != null) { // hidden проверка внутри чтобы вкладки если что уходили
                boolean read = refresh || !newPropIsShown.equals(oldPropIsShown) // если изменилось представление
                               || groupUpdated(drawProperty.getColumnGroupObjects(), UPDATED_CLASSVIEW); // изменились группы в колонки (так как отбираются только GRID)

                ImSet<GroupObjectInstance> propRowGrids = rowGrids.get(drawProperty);
                ImSet<GroupObjectInstance> propRowColumnGrids = rowColumnGrids.get(drawProperty);

                boolean hidden = isHidden(drawProperty, newPropIsShown);

                // расширенный fillChangedReader, но есть часть специфики, поэтому дублируется
                if (read || (!hidden && pendingHidden.contains(drawProperty)) || propertyUpdated(drawProperty.getDrawInstance(), propRowGrids, changedProps)) {
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
                    fillChangedReader(drawProperty.propertyShowIf, drawProperty.showIfReader, propRowColumnGrids, false, read, mReadProperties, changedProps);
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
            if (group.propertyBackground != null) {
                ImSet<GroupObjectInstance> gridGroups = (group.curClassView == GRID ? SetFact.singleton(group) : SetFact.<GroupObjectInstance>EMPTY());
                if (refresh || (group.updated & UPDATED_CLASSVIEW) != 0 || propertyUpdated(group.propertyBackground, gridGroups, changedProps))
                    mReadProperties.exclAdd(group.rowBackgroundReader, gridGroups);
            }
            if (group.propertyForeground != null) {
                ImSet<GroupObjectInstance> gridGroups = (group.curClassView == GRID ? SetFact.singleton(group) : SetFact.<GroupObjectInstance>EMPTY());
                if (refresh || (group.updated & UPDATED_CLASSVIEW) != 0 || propertyUpdated(group.propertyForeground, gridGroups, changedProps))
                    mReadProperties.exclAdd(group.rowForegroundReader, gridGroups);
            }
        }

        ImOrderMap<ImSet<GroupObjectInstance>, ImOrderSet<PropertyReaderInstance>> changedDrawProps = mReadProperties.immutableOrder().groupOrderValues();
        for (int i = 0, size = changedDrawProps.size(); i < size; i++) {
            updateDrawProps(result.properties, changedDrawProps.getKey(i), changedDrawProps.getValue(i));
        }
    }

    private void fillChangedReader(CalcPropertyObjectInstance<?> drawProperty, PropertyReaderInstance propertyReader, ImSet<GroupObjectInstance> columnGroupGrids, boolean hidden, boolean read, MOrderExclMap<PropertyReaderInstance, ImSet<GroupObjectInstance>> readProperties, FunctionSet<CalcProperty> changedProps) {
        if (drawProperty != null && (read || (!hidden && pendingHidden.contains(propertyReader)) || propertyUpdated(drawProperty, columnGroupGrids, changedProps))) {
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
        Set<GroupObjectInstance> reportObjects = new HashSet<GroupObjectInstance>();
        for (GroupObjectInstance group : getGroups())
            if (group.curClassView != HIDE)
                reportObjects.add(group);

        return reportObjects;
    }

    public FormData getFormData(int orderTop) throws SQLException {
        ImSet<PropertyDrawInstance> calcProps = properties.toOrderSet().getSet().filterFn(new SFunctionSet<PropertyDrawInstance>() {
            public boolean contains(PropertyDrawInstance property) {
                return property.propertyObject instanceof CalcPropertyObjectInstance;
            }
        });
        return getFormData(calcProps, getGroups(), orderTop);
    }

    public FormData getFormData(Collection<PropertyDrawInstance> propertyDraws, Set<GroupObjectInstance> classGroups) throws SQLException {
        return getFormData(ListFact.fromJavaCol(propertyDraws).toSet(), SetFact.fromJavaSet(classGroups), 0);
    }

    // считывает все данные с формы
    public FormData getFormData(ImSet<PropertyDrawInstance> propertyDraws, ImSet<GroupObjectInstance> classGroups, int orderTop) throws SQLException {

        applyFilters();
        applyOrders();

        // пока сделаем тупо получаем один большой запрос

        QueryBuilder<ObjectInstance, Object> query = new QueryBuilder<ObjectInstance, Object>(GroupObjectInstance.getObjects(classGroups));
        MOrderMap<Object, Boolean> mQueryOrders = MapFact.mOrderMap();

        for (GroupObjectInstance group : getGroups()) {

            if (classGroups.contains(group)) {

                // не фиксированные ключи
                query.and(group.getWhere(query.getMapExprs(), getModifier()));

                // закинем Order'ы
                for (int i=0,size=group.orders.size();i<size;i++) {
                    Object orderObject = new Object();
                    query.addProperty(orderObject, group.orders.getKey(i).getExpr(query.getMapExprs(), getModifier()));
                    mQueryOrders.add(orderObject, group.orders.getValue(i));
                }

                for (ObjectInstance object : group.objects) {
                    query.addProperty(object, object.getExpr(query.getMapExprs(), getModifier()));
                    mQueryOrders.add(object, false);
                }
            }
        }

        for (PropertyDrawInstance<?> property : propertyDraws)
            query.addProperty(property, property.getDrawInstance().getExpr(query.getMapExprs(), getModifier()));

        ImOrderMap<ImMap<ObjectInstance, Object>, ImMap<Object, Object>> resultSelect = query.execute(this, mQueryOrders.immutableOrder(), orderTop);

        MOrderExclMap<ImMap<ObjectInstance,Object>, ImMap<PropertyDrawInstance,Object>> mResult = MapFact.mOrderExclMap(resultSelect.size());
        for (int i=0,size=resultSelect.size();i<size;i++) {
            ImMap<ObjectInstance, Object> resultKey = resultSelect.getKey(i); ImMap<Object, Object> resultValue = resultSelect.getValue(i);

            MExclMap<ObjectInstance, Object> mGroupValue = MapFact.mExclMap();
            for (GroupObjectInstance group : getGroups())
                for (ObjectInstance object : group.objects)
                    if (classGroups.contains(group))
                        mGroupValue.exclAdd(object, resultKey.get(object));
                    else
                        mGroupValue.exclAdd(object, object.getObjectValue().getValue());
            mResult.exclAdd(mGroupValue.immutable(), resultValue.filterIncl(propertyDraws));
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
            mFixedFilters.add(new NotFilterEntity(new NotNullFilterEntity<MaxChangeProperty.Interface<P>>(
                    constrainedProperty.getPropertyObjectEntity(propValues.mapping, filterObject))));
        }

        for (FilterEntity filterEntity : entity.fixedFilters) {
            FilterInstance filter = filterEntity.getInstance(instanceFactory);
            if (filter.getApplyObject() == selectionGroupObject) {
                for (CalcPropertyValueImplement<?> filterImplement : filter.getResolveChangeProperties(implementProperty)) {
                    OnChangeProperty<F, P> onChangeProperty = (OnChangeProperty<F, P>) ((CalcProperty) filterImplement.property).getOnChangeProperty((CalcProperty) propValues.property);
                    mPullProps.add(onChangeProperty);
                    mFixedFilters.add(new NotNullFilterEntity<OnChangeProperty.Interface<F, P>>(
                            onChangeProperty.getPropertyObjectEntity((ImMap<F, DataObject>) filterImplement.mapping, propValues.mapping, filterObject)));
                }
            }
        }
        if (pullProps != null) {
            pullProps.set(mPullProps.immutable());
        }
        return mFixedFilters.immutable();
    }

    public <P extends PropertyInterface, F extends PropertyInterface> ImSet<FilterEntity> getObjectFixedFilters(ClassFormEntity <T> editForm, GroupObjectInstance selectionGroupObject) {
        MSet<FilterEntity> mFixedFilters = SetFact.mSet();
        ObjectEntity object = editForm.object;
        for (FilterEntity filterEntity : entity.fixedFilters) {
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

    public Object read(CalcPropertyObjectInstance<?> property) throws SQLException {
        return property.read(this);
    }

    public DialogInstance<T> createObjectDialog(CustomClass objectClass) throws SQLException {
        ClassFormEntity<T> classForm = objectClass.getEditForm(BL.LM);
        return new DialogInstance<T>(classForm.form, logicsInstance, session, securityPolicy, getFocusListener(), getClassListener(), classForm.object, NullValue.instance, instanceFactory.computer, instanceFactory.connection);
    }

    public DialogInstance<T> createObjectEditorDialog(CalcPropertyValueImplement propertyValues) throws SQLException {
        CustomClass objectClass = propertyValues.getDialogClass(session);
        ClassFormEntity<T> classForm = objectClass.getEditForm(BL.LM);

        ObjectValue currentObject = propertyValues.readClasses(this);
/*        if (currentObject == null && objectClass instanceof ConcreteCustomClass) {
            currentObject = addObject((ConcreteCustomClass)objectClass).object;
        }*/

        return currentObject == null
                ? null
                : new DialogInstance<T>(classForm.form, logicsInstance, session, securityPolicy, getFocusListener(), getClassListener(), classForm.object, currentObject, instanceFactory.computer, instanceFactory.connection);
    }

    public DialogInstance<T> createChangeEditorDialog(CalcPropertyValueImplement propertyValues, GroupObjectInstance groupObject, CalcProperty filterProperty) throws SQLException {

        ClassFormEntity<T> formEntity = propertyValues.getDialogClass(session).getDialogForm(BL.LM);
        Result<ImSet<PullChangeProperty>> pullProps = new Result<ImSet<PullChangeProperty>>();
        ImSet<FilterEntity> additionalFilters = getEditFixedFilters(formEntity, propertyValues, groupObject, pullProps);

        ObjectEntity dialogObject = formEntity.object;
        DialogInstance<T> dialog = new DialogInstance<T>(formEntity.form, logicsInstance, session, securityPolicy, getFocusListener(), getClassListener(), dialogObject, propertyValues.readClasses(this), instanceFactory.computer, instanceFactory.connection, additionalFilters, pullProps.result);

        if (filterProperty != null) {
            dialog.initFilterPropertyDraw = formEntity.form.getPropertyDraw(filterProperty, dialogObject);
        }

        dialog.undecorated = BL.isDialogUndecorated();

        return dialog;
    }

    public DialogInstance<T> createChangeObjectDialog(CustomClass dialogClass, ObjectValue dialogValue, GroupObjectInstance groupObject, CalcProperty filterProperty) throws SQLException {

        ClassFormEntity<T> formEntity = dialogClass.getDialogForm(BL.LM);
        ImSet<FilterEntity> additionalFilters = getObjectFixedFilters(formEntity, groupObject);

        ObjectEntity dialogObject = formEntity.object;
        DialogInstance<T> dialog = new DialogInstance<T>(formEntity.form, logicsInstance, session, securityPolicy, getFocusListener(), getClassListener(), dialogObject, dialogValue, instanceFactory.computer, instanceFactory.connection, additionalFilters, SetFact.<PullChangeProperty>EMPTY());

        if (filterProperty != null) {
            dialog.initFilterPropertyDraw = formEntity.form.getPropertyDraw(filterProperty, dialogObject);
        }

        dialog.undecorated = BL.isDialogUndecorated();

        return dialog;
    }

    // ---------------------------------------- Events ----------------------------------------

    private void fireObjectChanged(ObjectInstance object) throws SQLException {
        fireEvent(object.entity);
    }

    public void fireOnInit() throws SQLException {
        fireEvent(FormEventType.INIT);
    }

    public void fireOnApply() throws SQLException {
        fireEvent(FormEventType.APPLY);
    }

    public void fireOnCancel() throws SQLException {
        fireEvent(FormEventType.CANCEL);
    }

    public void fireOnOk() throws SQLException {
        formResult = FormCloseType.OK;
        fireEvent(FormEventType.OK);
    }

    public void fireOnClose() throws SQLException {
        formResult = FormCloseType.CLOSE;
        fireEvent(FormEventType.CLOSE);
    }

    public void fireOnDrop() throws SQLException {
        formResult = FormCloseType.DROP;
        fireEvent(FormEventType.DROP);
    }

    public void fireQueryOk() throws SQLException {
        fireEvent(FormEventType.QUERYOK);
    }

    public void fireQueryClose() throws SQLException {
        fireEvent(FormEventType.QUERYCLOSE);
    }

    private void fireEvent(Object eventObject) throws SQLException {
        List<ActionPropertyObjectEntity<?>> actionsOnEvent = entity.getActionsOnEvent(eventObject);
        if (actionsOnEvent != null) {
            for (ActionPropertyObjectEntity<?> autoAction : actionsOnEvent) {
                ActionPropertyObjectInstance<? extends PropertyInterface> autoInstance = instanceFactory.getInstance(autoAction);
                if (autoInstance.isInInterface(null) && securityPolicy.property.change.checkPermission(autoAction.property)) { // для проверки null'ов и политики безопасности
                    FlowResult result = autoInstance.execute(this);
                    if (result != FlowResult.FINISH)
                        return;
                }
            }
        }
    }

    private FormCloseType formResult = FormCloseType.DROP;

    public FormCloseType getFormResult() {
        return formResult;
    }

    public DataSession getSession() {
        return session;
    }

    private final IncrementChangeProps environmentIncrement;

    private SessionModifier createModifier() {
        FunctionSet<CalcProperty> noHints = getNoHints();
        return new OverrideSessionModifier(environmentIncrement, noHints, noHints, entity.getHintsIncrementTable(), entity.getHintsNoUpdate(), session.getModifier());
    }

    public Map<SessionModifier, SessionModifier> modifiers = new HashMap<SessionModifier, SessionModifier>();
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

    public void onQueryClose() throws SQLException {
        fireQueryClose();
    }

    public void onQueryOk() throws SQLException {
        fireQueryOk();
    }

    public void formApply() throws SQLException {
        apply(BL);
    }

    public void formCancel() throws SQLException {
        int result = (Integer) ThreadLocalContext.requestUserInteraction(new ConfirmClientAction("lsFusion", getString("form.do.you.really.want.to.undo.changes")));
        if (result == JOptionPane.YES_OPTION) {
            cancel();
        }
    }

    public void formClose() throws SQLException {
        if (manageSession && session.hasStoredChanges()) {
            int result = (Integer) ThreadLocalContext.requestUserInteraction(new ConfirmClientAction("lsFusion", getString("form.do.you.really.want.to.close.form")));
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        fireOnClose();
        ThreadLocalContext.delayUserInteraction(new HideFormClientAction());
        close();
    }

    public void formDrop() throws SQLException {
        fireOnDrop();

        ThreadLocalContext.delayUserInteraction(new HideFormClientAction());
        close();
    }

    public void formOk() throws SQLException {
        if (checkOnOk) {
            if (!checkApply()) {
                return;
            }
        }

        fireOnOk();

        if (manageSession && !apply(null)) {
            return;
        }

        ThreadLocalContext.delayUserInteraction(new HideFormClientAction());
        close();
    }

    public void formRefresh() throws SQLException {
        refreshData();
    }
}
