package platform.server.data.query;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.translators.Translator;
import platform.server.where.Where;
import platform.server.caches.Lazy;

import java.util.Map;

import net.jcip.annotations.Immutable;

@Immutable
public class TranslateJoin<U> implements Join<U>  {

    Translator translator;
    Join<U> join;

    public TranslateJoin(Translator iTranslator,Join<U> iJoin) {
        translator = iTranslator;
        join = iJoin;
    }

    @Lazy
    public Where getWhere() {
        return join.getWhere().translate(translator);
    }

    @Lazy
    public SourceExpr getExpr(U property) {
        return join.getExpr(property).translate(translator);
    }

    @Lazy
    public Map<U, SourceExpr> getExprs() {
        return translator.translate(join.getExprs());
    }

    public Context getContext() {
        return translator.getContext();
    }
}

