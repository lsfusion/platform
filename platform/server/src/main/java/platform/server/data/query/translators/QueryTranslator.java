package platform.server.data.query.translators;

import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;

import java.util.Map;

import net.jcip.annotations.Immutable;

@Immutable
public class QueryTranslator extends Translator<SourceExpr> {

    public QueryTranslator(Map<KeyExpr, ? extends SourceExpr> joinImplement, Map<ValueExpr, ValueExpr> iValues) {
        super((Map<KeyExpr, SourceExpr>) joinImplement, iValues);
    }
}
