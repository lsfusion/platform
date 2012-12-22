package platform.server.data.translator;

import platform.base.col.interfaces.immutable.*;
import platform.server.caches.OuterContext;
import platform.server.caches.TranslateContext;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.VariableClassExpr;
import platform.server.logics.DataObject;

import java.util.Map;

public interface MapTranslate extends MapObject {

    KeyExpr translate(KeyExpr expr);
    <V extends Value> V translate(V expr);

    MapTranslate filterValues(ImSet<? extends Value> values);

    // аналог mapKeys в HashValues - оставляет только трансляцию выражений
    MapValuesTranslate mapValues();
    MapTranslate onlyKeys();
    MapTranslate mapValues(MapValuesTranslate translate);

    // для кэша classWhere на самом деле надо
    <K> ImRevMap<K, VariableClassExpr> translateVariable(ImRevMap<K, ? extends VariableClassExpr> map);

    <K> ImMap<K, BaseExpr> translateDirect(ImMap<K, ? extends BaseExpr> map);

    <K> ImRevMap<K, KeyExpr> translateKey(ImRevMap<K, KeyExpr> map);

    <K> ImMap<BaseExpr, K> translateKeys(ImMap<? extends BaseExpr, K> map);

    <K, E extends Expr> ImMap<E, K> translateExprKeys(ImMap<E, K> map);

    <K extends TranslateContext, V extends TranslateContext> ImMap<K, V> translateMap(ImMap<? extends K, ? extends V> map);

    <K extends BaseExpr> ImRevMap<KeyExpr, K> translateRevMap(ImRevMap<KeyExpr, K> map); // по аналогии с верхним

    <K> ImMap<KeyExpr,K> translateMapKeys(ImMap<KeyExpr, K> map);

    <K> ImMap<K, Expr> translate(ImMap<K, ? extends Expr> map);

    <K> ImOrderMap<Expr, K> translate(ImOrderMap<? extends Expr, K> map);

    ImList<BaseExpr> translateDirect(ImList<BaseExpr> list);

    ImSet<BaseExpr> translateDirect(ImSet<BaseExpr> set);

    ImSet<KeyExpr> translateKeys(ImSet<KeyExpr> set);

    ImSet<VariableClassExpr> translateVariable(ImSet<VariableClassExpr> set);

    <V extends Value> ImSet<V> translateValues(ImSet<V> set);

    <K extends Value, V> ImMap<K, V> translateValuesMapKeys(ImMap<K, V> map);

    <K> ImMap<K, DataObject> translateDataObjects(ImMap<K, DataObject> map);

    ImList<Expr> translate(ImList<Expr> list);

    ImSet<Expr> translate(ImSet<Expr> set);

    MapTranslate reverseMap();

    boolean identityKeysValues(ImSet<KeyExpr> keys, ImSet<? extends Value> values);
    boolean identityKeys(ImSet<KeyExpr> keys);
    boolean identityValues(ImSet<? extends Value> values);
}
