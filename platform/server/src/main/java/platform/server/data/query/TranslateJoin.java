package platform.server.data.query;

import net.jcip.annotations.Immutable;
import platform.server.caches.Lazy;
import platform.server.data.expr.Expr;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.Translator;
import platform.server.data.where.Where;

import java.util.Collection;

@Immutable
public class TranslateJoin<U> extends Join<U>  {

    Translator translator;
    Join<U> join;

    public TranslateJoin(Translator iTranslator,Join<U> iJoin) {
        translator = iTranslator;
        join = iJoin;
    }

    @Lazy
    public Where getWhere() {
        if(translator instanceof KeyTranslator)
            return join.getWhere().translateDirect((KeyTranslator) translator);
        else
            return join.getWhere().translateQuery((QueryTranslator) translator);
    }

    @Lazy
    public Expr getExpr(U property) {
        if(translator instanceof KeyTranslator)
            return join.getExpr(property).translateDirect((KeyTranslator) translator);
        else
            return join.getExpr(property).translateQuery((QueryTranslator) translator);
    }

    public Collection<U> getProperties() {
        return join.getProperties();
    }
}

