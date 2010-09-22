package platform.server.form.instance;

import platform.interop.action.ClientAction;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ConcreteClass;
import platform.server.classes.CustomClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.data.SQLSession;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyValueImplement;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.form.instance.remote.RemoteForm;

import java.sql.SQLException;
import java.util.*;

public class PropertyObjectInstance<P extends PropertyInterface> extends PropertyImplement<PropertyObjectInterfaceInstance,P> implements OrderInstance {

    public PropertyObjectInstance(Property<P> property,Map<P,? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, (Map<P, PropertyObjectInterfaceInstance>) mapping);
    }

    // получает Grid в котором рисоваться
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
        if(any)
            return property.anyInInterface(classImplement);
        else
            return property.allInInterface(classImplement); 
    }

    // проверяет на то что изменился верхний объект
    public boolean objectUpdated(Set<GroupObjectInstance> skipGroups) {
        for(PropertyObjectInterfaceInstance intObject : mapping.values())
            if(intObject.objectUpdated(skipGroups)) return true;

        return false;
    }

    public boolean classUpdated(GroupObjectInstance classGroup) {
        for(PropertyObjectInterfaceInstance intObject : mapping.values())
            if(intObject.classUpdated(classGroup))
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
    public PropertyObjectInstance<?> getChangeInstance() {
        return property.getChangeImplement().mapObjects(mapping);
    }

    public Type getEditorType() {
        return property.getEditorType(mapping);
    }

    public Object read(SQLSession session, Modifier<? extends Changes> modifier) throws SQLException {
        return property.read(session, getInterfaceValues(), modifier);
    }    

    public List<ClientAction> execute(DataSession session, Object value, Modifier<? extends Changes> modifier, RemoteForm executeForm, GroupObjectInstance groupObject) throws SQLException {
        return property.execute(getInterfaceValues(), session, value, modifier, executeForm, mapping, groupObject);
    }

    public Expr getExpr(Map<ObjectInstance, ? extends Expr> classSource, Modifier<? extends Changes> modifier) throws SQLException {

        Map<P, Expr> joinImplement = new HashMap<P, Expr>();
        for(P propertyInterface : property.interfaces)
            joinImplement.put(propertyInterface, mapping.get(propertyInterface).getExpr(classSource, modifier));
        return property.getExpr(joinImplement,modifier,null);
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
}
