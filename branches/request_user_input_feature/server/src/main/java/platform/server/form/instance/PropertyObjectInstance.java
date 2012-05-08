package platform.server.form.instance;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.interop.Compare;
import platform.interop.action.ClientAction;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ConcreteClass;
import platform.server.classes.CustomClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.form.instance.filter.CompareValue;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyValueImplement;
import platform.server.session.DataSession;
import platform.server.session.ExecutionEnvironment;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.*;

public class PropertyObjectInstance<P extends PropertyInterface> extends PropertyImplement<P, PropertyObjectInterfaceInstance> implements OrderInstance {

    public PropertyObjectInstance(Property<P> property,Map<P,? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, (Map<P, PropertyObjectInterfaceInstance>) mapping);
    }

    // получает GRID в котором рисоваться
    public GroupObjectInstance getApplyObject() {
        GroupObjectInstance applyObject=null;
        for(ObjectInstance intObject : getObjectInstances())
            if(applyObject==null || intObject.groupTo.order >applyObject.order)
                applyObject = intObject.getApplyObject();

        return applyObject;
    }

    public Collection<ObjectInstance> getObjectInstances() {
        Collection<ObjectInstance> result = new ArrayList<ObjectInstance>();
        for(PropertyObjectInterfaceInstance object : mapping.values())
            if(object instanceof ObjectInstance)
                result.add((ObjectInstance) object);
        return result;
    }

    // в интерфейсе
    public boolean isInInterface(GroupObjectInstance classGroup) {
        return isInInterface(classGroup==null?new HashSet<GroupObjectInstance>():Collections.singleton(classGroup), false);
    }

    public boolean isInInterface(Set<GroupObjectInstance> classGroups, boolean any) {
        // assert что classGroups все в GRID представлении
        Map<P, AndClassSet> classImplement = new HashMap<P, AndClassSet>();
        for(P propertyInterface : property.interfaces)
            classImplement.put(propertyInterface, mapping.get(propertyInterface).getClassSet(classGroups));
        return property.isInInterface(classImplement, any);
    }

    // проверяет на то что изменился верхний объект
    public boolean objectUpdated(Set<GroupObjectInstance> gridGroups) {
        for(PropertyObjectInterfaceInstance intObject : mapping.values())
            if(intObject.objectUpdated(gridGroups)) return true;

        return false;
    }

    public boolean classUpdated(Set<GroupObjectInstance> gridGroups) {
        for(PropertyObjectInterfaceInstance intObject : mapping.values())
            if(intObject.classUpdated(gridGroups))
                return true;

        return false;
    }

    public boolean dataUpdated(Collection<Property> changedProps) {
        return changedProps.contains(property);
    }

    public void fillProperties(Set<Property> properties) {
        properties.add(property);
    }

    public Map<P, ConcreteClass> getInterfaceClasses(P overrideInterface, ConcreteClass overrideClass) {
        Map<P,ConcreteClass> mapInterface = new HashMap<P,ConcreteClass>();
        for(Map.Entry<P, PropertyObjectInterfaceInstance> implement : mapping.entrySet())
            if(overrideInterface!=null && implement.getKey().equals(overrideInterface))
                mapInterface.put(overrideInterface, overrideClass);
            else
                mapInterface.put(implement.getKey(),implement.getValue().getCurrentClass());
        return mapInterface;
    }

    public Map<P, ConcreteClass> getInterfaceClasses() {
        return getInterfaceClasses(null, null);
    }

    public Map<P, DataObject> getInterfaceValues() {
        Map<P,DataObject> mapInterface = new HashMap<P,DataObject>();
        for(Map.Entry<P, PropertyObjectInterfaceInstance> implement : mapping.entrySet())
            mapInterface.put(implement.getKey(),implement.getValue().getDataObject());
        return mapInterface;
    }

    public PropertyValueImplement<P> getValueImplement() {
        return new PropertyValueImplement<P>(property, getInterfaceValues());
    }

    public PropertyObjectInstance<?> getChangeInstance(Result<Property> aggProp, boolean aggValue, DataSession session, Modifier modifier) throws SQLException {
        if(aggValue)
            return property.getChangeImplement(aggProp, getInterfaceValues(), session, modifier).mapObjects(mapping);
        else
            return this;
    }

    public Type getEditorType() {
        return property.getEditorType(mapping);
    }

    public Object read(SQLSession session, Modifier modifier, QueryEnvironment env) throws SQLException {
        return property.read(session, getInterfaceValues(), modifier, env);
    }

    public Object read(DataSession session, Modifier modifier) throws SQLException {
        return read(session.sql, modifier, session.env);
    }

    public List<ClientAction> execute(ExecutionEnvironment env, CompareValue getterValue, GroupObjectInstance groupObject) throws SQLException {
        Map<P, KeyExpr> mapKeys = property.getMapKeys();
        Modifier modifier = env.getModifier();

        Map<ObjectInstance, ? extends Expr> groupKeys = new HashMap<ObjectInstance, KeyExpr>();
        Where changeWhere = Where.TRUE;
        if (groupObject != null) { // в общем то replace потому как changeImplement может быть с меньшим количеством ключей, а для getWhere они все равно нужны
            groupKeys = BaseUtils.replace(DataObject.getMapExprs(groupObject.getGroupObjectValue()), BaseUtils.crossJoin(mapping, mapKeys));
            changeWhere = groupObject.getWhere(groupKeys, modifier);
        }

        for (Map.Entry<P, PropertyObjectInterfaceInstance> mapObject : mapping.entrySet()) {
            changeWhere = changeWhere.and(mapKeys.get(mapObject.getKey()).compare(mapObject.getValue().getExpr(groupKeys, modifier), Compare.EQUALS));
        }

        return env.execute(property, new PropertyChange<P>(mapKeys, getterValue.getExpr(groupKeys, modifier), changeWhere), mapping);
    }

    public Expr getExpr(Map<ObjectInstance, ? extends Expr> classSource, Modifier modifier) {

        Map<P, Expr> joinImplement = new HashMap<P, Expr>();
        for(P propertyInterface : property.interfaces)
            joinImplement.put(propertyInterface, mapping.get(propertyInterface).getExpr(classSource, modifier));
        return property.getExpr(joinImplement,modifier);
    }

    public Type getType() {
        return property.getType();
    }

    public CustomClass getDialogClass() {
        return property.getDialogClass(getInterfaceValues(), getInterfaceClasses(), mapping);
    }

    @IdentityLazy
    public PropertyObjectInstance getRemappedPropertyObject(Map<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        Map<P, PropertyObjectInterfaceInstance> remapping = new HashMap<P, PropertyObjectInterfaceInstance>();
        remapping.putAll(mapping);
        for (P propertyInterface : property.interfaces) {

            DataObject dataObject = mapKeyValues.get(remapping.get(propertyInterface));
            if (dataObject != null) {
                remapping.put(propertyInterface, dataObject);
            }
        }

        return new PropertyObjectInstance<P>(property, remapping);
    }

    @Override
    public String toString() {
        return property.toString();
    }
}
