package platform.server.data.translator;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;

import java.util.*;

// в отдельный класс для allKeys и для аспектов
public class QueryTranslator {

    public final Map<KeyExpr,? extends Expr> keys;

    private final boolean allKeys;

    protected QueryTranslator(Map<KeyExpr, ? extends Expr> keys, boolean allKeys) {
        this.keys = keys;

        assert !keys.containsValue(null);

        this.allKeys = allKeys;
    }

    public QueryTranslator(Map<KeyExpr, ? extends Expr> joinImplement) {
        this(joinImplement, true);
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

    public Expr translate(KeyExpr key) {
        Expr transExpr = keys.get(key);
        if(transExpr==null) {
            if(allKeys)
                assert key instanceof PullExpr; // не должно быть
            return key;
        } else
            return transExpr;
    }

    public int hashCode() {
        return keys.hashCode();
    }

    public boolean equals(Object obj) {
        return obj==this || (obj instanceof QueryTranslator && keys.equals(((QueryTranslator)obj).keys));
    }
}
