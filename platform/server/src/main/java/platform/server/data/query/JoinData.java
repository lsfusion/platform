package platform.server.data.query;

import platform.server.data.query.exprs.SourceExpr;

public interface JoinData {
    Object getFJGroup();
    SourceExpr getFJExpr();
    String getFJString(String exprFJ);
}
