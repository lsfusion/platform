package platform.server.logics.property;

import platform.base.TwinImmutableInterface;
import platform.base.TwinImmutableObject;
import platform.server.classes.CustomClass;
import platform.server.data.expr.Expr;
import platform.server.logics.DataObject;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CalcPropertyValueImplement<P extends PropertyInterface> extends TwinImmutableObject {

    public final CalcProperty<P> property;
    public final Map<P, DataObject> mapping;

    public boolean twins(TwinImmutableInterface o) {
        return property.equals(((CalcPropertyValueImplement) o).property) && mapping.equals(((CalcPropertyValueImplement) o).mapping);
    }

    public int immutableHashCode() {
        return property.hashCode() * 31 + mapping.hashCode();
    }

    public CalcPropertyValueImplement(CalcProperty<P> property, Map<P, DataObject> mapping) {
        this.property = property;
        this.mapping = mapping;
    }

    @Override
    public String toString() {
        return property.toString();
    }

    public int getID() {
        return property.ID;
    }

    public PropertyChange<P> getPropertyChange(Expr expr) throws SQLException {
        return new PropertyChange<P>(expr, mapping);
    }

    public boolean canBeChanged(Modifier modifier) throws SQLException {
        return !property.getDataChanges(getPropertyChange(property.changeExpr), modifier).isEmpty();
    }
    
    public Object read(DataSession session, Modifier modifier) throws SQLException {
        return property.read(session, mapping, modifier);
    }

    public CustomClass getDialogClass(DataSession session) {
        return property.getDialogClass(mapping, session.getCurrentClasses(mapping));
    }

}
