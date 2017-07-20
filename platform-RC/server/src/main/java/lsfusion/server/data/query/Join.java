package lsfusion.server.data.query;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.TranslateValues;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;

public interface Join<U> extends AndContext<Join<U>>, TranslateValues<Join<U>> {

    Expr getExpr(U property);
    Where getWhere();

    ImSet<U> getProperties();

    ImMap<U, Expr> getExprs();
}

