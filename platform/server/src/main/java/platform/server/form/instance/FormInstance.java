package platform.server.form.instance;

import com.google.common.base.Throwables;
import platform.base.*;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.FormEventType;
import platform.interop.Scroll;
import platform.interop.action.ConfirmClientAction;
import platform.interop.action.EditNotPerformedClientAction;
import platform.interop.action.HideFormClientAction;
import platform.interop.action.LogMessageClientAction;
import platform.interop.form.FormUserPreferences;
import platform.interop.form.ColumnUserPreferences;
import platform.interop.form.GroupObjectUserPreferences;
import platform.interop.form.ServerResponse;
import platform.interop.form.layout.ContainerType;
import platform.server.Context;
import platform.server.Message;
import platform.server.ParamMessage;
import platform.server.Settings;
import platform.server.auth.SecurityPolicy;
import platform.server.caches.ManualLazy;
import platform.server.classes.*;
import platform.server.data.Modify;
import platform.server.data.QueryEnvironment;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.data.type.ParseException;
import platform.server.data.type.Type;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.entity.filter.NotFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.RegularFilterGroupInstance;
import platform.server.form.instance.filter.RegularFilterInstance;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.form.view.ComponentView;
import platform.server.form.view.ContainerView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.flow.FlowResult;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.logics.property.derived.OnChangeProperty;
import platform.server.session.*;

import javax.swing.*;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

import static platform.base.BaseUtils.mergeSet;
import static platform.base.BaseUtils.toOrderedMap;
import static platform.interop.ClassViewType.GRID;
import static platform.interop.ClassViewType.HIDE;
import static platform.interop.Order.*;
import static platform.server.form.instance.GroupObjectInstance.*;
import static platform.server.logics.ServerResourceBundle.getString;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который
//  разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class FormInstance<T extends BusinessLogics<T>> extends ExecutionEnvironment {

    public final T BL;

    public final FormEntity<T> entity;

    public final InstanceFactory instanceFactory;

    public final SecurityPolicy securityPolicy;

    private Modifier modifier;

    private Map<ObjectEntity, ? extends ObjectValue> mapObjects = null;
    public final List<GroupObjectInstance> groups = new ArrayList<GroupObjectInstance>();
    public final List<TreeGroupInstance> treeGroups = new ArrayList<TreeGroupInstance>();

    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    public final List<PropertyDrawInstance> properties = new ArrayList<PropertyDrawInstance>();

    private Collection<ObjectInstance> objects;

    public final boolean checkOnOk;

    public final boolean isModal;

    public final boolean manageSession;

    private boolean interactive = true; // важно для assertion'а в endApply

    // для импорта конструктор, объекты пустые
    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer) throws SQLException {
        this(entity, BL, session, securityPolicy, focusListener, classListener, computer, new HashMap<ObjectEntity, DataObject>(), false, true, false, false, null);
    }

    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer, Map<ObjectEntity, ? extends ObjectValue> mapObjects, boolean isModal, boolean manageSession, boolean checkOnOk, boolean interactive, Set<FilterEntity> additionalFixedFilters) throws SQLException {
        this.manageSession = manageSession;
        this.isModal = isModal;
        this.checkOnOk = checkOnOk;

        this.session = session;
        this.entity = entity;
        this.BL = BL;
        this.securityPolicy = securityPolicy;

        instanceFactory = new InstanceFactory(computer);

        this.weakFocusListener = new WeakReference<FocusListener<T>>(focusListener);
        this.weakClassListener = new WeakReference<CustomClassListener>(classListener);

        for (int i = 0; i < entity.groups.size(); i++) {
            GroupObjectInstance groupObject = instanceFactory.getInstance(entity.groups.get(i));
            groupObject.order = i;
            groupObject.setClassListener(classListener);
            groups.add(groupObject);
        }

        for (TreeGroupEntity treeGroup : entity.treeGroups) {
            treeGroups.add(instanceFactory.getInstance(treeGroup));
        }

        for (PropertyDrawEntity<?> propertyDrawEntity : entity.propertyDraws)
            if (this.securityPolicy.property.view.checkPermission(propertyDrawEntity.propertyObject.property)) {
                PropertyDrawInstance propertyDrawInstance = instanceFactory.getInstance(propertyDrawEntity);
                if (propertyDrawInstance.toDraw == null) // для Instance'ов проставляем не null, так как в runtime'е порядок меняться не будет
                    propertyDrawInstance.toDraw = instanceFactory.getInstance(propertyDrawEntity.getToDraw(entity));
                properties.add(propertyDrawInstance);
            }

        Set<FilterEntity> allFixedFilters = additionalFixedFilters == null
                ? entity.fixedFilters
                : mergeSet(entity.fixedFilters, additionalFixedFilters);
        for (FilterEntity filterEntity : allFixedFilters) {
            FilterInstance filter = filterEntity.getInstance(instanceFactory);
            filter.getApplyObject().fixedFilters.add(filter);
        }

        for (RegularFilterGroupEntity filterGroupEntity : entity.regularFilterGroups) {
            regularFilterGroups.add(instanceFactory.getInstance(filterGroupEntity));
        }

        for (Entry<OrderEntity<?>, Boolean> orderEntity : entity.fixedOrders.entrySet()) {
            OrderInstance orderInstance = orderEntity.getKey().getInstance(instanceFactory);
            orderInstance.getApplyObject().fixedOrders.put(orderInstance, orderEntity.getValue());
        }

        // в первую очередь ставим на объекты из cache'а
        if (classListener != null) {
            for (GroupObjectInstance groupObject : groups) {
                for (ObjectInstance object : groupObject.objects)
                    if (object.getBaseClass() instanceof CustomClass) {
                        Integer objectID = classListener.getObject((CustomClass) object.getBaseClass());
                        if (objectID != null)
                            groupObject.addSeek(object, session.getDataObject(objectID, ObjectType.instance), false);
                    }
            }
        }

        for (Entry<ObjectEntity, ? extends ObjectValue> mapObject : mapObjects.entrySet()) {
            ObjectInstance instance = instanceFactory.getInstance(mapObject.getKey());
            instance.groupTo.addSeek(instance, mapObject.getValue(), false);
        }

        //устанавливаем фильтры и порядки по умолчанию...
        for (RegularFilterGroupInstance filterGroup : regularFilterGroups) {
            int defaultInd = filterGroup.entity.defaultFilter;
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
        fireOnInit();

        if (!interactive) {
            endApply();
            this.mapObjects = mapObjects;
        }

        this.interactive = interactive; // обязательно в конце чтобы assertion с endApply не рушить
    }

    private static IncrementChangeProps createEnvironmentIncrement(boolean isModal, boolean isDialog, boolean manageSession, boolean isReadOnly) {
        IncrementChangeProps environment = new IncrementChangeProps();
        environment.add(FormEntity.isModal, PropertyChange.<ClassPropertyInterface>STATIC(isModal));
        environment.add(FormEntity.isDialog, PropertyChange.<ClassPropertyInterface>STATIC(isDialog));
        environment.add(FormEntity.manageSession, PropertyChange.<ClassPropertyInterface>STATIC(manageSession));
        environment.add(FormEntity.isReadOnly, PropertyChange.<ClassPropertyInterface>STATIC(isReadOnly));
        return environment;
    }

    public FormUserPreferences loadUserPreferences() {
        List<GroupObjectUserPreferences> preferences = new ArrayList<GroupObjectUserPreferences>();
        try {

            ObjectValue formValue = BL.LM.SIDToNavigatorElement.readClasses(session, new DataObject(entity.getSID(), StringClass.get(50)));
            if (formValue.isNull())
                return null;
            DataObject formObject = (DataObject) formValue;

            KeyExpr propertyDrawExpr = new KeyExpr("propertyDraw");

            Integer userId = (Integer) BL.LM.currentUser.read(session);
            DataObject currentUser = session.getDataObject(userId, ObjectType.instance);

            Expr customUserExpr = currentUser.getExpr();

            Map<Object, KeyExpr> newKeys = new HashMap<Object, KeyExpr>();
            newKeys.put("propertyDraw", propertyDrawExpr);

            Query<Object, Object> query = new Query<Object, Object>(newKeys);
            query.properties.put("propertyDrawSID", BL.LM.propertyDrawSID.getExpr(propertyDrawExpr));
            query.properties.put("nameShowOverridePropertyDrawCustomUser", BL.LM.nameShowOverridePropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.properties.put("columnWidthOverridePropertyDrawCustomUser", BL.LM.columnWidthOverridePropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.properties.put("columnOrderOverridePropertyDrawCustomUser", BL.LM.columnOrderOverridePropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.properties.put("columnSortOverridePropertyDrawCustomUser", BL.LM.columnSortOverridePropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.properties.put("columnAscendingSortOverridePropertyDrawCustomUser", BL.LM.columnAscendingSortOverridePropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.properties.put("groupObjectPropertyDraw", BL.LM.groupObjectPropertyDraw.getExpr(propertyDrawExpr));
            query.and(BL.LM.formPropertyDraw.getExpr(propertyDrawExpr).compare(formObject.getExpr(), Compare.EQUALS));

            OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);

            for (Map<Object, Object> values : result.values()) {
                String propertyDrawSID = values.get("propertyDrawSID").toString().trim();
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
                    String groupObjectSID = (String) BL.LM.groupObjectSID.read(session, new DataObject(groupObjectPropertyDraw, BL.LM.groupObject));
                    ColumnUserPreferences pref = new ColumnUserPreferences(needToHide, width, order, sort, ascendingSort != null ? ascendingSort : (sort != null ? false : null));
                    boolean found = false;
                    Object hasUserPreferences = BL.LM.hasUserPreferencesOverrideGroupObjectCustomUser.read(session, new DataObject(groupObjectPropertyDraw, BL.LM.groupObject), currentUser);
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
            DataObject userObject = dataSession.getDataObject(BL.LM.currentUser.read(dataSession), ObjectType.instance);
            for (GroupObjectUserPreferences groupObjectPreferences : preferences.getGroupObjectUserPreferencesList()) {
                for (Map.Entry<String, ColumnUserPreferences> entry : groupObjectPreferences.getColumnUserPreferences().entrySet()) {
                    Integer id = (Integer) BL.LM.SIDNavigatorElementSIDPropertyDrawToPropertyDraw.read(dataSession, new DataObject(entity.getSID(), StringClass.get(50)), new DataObject(entry.getKey(), StringClass.get(50)));
                    DataObject propertyDrawObject = dataSession.getDataObject(id, ObjectType.instance);
                    if (entry.getValue().isNeedToHide() != null) {
                        int idShow = entry.getValue().isNeedToHide() ? BL.LM.propertyDrawShowStatus.getID("Hide") : BL.LM.propertyDrawShowStatus.getID("Show");
                        BL.LM.showPropertyDrawCustomUser.change(idShow, dataSession, propertyDrawObject, userObject);
                        if (forAllUsers)
                            BL.LM.showPropertyDraw.change(idShow, dataSession, propertyDrawObject, userObject);
                    }
                    BL.LM.columnWidthPropertyDrawCustomUser.change(entry.getValue().getWidthUser(), dataSession, propertyDrawObject, userObject);
                    BL.LM.columnOrderPropertyDrawCustomUser.change(entry.getValue().getOrderUser(), dataSession, propertyDrawObject, userObject);
                    if (entry.getValue().getAscendingSortUser() != null) {
                        BL.LM.columnSortPropertyDrawCustomUser.change(entry.getValue().getSortUser(), dataSession, propertyDrawObject, userObject);
                        BL.LM.columnAscendingSortPropertyDrawCustomUser.change(entry.getValue().getAscendingSortUser(), dataSession, propertyDrawObject, userObject);
                        if (forAllUsers) {
                            BL.LM.columnSortPropertyDraw.change(entry.getValue().getSortUser(), dataSession, propertyDrawObject, userObject);
                            BL.LM.columnAscendingSortPropertyDraw.change(entry.getValue().getAscendingSortUser(), dataSession, propertyDrawObject, userObject);
                        }
                    }
                    if (forAllUsers) {
                        BL.LM.columnWidthPropertyDraw.change(entry.getValue().getWidthUser(), dataSession, propertyDrawObject);
                        BL.LM.columnOrderPropertyDraw.change(entry.getValue().getOrderUser(), dataSession, propertyDrawObject);
                    }
                }
                DataObject groupObjectObject = dataSession.getDataObject(BL.LM.SIDNavigatorElementSIDGroupObjectToGroupObject.read(dataSession, new DataObject(groupObjectPreferences.groupObjectSID, StringClass.get(50)), new DataObject(entity.getSID(), StringClass.get(50))), ObjectType.instance);
                BL.LM.hasUserPreferencesGroupObjectCustomUser.change(true, dataSession, groupObjectObject, userObject);
                if (forAllUsers)
                    BL.LM.hasUserPreferencesGroupObject.change(true, dataSession, groupObjectObject);
            }
            dataSession.apply(BL);
        } catch (SQLException e) {
            Throwables.propagate(e);
        }
    }

    public boolean areObjectsFound() {
        assert !interactive;
        for (Entry<ObjectEntity, ? extends ObjectValue> mapObjectInstance : mapObjects.entrySet())
            if (!instanceFactory.getInstance(mapObjectInstance.getKey()).getObjectValue().equals(mapObjectInstance.getValue()))
                return false;
        return true;
    }

    protected FunctionSet<CalcProperty> getNoHints() {
        if (Settings.instance.isDisableChangeModifierAllHints())
            return BaseUtils.universal(entity.getChangeModifierProps().isEmpty());
        else
            return CalcProperty.getDependsSet(entity.getChangeModifierProps()); // тут какая то проблема есть
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
    public Collection<ObjectInstance> getObjects() {
        if (objects == null) {
            objects = new ArrayList<ObjectInstance>();
            for (GroupObjectInstance group : groups)
                for (ObjectInstance object : group.objects)
                    objects.add(object);
        }
        return objects;
    }

    public void addFixedFilter(FilterEntity newFilter) {
        FilterInstance newFilterInstance = newFilter.getInstance(instanceFactory);
        newFilterInstance.getApplyObject().fixedFilters.add(newFilterInstance);
    }

    // ----------------------------------- Поиск объектов по ID ------------------------------ //
    public GroupObjectInstance getGroupObjectInstance(int groupID) {
        for (GroupObjectInstance groupObject : groups)
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
            if (property.equals(propertyDraw.propertyObject.property) && (group==null || group.equals(propertyDraw.toDraw)))
                return propertyDraw;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(Property<?> property) {
        return getPropertyDraw(property, (GroupObjectInstance)null);
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

    public void expandGroupObject(GroupObjectInstance group, Map<ObjectInstance, DataObject> value) throws SQLException {
        if(group.expandTable==null)
            group.expandTable = group.createKeyTable();
        group.expandTable.modifyRecord(session.sql, value, Modify.MODIFY);
        group.updated |= UPDATED_EXPANDS;
    }

    public void collapseGroupObject(GroupObjectInstance group, Map<ObjectInstance, DataObject> value) throws SQLException {
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
                Map<ObjectInstance, DataObject> value = new HashMap<ObjectInstance, DataObject>();
                for (ObjectInstance obj : groupObject.objects) {
                    ObjectValue objectValue = obj.getObjectValue();
                    if (objectValue instanceof DataObject)
                        value.put(obj, (DataObject)objectValue);
                }
                if (!value.isEmpty())
                    expandGroupObject(groupObject, value);
            } else {
                // раскрываем все верхние groupObject
                for (GroupObjectInstance group : groups) {
                    List<GroupObjectInstance> upGroups = group.getUpTreeGroups();
                    Map<ObjectInstance, DataObject> value = new HashMap<ObjectInstance, DataObject>();
                    int upObjects = 0;
                    for (GroupObjectInstance goi : upGroups) {
                        if (goi != null && !goi.equals(group)) {
                            upObjects += goi.objects.size();
                            value.putAll(goi.getGroupObjectValue());
                        }
                    }
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
    private boolean checkFilters(GroupObjectInstance groupTo) {
        Set<FilterInstance> filters = new HashSet<FilterInstance>();
        for(FilterInstance filter : groupTo.getSetFilters())
            if (!FilterInstance.ignoreInInterface || filter.isInInterface(groupTo))
                filters.add(filter);
        return filters.equals(groupTo.filters);
    }
    
    public DataObject addFormObject(CustomObjectInstance object, ConcreteCustomClass cls, DataObject pushed) throws SQLException {
        DataObject dataObject = session.addObject(cls, pushed);

        // резолвим все фильтры
        assert checkFilters(object.groupTo);
        for (FilterInstance filter : object.groupTo.filters)
            filter.resolveAdd(this, object, dataObject);

        for (LP lp : BL.LM.lproperties) {
            if(lp instanceof LCP) {
                LCP<?> lcp = (LCP<?>) lp;
                CalcProperty<?> property = lcp.property;
                if (property.autoset) {
                    ValueClass interfaceClass = BaseUtils.singleValue(property.getInterfaceClasses());
                    ValueClass valueClass = property.getValueClass();
                    if (valueClass instanceof CustomClass && interfaceClass instanceof CustomClass&&
                            cls.isChild((CustomClass) interfaceClass)) { // в общем то для оптимизации
                        Integer obj = getClassListener().getObject((CustomClass) valueClass);
                        if(obj!=null)
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

    public void executeEditAction(PropertyDrawInstance property, String editActionSID, Map<ObjectInstance, DataObject> keys) throws SQLException {
        executeEditAction(property, editActionSID, keys, null, null, false);
    }

    public void executeEditAction(PropertyDrawInstance property, String editActionSID, Map<ObjectInstance, DataObject> keys, ObjectValue pushChange, DataObject pushAdd, boolean pushConfirm) throws SQLException {
        if (property.propertyReadOnly != null && property.propertyReadOnly.getRemappedPropertyObject(keys).read(this) != null) {
            Context.context.get().delayUserInteraction(EditNotPerformedClientAction.instance);
            return;
        }

        if (editActionSID.equals(ServerResponse.CHANGE) || editActionSID.equals(ServerResponse.GROUP_CHANGE)) { //ask confirm logics...
            PropertyDrawEntity propertyDraw = property.getEntity();
            if (!pushConfirm && propertyDraw.askConfirm) {
                int result = (Integer) Context.context.get().requestUserInteraction(new ConfirmClientAction("LS Fusion",
                        entity.getRichDesign().get(propertyDraw).getAskConfirmMessage()));
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
        }

        ActionPropertyObjectInstance editAction = property.getEditAction(editActionSID, instanceFactory, entity);
        if (editAction != null) {
            editAction.getRemappedPropertyObject(keys).execute(this, pushChange, pushAdd, property);
        } else {
            Context.context.get().delayUserInteraction(EditNotPerformedClientAction.instance);
        }
    }

    public void pasteExternalTable(List<Integer> propertyIDs, List<List<Object>> table) throws SQLException {
        List<PropertyDrawInstance> properties = new ArrayList<PropertyDrawInstance>();
        for (Integer id : propertyIDs) {
            properties.add(getPropertyDraw(id));
        }
        GroupObjectInstance groupObject = properties.get(0).toDraw;
        List<Map<ObjectInstance, DataObject>> executeList = groupObject.seekObjects(session.sql, getQueryEnv(), getModifier(), BL.LM.baseClass, table.size()).keyList();

        //создание объектов
        int availableQuantity = executeList.size();
        if (availableQuantity < table.size()) {
            executeList = BaseUtils.mergeList(executeList, groupObject.createObjects(session, this, table.size() - availableQuantity));
        }

        for (int i = 0; i < properties.size(); i++) {
            PropertyDrawInstance property = properties.get(i);
            Type propertyType = property.propertyObject.getType();

            OrderedMap<Map<ObjectInstance, DataObject>, ObjectValue> pasteRows = new OrderedMap<Map<ObjectInstance, DataObject>, ObjectValue>();
            for (int j = 0; j < executeList.size(); j++) {
                pasteRows.put(executeList.get(j), session.getObjectValue(table.get(j).get(i), propertyType));
            }

            executePasteAction(property, pasteRows);
        }
    }

    private List<Map<ObjectInstance, DataObject>> readObjects(List<Map<Integer, Object>> keyIds) throws SQLException {

        List<Map<ObjectInstance, DataObject>> result = new ArrayList<Map<ObjectInstance, DataObject>>();
        for(Map<Integer, Object> keyId : keyIds) {
            Map<ObjectInstance, DataObject> key = new HashMap<ObjectInstance, DataObject>();
            for (Entry<Integer, Object> objectId : keyId.entrySet()) {
                ObjectInstance objectInstance = getObjectInstance(objectId.getKey());
                key.put(objectInstance, session.getDataObject(objectId.getValue(), objectInstance.getType()));
            }
            result.add(key);
        }
        return result;
    }

    public void pasteMulticellValue(Map<Integer, List<Map<Integer, Object>>> cells, Object value) throws SQLException {
        for (Integer propertyId : cells.keySet()) { // бежим по ячейкам
            PropertyDrawInstance property = getPropertyDraw(propertyId);

            Type type = property.propertyObject.getType();
            Object parseValue;
            try {
                parseValue = type.parseString((String) value);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            executePasteAction(property, toOrderedMap(readObjects(cells.get(propertyId)), session.getObjectValue(parseValue, type)));
        }
    }

    private void executePasteAction(PropertyDrawInstance property, OrderedMap<Map<ObjectInstance, DataObject>, ObjectValue> pasteRows) throws SQLException {
        if (!pasteRows.isEmpty()) {
            assert new HashSet<ObjectInstance>(property.toDraw.objects).equals(pasteRows.keySet().iterator().next().keySet());

            for (Map.Entry<Map<ObjectInstance, DataObject>, ObjectValue> row : pasteRows.entrySet()) {
                executeEditAction(property, ServerResponse.CHANGE_WYS, row.getKey(), row.getValue(), null, true);
            }
        }
    }

    public int countRecords(int groupObjectID) throws SQLException {
        GroupObjectInstance group = getGroupObjectInstance(groupObjectID);
        Expr expr = GroupExpr.create(new HashMap(), new ValueExpr(1, IntegerClass.instance), group.getWhere(group.getMapKeys(), getModifier()), GroupType.SUM, new HashMap());
        Query<Object, Object> query = new Query<Object, Object>(new HashMap<Object, KeyExpr>());
        query.properties.put("quant", expr);
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(this);
        Integer quantity = (Integer) result.getValue(0).get("quant");
        if (quantity != null) {
            return quantity;
        } else {
            return 0;
        }
    }

    public Object calculateSum(PropertyDrawInstance propertyDraw, Map<ObjectInstance, DataObject> columnKeys) throws SQLException {
        GroupObjectInstance groupObject = propertyDraw.toDraw;

        Map<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();
        Map<ObjectInstance, Expr> keys = new HashMap<ObjectInstance, Expr>(mapKeys);

        for (ObjectInstance object : columnKeys.keySet()) {
            keys.put(object, columnKeys.get(object).getExpr());
        }
        Expr expr = GroupExpr.create(new HashMap(), propertyDraw.getDrawInstance().getExpr(keys, getModifier()), groupObject.getWhere(mapKeys, getModifier()), GroupType.SUM, new HashMap());

        Query<Object, Object> query = new Query<Object, Object>(new HashMap<Object, KeyExpr>());
        query.properties.put("sum", expr);
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);
        return result.getValue(0).get("sum");
    }

    public Map<List<Object>, List<Object>> groupData(Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> toGroup,
                                                     Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> toSum,
                                                     Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> toMax, boolean onlyNotNull) throws SQLException {
        GroupObjectInstance groupObject = ((PropertyDrawInstance) toGroup.keySet().toArray()[0]).toDraw;
        Map<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();

        Map<Object, KeyExpr> keyExprMap = new HashMap<Object, KeyExpr>();
        Map<Object, Expr> exprMap = new HashMap<Object, Expr>();
        for (PropertyDrawInstance property : toGroup.keySet()) {
            int i = 0;
            for (Map<ObjectInstance, DataObject> columnKeys : toGroup.get(property)) {
                i++;
                Map<ObjectInstance, Expr> keys = new HashMap<ObjectInstance, Expr>(mapKeys);
                for (ObjectInstance object : columnKeys.keySet()) {
                    keys.put(object, columnKeys.get(object).getExpr());
                }
                keyExprMap.put(property.getsID() + i, new KeyExpr("expr"));
                exprMap.put(property.getsID() + i, property.getDrawInstance().getExpr(keys, getModifier()));
            }
        }

        Query<Object, Object> query = new Query<Object, Object>(keyExprMap);
        Expr exprQuant = GroupExpr.create(exprMap, new ValueExpr(1, IntegerClass.instance), groupObject.getWhere(mapKeys, getModifier()), GroupType.SUM, keyExprMap);
        query.and(exprQuant.getWhere());

        int separator = toSum.size();
        int idIndex = 0;
        for (int i = 0; i < toSum.size() + toMax.size(); i++) {
            Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> currentMap;
            int index;
            GroupType groupType;
            if (i < separator) {
                currentMap = toSum;
                groupType = GroupType.SUM;
                index = i;
            } else {
                currentMap = toMax;
                groupType = GroupType.MAX;
                index = i - separator;
            }
            PropertyDrawInstance property = (PropertyDrawInstance) currentMap.keySet().toArray()[index];
            if (property == null) {
                query.properties.put("quant", exprQuant);
                continue;
            }
            for (Map<ObjectInstance, DataObject> columnKeys : currentMap.get(property)) {
                idIndex++;
                Map<ObjectInstance, Expr> keys = new HashMap<ObjectInstance, Expr>(mapKeys);
                for (ObjectInstance object : columnKeys.keySet()) {
                    keys.put(object, columnKeys.get(object).getExpr());
                }
                Expr expr = GroupExpr.create(exprMap, property.getDrawInstance().getExpr(keys, getModifier()), groupObject.getWhere(mapKeys, getModifier()), groupType, keyExprMap);
                query.properties.put(property.getsID() + idIndex, expr);
                if (onlyNotNull) {
                    query.and(expr.getWhere());
                }
            }
        }

        Map<List<Object>, List<Object>> resultMap = new OrderedMap<List<Object>, List<Object>>();
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(this);
        for (Map<Object, Object> one : result.keyList()) {
            List<Object> groupList = new ArrayList<Object>();
            List<Object> sumList = new ArrayList<Object>();

            for (PropertyDrawInstance propertyDraw : toGroup.keySet()) {
                for (int i = 1; i <= toGroup.get(propertyDraw).size(); i++) {
                    groupList.add(one.get(propertyDraw.getsID() + i));
                }
            }
            int index = 1;
            for (PropertyDrawInstance propertyDraw : toSum.keySet()) {
                if (propertyDraw == null) {
                    sumList.add(result.get(one).get("quant"));
                    continue;
                }
                for (int i = 1; i <= toSum.get(propertyDraw).size(); i++) {
                    sumList.add(result.get(one).get(propertyDraw.getsID() + index));
                    index++;
                }
            }
            for (PropertyDrawInstance propertyDraw : toMax.keySet()) {
                for (int i = 1; i <= toMax.get(propertyDraw).size(); i++) {
                    sumList.add(result.get(one).get(propertyDraw.getsID() + index));
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
        return session.check(BL);
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
        boolean succeeded = session.apply(BL);

        if (!succeeded)
            return false;

        refreshData();
        fireOnApply();

        dataChanged = true; // временно пока applyChanges синхронен, для того чтобы пересылался факт изменения данных

        Context.context.get().delayUserInteraction(new LogMessageClientAction(getString("form.instance.changes.saved"), false));
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
        if(focusListener!=null)
            focusListener.gainedFocus(this);
    }

    private boolean closed = false;
    public void close() throws SQLException {
        closed = true;
        session.incrementChanges.remove(this);
        for (GroupObjectInstance group : groups) {
            if(group.keyTable!=null)
                group.keyTable.drop(session.sql);
            if(group.expandTable!=null)
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

    public FormInstance<T> createForm(FormEntity<T> form, Map<ObjectEntity, DataObject> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOK, boolean interactive) throws SQLException {
        return new FormInstance<T>(form, BL,
                                   sessionScope.isNewSession() ? session.createSession() : session,
                                   securityPolicy, getFocusListener(), getClassListener(), instanceFactory.computer, mapObjects, isModal, sessionScope.isManageSession(),
                                   checkOnOK, interactive, null);
    }

    public void forceChangeObject(ObjectInstance object, ObjectValue value) throws SQLException {

        if(object instanceof DataObjectInstance && !(value instanceof DataObject))
            object.changeValue(session, ((DataObjectInstance)object).getBaseClass().getDefaultObjectValue());
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

        if(entity.eventActions.size() > 0) { // дебилизм конечно но пока так
            forceChangeObject(object, value);
        } else {
            object.groupTo.addSeek(object, value, false);
        }
    }

    public void changeObject(PropertyObjectInterfaceInstance objectInstance, ObjectValue objectValue) throws SQLException {
        if(objectInstance instanceof ObjectInstance) {
            ObjectInstance object = (ObjectInstance) objectInstance;

            seekObject(object, objectValue);
            fireObjectChanged(object); // запускаем все Action'ы, которые следят за этим объектом
        }
    }

    // "закэшированная" проверка присутствия в интерфейсе, отличается от кэша тем что по сути функция от mutable объекта
    protected Map<PropertyDrawInstance, Boolean> isInInterface = new HashMap<PropertyDrawInstance, Boolean>();

    // проверки видимости (для оптимизации pageframe'ов)
    protected Set<PropertyReaderInstance> pendingHidden = new HashSet<PropertyReaderInstance>();

    private boolean isHidden(PropertyDrawInstance<?> property, boolean grid) {
        if (Settings.instance.isDisableTabbedOptimization())
            return false;

        ComponentView container = entity.getDrawTabContainer(property.entity, grid);
        return container != null && isHidden(container); // первая проверка - cheat / оптимизация
    }

    private boolean isHidden(GroupObjectInstance group) {
        if (Settings.instance.isDisableTabbedOptimization())
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
        assert parent.type == ContainerType.TABBED_PANE;

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
        assert view.type == ContainerType.TABBED_PANE;
        visibleTabs.put(view, page);
    }

    boolean refresh = true;

    private boolean classUpdated(Updated updated, GroupObjectInstance groupObject) {
        return updated.classUpdated(Collections.singleton(groupObject));
    }

    private boolean objectUpdated(Updated updated, GroupObjectInstance groupObject) {
        return updated.objectUpdated(Collections.singleton(groupObject));
    }

    private boolean objectUpdated(Updated updated, Set<GroupObjectInstance> groupObjects) {
        return updated.objectUpdated(groupObjects);
    }

    private boolean propertyUpdated(CalcPropertyObjectInstance updated, Set<GroupObjectInstance> groupObjects, FunctionSet<CalcProperty> changedProps) {
        return dataUpdated(updated, changedProps)
                || groupUpdated(groupObjects, UPDATED_KEYS)
                || objectUpdated(updated, groupObjects);
    }

    private boolean groupUpdated(Collection<GroupObjectInstance> groupObjects, int flags) {
        for (GroupObjectInstance groupObject : groupObjects)
            if ((groupObject.updated & flags) != 0)
                return true;
        return false;
    }

    private boolean dataUpdated(Updated updated, FunctionSet<CalcProperty> changedProps) {
        return updated.dataUpdated(changedProps);
    }

    void applyFilters() {
        for (GroupObjectInstance group : groups)
            group.filters = group.getSetFilters();
    }

    void applyOrders() {
        for (GroupObjectInstance group : groups)
            group.orders = group.getSetOrders();
    }

    private static class GroupObjectValue {
        private GroupObjectInstance group;
        private Map<ObjectInstance, DataObject> value;

        private GroupObjectValue(GroupObjectInstance group, Map<ObjectInstance, DataObject> value) {
            this.group = group;
            this.value = value;
        }
    }

    @Message("message.form.update.props")
    private void updateDrawProps(FormChanges result, Set<GroupObjectInstance> keyGroupObjects, @ParamMessage Set<PropertyReaderInstance> propertyList) throws SQLException {
        Query<ObjectInstance, PropertyReaderInstance> selectProps = new Query<ObjectInstance, PropertyReaderInstance>(GroupObjectInstance.getObjects(getUpTreeGroups(keyGroupObjects)));
        for (GroupObjectInstance keyGroup : keyGroupObjects) {
            NoPropertyTableUsage<ObjectInstance> groupTable = keyGroup.keyTable;
            selectProps.and(groupTable.getWhere(selectProps.mapKeys));
        }

        for (PropertyReaderInstance propertyReader : propertyList) {
            selectProps.properties.put(propertyReader, propertyReader.getPropertyObjectInstance().getExpr(selectProps.mapKeys, getModifier()));
        }

        OrderedMap<Map<ObjectInstance, DataObject>, Map<PropertyReaderInstance, ObjectValue>> queryResult = selectProps.executeClasses(this, BL.LM.baseClass);
        for (PropertyReaderInstance propertyReader : propertyList) {
            Map<Map<ObjectInstance, DataObject>, ObjectValue> propertyValues = new HashMap<Map<ObjectInstance, DataObject>, ObjectValue>();
            for (Entry<Map<ObjectInstance, DataObject>, Map<PropertyReaderInstance, ObjectValue>> resultRow : queryResult.entrySet())
                propertyValues.put(resultRow.getKey(), resultRow.getValue().get(propertyReader));
            result.properties.put(propertyReader, propertyValues);
        }
    }

    @Message("message.form.end.apply")
    public FormChanges endApply() throws SQLException {

        assert interactive;
        
        final FormChanges result = new FormChanges();

        if(closed)
            return result;

        QueryEnvironment queryEnv = getQueryEnv();

        // если изменились данные, применяем изменения
        FunctionSet<CalcProperty> changedProps;
        if (dataChanged) {
            session.executeSessionEvents();
            changedProps = CalcProperty.getDependsSet(session.update(this));
        } else
            changedProps = EmptyFunctionSet.instance();

        GroupObjectValue updateGroupObject = null; // так как текущий groupObject идет относительно treeGroup, а не group
        for (GroupObjectInstance group : groups) {
            Map<ObjectInstance, DataObject> selectObjects = group.updateKeys(session.sql, queryEnv, getModifier(), BL.LM.baseClass, isHidden(group), refresh, result, changedProps);
            if (selectObjects != null) // то есть нужно изменять объект
                updateGroupObject = new GroupObjectValue(group, selectObjects);

            if (group.getDownTreeGroups().size() == 0 && updateGroupObject != null) { // так как в tree группе currentObject друг на друга никак не влияют, то можно и нужно делать updateGroupObject в конце
                updateGroupObject.group.update(session, result, updateGroupObject.value);
                updateGroupObject = null;
            }
        }

        for (Entry<Set<GroupObjectInstance>, Set<PropertyReaderInstance>> entry : BaseUtils.groupSet(getChangedDrawProps(result, changedProps)).entrySet())
            updateDrawProps(result, entry.getKey(), entry.getValue());

        // сбрасываем все пометки
        for (GroupObjectInstance group : groups) {
            group.userSeeks = null;

            for (ObjectInstance object : group.objects)
                object.updated = 0;
            group.updated = 0;
        }
        refresh = false;
        dataChanged = false;

//        result.out(this);

        return result;
    }

    private void fillChangedReader(CalcPropertyObjectInstance<?> drawProperty, PropertyReaderInstance propertyReader, Set<GroupObjectInstance> columnGroupGrids, boolean hidden, boolean read, Map<PropertyReaderInstance, Set<GroupObjectInstance>> readProperties, FunctionSet<CalcProperty> changedProps) {
        if (drawProperty != null && (read || (!hidden && pendingHidden.contains(propertyReader)) || propertyUpdated(drawProperty, columnGroupGrids, changedProps))) {
            if (hidden)
                pendingHidden.add(propertyReader);
            else {
                readProperties.put(propertyReader, columnGroupGrids);
                pendingHidden.remove(propertyReader);
            }
        }
    }

    private Map<PropertyReaderInstance, Set<GroupObjectInstance>> getChangedDrawProps(FormChanges result, FunctionSet<CalcProperty> changedProps) {
        final Map<PropertyReaderInstance, Set<GroupObjectInstance>> readProperties = new HashMap<PropertyReaderInstance, Set<GroupObjectInstance>>();

        for (PropertyDrawInstance<?> drawProperty : properties) {
            ClassViewType curClassView = drawProperty.getCurClassView();
            if (curClassView == HIDE) continue;

            ClassViewType forceViewType = drawProperty.getForceViewType();
            if (forceViewType != null && forceViewType == HIDE) continue;

            Set<GroupObjectInstance> columnGroupGrids = new HashSet<GroupObjectInstance>();
            for (GroupObjectInstance columnGroup : drawProperty.columnGroupObjects)
                if (columnGroup.curClassView == GRID)
                    columnGroupGrids.add(columnGroup);

            Boolean inInterface = null;
            Set<GroupObjectInstance> drawGridObjects = null;
            if (curClassView == GRID && (forceViewType == null || forceViewType == GRID) &&
                    drawProperty.propertyObject.isInInterface(drawGridObjects = BaseUtils.addSet(columnGroupGrids, drawProperty.toDraw), forceViewType != null)) // в grid'е
                inInterface = true;
            else if (drawProperty.propertyObject.isInInterface(drawGridObjects = columnGroupGrids, false)) // в панели
                inInterface = false;

            Boolean previous = isInInterface.put(drawProperty, inInterface);
            if (inInterface != null) { // hidden проверка внутри чтобы вкладки если что уходили
                boolean hidden = isHidden(drawProperty, inInterface);

                boolean read = refresh || !inInterface.equals(previous) // если изменилось представление
                        || groupUpdated(drawProperty.columnGroupObjects, UPDATED_CLASSVIEW); // изменились группы в колонки (так как отбираются только GRID)

                // расширенный fillChangedReader, но есть часть специфики, поэтому дублируется
                if (read || (!hidden && pendingHidden.contains(drawProperty)) || propertyUpdated(drawProperty.getDrawInstance(), drawGridObjects, changedProps)) {
                    if (hidden) { // если спрятан
                        if (read) { // все равно надо отослать клиенту, так как влияет на наличие вкладки, но с "hidden" значениями
                            readProperties.put(drawProperty.hiddenReader, drawGridObjects);
                            if (!inInterface) // говорим клиенту, что свойство в панели
                                result.panelProperties.add(drawProperty);
                        }
                        pendingHidden.add(drawProperty); // помечаем что когда станет видимым надо будет обновить
                    } else {
                        readProperties.put(drawProperty, drawGridObjects);
                        if (!inInterface) // говорим клиенту что свойство в панели
                            result.panelProperties.add(drawProperty);
                        pendingHidden.remove(drawProperty);
                    }
                }

                // читаем всегда так как влияет на видимость, а соответственно на наличие вкладки (с hidden'ом избыточный функционал, но меньший, поэтому все же используем fillChangedReader)
                fillChangedReader(drawProperty.propertyCaption, drawProperty.captionReader, columnGroupGrids, false, read, readProperties, changedProps);

                fillChangedReader(drawProperty.propertyFooter, drawProperty.footerReader, columnGroupGrids, hidden, read, readProperties, changedProps);

                fillChangedReader(drawProperty.propertyBackground, drawProperty.backgroundReader, drawGridObjects, hidden, read, readProperties, changedProps);

                fillChangedReader(drawProperty.propertyForeground, drawProperty.foregroundReader, drawGridObjects, hidden, read, readProperties, changedProps);

            } else if (previous != null) // говорим клиенту что свойство надо удалить
                result.dropProperties.add(drawProperty);
        }

        for (GroupObjectInstance group : groups) { // читаем highlight'ы
            if (group.propertyBackground != null) {
                Set<GroupObjectInstance> gridGroups = (group.curClassView == GRID ? Collections.singleton(group) : new HashSet<GroupObjectInstance>());
                if (refresh || (group.updated & UPDATED_CLASSVIEW) != 0 || propertyUpdated(group.propertyBackground, gridGroups, changedProps))
                    readProperties.put(group.rowBackgroundReader, gridGroups);
            }
            if (group.propertyForeground != null) {
                Set<GroupObjectInstance> gridGroups = (group.curClassView == GRID ? Collections.singleton(group) : new HashSet<GroupObjectInstance>());
                if (refresh || (group.updated & UPDATED_CLASSVIEW) != 0 || propertyUpdated(group.propertyForeground, gridGroups, changedProps))
                    readProperties.put(group.rowForegroundReader, gridGroups);
            }
        }

        return readProperties;
    }

    // возвращает какие объекты на форме показываются
    private Set<GroupObjectInstance> getPropertyGroups() {

        Set<GroupObjectInstance> reportObjects = new HashSet<GroupObjectInstance>();
        for (GroupObjectInstance group : groups)
            if (group.curClassView != HIDE)
                reportObjects.add(group);

        return reportObjects;
    }

    // возвращает какие объекты на форме не фиксируются
    private Set<GroupObjectInstance> getClassGroups() {

        Set<GroupObjectInstance> reportObjects = new HashSet<GroupObjectInstance>();
        for (GroupObjectInstance group : groups)
            if (group.curClassView == GRID)
                reportObjects.add(group);

        return reportObjects;
    }

    public FormData getFormData(int orderTop) throws SQLException {
        List<PropertyDrawInstance> calcProps = new ArrayList<PropertyDrawInstance>();
        for (PropertyDrawInstance property : properties)
            if (property.propertyObject instanceof CalcPropertyObjectInstance)
                calcProps.add(property);
        return getFormData(calcProps, groups, orderTop);
    }

    public FormData getFormData(Collection<PropertyDrawInstance> propertyDraws, Collection<GroupObjectInstance> classGroups) throws SQLException {
        return getFormData(propertyDraws, classGroups, 0);
    }

    // считывает все данные с формы
    public FormData getFormData(Collection<PropertyDrawInstance> propertyDraws, Collection<GroupObjectInstance> classGroups, int orderTop) throws SQLException {

        applyFilters();
        applyOrders();

        // пока сделаем тупо получаем один большой запрос

        Query<ObjectInstance, Object> query = new Query<ObjectInstance, Object>(GroupObjectInstance.getObjects(classGroups));
        OrderedMap<Object, Boolean> queryOrders = new OrderedMap<Object, Boolean>();

        for (GroupObjectInstance group : groups) {

            if (classGroups.contains(group)) {

                // не фиксированные ключи
                query.and(group.getWhere(query.mapKeys, getModifier()));

                // закинем Order'ы
                for (Entry<OrderInstance, Boolean> order : group.orders.entrySet()) {
                    query.properties.put(order.getKey(), order.getKey().getExpr(query.mapKeys, getModifier()));
                    queryOrders.put(order.getKey(), order.getValue());
                }

                for (ObjectInstance object : group.objects) {
                    query.properties.put(object, object.getExpr(query.mapKeys, getModifier()));
                    queryOrders.put(object, false);
                }
            }
        }

        FormData result = new FormData();

        for (PropertyDrawInstance<?> property : propertyDraws)
            query.properties.put(property, property.getDrawInstance().getExpr(query.mapKeys, getModifier()));

        OrderedMap<Map<ObjectInstance, Object>, Map<Object, Object>> resultSelect = query.execute(this, queryOrders, orderTop);
        for (Entry<Map<ObjectInstance, Object>, Map<Object, Object>> row : resultSelect.entrySet()) {
            Map<ObjectInstance, Object> groupValue = new HashMap<ObjectInstance, Object>();
            for (GroupObjectInstance group : groups)
                for (ObjectInstance object : group.objects)
                    if (classGroups.contains(group))
                        groupValue.put(object, row.getKey().get(object));
                    else
                        groupValue.put(object, object.getObjectValue().getValue());

            Map<PropertyDrawInstance, Object> propertyValues = new HashMap<PropertyDrawInstance, Object>();
            for (PropertyDrawInstance property : propertyDraws)
                propertyValues.put(property, row.getValue().get(property));

            result.add(groupValue, propertyValues);
        }

        return result;
    }

    // pullProps чтобы запретить hint'ить
    public <P extends PropertyInterface, F extends PropertyInterface> Set<FilterEntity> getEditFixedFilters(ClassFormEntity<T> editForm, CalcPropertyValueImplement<P> implement, GroupObjectInstance selectionGroupObject, Collection<PullChangeProperty> pullProps) {
        Set<FilterEntity> fixedFilters = new HashSet<FilterEntity>();
        CalcProperty<P> implementProperty = (CalcProperty<P>) implement.property;

        for (MaxChangeProperty<?, P> constrainedProperty : implementProperty.getMaxChangeProperties(BL.getCheckConstrainedProperties(implementProperty))) {
            pullProps.add(constrainedProperty);
            fixedFilters.add(new NotFilterEntity(new NotNullFilterEntity<MaxChangeProperty.Interface<P>>(
                    constrainedProperty.getPropertyObjectEntity(implement.mapping, editForm.object))));
        }

        for (FilterEntity filterEntity : entity.fixedFilters) {
            FilterInstance filter = filterEntity.getInstance(instanceFactory);
            if (filter.getApplyObject() == selectionGroupObject) {
                for (CalcPropertyValueImplement<?> filterImplement : filter.getResolveChangeProperties(implementProperty)) {
                    OnChangeProperty<F, P> onChangeProperty = (OnChangeProperty<F, P>) ((CalcProperty) filterImplement.property).getOnChangeProperty((CalcProperty) implement.property);
                    pullProps.add(onChangeProperty);
                    fixedFilters.add(new NotNullFilterEntity<OnChangeProperty.Interface<F, P>>(
                            onChangeProperty.getPropertyObjectEntity((Map<F, DataObject>) filterImplement.mapping, implement.mapping, editForm.object)));
                }
            }
        }
        return fixedFilters;
    }

    public <P extends PropertyInterface, F extends PropertyInterface> Set<FilterEntity> getObjectFixedFilters(ClassFormEntity<T> editForm, GroupObjectInstance selectionGroupObject) {
        Set<FilterEntity> fixedFilters = new HashSet<FilterEntity>();
        ObjectEntity object = editForm.object;
        for (FilterEntity filterEntity : entity.fixedFilters) {
            FilterInstance filter = filterEntity.getInstance(instanceFactory);
            if (filter.getApplyObject() == selectionGroupObject) { // берем фильтры из этой группы
                for (ObjectEntity filterObject : filterEntity.getObjects()) {
                    //добавляем фильтр только, если есть хотя бы один объект который не будет заменён на константу
                    if (filterObject.baseClass == object.baseClass) {
                        fixedFilters.add(filterEntity.getRemappedFilter(filterObject, object, instanceFactory));
                        break;
                    }
                }
            }
        }
        return fixedFilters;
    }

    public Object read(CalcPropertyObjectInstance<?> property) throws SQLException {
        return property.read(this);
    }

    public DialogInstance<T> createObjectDialog(CustomClass objectClass) throws SQLException {
        ClassFormEntity<T> classForm = objectClass.getEditForm(BL.LM);
        return new DialogInstance<T>(classForm.form, BL, session, securityPolicy, getFocusListener(), getClassListener(), classForm.object, null, instanceFactory.computer);
    }

    public DialogInstance<T> createObjectEditorDialog(CalcPropertyValueImplement propertyValues) throws SQLException {
        CustomClass objectClass = propertyValues.getDialogClass(session);
        ClassFormEntity<T> classForm = objectClass.getEditForm(BL.LM);

        Object currentObject = propertyValues.read(this);
/*        if (currentObject == null && objectClass instanceof ConcreteCustomClass) {
            currentObject = addObject((ConcreteCustomClass)objectClass).object;
        }*/

        return currentObject == null
                ? null
                : new DialogInstance<T>(classForm.form, BL, session, securityPolicy, getFocusListener(), getClassListener(), classForm.object, currentObject, instanceFactory.computer);
    }

    public DialogInstance<T> createChangeEditorDialog(CalcPropertyValueImplement propertyValues, GroupObjectInstance groupObject, CalcProperty filterProperty) throws SQLException {

        ClassFormEntity<T> formEntity = propertyValues.getDialogClass(session).getDialogForm(BL.LM);
        Set<PullChangeProperty> pullProps = new HashSet<PullChangeProperty>();
        Set<FilterEntity> additionalFilters = getEditFixedFilters(formEntity, propertyValues, groupObject, pullProps);

        ObjectEntity dialogObject = formEntity.object;
        DialogInstance<T> dialog = new DialogInstance<T>(formEntity.form, BL, session, securityPolicy, getFocusListener(), getClassListener(), dialogObject, propertyValues.read(this), instanceFactory.computer, additionalFilters, pullProps);

        if (filterProperty != null) {
            dialog.initFilterPropertyDraw = formEntity.form.getPropertyDraw(filterProperty, dialogObject);
        }

        dialog.undecorated = BL.isDialogUndecorated();

        return dialog;
    }

    public DialogInstance<T> createChangeObjectDialog(CustomClass dialogClass, Object dialogValue, GroupObjectInstance groupObject, CalcProperty filterProperty) throws SQLException {

        ClassFormEntity<T> formEntity = dialogClass.getDialogForm(BL.LM);
        Set<FilterEntity> additionalFilters = getObjectFixedFilters(formEntity, groupObject);

        ObjectEntity dialogObject = formEntity.object;
        DialogInstance<T> dialog = new DialogInstance<T>(formEntity.form, BL, session, securityPolicy, getFocusListener(), getClassListener(), dialogObject, dialogValue, instanceFactory.computer, additionalFilters, new HashSet<PullChangeProperty>());

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

    public void fireOnNull() throws SQLException {
        formResult = FormCloseType.NULL;
        fireEvent(FormEventType.NULL);
    }

    private void fireEvent(Object eventObject) throws SQLException {
        List<ActionPropertyObjectEntity<?>> actionsOnEvent = entity.getActionsOnEvent(eventObject);
        if (actionsOnEvent != null) {
            for (ActionPropertyObjectEntity<?> autoAction : actionsOnEvent) {
                ActionPropertyObjectInstance<? extends PropertyInterface> autoInstance = instanceFactory.getInstance(autoAction);
                if (autoInstance.isInInterface(null)) { // для проверки null'ов
                    FlowResult result = autoInstance.execute(this);
                    if(result != FlowResult.FINISH)
                        return;
                }
            }
        }
    }

    private FormCloseType formResult = FormCloseType.NULL;

    public FormCloseType getFormResult() {
        return formResult;
    }

    public DataSession getSession() {
        return session;
    }

    @ManualLazy
    public Modifier getModifier() {
        if (modifier == null) {
            FunctionSet<CalcProperty> noHints = getNoHints();
            modifier = new OverrideSessionModifier(createEnvironmentIncrement(isModal, this instanceof DialogInstance, manageSession, entity.isReadOnly()), noHints, noHints, entity.hintsIncrementTable, entity.hintsNoUpdate, session.getModifier());
        }
        return modifier;
    }

    public FormInstance getFormInstance() {
        return this;
    }

    public boolean isInTransaction() {
        return false;
    }

    public void formApply() throws SQLException {
        apply(BL);
    }

    public void formCancel() throws SQLException {
        int result = (Integer) Context.context.get().requestUserInteraction(new ConfirmClientAction("LS Fusion", getString("form.do.you.really.want.to.undo.changes")));
        if (result == JOptionPane.YES_OPTION) {
            cancel();
        }
    }

    public void formClose() throws SQLException {
        if (manageSession && session.hasStoredChanges()) {
            int result = (Integer) Context.context.get().requestUserInteraction(new ConfirmClientAction("LS Fusion", getString("form.do.you.really.want.to.close.form")));
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        fireOnClose();
        Context.context.get().delayUserInteraction(new HideFormClientAction());
        close();
    }

    public void formNull() throws SQLException {
        fireOnNull();

        Context.context.get().delayUserInteraction(new HideFormClientAction());
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

        Context.context.get().delayUserInteraction(new HideFormClientAction());
        close();
    }

    public void formRefresh() throws SQLException {
        refreshData();
    }
}
