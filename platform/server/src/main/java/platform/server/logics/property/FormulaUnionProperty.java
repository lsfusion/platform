package platform.server.logics.property;

import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.classes.DataClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.FormulaUnionExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public abstract class FormulaUnionProperty extends UnionProperty {

    protected abstract String getFormula();
    protected abstract DataClass getDataClass();
    protected abstract ImMap<String, CalcPropertyInterfaceImplement<Interface>> getParams();

    protected FormulaUnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces) {
        super(sID, caption, interfaces);
    }

    public ImCol<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        return getParams().values();
    }

    @Override
    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, final boolean propClasses, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        ImMap<String,Expr> paramExprs = getParams().mapItValues(new GetValue<Expr, CalcPropertyInterfaceImplement<Interface>>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<Interface> value) {
                return value.mapExpr(joinImplement, propClasses, propChanges, changedWhere);
            }});
        return new FormulaUnionExpr(getFormula(), getDataClass(), paramExprs);
    }

    protected boolean useSimpleIncrement() {
        return true;
    }
}
