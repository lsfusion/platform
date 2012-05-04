package platform.server.data.query;

import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapTranslator;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

import java.util.Collection;

public class MapJoin<U> extends AbstractJoin<U>  {

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

    public Collection<U> getProperties() {
        return join.getProperties();
    }

    public Join<U> translateRemoveValues(MapValuesTranslate translate) {
        return new MapJoin<U>(translator.mapValues(translate), join);
    }
}

