package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.ArrayList;
import java.util.Map;

public class NullValueProperty extends FormulaProperty<PropertyInterface>{

    public NullValueProperty() {
        super("nullValue", "Значение NULL", new ArrayList<PropertyInterface>());

        finalizeInit();
    }

    @Override
    protected Expr calculateExpr(Map<PropertyInterface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return CaseExpr.NULL;
    }
}
