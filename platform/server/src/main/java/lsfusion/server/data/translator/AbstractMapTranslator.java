package lsfusion.server.data.translator;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.lru.LRUCache;
import lsfusion.base.col.lru.MCacheMap;
import lsfusion.server.caches.AbstractTranslateContext;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.caches.TranslateContext;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.VariableClassExpr;
import lsfusion.server.data.expr.VariableSingleClassExpr;
import lsfusion.server.logics.DataObject;

public abstract class AbstractMapTranslator extends TwinImmutableObject implements MapTranslate {

    private GetValue<TranslateContext, TranslateContext> trans;
    @ManualLazy
    private <V extends TranslateContext> GetValue<V, V> TRANS() {
        if(trans==null)
            trans = new GetValue<TranslateContext, TranslateContext>() {
            public TranslateContext getMapValue(TranslateContext value) {
                return value.translateOuter(AbstractMapTranslator.this);
            }};
        return (GetValue<V, V>)trans;
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

    public <K, E extends Expr> ImRevMap<E, K> translateExprRevKeys(ImRevMap<E, K> map) {
        return map.mapRevKeys(this.<E>TRANS());
    }

    public <K> ImMap<ParamExpr,K> translateMapKeys(ImMap<ParamExpr, K> map) {
        return map.mapKeys(this.<ParamExpr>TRANS());
    }

    public <K extends TranslateContext, V extends TranslateContext> ImMap<K, V> translateMap(ImMap<? extends K, ? extends V> map) {
        return ((ImMap<K, V>)map).mapKeyValues(this.<K>TRANS(), this.<V>TRANS());
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
        return map.mapValues(new GetValue<DataObject, DataObject>() {
            public DataObject getMapValue(DataObject value) {
                return translate(value.getExpr()).getDataObject();
            }});
    }

    private final MCacheMap<AbstractTranslateContext, AbstractTranslateContext> caches = LRUCache.mSmall(LRUCache.EXP_QUICK);

    public AbstractTranslateContext aspectGetCache(AbstractTranslateContext context) {
        return caches.get(context);
    }
    public void aspectSetCache(AbstractTranslateContext context, AbstractTranslateContext result) {
        caches.exclAdd(context, result);
    }
}
