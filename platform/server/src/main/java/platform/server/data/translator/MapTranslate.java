package platform.server.data.translator;

import platform.base.OrderedMap;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.VariableClassExpr;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MapTranslate {

    KeyExpr translate(KeyExpr expr);
    <V extends Value> V translate(V expr);

    // аналог mapKeys в HashValues - оставляет только трансляцию выражений
    MapValuesTranslate mapValues();

    // для кэша classWhere на самом деле надо
    <K> Map<K, VariableClassExpr> translateVariable(Map<K, ? extends VariableClassExpr> map);

    <K> Map<K, BaseExpr> translateDirect(Map<K, ? extends BaseExpr> map);

    <K> Map<K, KeyExpr> translateKey(Map<K, KeyExpr> map);

    <K> Map<BaseExpr,K> translateKeys(Map<? extends BaseExpr, K> map);

    <K> Map<Expr,K> translateExprKeys(Map<? extends Expr, K> map);

    <K> Map<KeyExpr,K> translateMapKeys(Map<KeyExpr, K> map);

    <K> Map<K, Expr> translate(Map<K, ? extends Expr> map);

    <K> OrderedMap<Expr, K> translate(OrderedMap<? extends Expr, K> map);

    List<BaseExpr> translateDirect(List<BaseExpr> list);

    Set<BaseExpr> translateDirect(Set<BaseExpr> set);

    Set<KeyExpr> translateKeys(Set<KeyExpr> set);

    <V extends Value> Set<V> translateValues(Set<V> set);

    List<Expr> translate(List<Expr> list);

    Set<Expr> translate(Set<Expr> set);

    MapTranslate reverseMap();

    boolean identityValues(Set<? extends Value> values);
}
