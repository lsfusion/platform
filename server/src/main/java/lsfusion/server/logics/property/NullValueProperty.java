package lsfusion.server.logics.property;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.cases.CaseExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.session.PropertyChanges;

public class NullValueProperty extends FormulaProperty<PropertyInterface>{

    private NullValueProperty() {
        super("Значение NULL", SetFact.<PropertyInterface>EMPTYORDER());

        finalizeInit();
    }

    public static final NullValueProperty instance = new NullValueProperty();

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return CaseExpr.NULL;
    }
}
