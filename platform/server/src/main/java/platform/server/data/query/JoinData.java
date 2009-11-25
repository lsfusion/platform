package platform.server.data.query;

import platform.server.data.expr.Expr;

public interface JoinData {
    InnerJoin getFJGroup();
    Expr getFJExpr();
    String getFJString(String exprFJ);
}
