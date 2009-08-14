package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.server.caches.Lazy;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.translators.Translator;
import platform.server.where.Where;

import java.util.Collection;

@Immutable
public class TranslateJoin<U> extends Join<U>  {

    Translator translator;
    Join<U> join;

    public TranslateJoin(Translator iTranslator,Join<U> iJoin) {
        translator = iTranslator;
        join = iJoin;

        assert translator instanceof KeyTranslator || translator instanceof QueryTranslator;
    }

    @Lazy
    public Where getWhere() {
        return join.getWhere().translate(translator);
    }

    @Lazy
    public SourceExpr getExpr(U property) {
        return join.getExpr(property).translate(translator);
    }

    public Collection<U> getProperties() {
        return join.getProperties();
    }
}

