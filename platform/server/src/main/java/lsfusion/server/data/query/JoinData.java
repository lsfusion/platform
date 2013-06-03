package lsfusion.server.data.query;

import lsfusion.server.data.expr.Expr;

public interface JoinData {
    Object getFJGroup();
    Expr getFJExpr();
    String getFJString(String exprFJ);
}
