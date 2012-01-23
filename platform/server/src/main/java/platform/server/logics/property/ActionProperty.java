package platform.server.logics.property;

import platform.base.QuickSet;
import platform.server.classes.ActionClass;
import platform.server.classes.DataClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.util.Map;

public abstract class ActionProperty extends ExecuteClassProperty {

    public ActionProperty(String sID, ValueClass... classes) {
        this(sID, "sysAction", classes);
    }

    public ActionProperty(String sID, String caption, ValueClass[] classes) {
        super(sID, caption, classes);
    }

    protected Expr getValueExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement) {
        return getValueClass().getDefaultExpr();
    }

    public DataClass getValueClass() {
        return ActionClass.instance;
    }
}
