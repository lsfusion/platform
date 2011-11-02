package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.server.caches.*;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.*;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.translator.HashLazy;
import platform.server.data.translator.HashOuterLazy;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class QueryJoin<K extends Expr,I extends OuterContext<I>> extends TwinsInnerContext<QueryJoin<K,I>> implements InnerJoin<K> {

    protected final I query;
    public final Map<K, BaseExpr> group; // вообще гря не reverseable

    public InnerExprSet getExprFollows(boolean recursive) {
        return InnerExpr.getExprFollows(this, recursive);
    }

    public InnerJoins getInnerJoins() {
        return InnerExpr.getInnerJoins(this);
    }

    public InnerJoins getJoinFollows(Result<Map<InnerJoin, Where>> upWheres) {
        return InnerExpr.getFollowJoins(this, upWheres);
    }

    @HashOuterLazy
    public int hashOuter(final HashContext hashContext) {
        return new QueryInnerHashContext() {
            protected int hashOuterExpr(BaseExpr outerExpr) {
                return outerExpr.hashOuter(hashContext);
            }
        }.hashValues(hashContext.values);
    }

    public InnerExpr getInnerExpr(WhereJoin join) {
        return QueryJoin.getInnerExpr(this, join);
    }

    public Map<K, BaseExpr> getJoins() {
        return group;
    }

    // множественное наследование
    public static InnerExpr getInnerExpr(InnerJoin<?> join, WhereJoin whereJoin) {
        InnerExprSet set = whereJoin.getExprFollows(true);
        for(int i=0;i<set.size;i++) {
            InnerExpr expr = set.get(i);
            if(BaseUtils.hashEquals(join,expr.getInnerJoin()))
                return expr;
        }
        return null;
    }

    // нужны чтобы при merge'е у транслятора хватало ключей/значений
    protected final Set<KeyExpr> keys;
    private final Set<Value> values;

    public Set<KeyExpr> getKeys() {
        return keys;
    }

    public Set<Value> getValues() {
        return values;
    }

    public SourceJoin[] getEnum() {
        return AbstractSourceJoin.merge(BaseUtils.merge(group.values(), group.keySet()), query.getEnum());
    }

    @IdentityLazy
    public Set<Value> getOuterValues() {
        return AbstractOuterContext.getOuterValues(this);
    }

    // дублируем аналогичную логику GroupExpr'а
    protected QueryJoin(QueryJoin<K,I> join, MapTranslate translator) {
        // надо еще транслировать "внутренние" values
        Set<Value> queryValues = join.getValues();
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

    public QueryJoin(Set<KeyExpr> keys, Set<Value> values, I inner, Map<K, BaseExpr> group) {
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
                hash += hashContext.keys.hash(key);
            hash = hash * 31;
            for(Value value : values)
                hash += hashContext.values.hash(value);
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
    };

    @HashLazy
    public int hashInner(HashContext hashContext) {
        return innerContext.hashInner(hashContext);
    }

    protected abstract QueryJoin<K, I> createThis(Set<KeyExpr> keys, Set<Value> values, I query, Map<K,BaseExpr> group);

    public QueryJoin<K, I> translateInner(MapTranslate translator) {
        return createThis(translator.translateKeys(keys), translator.translateValues(values), query.translateOuter(translator), (Map<K,BaseExpr>) translator.translateExprKeys(group));
    }

    public boolean equalsInner(QueryJoin<K, I> object) {
        return getClass() == object.getClass() && BaseUtils.hashEquals(query,object.query) && BaseUtils.hashEquals(group,object.group);
    }
}
