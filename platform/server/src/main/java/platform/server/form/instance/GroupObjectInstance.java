package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.ClassViewType;
import platform.interop.Order;
import platform.interop.form.PropertyRead;
import platform.interop.form.RemoteFormInterface;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.where.Where;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.Property;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.*;

public class GroupObjectInstance implements MapKeysInterface<ObjectInstance>, PropertyReadInstance {

    public final PropertyObjectInstance propertyHighlight;

    public PropertyObjectInstance getPropertyObject() {
        return propertyHighlight;
    }

    public byte getTypeID() {
        return PropertyRead.HIGHLIGHT;
    }

    public List<ObjectInstance> getSerializeList(Set<PropertyDrawInstance> panelProperties) {
        return curClassView == ClassViewType.GRID ? GroupObjectInstance.getObjects(getUpTreeGroups()) : new ArrayList<ObjectInstance>();
    }

    GroupObjectEntity entity;

    public static List<ObjectInstance> getObjects(Collection<GroupObjectInstance> groups) {
        List<ObjectInstance> result = new ArrayList<ObjectInstance>();
        for(GroupObjectInstance group : groups)
            result.addAll(group.objects);
        return result;
    }

    public Collection<ObjectInstance> objects;

    // глобальный идентификатор чтобы писать во GroupObjectTable
    public int getID() {
        return entity.getID();
    }

    private int pageSize = 0;
    public int getPageSize() {
        assert !isInTree();
        return pageSize;
    }
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public GroupObjectInstance(GroupObjectEntity entity, Collection<ObjectInstance> objects, PropertyObjectInstance propertyHighlight, Map<ObjectInstance, PropertyObjectInstance> parent) {

        this.entity = entity;

        assert (getID() < RemoteFormInterface.GID_SHIFT);

        this.objects = objects;

        this.propertyHighlight = propertyHighlight;

        for(ObjectInstance object : objects)
            object.groupTo = this;

        // текущее состояние
        if (this.curClassView != entity.initClassView) {
            this.curClassView = entity.initClassView;
            this.updated |= UPDATED_CLASSVIEW;
        }
        this.pageSize = entity.pageSize;

        this.parent = parent;
    }

    public Map<ObjectInstance, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(objects);
    }

    public Integer order = 0;

    // классовый вид включен или нет
    public ClassViewType curClassView = ClassViewType.GRID;

    // закэшированные

    public Set<FilterInstance> setFilters = null;
    public Set<FilterInstance> getSetFilters() {
        if(setFilters==null)
            setFilters = BaseUtils.mergeSet(BaseUtils.mergeSet(BaseUtils.mergeSet(fixedFilters,userFilters),regularFilters),tempFilters);
        return setFilters;
    }

    // вообще все фильтры
    public Set<FilterInstance> fixedFilters = new HashSet<FilterInstance>();

    private Set<FilterInstance> userFilters = new HashSet<FilterInstance>();
    public void clearUserFilters() {
        userFilters.clear();

        setFilters = null;
        updated |= UPDATED_FILTER;
    }
    public void addUserFilter(FilterInstance addFilter) {
        userFilters.add(addFilter);

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    private Set<FilterInstance> regularFilters = new HashSet<FilterInstance>();
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

    private Set<FilterInstance> tempFilters = new HashSet<FilterInstance>();
    public void clearTempFilters() {
        tempFilters.clear();

        setFilters = null;
        updated |= UPDATED_FILTER;
    }
    public void addTempFilter(FilterInstance addFilter) {
        tempFilters.add(addFilter);

        setFilters = null;
        updated |= UPDATED_FILTER;
    }

    // с активным интерфейсом
    public Set<FilterInstance> filters = new HashSet<FilterInstance>();

    public OrderedMap<OrderInstance, Boolean> fixedOrders = new OrderedMap<OrderInstance, Boolean>();

    // обертку потому как сложный assertion
    private OrderedMap<OrderInstance,Boolean> setOrders = null;
    public OrderedMap<OrderInstance,Boolean> getSetOrders() {
        if(setOrders==null) {
            setOrders = new OrderedMap<OrderInstance,Boolean>(userOrders);
            setOrders.putAll(fixedOrders);
            for(ObjectInstance object : objects)
                if(!(setOrders.containsKey(object)))
                    setOrders.put(object,false);
        }
        return setOrders;
    }
    private OrderedMap<OrderInstance,Boolean> userOrders = new OrderedMap<OrderInstance, Boolean>();

    public void changeOrder(OrderInstance property, Order modiType) {
        if (modiType == Order.REPLACE) {
            userOrders = new OrderedMap<OrderInstance, Boolean>();
        }

        if (modiType == Order.REMOVE) {
            userOrders.remove(property);
        } else if (modiType == Order.DIR) {
            userOrders.put(property, !userOrders.get(property));
        } else {
            userOrders.put(property, false);
        }

        setOrders = null;
        updated |= UPDATED_ORDER;
    }

    // с активным интерфейсом
    OrderedMap<OrderInstance,Boolean> orders = new OrderedMap<OrderInstance, Boolean>();

    public void fillUpdateProperties(Set<Property> properties) {
        for(FilterInstance filter : filters)
            filter.fillProperties(properties);
        for(OrderInstance order : orders.keySet())
            order.fillProperties(properties);
    }

    boolean upKeys, downKeys;
    OrderedMap<Map<ObjectInstance,DataObject>,Map<OrderInstance,ObjectValue>> keys = new OrderedMap<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>>();

    // 0 !!! - изменился объект, 1 - класс объекта, 2 !!! - отбор, 3 !!! - хоть один класс, 4 !!! - классовый вид

    public final static int UPDATED_OBJECT = (1);
    public final static int UPDATED_KEYS = (1 << 2);
    public final static int UPDATED_GRIDCLASS = (1 << 3);
    public final static int UPDATED_CLASSVIEW = (1 << 4);
    public final static int UPDATED_ORDER = (1 << 5);
    public final static int UPDATED_FILTER = (1 << 6);
    public final static int UPDATED_EXPANDS = (1 << 7);

    public int updated = UPDATED_GRIDCLASS | UPDATED_ORDER | UPDATED_FILTER;

    private boolean assertNull() {
        Iterator<ObjectInstance> it = objects.iterator();
        boolean isNull = it.next().getObjectValue() instanceof NullValue; 
        for(ObjectInstance object : objects)
            if((object.getObjectValue() instanceof NullValue)!=isNull)
                return false;
        return true;
    }

    public boolean isNull() {
        assert assertNull();
        return objects.iterator().next().getObjectValue() instanceof NullValue;
    }

    public Map<ObjectInstance,DataObject> getGroupObjectValue() {
        if(isNull())
            return new HashMap<ObjectInstance, DataObject>();
        
        Map<ObjectInstance,DataObject> result = new HashMap<ObjectInstance, DataObject>();
        for(ObjectInstance object : GroupObjectInstance.getObjects(getUpTreeGroups()))
            result.put(object,object.getDataObject());
        return result;
    }

    public Map<ObjectInstance,DataObject> findGroupObjectValue(Map<ObjectInstance,Object> map) {
        for(Map<ObjectInstance,DataObject> keyRow : keys.keySet()) {
            boolean equal = true;
            for(Map.Entry<ObjectInstance,DataObject> keyEntry : keyRow.entrySet()) {
                if(!keyEntry.getValue().object.equals(map.get(keyEntry.getKey()))) {
                    equal = false;
                    break;
                }
            }
            if(equal) {
                return keyRow;
            }
        }

        throw new RuntimeException("key not found");
    }

    public Where getFilterWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier) throws SQLException {
        Where where = Where.TRUE;
        for(FilterInstance filt : filters)
            where = where.and(filt.getWhere(mapKeys, modifier));
        return where;
    }

    private Where getClassWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier) {
        Where where = Where.TRUE;
        for(ObjectInstance object : objects)
            where = where.and(modifier.getSession().getIsClassWhere(mapKeys.get(object), object.getGridClass(), null));
        return where;
    }

    public Where getWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier) throws SQLException {
        return getFilterWhere(mapKeys, modifier).and(getClassWhere(mapKeys, modifier));
    }

    public Map<ObjectInstance,ObjectValue> getNulls() {
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
    public List<GroupObjectInstance> getDownTreeGroups() {
        return treeGroup!=null?treeGroup.getDownTreeGroups(this):new ArrayList<GroupObjectInstance>();
    }
    public GroupObjectInstance getUpTreeGroup() {
        return treeGroup!=null?treeGroup.getUpTreeGroup(this):null;
    }
    public List<GroupObjectInstance> getUpTreeGroups() {
        return treeGroup!=null?treeGroup.getUpTreeGroups(this):Collections.singletonList(this);
    }

    public static Set<GroupObjectInstance> getUpTreeGroups(Set<GroupObjectInstance> groups) {
        Set<GroupObjectInstance> result = new HashSet<GroupObjectInstance>();
        for(GroupObjectInstance group : groups)
            result.addAll(group.getUpTreeGroups());
        return result;
    }

    public final Map<ObjectInstance, PropertyObjectInstance> parent;
}
