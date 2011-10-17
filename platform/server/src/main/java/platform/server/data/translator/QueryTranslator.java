package platform.server.data.translator;

import platform.base.ImmutableObject;
import platform.base.OrderedMap;
import platform.base.TwinImmutableInterface;
import platform.base.TwinImmutableObject;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;

import java.util.*;

// в отдельный класс для allKeys и для аспектов
public class QueryTranslator extends TwinImmutableObject {

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

    public <K> OrderedMap<Expr, K> translate(OrderedMap<? extends Expr, K> map) {
        OrderedMap<Expr, K> transMap = new OrderedMap<Expr, K>();
        for(Map.Entry<? extends Expr,K> entry : map.entrySet())
            transMap.put(entry.getKey().translateQuery(this), entry.getValue());
        return transMap;
    }

    public <K> Map<Expr, K> translateKeys(Map<? extends Expr, K> map) {
        Map<Expr, K> transMap = new HashMap<Expr, K>();
        for(Map.Entry<? extends Expr,K> entry : map.entrySet())
            transMap.put(entry.getKey().translateQuery(this), entry.getValue());
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

    public boolean twins(TwinImmutableInterface o) {
        return keys.equals(((QueryTranslator)o).keys);
    }

    public int immutableHashCode() {
        return keys.hashCode();
    }
}
