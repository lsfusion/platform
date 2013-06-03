package lsfusion.server.form.instance;

import lsfusion.base.FunctionSet;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.CalcPropertyValueImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.Modifier;

import java.sql.SQLException;

public class CalcPropertyObjectInstance<P extends PropertyInterface> extends PropertyObjectInstance<P, CalcProperty<P>> implements OrderInstance {

    public CalcPropertyObjectInstance(CalcProperty<P> property, ImMap<P, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

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

    public ValueClass getValueClass() {
        return property.getValueClass();
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
