package lsfusion.server.data.query.compile;

import lsfusion.server.data.expr.Expr;

public interface FJData {
    Object getFJGroup();
    Expr getFJExpr();
    String getFJString(String exprFJ);
}
