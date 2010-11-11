package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.caches.InnerHashContext;
import platform.server.caches.OuterContext;
import platform.server.caches.TwinsInnerContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class QueryJoin<K extends BaseExpr,I extends OuterContext<I>> extends TwinsInnerContext<QueryJoin<K,I>> {

    private final I query;
    public final Map<K, BaseExpr> group; // вообще гря не reverseable

    // нужны чтобы при merge'е у транслятора хватало ключей/значений
    private final Set<KeyExpr> keys;
    private final Set<ValueExpr> values;

    public Set<KeyExpr> getKeys() {
        return keys;
    }

    public Set<ValueExpr> getValues() {
        return values;
    }

    // дублируем аналогичную логику GroupExpr'а
    protected QueryJoin(QueryJoin<K,I> join, MapTranslate translator) {
        // надо еще транслировать "внутренние" values
        Set<ValueExpr> queryValues = join.getValues();
        MapValuesTranslate mapValues = translator.mapValues().filter(queryValues);

        if(mapValues.identity()) { // если все совпадает то и не перетранслируем внутри ничего
            query = join.query;
            group = translator.translateDirect(join.group);
        } else { // еще values перетранслируем
            MapTranslate valueTranslator = mapValues.mapKeys();
            query = join.query.translateOuter(valueTranslator);
            group = new HashMap<K, BaseExpr>();
            for(Map.Entry<K, BaseExpr> groupJoin : join.group.entrySet())
                group.put((K) groupJoin.getKey().translateOuter(valueTranslator),groupJoin.getValue().translateOuter(translator));
        }
        keys = join.keys;
        values = mapValues.translateValues(queryValues);
    }

    public QueryJoin(Set<KeyExpr> keys, Set<ValueExpr> values, I inner, Map<K, BaseExpr> group) {
        this.keys = keys;
        this.values = values;

        this.query = inner;
        this.group = group;
    }

    protected abstract class QueryInnerHashContext extends InnerHashContext {

        protected abstract int hashOuterExpr(BaseExpr outerExpr);

        public int hashInner(HashContext hashContext) {
            int hash = 0;
            for(Map.Entry<K,BaseExpr> groupExpr : group.entrySet())
                hash += groupExpr.getKey().hashOuter(hashContext) ^ hashOuterExpr(groupExpr.getValue());
            hash = hash * 31;
            for(KeyExpr key : keys)
                hash += key.hashOuter(hashContext);
            hash = hash * 31;
            for(ValueExpr key : values)
                hash += key.hashOuter(hashContext);
            return hash * 31 + query.hashOuter(hashContext);
        }

        public Set<KeyExpr> getKeys() {
            return keys;
        }
    }
    // по сути тоже множественное наследование, правда нюанс что своего же Inner класса
    private QueryInnerHashContext innerContext = new QueryInnerHashContext() {
        protected int hashOuterExpr(BaseExpr outerExpr) {
            return outerExpr.hashCode();
        }

        public Set<ValueExpr> getValues() {
            return values;
        }
    };

    @IdentityLazy
    public int hashInner(HashContext hashContext) {
        return innerContext.hashInner(hashContext);
    }

    protected abstract QueryJoin<K, I> createThis(Set<KeyExpr> keys, Set<ValueExpr> values, I query, Map<K,BaseExpr> group);

    public QueryJoin<K, I> translateInner(MapTranslate translator) {
        return createThis(translator.translateKeys(keys), translator.translateValues(values), query.translateOuter(translator), (Map<K,BaseExpr>) translator.translateKeys(group));
    }

    public boolean equalsInner(QueryJoin<K, I> object) {
        return getClass() == object.getClass() && BaseUtils.hashEquals(query,object.query) && BaseUtils.hashEquals(group,object.group);
    }
}
