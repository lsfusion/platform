package lsfusion.server.form.instance;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetExValue;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.CalcPropertyValueImplement;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.Modifier;

import java.sql.SQLException;

public class CalcPropertyObjectInstance<P extends PropertyInterface> extends PropertyObjectInstance<P, CalcProperty<P>> implements OrderInstance {

    public CalcPropertyObjectInstance(CalcProperty<P> property, ImMap<P, ? extends PropertyObjectInterfaceInstance> mapping) {
        super(property, mapping);
    }

    public CalcPropertyObjectInstance<P> getRemappedPropertyObject(ImMap<? extends PropertyObjectInterfaceInstance, DataObject> mapKeyValues) {
        return new CalcPropertyObjectInstance<P>(property, remapSkippingEqualsObjectInstances(mapKeyValues));
    }

    public CalcPropertyValueImplement<P> getValueImplement() {
        return new CalcPropertyValueImplement<P>(property, getInterfaceValues());
    }

    public Object read(FormInstance formInstance) throws SQLException, SQLHandledException {
        return property.read(formInstance, getInterfaceValues());
    }

    public CalcPropertyObjectInstance getDrawProperty() {
        return this;
    }

    public ValueClass getValueClass() {
        return property.getValueClass(ClassType.formPolicy);
    }

    private Expr getExpr(final ImMap<ObjectInstance, ? extends Expr> classSource, final Modifier modifier, WhereBuilder whereBuilder) throws SQLException, SQLHandledException {

        ImMap<P, Expr> joinImplement = mapping.mapValuesEx(new GetExValue<Expr, PropertyObjectInterfaceInstance, SQLException, SQLHandledException>() {
            public Expr getMapValue(PropertyObjectInterfaceInstance value) throws SQLException, SQLHandledException {
                return value.getExpr(classSource, modifier);
            }
        });
        return property.getExpr(joinImplement, modifier, whereBuilder);
    }

    public Expr getExpr(final ImMap<ObjectInstance, ? extends Expr> classSource, final Modifier modifier) throws SQLException, SQLHandledException {
        return getExpr(classSource, modifier, (WhereBuilder)null);
    }

    public Expr getExpr(final ImMap<ObjectInstance, ? extends Expr> classSource, final Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        WhereBuilder changedWhere = null;
        if(reallyChanged!=null)
            changedWhere = new WhereBuilder();
        Expr expr = getExpr(classSource, modifier, changedWhere);
        if(reallyChanged!=null)
            if(!changedWhere.toWhere().isFalse())
                reallyChanged.addChange(this);            
        return expr;
    }

    public boolean isReallyChanged(Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        if(reallyChanged.containsChange(this))
            return true;
            
        ImRevMap<ObjectInstance,KeyExpr> keys = KeyExpr.getMapKeys(getObjectInstances().toSet());
        WhereBuilder changedWhere = new WhereBuilder();
        getExpr(keys, modifier, changedWhere);
        return !changedWhere.toWhere().isFalse();
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

    public boolean dataUpdated(ChangedData changedProps, ReallyChanged reallyChanged, Modifier modifier) throws SQLException, SQLHandledException {
        if(!changedProps.props.contains(property))
            return false;
        
        if(changedProps.wasRestart)
            return true;
        
        return isReallyChanged(modifier, reallyChanged); // cache пока не используем так как за многим надо следить
    }
}
