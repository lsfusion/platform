package platform.server.form.instance;

import platform.base.TwinImmutableInterface;
import platform.base.TwinImmutableObject;
import platform.interop.action.ClientAction;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ConcreteClass;
import platform.server.classes.CustomClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.*;
import platform.server.logics.property.actions.FormEnvironment;
import platform.server.session.DataSession;
import platform.server.session.ExecutionEnvironment;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.*;

public abstract class PropertyObjectInstance<P extends PropertyInterface, T extends Property<P>> extends TwinImmutableObject implements Updated {

    public T property;
    public Map<P, PropertyObjectInterfaceInstance> mapping;

    public boolean twins(TwinImmutableInterface o) {
        return property.equals(((PropertyObjectInstance) o).property) && mapping.equals(((PropertyObjectInstance) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }

    public PropertyObjectInstance(T property,Map<P,? extends PropertyObjectInterfaceInstance> mapping) {
        this.property = property;
        this.mapping = (Map<P, PropertyObjectInterfaceInstance>) mapping;
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
        return isInInterface(classGroup == null ? new HashSet<GroupObjectInstance>() : Collections.singleton(classGroup), false);
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

    public boolean dataUpdated(Collection<CalcProperty> changedProps) {
        return changedProps.contains(property);
    }

    public void fillProperties(Set<CalcProperty> properties) {
        properties.add((CalcProperty) property);
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

    public Map<P, DataObject> getInterfaceValues() {
        Map<P,DataObject> mapInterface = new HashMap<P,DataObject>();
        for(Map.Entry<P, PropertyObjectInterfaceInstance> implement : mapping.entrySet())
            mapInterface.put(implement.getKey(),implement.getValue().getDataObject());
        return mapInterface;
    }

    public PropertyValueImplement<P> getValueImplement() {
        return new PropertyValueImplement<P>(property, getInterfaceValues());
    }

    public Object read(SQLSession session, Modifier modifier, QueryEnvironment env) throws SQLException {
        return ((CalcProperty<P>)property).read(session, getInterfaceValues(), modifier, env);
    }

    public Object read(DataSession session, Modifier modifier) throws SQLException {
        return read(session.sql, modifier, session.env);
    }

    public List<ClientAction> execute(ExecutionEnvironment env, ObjectValue requestValue, PropertyDrawInstance propertyDraw) throws SQLException {
        return env.execute((ActionProperty) property, ActionProperty.cast(getInterfaceValues()), new FormEnvironment<ClassPropertyInterface>(ActionProperty.cast(mapping), propertyDraw), requestValue);
    }

    public Expr getExpr(Map<ObjectInstance, ? extends Expr> classSource, Modifier modifier) {

        Map<P, Expr> joinImplement = new HashMap<P, Expr>();
        for(P propertyInterface : property.interfaces)
            joinImplement.put(propertyInterface, mapping.get(propertyInterface).getExpr(classSource, modifier));
        return property.getExpr(joinImplement, modifier);
    }

    public Type getType() {
        return property.getType();
    }

    protected Map<P, PropertyObjectInterfaceInstance> remap(Map<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        Map<P, PropertyObjectInterfaceInstance> remapping = new HashMap<P, PropertyObjectInterfaceInstance>();
        remapping.putAll(mapping);
        for (P propertyInterface : property.interfaces) {

            DataObject dataObject = mapKeyValues.get(remapping.get(propertyInterface));
            if (dataObject != null) {
                remapping.put(propertyInterface, dataObject);
            }
        }
        return remapping;
    }

    @Override
    public String toString() {
        return property.toString();
    }
    
    public ActionPropertyObjectInstance getEditAction(String editActionSID) {
        return property.getEditAction(editActionSID).mapObjects(mapping);
    }
}
