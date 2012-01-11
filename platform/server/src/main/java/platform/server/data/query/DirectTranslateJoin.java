package platform.server.data.query;

import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

import java.util.Collection;

public class DirectTranslateJoin<U> extends AbstractJoin<U>  {

    MapTranslate translator;
    Join<U> join;

    public DirectTranslateJoin(MapTranslate translator,Join<U> join) {
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
}

