package platform.server.logics.property;

import platform.interop.action.ClientAction;
import platform.server.classes.ActionClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public abstract class ActionProperty extends ExecuteProperty {

    public ActionProperty(String sID, ValueClass... classes) {
        this(sID, "sysAction", classes);
    }

    public ActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    protected <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        return ValueClassProperty.getIsClassUsedChanges(interfaces, modifier);
    }
    
    protected Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return getValueClass().getDefaultExpr().and(ClassProperty.getIsClassWhere(joinImplement, modifier, changedWhere));
    }

    public DataClass getValueClass() {
        return ActionClass.instance;
    }
}
