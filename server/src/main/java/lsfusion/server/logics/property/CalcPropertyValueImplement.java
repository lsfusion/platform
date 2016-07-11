package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.Modifier;
import lsfusion.server.session.PropertyChange;

import java.sql.SQLException;

public class CalcPropertyValueImplement<P extends PropertyInterface> extends CalcPropertyImplement<P, DataObject> {

    public CalcPropertyValueImplement(CalcProperty<P> property, ImMap<P, DataObject> mapping) {
        super(property, mapping);
    }

    public PropertyChange<P> getPropertyChange(Expr expr) throws SQLException {
        return new PropertyChange<>(expr, mapping);
    }

    public boolean canBeChanged(Modifier modifier) throws SQLException {
        return !property.getDataChanges(getPropertyChange(property.getChangeExpr()), modifier).isEmpty();
    }

    public ObjectValue readClasses(FormInstance form) throws SQLException, SQLHandledException {
        return property.readClasses(form, mapping);
    }

    public CustomClass getDialogClass(DataSession session) throws SQLException, SQLHandledException {
        return property.getDialogClass(mapping, session.getCurrentClasses(mapping));
    }

}
