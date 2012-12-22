package platform.server.data.query;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.Where;

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

    public ImSet<U> getProperties() {
        return join.getProperties();
    }

    public Join<U> translateRemoveValues(MapValuesTranslate translate) {
        return new QueryTranslateJoin<U>(translator.translateRemoveValues(translate), join.translateRemoveValues(translate));
    }
}
