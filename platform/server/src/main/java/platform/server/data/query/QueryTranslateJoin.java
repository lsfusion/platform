package platform.server.data.query;

import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.Where;
import platform.server.data.expr.Expr;
import platform.server.caches.Lazy;

import java.util.Collection;

import net.jcip.annotations.Immutable;

@Immutable
public class QueryTranslateJoin<U> extends Join<U> {

    QueryTranslator translator;
    Join<U> join;

    public QueryTranslateJoin(QueryTranslator translator,Join<U> join) {
        this.translator = translator;
        this.join = join;
    }

    @Lazy
    public Where getWhere() {
        return join.getWhere().translateQuery(translator);
    }

    @Lazy
    public Expr getExpr(U property) {
        return join.getExpr(property).translateQuery(translator);
    }

    public Collection<U> getProperties() {
        return join.getProperties();
    }

}
