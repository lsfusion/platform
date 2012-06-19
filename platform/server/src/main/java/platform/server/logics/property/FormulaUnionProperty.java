package platform.server.logics.property;

import platform.server.classes.DataClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.FormulaUnionExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class FormulaUnionProperty extends UnionProperty {

    protected abstract String getFormula();
    protected abstract DataClass getDataClass();
    protected abstract Map<String, CalcPropertyInterfaceImplement<Interface>> getParams();

    protected FormulaUnionProperty(String sID, String caption, List<Interface> interfaces) {
        super(sID, caption, interfaces);
    }

    protected Collection<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        return getParams().values();
    }

    @Override
    protected Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Map<String, Expr> paramExprs = new HashMap<String, Expr>();
        for(Map.Entry<String, CalcPropertyInterfaceImplement<Interface>> param : getParams().entrySet())
            paramExprs.put(param.getKey(), param.getValue().mapExpr(joinImplement, propClasses, propChanges, changedWhere));
        return new FormulaUnionExpr(getFormula(), getDataClass(), paramExprs);
    }

    protected boolean useSimpleIncrement() {
        return true;
    }
}
