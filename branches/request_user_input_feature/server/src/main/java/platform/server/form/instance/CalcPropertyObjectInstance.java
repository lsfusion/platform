package platform.server.form.instance;

import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.CalcPropertyValueImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CalcPropertyObjectInstance<P extends PropertyInterface> extends PropertyObjectInstance<P, CalcProperty<P>> implements OrderInstance {

    public CalcPropertyObjectInstance(CalcProperty<P> property, Map<P, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

    @IdentityLazy
    public CalcPropertyObjectInstance<P> getRemappedPropertyObject(Map<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return new CalcPropertyObjectInstance<P>(property, remap(mapKeyValues));
    }

    public CalcPropertyValueImplement<P> getValueImplement() {
        return new CalcPropertyValueImplement<P>(property, getInterfaceValues());
    }

    public Object read(FormInstance formInstance) throws SQLException {
        return property.read(formInstance, getInterfaceValues());
    }

    public CalcPropertyObjectInstance getDrawProperty() {
        return this;
    }

    public Expr getExpr(Map<ObjectInstance, ? extends Expr> classSource, Modifier modifier) {

        Map<P, Expr> joinImplement = new HashMap<P, Expr>();
        for(P propertyInterface : property.interfaces)
            joinImplement.put(propertyInterface, mapping.get(propertyInterface).getExpr(classSource, modifier));
        return property.getExpr(joinImplement, modifier);
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

    public void fillProperties(Set<CalcProperty> properties) {
        properties.add(property);
    }

    public boolean dataUpdated(Collection<CalcProperty> changedProps) {
        return changedProps.contains(property);
    }
}
