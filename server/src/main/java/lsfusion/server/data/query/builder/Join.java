package lsfusion.server.data.query.builder;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.pull.AndContext;
import lsfusion.server.data.translate.TranslateValues;
import lsfusion.server.data.where.Where;

public interface Join<U> extends AndContext<Join<U>>, TranslateValues<Join<U>> {

    Expr getExpr(U property);
    Where getWhere();

    ImSet<U> getProperties();

    ImMap<U, Expr> getExprs();
}

