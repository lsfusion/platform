package lsfusion.server.data.translate;

import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.VariableClassExpr;
import lsfusion.server.data.expr.classes.VariableSingleClassExpr;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.classes.ClassWhere;

import java.util.function.Function;

public abstract class AbstractMapTranslator extends TwinImmutableObject implements MapTranslate {

    private Function<TranslateContext, TranslateContext> trans;
    @ManualLazy
    private <V extends TranslateContext> Function<V, V> TRANS() {
        if(trans==null)
            trans = value -> value.translateOuter(AbstractMapTranslator.this);
        return (Function<V, V>)trans;
    }

    public <K, V extends BaseExpr> ImMap<K, V> translateDirect(ImMap<K, V> map) {
        return map.mapValues(this.<V>TRANS());
    }

    public <K> ImMap<BaseExpr, K> translateKeys(ImMap<? extends BaseExpr, K> map) {
        return ((ImMap<BaseExpr, K>)map).mapKeys(this.<BaseExpr>TRANS());
    }

    public <K, E extends Expr> ImMap<E, K> translateExprKeys(ImMap<E, K> map) {
        return map.mapKeys(this.<E>TRANS());
    }

    public <K, E extends TranslateContext> ImMap<E, K> translateOuterKeys(ImMap<E, K> map) {
        return map.mapKeys(this.<E>TRANS());
    }

    public <K, E extends Expr> ImRevMap<E, K> translateExprRevKeys(ImRevMap<E, K> map) {
        return map.mapRevKeys(this.<E>TRANS());
    }

    public <K> ImMap<ParamExpr,K> translateMapKeys(ImMap<ParamExpr, K> map) {
        return map.mapKeys(this.<ParamExpr>TRANS());
    }

    public <K extends TranslateContext, V extends TranslateContext> ImMap<K, V> translateMap(ImMap<? extends K, ? extends V> map) {
        return ((ImMap<K, V>)map).mapKeyValues(this.<K>TRANS(), this.<V>TRANS());
    }

    public <K extends TranslateContext> ImSet<K> translateSet(ImSet<? extends K> set) {
        return ((ImSet<K>)set).mapSetValues(this.<K>TRANS());
    }

    public <K extends BaseExpr, V extends BaseExpr> ImRevMap<K, V> translateRevMap(ImRevMap<K, V> map) {
        return map.mapRevKeyValues(this.<K>TRANS(), this.<V>TRANS());
    }

    public <K, V extends BaseExpr> ImRevMap<K, V> translateRevValues(ImRevMap<K, V> map) {
        return map.mapRevValues(this.<V>TRANS());
    }

    // для кэша classWhere на самом деле надо
    public <K> ImRevMap<K, VariableSingleClassExpr> translateVariable(ImRevMap<K, ? extends VariableSingleClassExpr> map) {
        return ((ImRevMap<K,VariableSingleClassExpr>)map).mapRevValues(this.<VariableSingleClassExpr>TRANS());
    }

    public <K> ImMap<K, Expr> translate(ImMap<K, ? extends Expr> map) {
        return ((ImMap<K, Expr>)map).mapValues(this.<Expr>TRANS());
    }

    public <K> ImOrderMap<Expr, K> translate(ImOrderMap<? extends Expr, K> map) {
        return ((ImOrderMap<Expr, K>)map).mapOrderKeys(this.<Expr>TRANS());
    }

    public ImList<BaseExpr> translateDirect(ImList<BaseExpr> list) {
        return list.mapListValues(this.<BaseExpr>TRANS());
    }

    public <K extends BaseExpr> ImSet<K> translateDirect(ImSet<K> set) {
        return set.mapSetValues(this.<K>TRANS());
    }

    public ImSet<ParamExpr> translateKeys(ImSet<ParamExpr> set) {
        return set.mapSetValues(this.<ParamExpr>TRANS());
    }

    public <K> ImRevMap<K, ParamExpr> translateKey(ImRevMap<K, ParamExpr> map) {
        return map.mapRevValues(this.<ParamExpr>TRANS());
    }

    public ImSet<VariableClassExpr> translateVariable(ImSet<VariableClassExpr> set) {
        return set.mapSetValues(this.<VariableClassExpr>TRANS());
    }

    public <V extends Value> ImSet<V> translateValues(ImSet<V> set) {
        return mapValues().translateValues(set);
    }

    public ImList<Expr> translate(ImList<Expr> list) {
        return list.mapListValues(this.<Expr>TRANS());
    }

    public ImSet<Expr> translate(ImSet<Expr> set) {
        return set.mapSetValues(this.<Expr>TRANS());
    }

    public <K extends Value, U> ImMap<K, U> translateValuesMapKeys(ImMap<K, U> map) {
        return mapValues().translateValuesMapKeys(map);
    }

    public <K> ImMap<K, DataObject> translateDataObjects(ImMap<K, DataObject> map) {
        return map.mapValues(value -> translate(value.getExpr()).getDataObject());
    }

    public <K extends Expr> ClassWhere<K> translate(ClassWhere<K> classes) {
        return classes.remap(this.<K>TRANS());
    }
}
