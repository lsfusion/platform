package lsfusion.server.data.expr;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.formula.ExprSource;
import lsfusion.server.data.expr.formula.ExprType;
import lsfusion.server.data.expr.formula.FormulaJoinImpl;
import lsfusion.server.data.type.Type;

public class WindowFormulaImpl implements FormulaJoinImpl {

    public final static WindowFormulaImpl limit = new WindowFormulaImpl();
    public final static WindowFormulaImpl offset = new WindowFormulaImpl();

    @Override
    public Type getType(ExprType source) {
        return source.getType(0);
    }

    @Override
    public String getSource(ExprSource source) {
        int exprCount = source.getExprCount();
        assert exprCount == 1;
        return source.getSource(0);
    }

    @Override
    public boolean hasNotNull(ImList<BaseExpr> exprs) {
        return false;
    }
}
