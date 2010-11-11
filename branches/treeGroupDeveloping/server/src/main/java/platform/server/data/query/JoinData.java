package platform.server.data.query;

import platform.server.data.expr.Expr;

public interface JoinData {
    Object getFJGroup();
    Expr getFJExpr();
    String getFJString(String exprFJ);
}
