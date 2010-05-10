package platform.server.data.translator;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.data.expr.*;

import java.util.*;

@Immutable
public class DirectTranslator {

    // какой есть - какой нужен
    public final Map<KeyExpr,KeyExpr> keys;
    public final Map<ValueExpr,ValueExpr> values;

    public DirectTranslator(Map<KeyExpr, KeyExpr> keys, Map<ValueExpr, ValueExpr> values) {
        this.keys = keys;
        this.values = values;

        assert !keys.containsValue(null);
        assert !ValueExpr.removeStatic(values).containsValue(null);
    }

    public KeyExpr translate(KeyExpr key) {
        KeyExpr transExpr = keys.get(key);
        if(transExpr==null) {
            assert key instanceof PullExpr; // не должно быть
            return key;
        } else
            return transExpr;
    }

    public ValueExpr translate(ValueExpr expr) {
        return BaseUtils.nvl(values.get(expr),expr);
    }

    public int hashCode() {
        return keys.hashCode()*31+values.hashCode();
    }

    public boolean equals(Object obj) {
        return obj==this || (obj instanceof DirectTranslator && keys.equals(((DirectTranslator)obj).keys) && values.equals(((DirectTranslator)obj).values));
    }


    public <K> Map<K, BaseExpr> translateDirect(Map<K, ? extends BaseExpr> map) {
        Map<K, BaseExpr> transMap = new HashMap<K, BaseExpr>();
        for(Map.Entry<K,? extends BaseExpr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateDirect(this));
        return transMap;
    }

    public <K> Map<BaseExpr,K> translateKeys(Map<? extends BaseExpr, K> map) {
        Map<BaseExpr, K> transMap = new HashMap<BaseExpr, K>();
        for(Map.Entry<? extends BaseExpr,K> entry : map.entrySet())
            transMap.put(entry.getKey().translateDirect(this),entry.getValue());
        return transMap;
    }

    // для кэша classWhere на самом деле надо
    public <K> Map<K, VariableClassExpr> translateVariable(Map<K, ? extends VariableClassExpr> map) {
        Map<K,VariableClassExpr> transMap = new HashMap<K, VariableClassExpr>();
        for(Map.Entry<K,? extends VariableClassExpr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateDirect(this));
        return transMap;
    }

    public <K> Map<K, Expr> translate(Map<K, ? extends Expr> map) {
        Map<K, Expr> transMap = new HashMap<K, Expr>();
        for(Map.Entry<K,? extends Expr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateDirect(this));
        return transMap;
    }

    public List<BaseExpr> translateDirect(List<BaseExpr> list) {
        List<BaseExpr> result = new ArrayList<BaseExpr>();
        for(BaseExpr expr : list)
            result.add(expr.translateDirect(this));
        return result;
    }

    public Set<BaseExpr> translateDirect(Set<BaseExpr> set) {
        Set<BaseExpr> result = new HashSet<BaseExpr>();
        for(BaseExpr expr : set)
            result.add(expr.translateDirect(this));
        return result;
    }

    public List<Expr> translate(List<Expr> list) {
        List<Expr> result = new ArrayList<Expr>();
        for(Expr expr : list)
            result.add(expr.translateDirect(this));
        return result;
    }

    public Set<Expr> translate(Set<Expr> set) {
        Set<Expr> result = new HashSet<Expr>();
        for(Expr expr : set)
            result.add(expr.translateDirect(this));
        return result;
    }

    public boolean identity() {
        return BaseUtils.identity(keys) && BaseUtils.identity(values);
    }
}
