package platform.server.data.query;

import platform.server.data.query.exprs.SourceExpr;

public interface JoinData extends QueryData {
    Join getJoin();
    SourceExpr getFJExpr();
    String getFJString(String exprFJ);
}
