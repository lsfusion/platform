package platform.server.form.instance;

import platform.base.FunctionSet;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.CalcPropertyValueImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.Modifier;

import java.sql.SQLException;

public class CalcPropertyObjectInstance<P extends PropertyInterface> extends PropertyObjectInstance<P, CalcProperty<P>> implements OrderInstance {

    public CalcPropertyObjectInstance(CalcProperty<P> property, ImMap<P, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

    @IdentityLazy
    public CalcPropertyObjectInstance<P> getRemappedPropertyObject(ImMap<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
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

    public Expr getExpr(final ImMap<ObjectInstance, ? extends Expr> classSource, final Modifier modifier) {

        ImMap<P, Expr> joinImplement = mapping.mapValues(new GetValue<Expr, PropertyObjectInterfaceInstance>() {
            public Expr getMapValue(PropertyObjectInterfaceInstance value) {
                return value.getExpr(classSource, modifier);
            }});
        return property.getExpr(joinImplement, modifier);
    }

    // проверяет на то что изменился верхний объект
    public boolean objectUpdated(ImSet<GroupObjectInstance> gridGroups) {
        for(PropertyObjectInterfaceInstance intObject : mapping.valueIt())
            if(intObject.objectUpdated(gridGroups)) return true;

        return false;
    }

    public boolean classUpdated(ImSet<GroupObjectInstance> gridGroups) {
        for(PropertyObjectInterfaceInstance intObject : mapping.valueIt())
            if(intObject.classUpdated(gridGroups))
                return true;

        return false;
    }

    public void fillProperties(MSet<CalcProperty> properties) {
        properties.add(property);
    }

    public boolean dataUpdated(FunctionSet<CalcProperty> changedProps) {
        return changedProps.contains(property);
    }
}
