package lsfusion.server.data.query;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.where.Where;

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
