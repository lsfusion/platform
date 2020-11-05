package lsfusion.server.logics.form.interactive.instance.object;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingFunction;
import lsfusion.interop.form.UpdateMode;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.order.user.Order;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.Compare;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.interop.form.property.PropertyReadType;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.stack.ThisMessage;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.FormulaUnionExpr;
import lsfusion.server.data.expr.formula.StringOverrideFormulaImpl;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.RecursiveExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.expr.where.classes.data.CompareWhere;
import lsfusion.server.data.expr.where.ifs.IfExpr;
import lsfusion.server.data.query.MapKeysInterface;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.modify.Modify;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.table.SessionData;
import lsfusion.server.data.table.SessionTable;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.SessionChanges;
import lsfusion.server.logics.action.session.change.increment.IncrementChangeProps;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.action.session.table.NoPropertyTableUsage;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.OrderClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.form.interactive.UpdateType;
import lsfusion.server.logics.form.interactive.changed.ChangedData;
import lsfusion.server.logics.form.interactive.changed.MFormChanges;
import lsfusion.server.logics.form.interactive.changed.ReallyChanged;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.filter.AndFilterInstance;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.filter.OrFilterInstance;
import lsfusion.server.logics.form.interactive.instance.order.OrderInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyReaderInstance;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.property.GroupObjectProp;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.logics.property.implement.PropertyRevImplement;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.profiler.ProfiledObject;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

import static lsfusion.base.BaseUtils.immutableCast;

public class GroupObjectInstance implements MapKeysInterface<ObjectInstance>, ProfiledObject {
    public final SeekOrderObjects SEEK_HOME = new SeekOrderObjects(false); 
    public final SeekOrderObjects SEEK_END = new SeekOrderObjects(true); 
    public SeekOrderObjects SEEK_PREV() {
        return new SeekOrderObjects(false, getGroupObjectValue());
    }

    public final PropertyObjectInstance propertyBackground;
    public final PropertyObjectInstance propertyForeground;
    final static int DIRECTION_DOWN = 1;
    final static int DIRECTION_UP = 2;
    final static int DIRECTION_CENTER = 3;

    public RowBackgroundReaderInstance rowBackgroundReader = new RowBackgroundReaderInstance();
    public RowForegroundReaderInstance rowForegroundReader = new RowForegroundReaderInstance();

    public final GroupObjectEntity entity;

    public static ImSet<ObjectInstance> getObjects(ImSet<GroupObjectInstance> groups) {
        MExclSet<ObjectInstance> mResult = SetFact.mExclSet();
        for(GroupObjectInstance group : groups)
            mResult.exclAddAll(group.objects);
        return mResult.immutable();
    }

    public static ImOrderSet<ObjectInstance> getOrderObjects(ImOrderSet<GroupObjectInstance> groups) {
        MOrderExclSet<ObjectInstance> mResult = SetFact.mOrderExclSet();
        for(GroupObjectInstance group : groups)
            mResult.exclAddAll(group.getOrderObjects());
        return mResult.immutableOrder();
    }

    public ImSet<ObjectInstance> objects; //
    private final ImOrderSet<ObjectInstance> orderObjects;
    public ImOrderSet<ObjectInstance> getOrderObjects() {
        return orderObjects;
    }

    public String toString() {
        return objects.toString();
    }

    // глобальный идентификатор чтобы писать во GroupObjectTable
    public int getID() {
        return entity.getID();
    }

    public String getSID() {
        return entity.getSID();
    }

    public String getIntegrationSID() {
        return entity.getIntegrationSID();
    }

    public ObjectInstance getObjectInstance(String objectSID) {
        for (ObjectInstance object : objects)
            if (object.getSID().equals(objectSID))
                return object;
        return null;
    }

    public Integer pageSize;
    public int getPageSize() {
        assert !isInTree();
        return pageSize;
    }
    public int getGroupPageSize() {
        assert !isInTree();
        return getPageSize();
    }

    public void setPageSize(Integer pageSize) {
        if(entity.pageSize == null && !pageSize.equals(this.pageSize)){
            updated |= UPDATED_PAGESIZE;
            this.pageSize = pageSize;
        }
    }

    public void forceRefresh() {
        updated |= UPDATED_FORCE;
    }
    public boolean toRefresh() { // hack to not keep state at client, just resend it with every view (grid / pivot) change
        return (updated & UPDATED_FORCE) != 0;
    }

    private boolean autoUpdateMode = true;
    public void setUpdateMode(UpdateMode updateMode) {
        if(updateMode == UpdateMode.FORCE)
            updated |= UPDATED_FORCEMODE;
        else
            autoUpdateMode = updateMode == UpdateMode.AUTO;
    }
    public boolean toUpdate() {
        return autoUpdateMode || (updated & UPDATED_FORCEMODE) != 0;
    }

    public GroupMode groupMode; // active

    public GroupMode setGroupMode;
    public void changeGroupMode(GroupMode groupMode) {
        setGroupMode = groupMode;
    }

    public ClassViewType viewType = ClassViewType.DEFAULT;

    public ImRevMap<ObjectInstance, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(objects);
    }

    public Integer order = 0;

    public GroupObjectInstance(GroupObjectEntity entity, ImOrderSet<ObjectInstance> objects, PropertyObjectInstance propertyBackground, PropertyObjectInstance propertyForeground, ImMap<ObjectInstance, PropertyObjectInstance> parent, ImMap<GroupObjectProp, PropertyRevImplement<ClassPropertyInterface, ObjectInstance>> props) {

        this.entity = entity;

        this.objects = objects.getSet();
        this.orderObjects = objects;

        this.propertyBackground = propertyBackground;
        this.propertyForeground = propertyForeground;

        for(ObjectInstance object : objects)
            object.groupTo = this;

        this.viewType = entity.viewType;

        this.pageSize = entity.pageSize;

        this.parent = parent;
        this.props = props;
    }

    // caches
    public ImSet<FilterInstance> setFilters = null;
    public ImSet<FilterInstance> getSetFilters() {
        if(setFilters==null) {
            FilterInstance userComboFilter = combineUserFilters(userFilters);
            ImSet<FilterInstance> userComboSet = userComboFilter != null ? SetFact.singleton(userComboFilter) : userFilters.immutableOrder().getSet();
            setFilters = fixedFilters.merge(userComboSet).merge(SetFact.fromJavaSet(regularFilters));
        }

        if (listViewType == ListViewType.CALENDAR) {
            setFilters = setFilters.merge(SetFact.fromJavaSet(viewFilters));
        }

        return setFilters;
    }

    private FilterInstance combineUserFilters(MOrderSet<FilterInstance> filterSet) {
        FilterInstance comboFilter = null;
        List<List<FilterInstance>> organizedFilters = new ArrayList<>();
        List<FilterInstance> orFilters = new ArrayList<>();
        for (FilterInstance filter : filterSet.immutableOrder()) {
            orFilters.add(filter);
            if (filter.junction) {
                organizedFilters.add(orFilters);
                orFilters = new ArrayList<>();
            }
        }
        if (!orFilters.isEmpty())
            organizedFilters.add(orFilters);

        List<FilterInstance> ands = new ArrayList<>();
        for (List<FilterInstance> ors : organizedFilters) {
            FilterInstance filt = null;
            for (FilterInstance filter : ors) {
                if (filt == null) {
                    filt = filter;
                    continue;
                }
                filt = new OrFilterInstance(filt, filter);
            }
            ands.add(filt);
        }

        for (FilterInstance filter : ands) {
            if (comboFilter == null) {
                comboFilter = filter;
                continue;
            }
            comboFilter = new AndFilterInstance(comboFilter, filter);
        }
        return comboFilter;
    }

    // вообще все фильтры
    public ImSet<FilterInstance> fixedFilters = SetFact.EMPTY();

    private MOrderSet<FilterInstance> userFilters = SetFact.mOrderSet();
    public void clearUserFilters() {
        userFilters = SetFact.mOrderSet();

        setFilters = null;
        updated |= UPDATED_FILTER;
    }
    public void addUserFilter(FilterInstance addFilter) {
        userFilters.add(addFilter);

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    private Set<FilterInstance> regularFilters = new HashSet<>();
    public void addRegularFilter(FilterInstance filter) {
        regularFilters.add(filter);

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    public void removeRegularFilter(FilterInstance filter) {
        regularFilters.remove(filter);

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    private Set<FilterInstance> viewFilters = new HashSet<>();
    public void clearViewFilters() {
        viewFilters.clear();

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    public void addViewFilters(List<FilterInstance> filters) {
        clearViewFilters();
        viewFilters.addAll(filters);

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    // с активным интерфейсом
    public ImSet<FilterInstance> filters;

    public ImOrderMap<OrderInstance, Boolean> fixedOrders = MapFact.EMPTYORDER();

    private PropertyDrawInstance calendarDateProperty;
    public void setCalendarDateProperty(PropertyDrawInstance calendarDateProperty) {
        this.calendarDateProperty = calendarDateProperty;
    }

    // обертку потому как сложный assertion
    private ImOrderMap<OrderInstance,Boolean> setOrders = null;
    public ImOrderMap<OrderInstance,Boolean> getSetOrders() {
        if(setOrders==null)
            setOrders = userOrders.mergeOrder(fixedOrders).mergeOrder(getOrderObjects().toOrderMap(false));

        return setOrders;
    }
    private ImOrderMap<OrderInstance,Boolean> userOrders = MapFact.EMPTYORDER();

    public void changeOrder(OrderInstance property, Order modiType) {
        ImOrderMap<OrderInstance, Boolean> newOrders;
        if (modiType == Order.REPLACE) {
            newOrders = MapFact.singletonOrder(property, userOrders.containsKey(property) && !userOrders.get(property));
        } else if (modiType == Order.REMOVE) {
            newOrders = userOrders.removeOrderIncl(property);
        } else if (modiType == Order.DIR) {
            if(userOrders.containsKey(property))
                newOrders = userOrders.replaceValue(property, !userOrders.get(property));
            else
                newOrders = userOrders.addOrderExcl(property, true);
        } else {
            assert modiType == Order.ADD;
            newOrders = userOrders.addOrderExcl(property, false);
        }

        if(!BaseUtils.hashEquals(newOrders, userOrders)) {// оптимизация для пользовательских настроек
            userOrders = newOrders;
            setOrders = null;
            updated |= UPDATED_ORDER;
        }
    }

    public void clearOrders() {
        if(!userOrders.isEmpty()) { // оптимизация для пользовательских настроек
            userOrders = MapFact.EMPTYORDER();
            setOrders = null;
            updated |= UPDATED_ORDER;
        }
    }

    // с активным интерфейсом, assertion что содержит все ObjectInstance
    public ImOrderMap<OrderInstance,Boolean> orders = MapFact.EMPTYORDER();

    boolean upKeys, downKeys;
    public ImOrderMap<ImMap<ObjectInstance,DataObject>,ImMap<OrderInstance,ObjectValue>> keys = MapFact.EMPTYORDER();

    // 0 !!! - изменился объект, 1 - класс объекта, 2 !!! - отбор, 3 !!! - хоть один класс, 4 !!! - классовый вид

    public final static int UPDATED_OBJECT = (1);
    public final static int UPDATED_KEYS = (1 << 2);
    public final static int UPDATED_FORCEMODE = (1 << 4);
    public final static int UPDATED_ORDER = (1 << 5);
    public final static int UPDATED_FILTER = (1 << 6);
    public final static int UPDATED_EXPANDS = (1 << 7);
    public final static int UPDATED_PAGESIZE = (1 << 8);

    public final static int UPDATED_FORCE = (1 << 9);

    public int updated = UPDATED_ORDER | UPDATED_FILTER;

    private boolean assertNull() {
        Iterator<ObjectInstance> it = objects.iterator();
        boolean isNull = it.next().getObjectValue() instanceof NullValue; 
        for(ObjectInstance object : objects)
            if((object.getObjectValue() instanceof NullValue)!=isNull)
                return false;
        return true;
    }

    public void forceUpdateKeys() {
        updated |= UPDATED_FILTER;
    }

    public boolean isNull() {
//        assert assertNull();
//        return objects.iterator().next().getObjectValue() instanceof NullValue;
        for (ObjectInstance object : GroupObjectInstance.getObjects(getUpTreeGroups()))
            if (object.getObjectValue() instanceof NullValue) return true;
        return false;
    }

    public ImMap<ObjectInstance,DataObject> getGroupObjectValue() {
        if(isNull())
            return MapFact.EMPTY();

        return GroupObjectInstance.getObjects(getUpTreeGroups()).mapValues(ObjectInstance::getDataObject);
    }

    public ImMap<ObjectInstance,DataObject> findGroupObjectValue(ImMap<ObjectInstance, Object> map) {
        for(ImMap<ObjectInstance, DataObject> keyRow : keys.keyIt()) {
            boolean equal = true;
            for(int i=0,size=keyRow.size();i<size;i++) {
                if(!keyRow.getValue(i).object.equals(map.get(keyRow.getKey(i)))) {
                    equal = false;
                    break;
                }
            }
            if(equal) {
                return keyRow;
            }
        }

        // actually clients call this method only for grids, and rows that are in that grids (i.e in keys collection), so upper cycle should exit before getting to this code
        // however there was a comment that in web-client because of some race conditions this code also might be called
        return null;
//        throw new RuntimeException("key not found");
    }

    public UpdateType getUpdateType() throws SQLException, SQLHandledException {
        return entity.getUpdateType(this);
    }

    @Override
    public Object getProfiledObject() {
        return entity;
    }

    public interface FilterProcessor {
        ImMap<ObjectInstance, ? extends Expr> process(FilterInstance filt, ImMap<ObjectInstance, ? extends Expr> mapKeys);

        ImSet<FilterInstance> getFilters();
    }

    public Where getFilterWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged, FilterProcessor filterProcessor, MSet<Property> mUsedProps) throws SQLException, SQLHandledException {
        Where where = Where.TRUE();
        for(FilterInstance filt : (filterProcessor != null ? filterProcessor.getFilters() : filters)) {
            if(filterProcessor != null) {
                ImMap<ObjectInstance, ? extends Expr> overridedKeys = filterProcessor.process(filt, mapKeys);
                if(overridedKeys == null)
                    continue;
                mapKeys = overridedKeys;
            }
            where = where.and(filt.getWhere(mapKeys, modifier, reallyChanged, mUsedProps));
        }
        return where;
    }

    public static ImMap<ObjectInstance, ValueClass> getGridClasses(ImSet<ObjectInstance> objects) {
        return objects.filterFn(element -> !element.noClasses).mapValues(ObjectInstance::getGridClass);
    }
    public Where getClassWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, MSet<Property> mUsedProps) throws SQLException, SQLHandledException {
        return IsClassProperty.getWhere(getGridClasses(objects), mapKeys, modifier, mUsedProps);
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        return getWhere(mapKeys, modifier, reallyChanged, (MSet<Property>) null);
    }
    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged, MSet<Property> mUsedProps) throws SQLException, SQLHandledException {
        return getWhere(mapKeys, modifier, reallyChanged, null, mUsedProps);
    }
    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged, FilterProcessor processor) throws SQLException, SQLHandledException {
        return getWhere(mapKeys, modifier, reallyChanged, processor, null);
    }
    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged, FilterProcessor processor, MSet<Property> mUsedProps) throws SQLException, SQLHandledException {
        return getFilterWhere(mapKeys, modifier, reallyChanged, processor, mUsedProps).and(getClassWhere(mapKeys, modifier, mUsedProps));
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier) throws SQLException, SQLHandledException {
        return getWhere(mapKeys, modifier, null);
    }

    public ImMap<ObjectInstance,ObjectValue> getNulls() {
        return NullValue.getMap(getObjects(getUpTreeGroups()));
    }

    boolean isSolid() {
        Iterator<ObjectInstance> i = objects.iterator();
        boolean read = i.next() instanceof CustomObjectInstance;
        while(i.hasNext())
            if(read != i.next() instanceof CustomObjectInstance)
                return false;
        return true;
    }

    public void setClassListener(CustomClassListener classListener) {
        for (ObjectInstance object : objects)
            if (object instanceof CustomObjectInstance)
                ((CustomObjectInstance)object).setClassListener(classListener);
    }

    public boolean isInTree() {
        return treeGroup!=null || parent!=null;
    }

    public TreeGroupInstance treeGroup = null;
    public ImOrderSet<GroupObjectInstance> getOrderDownTreeGroups() {
        return treeGroup!=null?treeGroup.getDownTreeGroups(this):SetFact.EMPTYORDER();
    }
    public ImSet<GroupObjectInstance> getDownTreeGroups() {
        return getOrderDownTreeGroups().getSet();
    }
    public GroupObjectInstance getUpTreeGroup() {
        return treeGroup!=null?treeGroup.getUpTreeGroup(this):null;
    }
    public ImOrderSet<GroupObjectInstance> getOrderUpTreeGroups() {
        return treeGroup!=null?treeGroup.getUpTreeGroups(this):SetFact.singletonOrder(this);
    }
    public ImSet<GroupObjectInstance> getUpTreeGroups() {
        return getOrderUpTreeGroups().getSet();
    }

    public static ImSet<GroupObjectInstance> getUpTreeGroups(ImSet<GroupObjectInstance> groups) {
        MSet<GroupObjectInstance> mResult = SetFact.mSet();
        for(GroupObjectInstance group : groups)
            mResult.addAll(group.getUpTreeGroups());
        return mResult.immutable();
    }

    public final ImMap<ObjectInstance, PropertyObjectInstance> parent;

    // поиски по свойствам\объектам
    public SeekObjects userSeeks = null;

    public void addSeek(OrderInstance order, ObjectValue value, boolean addSeek) {
        if(userSeeks==null || userSeeks == SEEK_NULL)
            userSeeks = SEEK_PREV();
        userSeeks = ((SeekOrderObjects)userSeeks).add(order, value, addSeek);
    }

    public void dropSeek(ObjectInstance object) {
        if(userSeeks==null || userSeeks == SEEK_NULL)
            userSeeks = SEEK_PREV();
        userSeeks = ((SeekOrderObjects)userSeeks).remove(object);
    }

    public SeekObjects getSeekObjects(UpdateType seek) {
        switch (seek) {
            case FIRST:
                return SEEK_HOME;
            case LAST:
                return SEEK_END;
            case NULL:
                return SEEK_NULL;
            case PREV:
                return SEEK_PREV();
        }
        throw new UnsupportedOperationException();
    }
    public void seek(UpdateType seek) {
        userSeeks = getSeekObjects(seek);
    }
    public void seek(boolean end) {
        userSeeks = end ? SEEK_END : SEEK_HOME;
    }

    @IdentityLazy
    public ImSet<DataObjectInstance> getFreeDataObjects() throws SQLException, SQLHandledException {

        final ImRevMap<ObjectInstance, KeyExpr> mapKeys = getMapKeys();

        final ImSet<KeyExpr> usedContext = immutableCast(getFilterWhere(mapKeys, Property.defaultModifier, null, null, null).getOuterKeys());

        return immutableCast(objects.filterFn(object -> { // если DataObject и нету ключей
            return object instanceof DataObjectInstance && !usedContext.contains(mapKeys.get(object));
        }));
    }

    public NoPropertyTableUsage<ObjectInstance> keyTable = null;
    public NoPropertyTableUsage<ObjectInstance> expandTable = null;

    // так как в updateKeys мы не drop'аем таблицу (чтобы не collapse'илось дерево) надо обновлять классы
    public void updateExpandClasses(final UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
        if(expandTable != null) {
            final SessionData sessionData = expandTable.saveData();
            expandTable.updateCurrentClasses(session);
            session.addRollbackInfo(() -> {
                OperationOwner owner = session.env.getOpOwner();
                expandTable.drop(session.sql, owner);
                expandTable.rollData(session.sql, sessionData, owner);
            });
        }
    }

    private Where getExpandWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys) {
        if(expandTable==null)
            return Where.FALSE();
        else
            return expandTable.getWhere(mapKeys);
    }
    
    // the problem that here it is not guaranteed that DataObject types will be the same as in ObjectInstance (and this will lead to incorrect equals, for example in updateDrawProps where is equal to keysTable, which types are object base classes)
    private <V> ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<V, ObjectValue>> castExecuteObjects(ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<V, ObjectValue>> rows) {
        return SessionData.castTypes(rows, typeGetter);        
    }

    public void expandCollapseUp(FormInstance form, ImMap<ObjectInstance, DataObject> objects, boolean expand) throws SQLException, SQLHandledException {
        if (expandTable == null)
            expandTable = createKeyTable("expgo");

        if(parent != null) {
            Query<ObjectInstance, String> query = getRecursiveExpandQuery(false, objects, form.getModifier(), form);
            expandTable.modifyRows(form.session.sql, query, form.session.baseClass, expand ? Modify.LEFT : Modify.DELETE, form.getQueryEnv(), SessionTable.nonead);
        } else
            expandTable.modifyRecord(form.session.sql, objects, expand ? Modify.LEFT : Modify.DELETE, form.session.getOwner());

        updated |= UPDATED_EXPANDS;
    }

    public void expandCollapseDown(FormInstance form, ImMap<ObjectInstance, DataObject> value, boolean expand) throws SQLException, SQLHandledException {
        if (expandTable == null)
            expandTable = createKeyTable("expgo");

        expandTable.modifyRecord(form.session.sql, value, expand ? Modify.LEFT : Modify.DELETE, form.session.getOwner());

        updated |= UPDATED_EXPANDS;
    }

    public void expandCollapseAll(FormInstance form, boolean current, boolean expand) throws SQLException, SQLHandledException {
        if (current && !isNull()) {
            expandCollapseAll(form, getGroupObjectValue(), expand);
        } else {
            GroupObjectInstance upTreeGroup = getUpTreeGroup();
            expandCollapseAll(form, upTreeGroup == null ? null : upTreeGroup.expandTable, expand);
        }
    }

    private void expandCollapseAll(FormInstance form, ImMap<ObjectInstance, DataObject> objects, boolean expand) throws SQLException, SQLHandledException {
        if (expandTable == null)
            expandTable = createKeyTable("expgo");

        NoPropertyTableUsage<ObjectInstance> expandingTable = createKeyTable("expinggo");
        try {
            if(parent != null) {
                Query<ObjectInstance, String> query = getRecursiveExpandQuery(true, objects, form.getModifier(), form);
                expandingTable.writeRows(form.session.sql, query, form.session.baseClass, form.getQueryEnv(), SessionTable.nonead);
            } else
                expandingTable.modifyRecord(form.session.sql, objects, expand ? Modify.ADD : Modify.DELETE, form.session.getOwner());
            expandCollapseAllDown(form, expandingTable, expand);

            expandTable.modifyRows(form.session.sql, expandingTable.getQuery(), form.session.baseClass, expand ? Modify.LEFT : Modify.DELETE, form.getQueryEnv(), SessionTable.nonead);
        } finally {
            expandingTable.drop(form.session.sql, form.getQueryEnv().getOpOwner());
        }

        updated |= UPDATED_EXPANDS;
    }

    private void expandCollapseAllDown(FormInstance form, NoPropertyTableUsage<ObjectInstance> expandingTable, boolean expand) throws SQLException, SQLHandledException {
        GroupObjectInstance downGroup = treeGroup.getDownTreeGroup(this);
        if (downGroup != null)
            downGroup.expandCollapseAll(form, expandingTable, expand);
    }

    private void expandCollapseAll(FormInstance form, NoPropertyTableUsage<ObjectInstance> expandingTable, boolean expand) throws SQLException, SQLHandledException {
        if (expandTable == null)
            expandTable = createKeyTable("expgo");

        Query<ObjectInstance, String> query = getAllExpandQuery(expandingTable, form.getModifier(), form);
        if (expand)
            expandTable.writeRows(form.session.sql, query, form.session.baseClass, form.getQueryEnv(), SessionTable.nonead);
        else
            expandTable.modifyRows(form.session.sql, query, form.session.baseClass, Modify.DELETE, form.getQueryEnv(), SessionTable.nonead);

        expandCollapseAllDown(form, expandTable, expand);

        updated |= UPDATED_EXPANDS;
    }

    private Where getRecursiveStepWhere(ImRevMap<ObjectInstance, KeyExpr> mapKeys1, ImMap<ObjectInstance, ? extends Expr> mapKeys2, Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        return CompareWhere.compare(mapKeys1, parent.<Expr, SQLException, SQLHandledException>mapValuesEx(value -> value.getExpr(mapKeys2, modifier, reallyChanged)));
    }

    private Where getRecursiveExpandWhere(ImMap<ObjectInstance, DataObject> objects, boolean down, final ImRevMap<ObjectInstance, ? extends Expr> mapExprs, Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        final ImRevMap<ObjectInstance, KeyExpr> mapKeys = getMapKeys();
        ImRevMap<KeyExpr, KeyExpr> mapKeysPrevKeys = KeyExpr.getMapKeys(mapKeys.valuesSet());
        final ImRevMap<ObjectInstance, KeyExpr> mapPrevKeys = mapKeys.join(mapKeysPrevKeys);

        Where initialWhere = CompareWhere.compareValues(mapKeys, objects);

        ImMap<ObjectInstance, DataObject> upObjects = objects.removeIncl(mapKeys.keys());
        ImMap<ObjectInstance, Expr> upExprs = ObjectValue.getMapExprs(upObjects);
        Where stepWhere = getWhere(upExprs.addExcl(mapKeys), modifier, reallyChanged).and(
                          down ? getRecursiveStepWhere(mapPrevKeys, upExprs.addExcl(mapKeys), modifier, reallyChanged)
                               : getRecursiveStepWhere(mapKeys, upExprs.addExcl(mapPrevKeys), modifier, reallyChanged));
        Where recursiveWhere = RecursiveExpr.create(mapKeysPrevKeys, ValueExpr.get(initialWhere), ValueExpr.get(stepWhere), mapKeys.crossJoin(mapExprs)).getWhere();

        return recursiveWhere.and(CompareWhere.compareInclValues(mapExprs, upObjects));
    }

    private Query<ObjectInstance, String> getRecursiveExpandQuery(boolean down, ImMap<ObjectInstance, DataObject> objects, Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        final ImRevMap<ObjectInstance, KeyExpr> mapKeys = KeyExpr.getMapKeys(GroupObjectInstance.getObjects(getUpTreeGroups()));

        assert parent != null;
        Where expandWhere = getRecursiveExpandWhere(objects, down, mapKeys, modifier, reallyChanged);

        return new Query<>(mapKeys, MapFact.EMPTY(), expandWhere);
    }

    private Query<ObjectInstance, String> getAllExpandQuery(NoPropertyTableUsage<ObjectInstance> expandingTable, Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        final ImRevMap<ObjectInstance, KeyExpr> mapKeys = KeyExpr.getMapKeys(GroupObjectInstance.getObjects(getUpTreeGroups()));

        Where expandWhere = getWhere(mapKeys, modifier, reallyChanged).and(
                            expandingTable != null ? expandingTable.join(mapKeys).getWhere() : Where.TRUE());

        return new Query<>(mapKeys, MapFact.EMPTY(), expandWhere);
    }

    public void collapse(DataSession session, ImMap<ObjectInstance, DataObject> value) throws SQLException, SQLHandledException {
        if (expandTable != null) {
            expandTable.modifyRecord(session.sql, value, Modify.DELETE, session.getOwner());
            updated |= UPDATED_EXPANDS;
        }
    }

    private ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> executeGroup(SQLSession session, QueryEnvironment env, final Modifier modifier, BaseClass baseClass, int readSize, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        assert !isInTree();

        assert groupMode != null;
        final ImRevMap<ObjectInstance, KeyExpr> mapKeys = getMapKeys();

        Where where = getWhere(mapKeys, modifier, reallyChanged);

        // first we need exprs to group
        ImMap<GroupColumn, Expr> groupExprs = getGroupExprs(mapKeys, modifier, reallyChanged);

        // group by this exprs - max(key)
        ImRevMap<GroupColumn, KeyExpr> groupKeys = KeyExpr.getMapKeys(groupMode.groupProps);
        ImMap<ObjectInstance, Expr> mapExprKeys = mapKeys.mapValues(keyExpr -> GroupExpr.create(groupExprs, keyExpr, where, GroupType.MAX, groupKeys));

        // group by max(key)
        Where groupWhere = GroupExpr.create(mapExprKeys, Where.TRUE(), mapKeys).getWhere();

        return castExecuteObjects(new Query<ObjectInstance, OrderInstance>(mapKeys, groupWhere).
                executeClasses(session, env, baseClass, readSize));
    }

    public ImMap<GroupColumn, Expr> getGroupExprs(ImMap<ObjectInstance, KeyExpr> mapKeys, Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        return groupMode.groupProps.<Expr, SQLException, SQLHandledException>mapValuesEx(value -> {
            ImMap<ObjectInstance, Expr> mapObjects = MapFact.addExcl(mapKeys, value.columnKeys.mapValues((Function<DataObject, ValueExpr>) DataObject::getExpr));
            Expr expr = value.property.getDrawInstance().getExpr(mapObjects, modifier, reallyChanged);
            return FormulaUnionExpr.create(StringOverrideFormulaImpl.instance, ListFact.toList(expr, ValueExpr.IMPOSSIBLESTRING)); // override to support NULLs
        });
    }

    private ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<Object, ObjectValue>> executeTree(SQLSession session, QueryEnvironment env, final Modifier modifier, BaseClass baseClass, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        assert isInTree();

        final ImRevMap<ObjectInstance, KeyExpr> mapKeys = KeyExpr.getMapKeys(GroupObjectInstance.getObjects(getUpTreeGroups()));

        MExclMap<Object, Expr> mPropertyExprs = MapFact.mExclMap();

        Where expandWhere;
        if(getUpTreeGroup()!=null)
            expandWhere = getUpTreeGroup().getExpandWhere(mapKeys); // для верхней группы брать только из expandTable'а
        else
            expandWhere = Where.TRUE();

        if (parent != null) {
            ImMap<ObjectInstance, Expr> parentExprs = parent.mapValuesEx((ThrowingFunction<PropertyObjectInstance, Expr, SQLException, SQLHandledException>) value -> value.getExpr(mapKeys, modifier));

            Where nullWhere = Where.FALSE();
            for (Expr parentExpr : parentExprs.valueIt()) {
                nullWhere = nullWhere.or(parentExpr.getWhere().not());
            }
            expandWhere = expandWhere.and(nullWhere).or(getExpandWhere(MapFact.override(mapKeys, parentExprs))); // если есть parent, то те, чей parent равен null или expanded

            mPropertyExprs.exclAddAll(parentExprs);

            mPropertyExprs.exclAdd("expandable", GroupExpr.create(parentExprs, ValueExpr.TRUE, GroupType.LOGICAL(), mapKeys.filter(parentExprs.keys()))); // boolean
            if (treeGroup != null) {
                GroupObjectInstance subGroup = treeGroup.getDownTreeGroup(this);
                if (subGroup != null) {
                    //если не последняя группа
                    mPropertyExprs.exclAdd("expandable2", subGroup.getHasSubElementsExpr(mapKeys, modifier, reallyChanged));
                }
            }
        }

        ImOrderMap<Expr, Boolean> orderExprs = orders.mapMergeOrderKeysEx((ThrowingFunction<OrderInstance, Expr, SQLException, SQLHandledException>) value -> value.getExpr(mapKeys, modifier));

        return castExecuteObjects(new Query<>(mapKeys, mPropertyExprs.immutable(), getWhere(mapKeys, modifier, reallyChanged).and(expandWhere)).
                    executeClasses(session, env, baseClass, orderExprs));
    }

    private Expr getHasSubElementsExpr(final ImRevMap<ObjectInstance, KeyExpr> outerMapKeys, final Modifier modifier, final ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        final ImRevMap<ObjectInstance, KeyExpr> mapKeys = KeyExpr.getMapKeys(GroupObjectInstance.getObjects(getUpTreeGroups()));

        Where subGroupWhere = Where.TRUE();

        if (parent != null) {
            ImMap<ObjectInstance, Expr> parentExprs = parent.mapValuesEx((ThrowingFunction<PropertyObjectInstance, Expr, SQLException, SQLHandledException>) value -> value.getExpr(mapKeys, modifier));

            Where nullWhere = Where.FALSE();
            for (Expr parentExpr : parentExprs.valueIt()) {
                nullWhere = nullWhere.or(parentExpr.getWhere().not());
            }
            subGroupWhere = subGroupWhere.and(nullWhere);
        }

        subGroupWhere = subGroupWhere.and(getWhere(mapKeys, modifier, reallyChanged));

        Expr validSubElementExpr = IfExpr.create(subGroupWhere, ValueExpr.TRUE, ValueExpr.NULL());

        final ImMap<ObjectInstance, KeyExpr> upKeys = mapKeys.remove(objects);
        return GroupExpr.create(upKeys, validSubElementExpr, GroupType.LOGICAL(), outerMapKeys); // boolean
    }

    public void change(SessionChanges session, ImMap<ObjectInstance, DataObject> value, FormInstance eventForm, ExecutionStack stack) throws SQLException, SQLHandledException {
        // проставим все объектам метки изменений
        ImSet<ObjectInstance> upGroups = GroupObjectInstance.getObjects(getUpTreeGroups());
        assert value.isEmpty() || value.keys().equals(upGroups);
        for (ObjectInstance object : upGroups)
            object.changeValue(session, value.isEmpty()? NullValue.instance:value.get(object));
        ImSet<ObjectInstance> downGroups = GroupObjectInstance.getObjects(getDownTreeGroups());
        for(ObjectInstance object : downGroups)
            object.changeValue(session, NullValue.instance);

        eventForm.changeGroupObject(upGroups.addExcl(downGroups), stack);
    }

    public void update(SessionChanges session, MFormChanges changes, FormInstance eventForm, ImMap<ObjectInstance, DataObject> value, ExecutionStack stack) throws SQLException, SQLHandledException {
        changes.objects.exclAdd(this, value.isEmpty() ? NullValue.getMap(getObjects(getUpTreeGroups())) : value);
        change(session, value, eventForm, stack);
    }

    public ImMap<GroupObjectProp, PropertyRevImplement<ClassPropertyInterface, ObjectInstance>> props;

    private boolean pendingUpdateKeys;
    private boolean pendingUpdateScroll;
    private boolean pendingUpdateObjects;
    private boolean pendingUpdateFilters;
    private boolean pendingUpdateObject;
    private boolean pendingUpdatePageSize;

    public final Set<PropertyReaderInstance> pendingUpdateProps = new HashSet<>();
    private boolean isPending() {
        return pendingUpdateKeys || pendingUpdateScroll || !pendingUpdateProps.isEmpty();
    }
    // we won't check for groupMode change
    public void checkPending(MFormChanges changes, Runnable change) {
        boolean wasPending = isPending();
        change.run();
        boolean isPending = isPending();
        if(isPending != wasPending)
            changes.updateStateObjects.add(this, isPending);
    }
    private boolean hasUpdateEnvironmentIncrementProp(GroupObjectProp propType) { // оптимизация
        return props.get(propType) != null;
    }
    
    private MAddMap<GroupObjectProp, ImSet<Property>> usedEnvironmentIncrementProps = MapFact.mAddOverrideMap();
    public ImSet<Property> getUsedEnvironmentIncrementProps(GroupObjectProp propType) {
        return usedEnvironmentIncrementProps.get(propType);
    }
            
    public void updateEnvironmentIncrementProp(IncrementChangeProps environmentIncrement, final Modifier modifier, Result<ChangedData> changedProps, final ReallyChanged reallyChanged, GroupObjectProp propType, boolean propsChanged, boolean dataChanged) throws SQLException, SQLHandledException {
        PropertyRevImplement<ClassPropertyInterface, ObjectInstance> mappedProp = props.get(propType);
        if(mappedProp != null) {
            MSet<Property> mUsedProps = null;
            if(propsChanged)
                mUsedProps = SetFact.mSet();
            
            final ImRevMap<ObjectInstance, KeyExpr> mapKeys = getMapKeys();
            PropertyChange<ClassPropertyInterface> change;
            switch (propType) {
                case FILTER:
                    change = new PropertyChange<>(mappedProp.mapping.join(mapKeys), ValueExpr.TRUE, getWhere(mapKeys, modifier, reallyChanged, mUsedProps));
                    break;
                case ORDER:
                    if(orders.isEmpty())
                        change = mappedProp.property.getNoChange();
                    else {
                        final MSet<Property> fmUsedProps = mUsedProps;
                        ImOrderMap<Expr, Boolean> orderExprs = orders.mapOrderKeysEx((ThrowingFunction<OrderInstance, Expr, SQLException, SQLHandledException>) value -> value.getExpr(mapKeys, modifier, reallyChanged, fmUsedProps));
                        OrderClass orderClass = OrderClass.get(orders.keyOrderSet().mapListValues(new Function<OrderInstance, Type>() {
                            public Type apply(OrderInstance value) {
                                return value.getType();
                            }}), orderExprs.valuesList());
                        change = new PropertyChange<>(mappedProp.mapping.join(mapKeys), FormulaUnionExpr.create(orderClass, orderExprs.keyOrderSet()));
                    }
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            environmentIncrement.add(mappedProp.property, change, dataChanged);
            if(propsChanged)
                usedEnvironmentIncrementProps.add(propType, mUsedProps.immutable());

            if(changedProps != null)
                changedProps.set(changedProps.result.merge(new ChangedData(SetFact.singleton(mappedProp.property), false)));
        }
    }

    @StackMessage("{message.form.update.group.keys}")
    @ThisMessage
    public ImMap<ObjectInstance, DataObject> updateKeys(SQLSession sql, QueryEnvironment env, final FormInstance.FormModifier modifier, IncrementChangeProps environmentIncrement, ExecutionEnvironment execEnv, BaseClass baseClass, boolean hidden, final boolean refresh, MFormChanges result, MSet<PropertyDrawInstance> mChangedDrawProps, Result<ChangedData> changedProps, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        if (keyTable == null) // в общем то только для hidden'а но может и потом понадобиться
            keyTable = createKeyTable("upktable-" + System.identityHashCode(execEnv));

        boolean updateObjects = false; // updated upper objects in filters / orders

        // FILTERS

        boolean updateFilters = refresh || toRefresh() || (updated & UPDATED_FILTER) != 0;
        ImSet<GroupObjectInstance> sThis = SetFact.singleton(this);
        filters = getSetFilters();

        if (!updateFilters) // изменились "верхние" объекты для фильтров
            for (FilterInstance filt : filters)
                if (filt.objectUpdated(sThis)) {
                    updateFilters = true;
                    updateObjects = true;
                    break;
                }

        if (!updateFilters) // изменились данные по фильтрам
            for (FilterInstance filt : filters)
                if (filt.dataUpdated(changedProps.result, reallyChanged, modifier, hidden, sThis)) {
                    updateFilters = true;
                    break;
                }
        if (!updateFilters) // классы удалились\добавились
            for (ObjectInstance object : objects)
                if (object.classChanged(changedProps.result)) {  // || object.classUpdated() сомнительный or
                    updateFilters = true;
                    break;
                }

        if(updateFilters) // изменились фильтры, надо обновить свойства созданные при помощи соответствующих операторов форм, сейчас будет определенная избыточность для dataUpdated (так как через eventChange уже должны изменится), но пока не критично
            modifier.updateEnvironmentIncrementProp(new Pair<>(this, GroupObjectProp.FILTER), environmentIncrement, changedProps, reallyChanged, true, true);

        // ORDERS

        boolean hasOrderProperty = hasUpdateEnvironmentIncrementProp(GroupObjectProp.ORDER); // optimization
        boolean updateOrders = (updated & UPDATED_ORDER) != 0;
        orders = getSetOrders();

        if (!updateOrders && (!updateFilters || hasOrderProperty)) // изменились "верхние" объекты для порядков
            for (OrderInstance order : orders.keyIt())
                if (order.objectUpdated(sThis)) {
                    updateOrders = true;
                    updateObjects = true;
                    break;
                }
        if (!updateOrders && (!updateFilters || hasOrderProperty)) // изменились данные по порядкам
            for (OrderInstance order : orders.keyIt())
                if (order.dataUpdated(changedProps.result, reallyChanged, modifier, hidden, sThis)) {
                    updateOrders = true;
                    break;
                }

        if(updateOrders) // изменились порядки, надо обновить свойства созданные при помощи соответствующих операторов форм, сейчас будет определенная избыточность для dataUpdated (так как через eventChange уже должны изменится), но пока не критично
            modifier.updateEnvironmentIncrementProp(new Pair<>(this, GroupObjectProp.ORDER), environmentIncrement, changedProps, reallyChanged, true, true);

        boolean updateKeys = updateFilters || updateOrders || (setGroupMode == null && userSeeks != null);

        boolean updatePageSize = (updated & UPDATED_PAGESIZE) != 0;

        // EXTRA CHECKS

        if(isInTree()) {
            if (!updateKeys && (getUpTreeGroup() != null && ((getUpTreeGroup().updated & UPDATED_EXPANDS) != 0))) {
                updateKeys = true;
            }
            if (parent != null) {
                if (!updateKeys && (updated & UPDATED_EXPANDS) != 0)
                    updateKeys = true;
                if (!updateKeys) {
                    for (PropertyObjectInstance parentProp : parent.valueIt()) {
                        if (parentProp.objectUpdated(sThis)) {
                            updateKeys = true;
                            break;
                        }
                    }
                    for (PropertyObjectInstance parentProp : parent.valueIt()) {
                        if (parentProp.dataUpdated(changedProps.result, reallyChanged, modifier, hidden, sThis)) {
                            updateKeys = true;
                            break;
                        }
                    }
                }
            }
        } else {
            if (setGroupMode != null) {
                if (updatePageSize) // if pagesize is changed there can be switch from group mode to regular and vice versa
                    updateKeys = true;

                if (!updateKeys && groupMode != null) {
                    if (!groupMode.groupProps.containsAll(setGroupMode.groupProps)) // groups added - update anyway
                        updateKeys = true;
                    int groupPageSize = getGroupPageSize();
                    if(keys.size() == groupPageSize && !BaseUtils.hashEquals(groupMode.groupProps, setGroupMode.groupProps)) // maximum number of groups and groups changed (removed) - update anyway
                        updateKeys = true;
                    if(!updateKeys) {
                        ImMap<PropertyDrawInstance, ImMap<ImMap<ObjectInstance, DataObject>, PropertyGroupType>> changedAggrProps = setGroupMode.aggrProps.removeEquals(groupMode.aggrProps);
                        if (!changedAggrProps.isEmpty()) {
                            if (BaseUtils.hashEquals(groupMode.groupProps, setGroupMode.groupProps)) { // groups are the same, so just set changed props for update
                                mChangedDrawProps.addAll(changedAggrProps.keys());
                                this.groupMode = new GroupMode(groupMode.groupProps, groupMode.aggrProps.override(changedAggrProps));
                            } else {
                                if (!changedAggrProps.isEmpty()) // if groups are changed and we need more props it makes sense to update everything
                                    updateKeys = true;
                            }
                        }
                    }
                }
            } else {
                if (groupMode != null)
                    updateKeys = true;
            }
        }

        boolean updateObject = (updated & UPDATED_OBJECT) != 0;

        if(toRefresh())
            result.updateStateObjects.add(this, isPending());
        if(!hidden && toUpdate()) {
            updateKeys |= pendingUpdateKeys; updateObjects |= pendingUpdateObjects; updateObject |= pendingUpdateObject; updatePageSize |= pendingUpdatePageSize; updateFilters |= pendingUpdateFilters;
            checkPending(result, () -> { pendingUpdateKeys = false; pendingUpdateScroll = false; } );
            pendingUpdateObjects = false; pendingUpdateObject = false; pendingUpdatePageSize = false; pendingUpdateFilters = false;
        } else {
            boolean finalUpdateKeys = updateKeys; boolean changedScroll = updateObject || updatePageSize;
            checkPending(result, () -> { pendingUpdateKeys |= finalUpdateKeys; if(changedScroll) pendingUpdateScroll = updateScroll() != null; });
            pendingUpdateObjects |= updateObjects; pendingUpdateObject |= updateObject; pendingUpdatePageSize |= updatePageSize; pendingUpdateFilters |= updateFilters;
            return null;
        }

        ImMap<ObjectInstance, DataObject> currentObject = getGroupObjectValue();
        SeekObjects seeks = null;
        int direction = DIRECTION_CENTER;

        if(isInTree()) {
            if (userSeeks != null) // if there is user seek, force refresh (because delete for example uses user seeks to update view), this hack will be gone when seeks will be supported in tree
                userSeeks = null;
        } else {
            if(setGroupMode == null) {
                if (userSeeks != null) { // пользовательский поиск
                    seeks = userSeeks;
                    userSeeks = null;
                    currentObject = MapFact.EMPTY();
                } else if (updateKeys) {
                    UpdateType updateType = getUpdateType();
                    if (!updateObjects) // не изменились фильтры, порядки, ищем текущий объект
                        updateType = UpdateType.PREV;
                    seeks = getSeekObjects(updateType);
                } else if (updateObject || updatePageSize) {
                    Pair<SeekObjects, Integer> scroll = updateScroll();
                    if(scroll != null) {
                        updateKeys = true;
                        seeks = scroll.first;
                        if(scroll.second != null)
                            direction = scroll.second;
                    }
                }
            }
        }

        if (updateKeys)
            return readKeys(result, updateFilters, updatePageSize, currentObject, seeks, direction, sql, env, modifier, execEnv, baseClass, reallyChanged);

        return null; // ничего не изменилось
    }

    public Pair<SeekObjects, Integer> updateScroll() {
        Pair<SeekObjects, Integer> scroll = null;
        ImMap<ObjectInstance, DataObject> currentObject;
        if (viewType.isList() && !(currentObject = getGroupObjectValue()).isEmpty()) { // scrolling in grid
            int keyNum = keys.indexOf(currentObject);
            int pageSize = getPageSize();
            if (upKeys && keyNum < pageSize) { // если меньше PageSize осталось и сверху есть ключи
                int lowestInd = pageSize * 2 - 1;
                if (lowestInd >= keys.size()) // по сути END
                    scroll = new Pair<>(SEEK_END, null);
                else
                    scroll = new Pair<>(new SeekOrderObjects(keys.getValue(lowestInd), false), DIRECTION_UP);
            } else // наоборот вниз
                if (downKeys && keyNum >= keys.size() - pageSize) { // assert что pageSize не null
                    int highestInd = keys.size() - pageSize * 2;
                    if (highestInd < 0) // по сути HOME
                        scroll = new Pair<>(SEEK_HOME, null);
                    else
                        scroll = new Pair<>(new SeekOrderObjects(keys.getValue(highestInd), false), DIRECTION_DOWN);
                }
        }
        return scroll;
    }

    public ImMap<ObjectInstance, DataObject> readKeys(MFormChanges result, boolean updateFilters, boolean updatePageSize, ImMap<ObjectInstance, DataObject> currentObject, SeekObjects seeks, int direction, SQLSession sql, QueryEnvironment env, Modifier modifier, ExecutionEnvironment execEnv, BaseClass baseClass, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        updated = (updated | UPDATED_KEYS);

        if (!viewType.isList()) { // панель
            ImMap<ObjectInstance, DataObject> readKeys = seeks.readKeys(sql, env, modifier, baseClass, reallyChanged);
            updateViewProperty(execEnv, readKeys);
            return readKeys;
        } else {
            int activeRow = -1; // какой ряд выбранным будем считать
            if (isInTree()) { // если дерево, то без поиска, но возможно с parent'ами
                ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<Object, ObjectValue>> treeElements = executeTree(sql, env, modifier, baseClass, reallyChanged);

                ImList<ImMap<ObjectInstance, DataObject>> expandParents = treeElements.mapListValues(
                        value -> immutableCast(
                                value.filterFn(
                                        (key, value1) -> key instanceof ObjectInstance && value1 instanceof DataObject)));
                keys = treeElements.mapOrderValues(MapFact::EMPTY);

                ImOrderMap<ImMap<ObjectInstance, DataObject>, Boolean> groupExpandables =
                        treeElements
                                .filterOrderValuesMap(element -> element.containsKey("expandable") || element.containsKey("expandable2"))
                                .mapOrderValues(
                                        (Function<ImMap<Object, ObjectValue>, Boolean>) value -> {
                                            ObjectValue expandable1 = value.get("expandable");
                                            ObjectValue expandable2 = value.get("expandable2");
                                            return expandable1 instanceof DataObject || expandable2 instanceof DataObject;
                                        });

                result.expandables.exclAdd(this, groupExpandables.getMap());
                result.parentObjects.exclAdd(this, expandParents);
                activeRow = keys.size() == 0 ? -1 : 0;
            } else {
                keys = MapFact.EMPTYORDER();

                if(setGroupMode != null) {
                    boolean useGroupMode = groupMode != null;
                    if(updateFilters || updatePageSize)
                        useGroupMode = false;

                    if(!useGroupMode) { // we are not sure
                        int pageSize = getPageSize();
                        keys = SEEK_HOME.executeOrders(sql, env, modifier, baseClass, pageSize, true, reallyChanged);
                        if(keys.size() == pageSize)
                            useGroupMode = true;
                    }

                    if(useGroupMode) {
                        this.groupMode = setGroupMode;
                        keys = executeGroup(sql, env, modifier, baseClass, getGroupPageSize(), reallyChanged);
                    } else
                        this.groupMode = null;
                } else {
                    this.groupMode = null;

                    int pageSize = getPageSize();
                    // so far we'll consider that all lists are dynamic, i.e. show rows around current object
//                    if(isDynamicList()) {
                        SeekOrderObjects orderSeeks;
                        if (seeks == SEEK_NULL)
                            orderSeeks = SEEK_HOME;
                        else
                            orderSeeks = (SeekOrderObjects) seeks;

                        if (!orders.starts(orderSeeks.values.keys())) // если не "хватает" спереди ключей, дочитываем
                            orderSeeks = orderSeeks.readValues(sql, env, modifier, baseClass, reallyChanged);

                        if (direction == DIRECTION_CENTER) { // оптимизируем если HOME\END, то читаем одним запросом
                            if (orderSeeks.values.isEmpty()) {
                                if (orderSeeks.end) { // END
                                    direction = DIRECTION_UP;
                                    downKeys = false;
                                } else { // HOME
                                    direction = DIRECTION_DOWN;
                                    upKeys = false;
                                }
                            }
                        } else {
                            downKeys = true;
                            upKeys = true;
                            assert !orderSeeks.values.isEmpty();
                        }

                        int readSize = pageSize * 3 / (direction == DIRECTION_CENTER ? 2 : 1);

                        if (direction == DIRECTION_UP || direction == DIRECTION_CENTER) { // сначала Up
                            keys = keys.addOrderExcl(orderSeeks.executeOrders(sql, env, modifier, baseClass, readSize, false, reallyChanged).reverseOrder());
                            upKeys = (keys.size() == readSize);
                            activeRow = keys.size() - 1;
                        }
                        if (direction == DIRECTION_DOWN || direction == DIRECTION_CENTER) { // затем Down
                            ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> executeList = orderSeeks.executeOrders(sql, env, modifier, baseClass, readSize, true, reallyChanged);
                            if (executeList.size() > 0 && !(orderSeeks.end && activeRow > 0))
                                activeRow = keys.size(); // не выбираем если идет seekDown и уже выбран ряд - это то что надо
                            keys = keys.addOrderExcl(executeList);
                            downKeys = (executeList.size() == readSize);
                        }
//                    } else {
//                        keys = SEEK_HOME.executeOrders(sql, env, modifier, baseClass, pageSize, true, reallyChanged);
//                    }
                }
            }

            // параллельно будем обновлять ключи чтобы JoinSelect'ить
            keyTable.writeKeys(sql, keys.keys(), env.getOpOwner());
            result.gridObjects.exclAdd(this, keys.keyOrderSet());

            updateViewProperty(execEnv, keyTable);

            if(seeks == SEEK_NULL)
                return MapFact.EMPTY();

            if (!keys.containsKey(currentObject)) { // если нету currentObject'а, его нужно изменить
                if(getUpTreeGroup()==null) // если верхняя группа
                    return activeRow>=0?keys.getKey(activeRow):MapFact.EMPTY();
                else // иначе assertion что activeRow < 0, выбираем верхнюю
                    return keys.size()>0 && !currentObject.isEmpty()?keys.getKey(0):null;
            } else // так как сейчас клиент требует прислать ему groupObject даже если он не изменился, если приходят ключи
                return currentObject;
        }
    }

    public ListViewType listViewType;
    public void changeListViewType(ExecutionEnvironment execEnv, ConcreteCustomClass listViewType, ListViewType value) throws SQLException, SQLHandledException {
        this.listViewType = value;
        execEnv.change(entity.getListViewType(listViewType).property, new PropertyChange<>(listViewType.getDataObject(value.getObjectName())));
    }

    private void updateViewProperty(ExecutionEnvironment execEnv, ImMap<ObjectInstance, DataObject> keys) throws SQLException, SQLHandledException {
        PropertyRevImplement<ClassPropertyInterface, ObjectInstance> viewProperty = props.get(GroupObjectProp.VIEW);
        if(viewProperty != null) {
            updateViewProperty(execEnv, viewProperty, keys.isEmpty() ? new PropertyChange<>(viewProperty.property.getMapKeys(), ValueExpr.TRUE, Where.FALSE()) :
                    new PropertyChange<>(ValueExpr.TRUE, viewProperty.mapping.join(keys)));
        }
    }
    
    private void updateViewProperty(ExecutionEnvironment execEnv, NoPropertyTableUsage<ObjectInstance> keyTable1) throws SQLException, SQLHandledException {
        PropertyRevImplement<ClassPropertyInterface, ObjectInstance> viewProperty = props.get(GroupObjectProp.VIEW);
        if(viewProperty != null) {
            ImRevMap<ObjectInstance, KeyExpr> mapKeys = getMapKeys();
            updateViewProperty(execEnv, viewProperty, new PropertyChange<>(viewProperty.mapping.join(mapKeys), ValueExpr.TRUE, keyTable1.join(mapKeys).getWhere()));
        }
    }

    private void updateViewProperty(ExecutionEnvironment execEnv, PropertyRevImplement<ClassPropertyInterface, ObjectInstance> viewProperty, PropertyChange<ClassPropertyInterface> change) throws SQLException, SQLHandledException {
        execEnv.getSession().dropChanges((SessionDataProperty)viewProperty.property);
        execEnv.change(viewProperty.property, change);
    }

    public ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> seekObjects(SQLSession sql, QueryEnvironment env, Modifier modifier, BaseClass baseClass, int readSize) throws SQLException, SQLHandledException {
        SeekOrderObjects orderSeeks = new SeekOrderObjects(keys.getValue(keys.indexOf(getGroupObjectValue())), false);
        return orderSeeks.executeOrders(sql, env, modifier, baseClass, readSize, true, null);
    }

    public ImOrderSet<ImMap<ObjectInstance, DataObject>> createObjects(DataSession session, FormInstance form, int quantity, ExecutionStack stack) throws SQLException, SQLHandledException {
        if (objects.size() > 1) {
            return SetFact.EMPTYORDER();
        }
        MOrderExclSet<ImMap<ObjectInstance, DataObject>> mResultSet = SetFact.mOrderExclSet(quantity);
        for (int i = 0; i < quantity; i++) {
            ImFilterValueMap<ObjectInstance, DataObject> mvObjectKeys = objects.mapFilterValues();
            for (int j=0,size=objects.size();j<size;j++) {
                ObjectInstance objectInstance = objects.get(j);
                if (objectInstance.getBaseClass() instanceof ConcreteCustomClass)
                    mvObjectKeys.mapValue(j, form.addFormObject((CustomObjectInstance)objectInstance, (ConcreteCustomClass) objectInstance.getBaseClass(), null, stack));
            }
            mResultSet.exclAdd(mvObjectKeys.immutableValue());
        }
        return mResultSet.immutableOrder();
    }

    private GroupObjectInstance() {
        propertyBackground = null;
        propertyForeground = null;

        entity = null;

        orderObjects = null;

        parent = null;
    }
    public static final GroupObjectInstance NULL = new GroupObjectInstance(); // hack for ImMap key
    
    public interface SeekObjects {
        ImMap<ObjectInstance, DataObject> readKeys(SQLSession session, QueryEnvironment env, Modifier modifier, BaseClass baseClass, ReallyChanged reallyChanged) throws SQLException, SQLHandledException;
    }
    
    public class SeekNullObjects implements SeekObjects {

        private SeekNullObjects() {
        }

        @Override
        public ImMap<ObjectInstance, DataObject> readKeys(SQLSession session, QueryEnvironment env, Modifier modifier, BaseClass baseClass, ReallyChanged reallyChanged) {
            return MapFact.EMPTY();
        }
    }
    private final SeekNullObjects SEEK_NULL = new SeekNullObjects();

    public class SeekOrderObjects implements SeekObjects {
        public ImMap<OrderInstance, ObjectValue> values;
        public boolean end;

        public SeekOrderObjects(ImMap<OrderInstance, ObjectValue> values, boolean end) {
            this.values = values;
            this.end = end;
        }

        public SeekOrderObjects(boolean end, ImMap<ObjectInstance, DataObject> values) {
            this(BaseUtils.immutableCast(values), end);
        }

        public SeekOrderObjects(boolean end) {
            this(MapFact.EMPTY(), end);
        }

        public SeekOrderObjects add(OrderInstance order, ObjectValue value, boolean down) {
            return new SeekOrderObjects(values.override(order, value), this.end || down);
        }

        public SeekOrderObjects remove(ObjectInstance object) {
            return new SeekOrderObjects(values.remove(object), end);
        }

        // оптимизация, так как при Limit 1 некоторые СУБД начинают чудить
        private ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> executeSingleOrders(SQLSession session, QueryEnvironment env, final Modifier modifier, BaseClass baseClass, ReallyChanged reallyChanged, Result<ImMap<OrderInstance, ObjectValue>> fullOrderValues) throws SQLException, SQLHandledException {
            assert !isInTree();

            ImMap<ObjectInstance, ObjectValue> objectValues;
            ImMap<ObjectInstance, DataObject> dataObjects;
            if(!(Settings.get().isEnableSingleReadObjectsOptimization() && // работает, только, если в поиске есть все объекты и все DataObject
                    (objectValues = values.filter(objects)).size() == objects.size() &&
                    (dataObjects = DataObject.onlyDataObjects(objectValues)) != null)) {
                fullOrderValues.set(values);
                return MapFact.EMPTYORDER();
            }

            final ImRevMap<ObjectInstance, KeyExpr> mapKeys = getMapKeys();

            assert orders.keys().containsAll(values.keys());

            ImMap<OrderInstance, Expr> orderExprs = orders.getMap().mapKeyValuesEx((ThrowingFunction<OrderInstance, Expr, SQLException, SQLHandledException>) value -> value.getExpr(mapKeys, modifier));

            String filterKey = "filter";
            ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<Object, ObjectValue>> orderFilterValues = 
                    castExecuteObjects(new Query<>(mapKeys, MapFact.addExcl(orderExprs, filterKey, ValueExpr.get(getWhere(mapKeys, modifier, reallyChanged))), CompareWhere.compareInclValues(orderExprs, dataObjects)).
                        executeClasses(session, env, baseClass));

            ObjectValue filterValue = orderFilterValues.singleValue().get(filterKey);
            ImMap<OrderInstance, ObjectValue> orderValues = BaseUtils.immutableCast(orderFilterValues.singleValue().removeIncl(filterKey));
            if(filterValue.isNull()) { // не попал в фильтр
                fullOrderValues.set(orderValues);
                return MapFact.EMPTYORDER();
            } else {
                return MapFact.singletonOrder(orderFilterValues.singleKey(), orderValues);
            }
        }

        // возвращает OrderInstance из orderSeeks со значениями, а также если есть parent, то parent'ы
        public ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> executeOrders(SQLSession session, QueryEnvironment env, final Modifier modifier, BaseClass baseClass, int readSize, boolean down, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
            assert !isInTree();

            final ImRevMap<ObjectInstance, KeyExpr> mapKeys = getMapKeys();

            // assertion что group.orders начинается с orderSeeks
            ImOrderMap<OrderInstance, Boolean> orders;
            if (readSize == 1)
                orders = GroupObjectInstance.this.orders.moveStart(values.keys());
            else
                orders = GroupObjectInstance.this.orders;

            assert orders.starts(values.keys());

            ImMap<OrderInstance, Expr> orderExprs = orders.getMap().mapKeyValuesEx((ThrowingFunction<OrderInstance, Expr, SQLException, SQLHandledException>) value -> value.getExpr(mapKeys, modifier));

            Where orderWhere = end?Where.FALSE():Where.TRUE(); // строим условия на упорядочивание
            ImOrderMap<OrderInstance, Boolean> reverseOrder = orders.reverseOrder();
            for (int i=0,size=reverseOrder.size();i<size;i++) {
                OrderInstance orderInstance = reverseOrder.getKey(i);
                ObjectValue toSeek = values.get(orderInstance);
                if (toSeek != null)
                    orderWhere = toSeek.order(orderExprs.get(orderInstance), reverseOrder.getValue(i), orderWhere);
            }

            if(!down)
                orderWhere = orderWhere.not();

            if (readSize == 1) { // в частном случае если есть "висячие" ключи не в фильтре и нужна одна запись ставим равно вместо >
                for(DataObjectInstance freeObject : getFreeDataObjects()) {
                    ObjectValue freeValue = values.get(freeObject);
                    if(freeValue==null || !(freeValue instanceof DataObject))
                        freeValue = freeObject.getBaseClass().getDefaultObjectValue();
                    orderWhere = orderWhere.and(end==!down?mapKeys.get(freeObject).compare((DataObject)freeValue, Compare.EQUALS):Where.FALSE()); // seekDown==!down, чтобы и вверх и вниз не попали одни и те же ключи
                }
            }

            return castExecuteObjects(new Query<>(mapKeys, orderExprs, getWhere(mapKeys, modifier, reallyChanged).and(orderWhere)).
                        executeClasses(session, down ? orders : Query.reverseOrder(orders), readSize, baseClass, env));
        }

        // считывает одну запись
        private Pair<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> readObjects(SQLSession session, QueryEnvironment env, Modifier modifier, BaseClass baseClass, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
            Result<ImMap<OrderInstance, ObjectValue>> fullOrderValues = new Result<>();
            ImOrderMap<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> result = executeSingleOrders(session, env, modifier, baseClass, reallyChanged, fullOrderValues);
            if(result.size() == 0)
                result = new SeekOrderObjects(fullOrderValues.result, end).executeOrders(session, env, modifier, baseClass, 1, !end, reallyChanged);
            if (result.size() == 0)
                result = new SeekOrderObjects(fullOrderValues.result, !end).executeOrders(session, env, modifier, baseClass, 1, end, reallyChanged);
            if (result.size() > 0)
                return new Pair<>(result.singleKey(), result.singleValue());
            else
                return null;
        }

        public ImMap<ObjectInstance, DataObject> readKeys(SQLSession session, QueryEnvironment env, Modifier modifier, BaseClass baseClass, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
            Pair<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> objects = readObjects(session, env, modifier, baseClass, reallyChanged);
            if (objects != null)
                return objects.first;
            else
                return MapFact.EMPTY();
        }

        public SeekOrderObjects readValues(SQLSession session, QueryEnvironment env, Modifier modifier, BaseClass baseClass, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
            Pair<ImMap<ObjectInstance, DataObject>, ImMap<OrderInstance, ObjectValue>> objects = readObjects(session, env, modifier, baseClass, reallyChanged);
            if (objects != null)
                return new SeekOrderObjects(objects.second, end);
            else
                return SEEK_HOME;
        }
    }

    public NoPropertyTableUsage<ObjectInstance> createKeyTable(String debugInfo) {
        return new NoPropertyTableUsage<>(debugInfo, GroupObjectInstance.getOrderObjects(getOrderUpTreeGroups()), typeGetter);                
    }

    private final static Type.Getter<ObjectInstance> typeGetter = ObjectInstance::getType;

    public class RowBackgroundReaderInstance implements PropertyReaderInstance {
        public PropertyObjectInstance getPropertyObjectInstance() {
            return propertyBackground;
        }

        public byte getTypeID() {
            return PropertyReadType.ROW_BACKGROUND;
        }

        public int getID() {
            return GroupObjectInstance.this.getID();
        }

        @Override
        public String toString() {
            return ThreadLocalContext.localize("{logics.background} (") + GroupObjectInstance.this.toString() + ")";
        }

        @Override
        public Object getProfiledObject() {
            return entity.propertyBackground;
        }
    }

    public class RowForegroundReaderInstance implements PropertyReaderInstance {
        public PropertyObjectInstance getPropertyObjectInstance() {
            return propertyForeground;
        }

        public byte getTypeID() {
            return PropertyReadType.ROW_FOREGROUND;
        }

        public int getID() {
            return GroupObjectInstance.this.getID();
        }

        @Override
        public String toString() {
            return ThreadLocalContext.localize("{logics.foreground}") + " (" + GroupObjectInstance.this.toString() + ")";
        }

        @Override
        public Object getProfiledObject() {
            return entity.propertyForeground;
        }
    }
}
