package platform.server.logics.property;

import platform.base.TwinImmutableInterface;
import platform.base.TwinImmutableObject;
import platform.server.classes.CustomClass;
import platform.server.data.expr.Expr;
import platform.server.form.instance.FormInstance;
import platform.server.logics.DataObject;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CalcPropertyValueImplement<P extends PropertyInterface> extends CalcPropertyImplement<P, DataObject> {

    public CalcPropertyValueImplement(CalcProperty<P> property, Map<P, DataObject> mapping) {
        super(property, mapping);
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

    public Object read(FormInstance form) throws SQLException {
        return property.read(form, mapping);
    }

    public CustomClass getDialogClass(DataSession session) throws SQLException {
        return property.getDialogClass(mapping, session.getCurrentClasses(mapping));
    }

}
