package platform.server.logics.property;

import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.data.expr.Expr;
import platform.server.data.expr.FormulaUnionExpr;
import platform.server.data.expr.formula.FormulaImpl;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public abstract class FormulaUnionProperty extends UnionProperty {

    protected FormulaUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces) {
        super(sID, caption, interfaces);
    }

    @Override
    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, final boolean propClasses, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        ImCol<Expr> exprs = getOperands().mapColValues(new GetValue<Expr, CalcPropertyInterfaceImplement<Interface>>() {
            @Override
            public Expr getMapValue(CalcPropertyInterfaceImplement<Interface> value) {
                return value.mapExpr(joinImplement, propClasses, propChanges, changedWhere);
            }
        });
        return FormulaUnionExpr.create(getFormula(), exprs.toList());
    }

    protected boolean useSimpleIncrement() {
        return true;
    }

    protected abstract FormulaImpl getFormula();
}
