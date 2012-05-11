package platform.server.logics.property;

import platform.server.classes.CustomClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.logics.DataObject;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.Map;

public class PropertyValueImplement<P extends PropertyInterface> extends PropertyImplement<P, DataObject> {

    public PropertyValueImplement(Property<P> property, Map<P, DataObject> mapping) {
        super(property, mapping);
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
        return !property.getDataChanges(getPropertyChange(property.changeExpr), modifier).changes.isEmpty();
    }
    
    public Object read(DataSession session, Modifier modifier) throws SQLException {
        return property.read(session, mapping, modifier);
    }

    public CustomClass getDialogClass(DataSession session) {
        return property.getDialogClass(mapping, session.getCurrentClasses(mapping));
    }

}
