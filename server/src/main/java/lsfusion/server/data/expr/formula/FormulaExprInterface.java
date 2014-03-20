package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.BaseExpr;

public interface FormulaExprInterface {
    
    ImList<BaseExpr> getFParams();
    FormulaJoinImpl getFormula();

    boolean hasFNotNull();
}
