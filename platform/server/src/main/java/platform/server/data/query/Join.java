package platform.server.data.query;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.where.Where;

import java.util.Map;

public interface Join<U> {

    public SourceExpr getExpr(U property);
    public Map<U,SourceExpr> getExprs();
    public Where<?> getWhere();

    public Context getContext();
}

