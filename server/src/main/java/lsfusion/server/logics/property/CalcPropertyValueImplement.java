package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.change.PropertyChange;

import java.sql.SQLException;

public class CalcPropertyValueImplement<P extends PropertyInterface> extends CalcPropertyImplement<P, DataObject> {

    public CalcPropertyValueImplement(CalcProperty<P> property, ImMap<P, DataObject> mapping) {
        super(property, mapping);
    }

    public PropertyChange<P> getPropertyChange(Expr expr) throws SQLException {
        return new PropertyChange<>(expr, mapping);
    }

    public boolean canBeChanged(Modifier modifier) throws SQLException, SQLHandledException {
        return !property.getDataChanges(getPropertyChange(property.getChangeExpr()), modifier).isEmpty();
    }

    public ObjectValue readClasses(FormInstance form) throws SQLException, SQLHandledException {
        return property.readClasses(form, mapping);
    }

    public ObjectValue readClasses(ExecutionContext context) throws SQLException, SQLHandledException {
        return property.readClasses(context, mapping);
    }

    public CustomClass getDialogClass(DataSession session) throws SQLException, SQLHandledException {
        return property.getDialogClass(mapping, session.getCurrentClasses(mapping));
    }

}
