package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.server.caches.Lazy;
import platform.server.data.expr.Expr;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.Where;

import java.util.Collection;

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
