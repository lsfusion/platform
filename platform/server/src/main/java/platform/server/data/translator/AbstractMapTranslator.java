package platform.server.data.translator;

import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.add.MAddExclMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.AbstractTranslateContext;
import platform.server.caches.ManualLazy;
import platform.server.caches.TranslateContext;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.VariableClassExpr;
import platform.server.logics.DataObject;

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

    public <K> ImMap<K, BaseExpr> translateDirect(ImMap<K, ? extends BaseExpr> map) {
        return ((ImMap<K, BaseExpr>)map).mapValues(this.<BaseExpr>TRANS());
    }

    public <K> ImMap<BaseExpr, K> translateKeys(ImMap<? extends BaseExpr, K> map) {
        return ((ImMap<BaseExpr, K>)map).mapKeys(this.<BaseExpr>TRANS());
    }

    public <K, E extends Expr> ImMap<E, K> translateExprKeys(ImMap<E, K> map) {
        return map.mapKeys(this.<E>TRANS());
    }

    public <K> ImMap<KeyExpr,K> translateMapKeys(ImMap<KeyExpr, K> map) {
        return translateExprKeys(map);
    }

    public <K extends TranslateContext, V extends TranslateContext> ImMap<K, V> translateMap(ImMap<? extends K, ? extends V> map) {
        return ((ImMap<K, V>)map).mapKeyValues(this.<K>TRANS(), this.<V>TRANS());
    }

    public <K extends BaseExpr> ImRevMap<KeyExpr, K> translateRevMap(ImRevMap<KeyExpr, K> map) {
        return map.mapRevKeyValues(this.<KeyExpr>TRANS(), this.<K>TRANS());
    }

    // для кэша classWhere на самом деле надо
    public <K> ImRevMap<K, VariableClassExpr> translateVariable(ImRevMap<K, ? extends VariableClassExpr> map) {
        return ((ImRevMap<K,VariableClassExpr>)map).mapRevValues(this.<VariableClassExpr>TRANS());
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

    public ImSet<BaseExpr> translateDirect(ImSet<BaseExpr> set) {
        return set.mapSetValues(this.<BaseExpr>TRANS());
    }

    public ImSet<KeyExpr> translateKeys(ImSet<KeyExpr> set) {
        return set.mapSetValues(this.<KeyExpr>TRANS());
    }

    public <K> ImRevMap<K, KeyExpr> translateKey(ImRevMap<K, KeyExpr> map) {
        return map.mapRevValues(this.<KeyExpr>TRANS());
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

    private final MAddExclMap<AbstractTranslateContext, AbstractTranslateContext> caches = MapFact.mSmallCacheMap();
    public AbstractTranslateContext aspectGetCache(AbstractTranslateContext context) {
        return caches.get(context);
    }
    public void aspectSetCache(AbstractTranslateContext context, AbstractTranslateContext result) {
        caches.exclAdd(context, result);
    }
}
