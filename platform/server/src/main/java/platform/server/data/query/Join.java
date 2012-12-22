package platform.server.data.query;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.TranslateValues;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;

public interface Join<U> extends AndContext<Join<U>>, TranslateValues<Join<U>> {

    Expr getExpr(U property);
    Where getWhere();

    ImSet<U> getProperties();

    ImMap<U, Expr> getExprs();
}

