package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.classes.ActionClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.NullValue;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.type.Type;
import platform.server.view.form.client.RemoteFormView;
import platform.interop.form.RemoteFormInterface;
import platform.interop.action.ClientAction;

import java.util.Map;
import java.util.List;
import java.sql.SQLException;

public abstract class ActionProperty extends UserProperty {

    public ActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    public abstract void execute(Map<ClassPropertyInterface, DataObject> keys, List<ClientAction> actions, RemoteFormView executeForm);

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteFormView executeForm) throws SQLException {
        assert value instanceof NullValue;
        execute(keys, actions, executeForm);
    }

    protected <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        return ClassProperty.getIsClassUsedChanges(interfaces, modifier);
    }
    
    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return new ValueExpr(true,ActionClass.instance).and(ClassProperty.getIsClassWhere(joinImplement, modifier, changedWhere));
    }

    public Type getType() {
        return ActionClass.instance;
    }

    protected AndClassSet getValueSet() {
        return ActionClass.instance;
    }
}
