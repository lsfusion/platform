package platform.server.view.form;

import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.logics.DataObject;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyImplement;
import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.properties.DataPropertyInterface;
import platform.server.session.MapChangeDataProperty;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;

import java.sql.SQLException;
import java.util.*;

public class PropertyObjectImplement<P extends PropertyInterface> extends PropertyImplement<PropertyObjectInterface,P> implements OrderView {

    public PropertyObjectImplement(Property<P> property,Map<P,? extends PropertyObjectInterface> mapping) {
        super(property, (Map<P,PropertyObjectInterface>) mapping);
    }

    // получает Grid в котором рисоваться
    public GroupObjectImplement getApplyObject() {
        GroupObjectImplement applyObject=null;
        for(PropertyObjectInterface intObject : mapping.values())
            if(applyObject==null || intObject.getApplyObject().order >applyObject.order)
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
    
    public MapChangeDataProperty<P> getChangeProperty(ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        Map<P, ConcreteClass> interfaceClasses = new HashMap<P,ConcreteClass>();
        for(Map.Entry<P,PropertyObjectInterface> implement : mapping.entrySet())
            interfaceClasses.put(implement.getKey(),implement.getValue().getObjectClass());
        return property.getChangeProperty(interfaceClasses,securityPolicy, externalID);
    }

    public SourceExpr getSourceExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends SourceExpr> classSource, TableModifier<? extends TableChanges> modifier) throws SQLException {

        Map<P, SourceExpr> joinImplement = new HashMap<P,SourceExpr>();
        for(P propertyInterface : property.interfaces)
            joinImplement.put(propertyInterface, mapping.get(propertyInterface).getSourceExpr(classGroup, classSource, modifier));
        return property.getSourceExpr(joinImplement,modifier,null);
    }

    public Type getType() {
        return property.getType();
    }
}
