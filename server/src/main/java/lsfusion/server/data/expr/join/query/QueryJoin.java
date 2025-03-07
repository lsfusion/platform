package lsfusion.server.data.expr.join.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.ContextEnumerator;
import lsfusion.server.data.caches.AbstractInnerContext;
import lsfusion.server.data.caches.AbstractInnerHashContext;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.NullableExpr;
import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.expr.join.base.BaseJoin;
import lsfusion.server.data.expr.join.classes.InnerExprFollows;
import lsfusion.server.data.expr.join.inner.InnerJoin;
import lsfusion.server.data.expr.join.inner.InnerJoins;
import lsfusion.server.data.expr.join.where.WhereJoin;
import lsfusion.server.data.expr.join.where.WhereJoins;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.expr.query.QueryExpr;
import lsfusion.server.data.expr.value.StaticValueExpr;
import lsfusion.server.data.stat.*;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.Where;

// query именно Outer а не Inner, потому как его контекст "связан" с group, и его нельзя прозрачно подменять
public abstract class QueryJoin<K extends Expr,I extends QueryJoin.Query<K, I>, T extends QueryJoin<K, I, T, OC>,
        OC extends QueryJoin.QueryOuterContext<K,I,T,OC>> extends AbstractInnerContext<T> implements InnerJoin<K, T> {

    protected final I query;
    public final ImMap<K, BaseExpr> group; // вообще гря не reverseable

    protected abstract static class Query<K extends Expr, Q extends Query<K, Q>> extends AbstractOuterContext<Q> {

        protected final InnerExprFollows<K> follows;

        public Query(InnerExprFollows<K> follows) {
            this.follows = follows;
        }

        public Query(Q query, MapTranslate translate) {
            this.follows = query.follows.translateOuter(translate);
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return follows.equals(((Query) o).follows);
        }

        public int hash(HashContext hashContext) {
            return follows.hashOuter(hashContext);
        }

        public ImSet<OuterContext> calculateOuterDepends() {
            return SetFact.singleton(follows);
        }
    }

    public abstract static class QueryOuterContext<K extends Expr,I extends QueryJoin.Query<K, I>, T extends QueryJoin<K, I, T, OC>,
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
        public ImSet<OuterContext> calculateOuterDepends() {
            return BaseUtils.immutableCast(thisObj.group.values().toSet());
        }

        @Override
        public ImSet<Value> getValues() {
            return super.getValues().merge(thisObj.getInnerValues());
        }

        @Override
        public ImSet<StaticValueExpr> getOuterStaticValues() {
            throw new RuntimeException("should not be");
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return thisObj.equals(((QueryOuterContext)o).thisObj);
        }

        public InnerExpr getInnerExpr(WhereJoin join) {
            return QueryJoin.getInnerExpr(thisObj, join);
        }

        public boolean hasExprFollowsWithoutNotNull() {
            return InnerExpr.hasExprFollowsWithoutNotNull(thisObj);
        }
        public InnerJoins getInnerJoins() {
            return InnerExpr.getInnerJoins(thisObj);
        }

        public abstract T translateThis(MapTranslate translate);

        protected OC translate(MapTranslate translator) {
            return translateThis(translator).getOuter();
        }
    }
    protected abstract OC createOuterContext();
    protected OC outer;
    protected OC getOuter() {
        if(outer==null)
            outer = createOuterContext();
        return outer;
    }
    public ImSet<ParamExpr> getOuterKeys() {
        return getOuter().getOuterKeys();
    }
    public ImSet<Value> getOuterValues() {
        return getOuter().getOuterValues();
    }
    public int hashOuter(HashContext hashContext) {
        return getOuter().hashOuter(hashContext);
    }
    public ImSet<OuterContext> getOuterDepends() {
        return getOuter().getOuterDepends();
    }
    public boolean enumerate(ContextEnumerator enumerator) {
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
    public boolean isValue() {
        return getOuter().isValue();
    }
    public boolean hasExprFollowsWithoutNotNull() {
        return getOuter().hasExprFollowsWithoutNotNull();
    }
    public InnerJoins getInnerJoins() {
        return getOuter().getInnerJoins();
    }

    public ImMap<K, BaseExpr> getJoins() {
        return group;
    }

    // множественное наследование
    public static InnerExpr getInnerExpr(InnerJoin<?, ?> join, BaseJoin<?> whereJoin) {
        ImSet<InnerExpr> set = NullableExpr.getInnerExprs(whereJoin.getExprFollows(NullableExpr.INNERJOINS, true), null);
        for(int i=0,size=set.size();i<size;i++) {
            InnerExpr expr = set.get(i);
            if(BaseUtils.hashEquals(join,expr.getInnerJoin()))
                return expr;
        }
        return null;
    }

    // множественное наследование
    public static <K> StatKeys<K> getStatKeys(InnerJoin<K, ?> join, KeyStat keyStat, StatType type) {
        return join.getInnerStatKeys(type);
    }

    @Override
    public StatKeys<K> getStatKeys(KeyStat keyStat, StatType type) {
        return getStatKeys(this, keyStat, type);
    }


    @Override
    public StatKeys<K> getInnerStatKeys(StatType type) {
        return getPushedStatKeys(type, StatKeys.NOPUSH());
    }

    // важно делать IdentityLazy для мемоизации
    public abstract StatKeys<K> getPushedStatKeys(StatType type, StatKeys<K> pushStatKeys);

    public static <K> StatKeys<K> adjustNotNullStats(Cost pushCost, Stat pushStat, ImMap<K, Stat> pushKeys, ImMap<K, Stat> pushNotNullKeys) {
        Stat min = pushStat;
        for(Stat stat : pushNotNullKeys.valueIt())
            min = min.min(stat);
        return StatKeys.create(pushCost, pushStat, new DistinctKeys<>(pushKeys)).replaceStat(min);
    }

    public StatKeys<K> getPushedStatKeys(StatType type, Cost pushCost, Stat pushStat, ImMap<K, Stat> pushKeys, ImMap<K, Stat> pushNotNullKeys, Result<ImSet<K>> rPushedKeys) {

        ImSet<K> pushedKeys = getPushKeys(pushKeys.keys());

        StatKeys<K> pushedStatKeys = getPushedStatKeys(type, pushedKeys, pushCost, pushStat, pushKeys, pushNotNullKeys);

        if(rPushedKeys != null)
            return getReducePushedStatKeys(type, pushedKeys, rPushedKeys, pushedStatKeys, pushCost, pushStat, pushKeys, pushNotNullKeys);

        return pushedStatKeys;
    }

    private StatKeys<K> getPushedStatKeys(StatType type, ImSet<K> pushedKeys, Cost pushCost, Stat pushStat, ImMap<K, Stat> pushKeys, ImMap<K, Stat> pushNotNullKeys) {
        if(pushedKeys.size() < pushKeys.size()) {
            pushKeys = pushKeys.filterIncl(pushedKeys);
            pushNotNullKeys = pushNotNullKeys.filter(pushedKeys);
        }

        return getPushedStatKeys(type, adjustNotNullStats(pushCost, pushStat, pushKeys, pushNotNullKeys));
    }

    private boolean needPredicatePushDown(StatType type, StatKeys pushedStatKeys) {
        return Stat.AGGR.lessEquals(getInnerStatKeys(type).getRows().div(pushedStatKeys.getRows()));
    }

    private StatKeys<K> getReducePushedStatKeys(StatType type, ImSet<K> pushedKeys, Result<ImSet<K>> rPushedKeys, StatKeys<K> pushedStatKeys, Cost pushCost, Stat pushStat, ImMap<K, Stat> pushKeys, ImMap<K, Stat> pushNotNullKeys) {
        boolean needPredicatePushDown = needPredicatePushDown(type, pushedStatKeys); // need predicatePushDown
        for(K pushedKey : pushedKeys) {
            ImSet<K> reducedKeys = pushedKeys.removeIncl(pushedKey);
            StatKeys<K> reducedStatKeys = getPushedStatKeys(type, reducedKeys, pushCost, pushStat, pushKeys, pushNotNullKeys);

            boolean remove = WhereJoins.pushCompareTo(reducedStatKeys, pushedStatKeys) <= 0;

            if(!remove && needPredicatePushDown && WhereJoins.pushCompareCost(reducedStatKeys, pushedStatKeys) <= 0) {
                // here it's tricky, the problem is that removeJoin removes join completely (not only the branch with the subQuery)
                // so if the query itself has not enough keys, and the subQuery also has not enough keys
                // then there is a risk that the "pushed join" will be removed for the subQuery and the subQuery will get the incorrect operation exception
                // of course it's not the clean solution (because we don't know if the subquery predicate push down need is related to the query push down), but it will work in the most cases
                Result<Boolean> keyNeedPredicatePushDown = new Result<>(false);
                StatKeys<K> fPushedStatKeys = pushedStatKeys;
                pushedKey.enumerate(expr -> {
                    if (expr instanceof QueryExpr) {
                        keyNeedPredicatePushDown.set(((QueryExpr) expr).getInnerJoin().needPredicatePushDown(type, fPushedStatKeys));
                        return false;
                    }
                    return true;
                });
                if(keyNeedPredicatePushDown.result)
                    remove = true;
            }
            if(remove) {
                pushedKeys = reducedKeys;
                pushedStatKeys = reducedStatKeys;
            }
        }

        rPushedKeys.set(pushedKeys);
        return pushedStatKeys;
    }

    public Cost getPushedCost(KeyStat keyStat, StatType type, Cost pushCost, Stat pushStat, ImMap<K, Stat> pushKeys, ImMap<K, Stat> pushNotNullKeys, ImMap<BaseExpr, Stat> pushProps, Result<ImSet<K>> rPushedKeys, Result<ImSet<BaseExpr>> rPushedProps) {
        return getPushedStatKeys(type, pushCost, pushStat, pushKeys, pushNotNullKeys, rPushedKeys).getCost();
    }

    public ImMap<Expr, ? extends Expr> getPushGroup(ImMap<K, ? extends Expr> group, Result<Where> pushExtraWhere) {
        return BaseUtils.immutableCast(group);
    }

    public ImSet<K> getPushKeys(ImSet<K> pushKeys) {
        return pushKeys;
    }

    // нужны чтобы при merge'е у транслятора хватало ключей/значений
    protected final ImSet<KeyExpr> keys;
    protected final ImSet<Value> values;

    public ImSet<ParamExpr> getKeys() {
        return BaseUtils.immutableCast(keys);
    }

    public ImSet<Value> getValues() {
        return values;
    }

    // дублируем аналогичную логику GroupExpr'а
    protected QueryJoin(T join, MapTranslate translator) {
        // надо еще транслировать "внутренние" values
        MapValuesTranslate mapValues = translator.mapValues().filter(join.values);
        MapTranslate valueTranslator = mapValues.mapKeys();
        query = join.query.translateOuter(valueTranslator);
        group = valueTranslator.translateExprKeys(translator.translateDirect(join.group));
        keys = join.keys;
        values = mapValues.translateValues(join.values);
    }

    // для проталкивания
    protected QueryJoin(T join, I query) {
        // надо еще транслировать "внутренние" values
        this.query = query;
        group = join.group;
        keys = join.keys;
        values = join.values;
    }

    public QueryJoin(ImSet<KeyExpr> keys, ImSet<Value> values, I inner, ImMap<K, BaseExpr> group) {
        this.keys = keys;
        this.values = values;

        this.query = inner;
        this.group = group;
    }

    public InnerExprFollows<K> getInnerFollows() {
        return query.follows;
    }

    protected abstract static class QueryInnerHashContext<K extends Expr,I extends QueryJoin.Query<K, I>> extends AbstractInnerHashContext {

        protected final QueryJoin<K, I, ?, ?> thisObj;
        protected QueryInnerHashContext(QueryJoin<K, I, ?, ?> thisObj) {
            this.thisObj = thisObj;
        }

        protected abstract int hashOuterExpr(BaseExpr outerExpr);

        public int hashInner(HashContext hashContext) {
            int hash = 0;
            for(int i=0,size=thisObj.group.size();i<size;i++)
                hash += thisObj.group.getKey(i).hashOuter(hashContext) ^ hashOuterExpr(thisObj.group.getValue(i));
            hash = hash * 31;
            for(KeyExpr key : thisObj.keys)
                hash += hashContext.keys.hash(key);
            hash = hash * 31;
            for(Value value : thisObj.values)
                hash += hashContext.values.hash(value);
            return hash * 31 + thisObj.query.hashOuter(hashContext);
        }

        public ImSet<ParamExpr> getInnerKeys() {
            return BaseUtils.immutableCast(thisObj.keys);
        }
        public ImSet<Value> getInnerValues() {
            return thisObj.values;
        }
        protected boolean isComplex() {
            return thisObj.isComplex();
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

    protected abstract T createThis(ImSet<KeyExpr> keys, ImSet<Value> values, I query, ImMap<K, BaseExpr> group);

    protected T translate(MapTranslate translator) {
        return createThis(translator.translateDirect(keys), translator.translateValues(values), query.translateOuter(translator), translator.translateExprKeys(group));
    }

    public boolean equalsInner(T object) {
        return getClass() == object.getClass() && BaseUtils.hashEquals(query, object.query) && BaseUtils.hashEquals(group,object.group);
    }

    @Override
    public ImSet<StaticValueExpr> getOuterStaticValues() {
        throw new RuntimeException("should not be");
    }
}
