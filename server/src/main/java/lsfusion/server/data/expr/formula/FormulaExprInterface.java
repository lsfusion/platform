package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.where.Where;

public interface FormulaExprInterface {
    
    ImList<BaseExpr> getFParams();
    FormulaJoinImpl getFormula();

    Where getWhere();
}
