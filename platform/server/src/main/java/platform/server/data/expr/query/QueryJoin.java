package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.base.Result;
import platform.base.TwinImmutableInterface;
import platform.server.caches.*;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.*;
import platform.server.data.query.*;
import platform.server.data.query.stat.UnionJoin;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// query именно Outer а не Inner, потому как его контекст "связан" с group, и его нельзя прозрачно подменять
public abstract class QueryJoin<K extends Expr,I extends OuterContext<I>, T extends QueryJoin<K, I, T, OC>,
        OC extends QueryJoin.QueryOuterContext<K,I,T,OC>> extends AbstractInnerContext<T> implements InnerJoin<K, T> {

    protected final I query;
    public final Map<K, BaseExpr> group; // вообще гря не reverseable

    public abstract static class QueryOuterContext<K extends Expr,I extends OuterContext<I>, T extends QueryJoin<K, I, T, OC>,
            OC extends QueryJoin.QueryOuterContext<K,I,T,OC>> extends AbstractOuterContext<OC> {

        protected final T thisObj;
        protected QueryOuterContext(T thisObj) {
            this.thisObj = thisObj;
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(final HashContext hashContext) {
            return new QueryInnerHashContext<K, I>(thisObj) {
                protected int hashOuterExpr(BaseExpr outerExpr) {
                    return outerExpr.hashOuter(hashContext);
                }
            }.hashValues(hashContext.values);
        }

        public T getThis() {
            return thisObj;
        }

        @Override
        public QuickSet<OuterContext> calculateOuterDepends() {
            return new QuickSet<OuterContext>(thisObj.group.values());
        }

        public QuickSet<Value> getValues() {
            return super.getValues().merge(thisObj.getInnerValues());
        }

        public boolean twins(TwinImmutableInterface o) {
            return thisObj.equals(thisObj);
        }

        public InnerExpr getInnerExpr(WhereJoin join) {
            return QueryJoin.getInnerExpr(thisObj, join);
        }
        public NotNullExprSet getExprFollows(boolean recursive) {
            return InnerExpr.getExprFollows(thisObj, recursive);
        }
        public InnerJoins getInnerJoins() {
            return InnerExpr.getInnerJoins(thisObj);
        }
        public InnerJoins getJoinFollows(Result<Map<InnerJoin, Where>> upWheres, Collection<UnionJoin> unionJoins) {
            return InnerExpr.getFollowJoins(thisObj, upWheres, unionJoins);
        }

        public abstract T translateThis(MapTranslate translate);

        protected OC translate(MapTranslate translator) {
            return translateThis(translator).getOuter();
        }
    }
    protected abstract OC createOuterContext();
    protected OC outer;
    private OC getOuter() {
        if(outer==null)
            outer = createOuterContext();
        return outer;
    }
    public QuickSet<KeyExpr> getOuterKeys() {
        return getOuter().getOuterKeys();
    }
    public QuickSet<Value> getOuterValues() {
        return getOuter().getOuterValues();
    }
    public int hashOuter(HashContext hashContext) {
        return getOuter().hashOuter(hashContext);
    }
    public QuickSet<platform.server.caches.OuterContext> getOuterDepends() {
        return getOuter().getOuterDepends();
    }
    public boolean enumerate(ExprEnumerator enumerator) {
        return getOuter().enumerate(enumerator);
    }
    protected long calculateComplexity(boolean outer) {
        return getOuter().getComplexity(outer);
    }
    public T translateOuter(MapTranslate translator) {
        return getOuter().translateOuter(translator).getThis();
    }

    public InnerExpr getInnerExpr(WhereJoin join) {
        return getOuter().getInnerExpr(join);
    }
    public NotNullExprSet getExprFollows(boolean recursive) {
        return getOuter().getExprFollows(recursive);
    }
    public InnerJoins getInnerJoins() {
        return getOuter().getInnerJoins();
    }
    public InnerJoins getJoinFollows(Result<Map<InnerJoin, Where>> upWheres, Collection<UnionJoin> unionJoins) {
        return getOuter().getJoinFollows(upWheres, unionJoins);
    }

    public Map<K, BaseExpr> getJoins() {
        return group;
    }

    // множественное наследование
    public static InnerExpr getInnerExpr(InnerJoin<?, ?> join, WhereJoin whereJoin) {
        QuickSet<InnerExpr> set = whereJoin.getExprFollows(true).getInnerExprs(null);
        for(int i=0;i<set.size;i++) {
            InnerExpr expr = set.get(i);
            if(BaseUtils.hashEquals(join,expr.getInnerJoin()))
                return expr;
        }
        return null;
    }

    // нужны чтобы при merge'е у транслятора хватало ключей/значений
    protected final QuickSet<KeyExpr> keys;
    private final QuickSet<Value> values;

    public QuickSet<KeyExpr> getKeys() {
        return keys;
    }

    public QuickSet<Value> getValues() {
        return values;
    }

    // дублируем аналогичную логику GroupExpr'а
    protected QueryJoin(T join, MapTranslate translator) {
        // надо еще транслировать "внутренние" values
        MapValuesTranslate mapValues = translator.mapValues().filter(join.values);
        MapTranslate valueTranslator = mapValues.mapKeys();
        query = join.query.translateOuter(valueTranslator);
        group = new HashMap<K, BaseExpr>();
        for(Map.Entry<K, BaseExpr> groupJoin : join.group.entrySet())
            group.put((K) groupJoin.getKey().translateOuter(valueTranslator),groupJoin.getValue().translateOuter(translator));
        keys = join.keys;
        values = mapValues.translateValues(join.values);
    }

    public QueryJoin(QuickSet<KeyExpr> keys, QuickSet<Value> values, I inner, Map<K, BaseExpr> group) {
        this.keys = keys;
        this.values = values;

        this.query = inner;
        this.group = group;
    }

    protected abstract static class QueryInnerHashContext<K extends Expr,I extends OuterContext<I>> extends AbstractInnerHashContext {

        protected final QueryJoin<K, I, ?, ?> thisObj;
        protected QueryInnerHashContext(QueryJoin<K, I, ?, ?> thisObj) {
            this.thisObj = thisObj;
        }

        protected abstract int hashOuterExpr(BaseExpr outerExpr);

        public int hashInner(HashContext hashContext) {
            int hash = 0;
            for(Map.Entry<K,BaseExpr> groupExpr : thisObj.group.entrySet())
                hash += groupExpr.getKey().hashOuter(hashContext) ^ hashOuterExpr(groupExpr.getValue());
            hash = hash * 31;
            for(KeyExpr key : thisObj.keys)
                hash += hashContext.keys.hash(key);
            hash = hash * 31;
            for(Value value : thisObj.values)
                hash += hashContext.values.hash(value);
            return hash * 31 + thisObj.query.hashOuter(hashContext);
        }

        public QuickSet<KeyExpr> getInnerKeys() {
            return thisObj.keys;
        }
    }
    private QueryInnerHashContext<K, I> innerHashContext = new QueryInnerHashContext<K, I>(this) { // по сути тоже множественное наследование, правда нюанс что своего же Inner класса
        protected int hashOuterExpr(BaseExpr outerExpr) {
            return outerExpr.hashCode();
        }
    };
    protected boolean isComplex() {
        return true;
    }
    public int hash(HashContext hashContext) {
        return innerHashContext.hashInner(hashContext);
    }

    protected abstract T createThis(QuickSet<KeyExpr> keys, QuickSet<Value> values, I query, Map<K,BaseExpr> group);

    protected T translate(MapTranslate translator) {
        return createThis(translator.translateKeys(keys), translator.translateValues(values), query.translateOuter(translator), (Map<K,BaseExpr>) translator.translateExprKeys(group));
    }

    public boolean equalsInner(T object) {
        return getClass() == object.getClass() && BaseUtils.hashEquals(query, object.query) && BaseUtils.hashEquals(group,object.group);
    }
}
