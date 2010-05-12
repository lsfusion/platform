package platform.server.view.form;

import platform.server.logics.control.ControlInterface;
import platform.server.logics.control.Control;
import platform.server.logics.control.ControlImplement;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyValueImplement;
import platform.server.logics.DataObject;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.CustomClass;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.*;
import java.sql.SQLException;

public abstract class ControlObjectImplement<P extends ControlInterface, C extends Control<P>> extends ControlImplement<PropertyObjectInterface, P, C> implements Updated {

    public ControlObjectImplement(C property, Map<P,? extends PropertyObjectInterface> mapping) {
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

    public Map<P, DataObject> getInterfaceValues() {
        Map<P,DataObject> mapInterface = new HashMap<P,DataObject>();
        for(Map.Entry<P,PropertyObjectInterface> implement : mapping.entrySet())
            mapInterface.put(implement.getKey(),implement.getValue().getDataObject());
        return mapInterface;
    }

    public abstract Expr getExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends Expr> classSource, Modifier<? extends Changes> modifier) throws SQLException;
}
