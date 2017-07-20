package lsfusion.server.data.translator;

import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.caches.TranslateContext;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.VariableClassExpr;
import lsfusion.server.data.expr.VariableSingleClassExpr;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DataObject;

public interface MapTranslate extends MapObject {

    ParamExpr translate(ParamExpr expr);
    <V extends Value> V translate(V expr);

    MapTranslate filterValues(ImSet<? extends Value> values);

    // аналог mapKeys в HashValues - оставляет только трансляцию выражений
    MapValuesTranslate mapValues();
    MapTranslate onlyKeys();
    MapTranslate mapValues(MapValuesTranslate translate);

    // для кэша classWhere на самом деле надо
    <K> ImRevMap<K, VariableSingleClassExpr> translateVariable(ImRevMap<K, ? extends VariableSingleClassExpr> map);

    <K, V extends BaseExpr> ImMap<K, V> translateDirect(ImMap<K, V> map);

    <K> ImRevMap<K, ParamExpr> translateKey(ImRevMap<K, ParamExpr> map);

    <K> ImMap<BaseExpr, K> translateKeys(ImMap<? extends BaseExpr, K> map);

    <K, E extends Expr> ImMap<E, K> translateExprKeys(ImMap<E, K> map);
    <K, E extends Expr> ImRevMap<E, K> translateExprRevKeys(ImRevMap<E, K> map);

    <K, E extends TranslateContext> ImMap<E, K> translateOuterKeys(ImMap<E, K> map);

    <K extends TranslateContext, V extends TranslateContext> ImMap<K, V> translateMap(ImMap<? extends K, ? extends V> map);
    <K extends TranslateContext> ImSet<K> translateSet(ImSet<? extends K> set);

    <K extends BaseExpr, V extends BaseExpr> ImRevMap<K, V> translateRevMap(ImRevMap<K, V> map); // по аналогии с верхним
    <K, V extends BaseExpr> ImRevMap<K, V> translateRevValues(ImRevMap<K, V> map); // по аналогии с верхним

    <K> ImMap<ParamExpr,K> translateMapKeys(ImMap<ParamExpr, K> map);

    <K> ImMap<K, Expr> translate(ImMap<K, ? extends Expr> map);

    <K> ImOrderMap<Expr, K> translate(ImOrderMap<? extends Expr, K> map);

    ImList<BaseExpr> translateDirect(ImList<BaseExpr> list);

    <K extends BaseExpr> ImSet<K> translateDirect(ImSet<K> set);

    ImSet<ParamExpr> translateKeys(ImSet<ParamExpr> set);

    ImSet<VariableClassExpr> translateVariable(ImSet<VariableClassExpr> set);

    <V extends Value> ImSet<V> translateValues(ImSet<V> set);

    <K extends Value, V> ImMap<K, V> translateValuesMapKeys(ImMap<K, V> map);

    <K> ImMap<K, DataObject> translateDataObjects(ImMap<K, DataObject> map);

    ImList<Expr> translate(ImList<Expr> list);

    ImSet<Expr> translate(ImSet<Expr> set);

    <K extends Expr> ClassWhere<K> translate(ClassWhere<K> classes);

    MapTranslate reverseMap();

    boolean identityKeysValues(ImSet<ParamExpr> keys, ImSet<? extends Value> values);
    boolean identityKeys(ImSet<ParamExpr> keys);
    boolean identityValues(ImSet<? extends Value> values);
}
