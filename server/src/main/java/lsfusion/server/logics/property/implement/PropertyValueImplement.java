package lsfusion.server.logics.property.implement;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class PropertyValueImplement<P extends PropertyInterface> extends PropertyImplement<P, DataObject> {

    public PropertyValueImplement(Property<P> property, ImMap<P, DataObject> mapping) {
        super(property, mapping);
    }

    public PropertyChange<P> getPropertyChange(Expr expr) {
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
