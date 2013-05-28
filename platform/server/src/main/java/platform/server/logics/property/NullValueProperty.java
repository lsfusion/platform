package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public class NullValueProperty extends FormulaProperty<PropertyInterface>{

    private NullValueProperty() {
        super("nullValue", "Значение NULL", SetFact.<PropertyInterface>EMPTYORDER());

        finalizeInit();
    }

    public static final NullValueProperty instance = new NullValueProperty();

    protected Expr calculateExpr(ImMap<PropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return CaseExpr.NULL;
    }
}
