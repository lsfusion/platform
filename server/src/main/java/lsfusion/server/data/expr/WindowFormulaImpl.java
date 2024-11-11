package lsfusion.server.data.expr;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.formula.ExprSource;
import lsfusion.server.data.expr.formula.ExprType;
import lsfusion.server.data.expr.formula.FormulaJoinImpl;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.classes.data.StringClass;

public class WindowFormulaImpl implements FormulaJoinImpl {

    public final static WindowFormulaImpl instance = new WindowFormulaImpl();
    public static String separator = "\uFFFE";

    @Override
    public Type getType(ExprType source) {
        return StringClass.instance;
    }

    @Override
    public String getSource(ExprSource source) {
        int exprCount = source.getExprCount();
        assert exprCount == 1 || exprCount == 2;
        String result = source.getSource(0);

        if(exprCount == 2)
            result += separator + source.getSource(1);

        return result;
    }

    @Override
    public boolean hasNotNull(ImList<BaseExpr> exprs) {
        return false;
    }
}
