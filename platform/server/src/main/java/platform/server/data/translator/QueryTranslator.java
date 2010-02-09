package platform.server.data.translator;

import net.jcip.annotations.Immutable;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;

import java.util.HashMap;
import java.util.Map;

@Immutable
public class QueryTranslator extends Translator<Expr> {

    public QueryTranslator(Map<KeyExpr, ? extends Expr> joinImplement, Map<ValueExpr, ValueExpr> values, boolean allKeys) {
        super((Map<KeyExpr, Expr>) joinImplement, values, allKeys);
    }

    public <K> Map<K, Expr> translate(Map<K, ? extends Expr> map) {
        Map<K, Expr> transMap = new HashMap<K, Expr>();
        for(Map.Entry<K,? extends Expr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateQuery(this));
        return transMap;
    }
}
