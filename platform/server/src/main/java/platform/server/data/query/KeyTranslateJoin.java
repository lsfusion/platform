package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.where.Where;
import platform.server.data.expr.Expr;
import platform.server.caches.Lazy;

import java.util.Collection;

@Immutable
public class KeyTranslateJoin<U> extends Join<U>  {

    KeyTranslator translator;
    Join<U> join;

    public KeyTranslateJoin(KeyTranslator translator,Join<U> join) {
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

