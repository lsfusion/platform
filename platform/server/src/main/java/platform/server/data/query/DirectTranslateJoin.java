package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.server.data.translator.DirectTranslator;
import platform.server.data.where.Where;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.caches.Lazy;

import java.util.Collection;
import java.util.Map;

@Immutable
public class DirectTranslateJoin<U> extends Join<U>  {

    DirectTranslator translator;
    Join<U> join;

    public DirectTranslateJoin(DirectTranslator translator,Join<U> join) {
        this.translator = translator;
        this.join = join;
    }

    @Lazy
    public Where getWhere() {
        return join.getWhere().translateDirect(translator);
    }

    @Lazy
    public Expr getExpr(U property) {
        return join.getExpr(property).translateDirect(translator);
    }

    public Collection<U> getProperties() {
        return join.getProperties();
    }
}

