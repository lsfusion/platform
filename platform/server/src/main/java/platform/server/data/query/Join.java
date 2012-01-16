package platform.server.data.query;

import platform.base.ImmutableObject;
import platform.server.caches.TranslateValues;
import platform.server.caches.TwinLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.cases.CaseJoin;
import platform.server.data.expr.where.ifs.IfJoin;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public interface Join<U> extends AndContext<Join<U>>, TranslateValues<Join<U>> {

    Expr getExpr(U property);
    Where getWhere();

    Collection<U> getProperties();

    Map<U, Expr> getExprs();
}

