package platform.server.data.translator;

import platform.base.*;
import platform.server.caches.AbstractTranslateContext;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.VariableClassExpr;
import platform.server.logics.DataObject;

import java.util.*;

public abstract class AbstractMapTranslator extends TwinImmutableObject implements MapTranslate  {

    public <K> Map<K, BaseExpr> translateDirect(Map<K, ? extends BaseExpr> map) {
        Map<K, BaseExpr> transMap = new HashMap<K, BaseExpr>();
        for(Map.Entry<K,? extends BaseExpr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateOuter(this));
        return transMap;
    }

    public <K> Map<BaseExpr,K> translateKeys(Map<? extends BaseExpr, K> map) {
        Map<BaseExpr, K> transMap = new HashMap<BaseExpr, K>();
        for(Map.Entry<? extends BaseExpr,K> entry : map.entrySet())
            transMap.put(entry.getKey().translateOuter(this),entry.getValue());
        return transMap;
    }

    public <K> Map<Expr,K> translateExprKeys(Map<? extends Expr, K> map) {
        Map<Expr, K> transMap = new HashMap<Expr, K>();
        for(Map.Entry<? extends Expr, K> entry : map.entrySet())
            transMap.put(entry.getKey().translateOuter(this),entry.getValue());
        return transMap;
    }

    public <K> Map<KeyExpr,K> translateMapKeys(Map<KeyExpr, K> map) {
        Map<KeyExpr, K> transMap = new HashMap<KeyExpr, K>();
        for(Map.Entry<KeyExpr,K> entry : map.entrySet())
            transMap.put(entry.getKey().translateOuter(this),entry.getValue());
        return transMap;
    }

    public <K extends BaseExpr> Map<KeyExpr,K> translateMap(Map<KeyExpr, K> map) {
        Map<KeyExpr, K> transMap = new HashMap<KeyExpr, K>();
        for(Map.Entry<KeyExpr,K> entry : map.entrySet())
            transMap.put(entry.getKey().translateOuter(this), (K) entry.getValue().translateOuter(this));
        return transMap;
    }

    public <K> QuickMap<KeyExpr,K> translateMapKeys(QuickMap<KeyExpr, K> map) {
        QuickMap<KeyExpr, K> transMap = new SimpleMap<KeyExpr, K>();
        for(int i=0;i<map.size;i++)
            transMap.add(map.getKey(i).translateOuter(this),map.getValue(i));
        return transMap;
    }

    // для кэша classWhere на самом деле надо
    public <K> Map<K, VariableClassExpr> translateVariable(Map<K, ? extends VariableClassExpr> map) {
        Map<K,VariableClassExpr> transMap = new HashMap<K, VariableClassExpr>();
        for(Map.Entry<K,? extends VariableClassExpr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateOuter(this));
        return transMap;
    }

    public <K> Map<K, Expr> translate(Map<K, ? extends Expr> map) {
        Map<K, Expr> transMap = new HashMap<K, Expr>();
        for(Map.Entry<K,? extends Expr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateOuter(this));
        return transMap;
    }

    public <K> OrderedMap<Expr, K> translate(OrderedMap<? extends Expr, K> map) {
        OrderedMap<Expr, K> transMap = new OrderedMap<Expr, K>();
        for(Map.Entry<? extends Expr,K> entry : map.entrySet())
            transMap.put(entry.getKey().translateOuter(this),entry.getValue());
        return transMap;
    }

    public List<BaseExpr> translateDirect(List<BaseExpr> list) {
        List<BaseExpr> result = new ArrayList<BaseExpr>();
        for(BaseExpr expr : list)
            result.add(expr.translateOuter(this));
        return result;
    }

    public Set<BaseExpr> translateDirect(Set<BaseExpr> set) {
        Set<BaseExpr> result = new HashSet<BaseExpr>();
        for(BaseExpr expr : set)
            result.add(expr.translateOuter(this));
        return result;
    }

    public Set<KeyExpr> translateKeys(Set<KeyExpr> set) {
        Set<KeyExpr> result = new HashSet<KeyExpr>();
        for(KeyExpr expr : set)
            result.add(expr.translateOuter(this));
        return result;
    }

    public QuickSet<KeyExpr> translateKeys(QuickSet<KeyExpr> set) {
        QuickSet<KeyExpr> result = new QuickSet<KeyExpr>();
        for(KeyExpr expr : set)
            result.add(expr.translateOuter(this));
        return result;
    }

    public QuickSet<VariableClassExpr> translateVariable(QuickSet<VariableClassExpr> set) {
        QuickSet<VariableClassExpr> result = new QuickSet<VariableClassExpr>();
        for(VariableClassExpr expr : set)
            result.add(expr.translateOuter(this));
        return result;
    }

    public <V extends Value> Set<V> translateValues(Set<V> set) {
        Set<V> result = new HashSet<V>();
        for(V expr : set)
            result.add(translate(expr));
        return result;
    }

    public <V extends Value> QuickSet<V> translateValues(QuickSet<V> set) {
        QuickSet<V> result = new QuickSet<V>();
        for(V expr : set)
            result.add(translate(expr));
        return result;
    }

    public List<Expr> translate(List<Expr> list) {
        List<Expr> result = new ArrayList<Expr>();
        for(Expr expr : list)
            result.add(expr.translateOuter(this));
        return result;
    }

    public Set<Expr> translate(Set<Expr> set) {
        Set<Expr> result = new HashSet<Expr>();
        for(Expr expr : set)
            result.add(expr.translateOuter(this));
        return result;
    }

    public <K extends Value, U> QuickMap<K, U> translateValuesMapKeys(QuickMap<K, U> map) {
        QuickMap<K,U> result = new SimpleMap<K,U>();
        for(int i=0;i<map.size;i++)
            result.add(translate(map.getKey(i)), map.getValue(i));
        return result;
    }

    public <K> Map<K, DataObject> translateDataObjects(Map<K, DataObject> map) {
        Map<K, DataObject> result = new HashMap<K, DataObject>();
        for(Map.Entry<K, DataObject> entry : map.entrySet())
            result.put(entry.getKey(), translate(entry.getValue().getExpr()).getDataObject());
        return result;
    }

    private final QuickMap<AbstractTranslateContext, AbstractTranslateContext> caches = new SimpleMap<AbstractTranslateContext, AbstractTranslateContext>();
    public AbstractTranslateContext aspectGetCache(AbstractTranslateContext context) {
        return caches.get(context);
    }
    public void aspectSetCache(AbstractTranslateContext context, AbstractTranslateContext result) {
        caches.add(context, result);
    }
}
