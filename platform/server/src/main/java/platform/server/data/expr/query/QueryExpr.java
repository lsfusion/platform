package platform.server.data.expr.query;

import platform.server.caches.IdentityLazy;
import platform.server.caches.InnerHashContext;
import platform.server.caches.OuterContext;
import platform.server.caches.TwinsInnerContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.*;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;

import java.util.*;

public abstract class QueryExpr<K extends BaseExpr,I extends OuterContext<I>,J extends QueryJoin> extends InnerExpr {

    public I query;
    Map<K, BaseExpr> group; // вообще гря не reverseable

    protected QueryExpr(I query, Map<K, BaseExpr> group) {
        this.query = query;
        this.group = group;

        assert checkExpr();
    }

    protected boolean checkExpr() {
        return true;
    }

    // трансляция
    protected QueryExpr(QueryExpr<K,I,J> queryExpr, MapTranslate translator) {
        // надо еще транслировать "внутренние" values
        MapValuesTranslate mapValues = translator.mapValues().filter(queryExpr.innerContext.getValues());

        if(mapValues.identity()) { // если все совпадает то и не перетранслируем внутри ничего
            query = queryExpr.query;
            group = translator.translateDirect(queryExpr.group);
        } else { // еще values перетранслируем
            MapTranslate valueTranslator = mapValues.mapKeys();
            query = queryExpr.query.translateOuter(valueTranslator);
            group = new HashMap<K, BaseExpr>();
            for(Map.Entry<K, BaseExpr> keyJoin : queryExpr.group.entrySet())
                group.put((K)keyJoin.getKey().translateOuter(valueTranslator),keyJoin.getValue().translateOuter(translator));
        }

        assert checkExpr();
    }

    protected abstract QueryExpr<K,I,J> createThis(I query, Map<K,BaseExpr> group);

    protected abstract class QueryInnerHashContext extends InnerHashContext {

        protected abstract int hashOuterExpr(BaseExpr outerExpr);

        public int hashInner(HashContext hashContext) {
            int hash = 0;
            for(Map.Entry<K,BaseExpr> groupExpr : group.entrySet())
                hash += groupExpr.getKey().hashOuter(hashContext) ^ hashOuterExpr(groupExpr.getValue());
            return query.hashOuter(hashContext) * 31 + hash;
        }

        public Set<KeyExpr> getKeys() {
            return QueryExpr.this.getKeys();
        }
    }

    // вообще должно быть множественное наследование самого QueryExpr от TwinsInnerContext
    protected class QueryInnerContext extends TwinsInnerContext<QueryInnerContext> {

        // вообще должно быть множественное наследование от QueryInnerHashContext, правда нюанс что вместе с верхним своего же Inner класса
        private final QueryInnerHashContext inherit = new QueryInnerHashContext() {
            protected int hashOuterExpr(BaseExpr outerExpr) {
                return outerExpr.hashCode();
            }
        };

        public int hashInner(HashContext hashContext) {
            return inherit.hashInner(hashContext);
        }

        public Set<KeyExpr> getKeys() {
            return inherit.getKeys();
        }

        public Set<ValueExpr> getValues() {
            return QueryExpr.this.getValues();
        }

        public QueryInnerContext translateInner(MapTranslate translate) {
            return createThis(query.translateOuter(translate), (Map<K,BaseExpr>) translate.translateKeys(group)).innerContext;
        }

        private QueryExpr<K,I,J> getThis() {
            return QueryExpr.this;
        }

        public boolean equalsInner(QueryInnerContext object) {
            return QueryExpr.this.getClass()==object.getThis().getClass() &&  query.equals(object.getThis().query) && group.equals(object.getThis().group);
        }
    }
    protected final QueryInnerContext innerContext = new QueryInnerContext();

    // чисто для Lazy
    @IdentityLazy
    public Set<KeyExpr> getKeys() {
        return getKeys(query, group);
    }

    @IdentityLazy
    public Set<ValueExpr> getValues() {
        return enumValues(group.keySet(), query.getEnum());
    }

    @IdentityLazy
    public int hashOuter(final HashContext hashContext) {
        return new QueryInnerHashContext() {
            protected int hashOuterExpr(BaseExpr outerExpr) {
                return outerExpr.hashOuter(hashContext);
            }
        }.hashInner(hashContext.values);
    }

    public void enumerate(ContextEnumerator enumerator) {
        enumerator.fill(group);
        for(ValueExpr value : innerContext.getValues())
            enumerator.add(value);
    }

    public VariableExprSet getJoinFollows() {
        return InnerExpr.getExprFollows(group);
    }

    public boolean twins(AbstractSourceJoin obj) {
        QueryExpr<K,I,J> groupExpr = (QueryExpr)obj;

        assert hashCode()==groupExpr.hashCode();

        return innerContext.equals(groupExpr.innerContext);
    }

    public abstract J getGroupJoin();

    public J getFJGroup() {
        return getGroupJoin();
    }

    protected static <I extends OuterContext<I>,K extends BaseExpr> Set<KeyExpr> getKeys(I expr, Map<K, BaseExpr> group) {
        return enumKeys(group.keySet(),expr.getEnum());
    }

    public long calculateComplexity() {
        return (getComplexity(Arrays.asList(query.getEnum())) + getComplexity(group.keySet())) * 100 + getComplexity(group.values());
    }
}
