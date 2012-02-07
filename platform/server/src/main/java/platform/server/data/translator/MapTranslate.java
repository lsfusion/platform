package platform.server.data.translator;

import platform.base.OrderedMap;
import platform.base.QuickMap;
import platform.base.QuickSet;
import platform.server.caches.hash.HashKeys;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.VariableClassExpr;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MapTranslate extends MapObject {

    KeyExpr translate(KeyExpr expr);
    <V extends Value> V translate(V expr);

    // аналог mapKeys в HashValues - оставляет только трансляцию выражений
    MapValuesTranslate mapValues();
    MapTranslate onlyKeys();
    MapTranslate mapValues(MapValuesTranslate translate);

    // для кэша classWhere на самом деле надо
    <K> Map<K, VariableClassExpr> translateVariable(Map<K, ? extends VariableClassExpr> map);

    <K> Map<K, BaseExpr> translateDirect(Map<K, ? extends BaseExpr> map);

    <K> Map<K, KeyExpr> translateKey(Map<K, KeyExpr> map);

    <K> Map<BaseExpr,K> translateKeys(Map<? extends BaseExpr, K> map);

    <K> Map<Expr,K> translateExprKeys(Map<? extends Expr, K> map);

    <K> Map<KeyExpr,K> translateMapKeys(Map<KeyExpr, K> map);

    <K> QuickMap<KeyExpr,K> translateMapKeys(QuickMap<KeyExpr, K> map);

    <K> Map<K, Expr> translate(Map<K, ? extends Expr> map);

    <K> OrderedMap<Expr, K> translate(OrderedMap<? extends Expr, K> map);

    List<BaseExpr> translateDirect(List<BaseExpr> list);

    Set<BaseExpr> translateDirect(Set<BaseExpr> set);

    Set<KeyExpr> translateKeys(Set<KeyExpr> set);

    QuickSet<KeyExpr> translateKeys(QuickSet<KeyExpr> set);

    QuickSet<VariableClassExpr> translateVariable(QuickSet<VariableClassExpr> set);

    <V extends Value> Set<V> translateValues(Set<V> set);

    <V extends Value> QuickSet<V> translateValues(QuickSet<V> set);
    
    <K extends Value, V> QuickMap<K,V> translateValuesMapKeys(QuickMap<K, V> map);

    List<Expr> translate(List<Expr> list);

    Set<Expr> translate(Set<Expr> set);

    MapTranslate reverseMap();

    boolean identityKeysValues(QuickSet<KeyExpr> keys, QuickSet<? extends Value> values);
    boolean identityKeys(QuickSet<KeyExpr> keys);
    boolean identityValues(QuickSet<? extends Value> values);
}
