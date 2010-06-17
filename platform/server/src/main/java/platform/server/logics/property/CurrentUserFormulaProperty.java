package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.expr.CurrentUserExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.classes.ConcreteValueClass;
import platform.server.classes.ValueClass;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class CurrentUserFormulaProperty extends FormulaProperty<PropertyInterface> {

    ValueClass userClass;

    public CurrentUserFormulaProperty(String sID, ValueClass userClass) {
        super(sID, "Тек. польз.", new ArrayList<PropertyInterface>());
        this.userClass = userClass;
    }

    protected Expr calculateExpr(Map<PropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return new CurrentUserExpr(userClass);
    }
}
