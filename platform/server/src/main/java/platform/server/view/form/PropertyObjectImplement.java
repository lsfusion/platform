package platform.server.view.form;

import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

public class PropertyObjectImplement<P extends PropertyInterface> extends PropertyImplement<PropertyObjectInterface,P> implements OrderView {

    public PropertyObjectImplement(Property<P> property,Map<P,? extends PropertyObjectInterface> mapping) {
        super(property, (Map<P,PropertyObjectInterface>) mapping);
    }

    // получает Grid в котором рисоваться
    public GroupObjectImplement getApplyObject() {
        GroupObjectImplement applyObject=null;
        for(ObjectImplement intObject : getObjectImplements())
            if(applyObject==null || intObject.groupTo.order >applyObject.order)
                applyObject = intObject.getApplyObject();

        return applyObject;
    }

    public Collection<ObjectImplement> getObjectImplements() {
        Collection<ObjectImplement> result = new ArrayList<ObjectImplement>();
        for(PropertyObjectInterface object : mapping.values())
            if(object instanceof ObjectImplement)
                result.add((ObjectImplement) object);
        return result;
    }

    // в интерфейсе
    public boolean isInInterface(GroupObjectImplement classGroup) {

        Map<P, AndClassSet> classImplement = new HashMap<P, AndClassSet>();
        for(P propertyInterface : property.interfaces)
            classImplement.put(propertyInterface, mapping.get(propertyInterface).getClassSet(classGroup));
        return property.allInInterface(classImplement);
    }

    // проверяет на то что изменился верхний объект
    public boolean objectUpdated(GroupObjectImplement classGroup) {
        for(PropertyObjectInterface intObject : mapping.values())
            if(intObject.objectUpdated(classGroup)) return true;

        return false;
    }

    public boolean classUpdated(GroupObjectImplement classGroup) {
        for(PropertyObjectInterface intObject : mapping.values())
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

    public Map<P, DataObject> getInterfaceValues() {
        Map<P,DataObject> mapInterface = new HashMap<P,DataObject>();
        for(Map.Entry<P,PropertyObjectInterface> implement : mapping.entrySet())
            mapInterface.put(implement.getKey(),implement.getValue().getDataObject());
        return mapInterface;
    }
    
    public DataChange getChangeProperty(DataSession session, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        return property.getChangeProperty(session, getInterfaceValues(), modifier, securityPolicy, externalID);
    }

    public Expr getExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends Expr> classSource, TableModifier<? extends TableChanges> modifier) throws SQLException {

        Map<P, Expr> joinImplement = new HashMap<P, Expr>();
        for(P propertyInterface : property.interfaces)
            joinImplement.put(propertyInterface, mapping.get(propertyInterface).getExpr(classGroup, classSource, modifier));
        return property.getExpr(joinImplement,modifier,null);
    }

    public Type getType() {
        return property.getType();
    }
}
