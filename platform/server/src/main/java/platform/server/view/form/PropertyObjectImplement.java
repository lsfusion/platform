package platform.server.view.form;

import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.DataObject;
import platform.server.logics.properties.*;
import platform.server.session.MapChangeDataProperty;
import platform.server.session.TableChanges;
import platform.server.view.form.filter.CompareValue;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PropertyObjectImplement<P extends PropertyInterface> extends PropertyImplement<ObjectImplement,P> implements CompareValue {

    public PropertyObjectImplement(Property<P> iProperty,Map<P,ObjectImplement> iMapping) {
        super(iProperty,iMapping);
    }

    // получает Grid в котором рисоваться
    public GroupObjectImplement getApplyObject() {
        GroupObjectImplement applyObject=null;
        for(ObjectImplement intObject : mapping.values())
            if(applyObject==null || intObject.groupTo.order >applyObject.order) applyObject = intObject.groupTo;

        return applyObject;
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
        for(ObjectImplement intObject : mapping.values())
            if(intObject.groupTo !=classGroup && intObject.objectUpdated()) return true;

        return false;
    }

    // изменился хоть один из классов интерфейса (могло повлиять на вхождение в интерфейс)
    public boolean classUpdated(GroupObjectImplement classGroup) {
        for(ObjectImplement intObject : mapping.values())
            if(intObject.groupTo==classGroup) {
                if(intObject.classUpdated())
                    return true;
            } else {
                if(intObject.objectUpdated())
                    return true;
            }

        return false;
    }

    public boolean dataUpdated(Collection<Property> changedProps) {
        return changedProps.contains(property);
    }

    public void fillProperties(Collection<Property> properties) {
        properties.add(property);
    }

    public Map<P, DataObject> getInterfaceValues() {
        Map<P,DataObject> mapInterface = new HashMap<P,DataObject>();
        for(Map.Entry<P, ObjectImplement> implement : mapping.entrySet())
            mapInterface.put(implement.getKey(),implement.getValue().getValue());
        return mapInterface;
    }
    
    public MapChangeDataProperty<P> getChangeProperty(ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        Map<P, ConcreteClass> interfaceClasses = new HashMap<P,ConcreteClass>();
        for(Map.Entry<P, ObjectImplement> implement : mapping.entrySet())
            interfaceClasses.put(implement.getKey(),implement.getValue().getObjectClass());
        return property.getChangeProperty(interfaceClasses,securityPolicy, externalID);
    }

    public SourceExpr getSourceExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends SourceExpr> classSource, TableChanges session, Property.TableDepends<? extends Property.TableUsedChanges> depends) {

        Map<P, SourceExpr> joinImplement = new HashMap<P,SourceExpr>();
        for(P propertyInterface : property.interfaces)
            joinImplement.put(propertyInterface, mapping.get(propertyInterface).getSourceExpr(classGroup, classSource));
        return property.getSourceExpr(joinImplement,session,depends,null);
    }
}
