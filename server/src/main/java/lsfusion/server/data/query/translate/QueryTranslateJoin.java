package lsfusion.server.data.query.translate;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.build.AbstractJoin;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.translate.KeyExprTranslator;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.where.Where;

public class QueryTranslateJoin<U> extends AbstractJoin<U> {

    public final KeyExprTranslator translator;
    public final Join<U> join;

    public QueryTranslateJoin(KeyExprTranslator translator, Join<U> join) {
        this.translator = translator;
        this.join = join;
    }

    @IdentityLazy
    public Where getWhere() {
        return join.getWhere().translateExpr(translator);
    }

    @IdentityLazy
    public Expr getExpr(U property) {
        return join.getExpr(property).translateExpr(translator);
    }

    public Join<U> translateRemoveValues(MapValuesTranslate translate) {
        return new QueryTranslateJoin<>(translator.translateRemoveValues(translate), join.translateRemoveValues(translate));
    }
}
