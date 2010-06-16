package platform.server.data.expr.query;

import platform.server.caches.*;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@GenericImmutable
public abstract class QueryJoin<K extends BaseExpr,I extends TranslateContext<I>> implements MapContext {

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
            query = join.query.translate(valueTranslator);
            group = new HashMap<K, BaseExpr>();
            for(Map.Entry<K, BaseExpr> groupJoin : join.group.entrySet())
                group.put((K) groupJoin.getKey().translate(valueTranslator),groupJoin.getValue().translate(translator));
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

    // извращенное множественное наследование
    protected QueryHashes<K> hashes = new QueryHashes<K>() {
        protected int hashValue(HashContext hashContext) {
            int hash = 0;
            for(KeyExpr key : keys)
                hash += key.hashContext(hashContext);
            hash = hash * 31;
            for(ValueExpr key : values)
                hash += key.hashContext(hashContext);
            return hash * 31 + query.hashContext(hashContext);
        }
        protected Map<K, BaseExpr> getGroup() {
            return group;
        }
    };
    @GenericLazy
    public int hash(HashContext hashContext) {
        return hashes.hash(hashContext);
    }

    public MapTranslate merge(QueryJoin<K,I> groupJoin) {
        if(hashCode()!=groupJoin.hashCode())
            return null;

        for(MapTranslate translator : new MapHashIterable(this,groupJoin,false))
            if(query.translate(translator).equals(groupJoin.query) &&
               translator.translateKeys(group).equals(groupJoin.group)) // нельзя reverse'ить
                    return translator;
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || getClass()==o.getClass() && merge((QueryJoin) o)!=null;
    }

    boolean hashCoded = false;
    int hashCode;
    public int hashCode() {
        if(!hashCoded) {
            hashCode = MapParamsIterable.hash(this,false);
            hashCoded = true;
        }
        return hashCode;
    }
}
