package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.server.caches.Lazy;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

import java.util.Collection;

@Immutable
public class DirectTranslateJoin<U> extends Join<U>  {

    MapTranslate translator;
    Join<U> join;

    public DirectTranslateJoin(MapTranslate translator,Join<U> join) {
        this.translator = translator;
        this.join = join;
    }

    @Lazy
    public Where getWhere() {
        return join.getWhere().translate(translator);
    }

    @Lazy
    public Expr getExpr(U property) {
        return join.getExpr(property).translate(translator);
    }

    public Collection<U> getProperties() {
        return join.getProperties();
    }
}

