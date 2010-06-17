package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.expr.CurrentUserExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.classes.ConcreteValueClass;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class CurrentUserFormulaProperty extends ValueFormulaProperty<PropertyInterface> {

    public CurrentUserFormulaProperty(String sID, ConcreteValueClass userClass) {
        super(sID, "Тек. польз.", new ArrayList<PropertyInterface>(), userClass);
    }

    protected Expr calculateExpr(Map<PropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return new CurrentUserExpr(value);
    }
}
