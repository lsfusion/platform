package platform.server.data.query;

import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.Where;

import java.util.Collection;

public class QueryTranslateJoin<U> extends AbstractJoin<U> {

    QueryTranslator translator;
    Join<U> join;

    public QueryTranslateJoin(QueryTranslator translator,Join<U> join) {
        this.translator = translator;
        this.join = join;
    }

    @IdentityLazy
    public Where getWhere() {
        return join.getWhere().translateQuery(translator);
    }

    @IdentityLazy
    public Expr getExpr(U property) {
        return join.getExpr(property).translateQuery(translator);
    }

    public Collection<U> getProperties() {
        return join.getProperties();
    }

}
