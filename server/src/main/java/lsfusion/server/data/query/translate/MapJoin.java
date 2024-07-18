package lsfusion.server.data.query.translate;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.query.build.AbstractJoin;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.where.Where;

public class MapJoin<U> extends AbstractJoin<U> {

    private final MapTranslate translator;
    private final Join<U> join;

    public MapJoin(MapValuesTranslate translator, Join<U> join) {
        this(translator.mapKeys(), join);
    }

    public MapJoin(MapTranslate translator, Join<U> join) {
        this.translator = translator;
        this.join = join;
    }

    @IdentityLazy
    public Where getWhere() {
        return join.getWhere().translateOuter(translator);
    }

    @IdentityLazy
    public Expr getExpr(U property) {
        return join.getExpr(property).translateOuter(translator);
    }

    public Join<U> translateRemoveValues(MapValuesTranslate translate) {
        return new MapJoin<>(translator.mapValues(translate), join);
    }
}

