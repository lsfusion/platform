package platform.server.data.translator;

import net.jcip.annotations.Immutable;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.BaseExpr;

import java.util.*;

@Immutable
public class QueryTranslator extends Translator<Expr> {

    public QueryTranslator(Map<KeyExpr, ? extends Expr> joinImplement, Map<ValueExpr, ValueExpr> values, boolean allKeys) {
        super((Map<KeyExpr, Expr>) joinImplement, values, allKeys);
    }

    public QueryTranslator(Map<KeyExpr, BaseExpr> joinImplement, boolean allKeys) {
        this(joinImplement, new HashMap<ValueExpr, ValueExpr>(), allKeys);
    }

    public <K> Map<K, Expr> translate(Map<K, ? extends Expr> map) {
        Map<K, Expr> transMap = new HashMap<K, Expr>();
        for(Map.Entry<K,? extends Expr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateQuery(this));
        return transMap;
    }

    public List<Expr> translate(List<? extends Expr> list) {
        List<Expr> result = new ArrayList<Expr>();
        for(Expr expr : list)
            result.add(expr.translateQuery(this));
        return result;
    }

    public Set<Expr> translate(Set<? extends Expr> set) {
        Set<Expr> result = new HashSet<Expr>();
        for(Expr expr : set)
            result.add(expr.translateQuery(this));
        return result;
    }

}
