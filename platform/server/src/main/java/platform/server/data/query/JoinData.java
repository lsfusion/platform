package platform.server.data.query;

import platform.server.data.query.exprs.SourceExpr;

public interface JoinData {
    InnerJoin getFJGroup();
    SourceExpr getFJExpr();
    String getFJString(String exprFJ);
}
