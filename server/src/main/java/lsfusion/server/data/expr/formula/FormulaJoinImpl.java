package lsfusion.server.data.expr.formula;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.expr.BaseExpr;

public interface FormulaJoinImpl extends FormulaImpl {
    
    boolean hasNotNull(ImList<BaseExpr> exprs); // true - если может возвращать null, при не null аргументах
}
