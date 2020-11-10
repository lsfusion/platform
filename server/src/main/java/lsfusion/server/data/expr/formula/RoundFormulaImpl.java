package lsfusion.server.data.expr.formula;

import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.value.AbstractValueExpr;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.data.integral.NumericClass;

public class RoundFormulaImpl implements FormulaJoinImpl {

    public final static RoundFormulaImpl instance = new RoundFormulaImpl();

    @Override
    public Type getType(ExprType source) {
        Type exprType = source.getType(0);
        Integer scale = getScale(source);
        return exprType instanceof IntegralClass && scale != null ? NumericClass.get(((IntegralClass) exprType).getWhole() + scale, scale) : exprType;
    }

    private Integer getScale(ExprType source) {
        //todo: refactor
        Integer result = null;
        if (hasScale(source)) {
            if (source instanceof ListExprType) {
                Expr scaleExpr = ((ListExprType) source).exprs.get(1);
                if (scaleExpr instanceof AbstractValueExpr) {
                    Object scale = ((AbstractValueExpr) scaleExpr).getObject();
                    if (scale instanceof Integer) {
                        result = (Integer) scale;
                    }
                }
            }
        } else {
            result = 0;
        }
        return result;
    }

    @Override
    public String getSource(ExprSource source) {
        return "ROUND(" + source.getSource(0) + (hasScale(source) ? (", " + source.getSource(1)) : "") + ")";
    }

    private boolean hasScale(ExprType source) {
        return source.getExprCount() == 2;
    }

    public boolean hasNotNull() {
        return true;
    }
}
