package platform.server.data.query.translators;

import net.jcip.annotations.Immutable;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;

import java.util.HashMap;
import java.util.Map;

@Immutable
public class QueryTranslator extends Translator<SourceExpr> {

    public QueryTranslator(Map<KeyExpr, ? extends SourceExpr> joinImplement, Map<ValueExpr, ValueExpr> iValues) {
        super((Map<KeyExpr, SourceExpr>) joinImplement, iValues);
    }

    public <K> Map<K, SourceExpr> translate(Map<K, ? extends SourceExpr> map) {
        Map<K,SourceExpr> transMap = new HashMap<K,SourceExpr>();
        for(Map.Entry<K,? extends SourceExpr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateQuery(this));
        return transMap;
    }
}
