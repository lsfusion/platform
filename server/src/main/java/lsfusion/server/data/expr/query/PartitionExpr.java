package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MOrderFilterMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.ParamLazy;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.PullExpr;
import lsfusion.server.data.expr.WindowExpr;
import lsfusion.server.data.expr.classes.VariableSingleClassExpr;
import lsfusion.server.data.expr.join.query.PartitionJoin;
import lsfusion.server.data.expr.join.where.KeyEqual;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.where.classes.data.CompareWhere;
import lsfusion.server.data.expr.where.pull.ExclExprPullWheres;
import lsfusion.server.data.expr.where.pull.ExprPullWheres;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.KeyExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.PartialKeyExprTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.physics.admin.Settings;

public class PartitionExpr extends AggrExpr<KeyExpr, PartitionType, PartitionExpr.Query, PartitionJoin, PartitionExpr, PartitionExpr.QueryInnerContext> {

    public static class Query extends AggrExpr.Query<PartitionType, Query> {
        public ImSet<Expr> partitions;

        public Query(ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull, ImSet<Expr> partitions, PartitionType type, boolean noInnerFollows) {
            super(exprs, orders, ordersNotNull, type, noInnerFollows);
            this.partitions = partitions;
        }

        public Query(Query query, MapTranslate translate) {
            super(query, translate);
            this.partitions = translate.translate(query.partitions);
        }

        protected Query translate(MapTranslate translator) {
            return new Query(this, translator);
        }

        private Query(Query query, ExprTranslator translator, ImSet<Expr> restPartitions) {
            super(query, translator);
            this.partitions = translator.translate(restPartitions);
        }

        public Query translateExpr(ExprTranslator translator, ImSet<Expr> restPartitions) {
            return new Query(this, translator, restPartitions);
        }

        public Query and(final Where where, ImSet<Expr> newPartitions) { // there is an assertion that first expr is in where, see (PartitionExpr / GroupExpr).Query.and
            return new Query(exprs.mapListValues((i, value) -> {
                if(i==0)
                    value = value.and(where);
                return value;
            }), orders, ordersNotNull, newPartitions, type, noInnerFollows);
        }

        @Override
        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && partitions.equals(((Query) o).partitions);
        }

        public int hash(HashContext hashContext) {
            return super.hash(hashContext) * 31 + hashOuter(partitions, hashContext);
        }

        @IdentityLazy
        public Type getPartitionType() {
            return getType(getWhere());
        }

        @Override
        public String toString() {
            return "INNER(" + exprs + "," + orders + "," + partitions + "," + type + ")";
        }

        @Override
        public Query calculatePack() {
            return new Query(Expr.pack(exprs), Expr.pack(orders), ordersNotNull, Expr.pack(partitions), type, noInnerFollows);
        }

        @Override
        protected Where calculateWhere() {
            return super.calculateWhere().and(Expr.getWhere(partitions));
        }

        @Override
        public ImSet<Expr> getExprs() { // получает все выражения
            return partitions.merge(super.getExprs());
        }
    }

    public static class QueryInnerContext extends AggrExpr.QueryInnerContext<KeyExpr, PartitionType, PartitionExpr.Query, PartitionJoin, PartitionExpr, QueryInnerContext> {
        public QueryInnerContext(PartitionExpr thisObj) {
            super(thisObj);
        }

        protected Where getFullWhere() {
            return thisObj.query.getWhere();
        }
    }
    protected QueryInnerContext createInnerContext() {
        return new QueryInnerContext(this);
    }

    private PartitionExpr(ImMap<KeyExpr, BaseExpr> group, Query query) {
        this(query, group);
    }

    // трансляция
    private PartitionExpr(PartitionExpr partitionExpr, MapTranslate translator) {
        super(partitionExpr, translator);
    }

    protected PartitionExpr translate(MapTranslate translator) {
        return new PartitionExpr(this,translator);
    }

    private PartitionExpr(Query query, ImMap<KeyExpr, BaseExpr> group) {
        super(query, group);
    }

    protected PartitionExpr createThis(Query query, ImMap<KeyExpr, BaseExpr> group) {
        return new PartitionExpr(query, group);
    }

    public class NotNull extends QueryExpr.NotNull {
    }

    public Where calculateOrWhere() {
        Where where = getInnerJoin().getOrWhere();
        assert BaseUtils.hashEquals(where, getInner().getFullWhere().mapWhere(group));
        return where; //query.type.canBeNull() ? Where.TRUE() : getInner().getFullWhere().map(group);
    }

    public Where calculateNotNullWhere() {
        return hasNotNull() ? new NotNull() : super.calculateNotNullWhere();
    }

    public String getSource(CompileSource compile, boolean needValue) {
        return compile.getSource(this, needValue);
    }

    @ParamLazy
    public Expr translate(ExprTranslator translator) {
        return createExpr(query, translator.translate(group));
    }

    @Override
    public String toString() {
        return "ORDER(" + query + "," + group + ")";
    }

    // пока как и в RecursiveExpr классы не пакуются, так как их predicate push down с большой вероятностью спакует, но видимо потом придется доделать
    @Override
    public Expr packFollowFalse(Where falseWhere) {
        ImMap<KeyExpr, Expr> packedGroup = packPushFollowFalse(group, falseWhere);
        Query packedQuery = query.pack();
        if(!(BaseUtils.hashEquals(packedQuery, query) && BaseUtils.hashEquals(packedGroup,group)))
            return createExpr(packedQuery, packedGroup);
        else
            return this;
    }

    protected static Expr createBase(ImMap<KeyExpr, BaseExpr> group, Query query) {
        // проверим если в group есть ключи которые ссылаются на ValueExpr и они есть в partition'е - убираем их из partition'а
        Result<ImMap<KeyExpr, BaseExpr>> restGroup = new Result<>();
        final Query fQuery = query;
        ImMap<KeyExpr, BaseExpr> translate = group.splitKeys((key, value) -> value.isValue() && fQuery.partitions.contains(key) && !WindowExpr.is(key), restGroup);

        ImSet<Expr> restPartitions = query.partitions.remove(translate.keys());

        if(translate.size()>0)
            query = query.translateExpr(new PartialKeyExprTranslator(translate, true), restPartitions);
        else
            assert BaseUtils.hashEquals(restPartitions, query.partitions);

        return createKeyEqual(restGroup.result, query);
    }

    // нижние оптимизации важны так как некоторые SQL SERVER'а константы где не надо не любят
    private static Expr createKeyEqual(ImMap<KeyExpr, BaseExpr> group, Query query) {
        final KeyEqual keyEqual = query.getWhere().getKeyEquals().getSingle();
        if(!keyEqual.isEmpty()) {
            Result<ImMap<KeyExpr,BaseExpr>> restGroup = new Result<>();
            ImMap<KeyExpr, BaseExpr> groupValues = group.splitKeys(element -> {
                BaseExpr baseExpr = keyEqual.keyExprs.get(element);
                return baseExpr != null && baseExpr.isValue(); // тут можно было бы и не isValue брать, translate'ить их и получать такой Where, но это сложнее + как-то сдедать так, чтобы не делать это для ключей созданных в transformPartitionExprsToKeys (можно просто выше проверку сделать)
            }, restGroup);
            if(!groupValues.isEmpty()) {
                ImMap<KeyExpr, BaseExpr> keyExprValues = keyEqual.keyExprs.filterIncl(groupValues.keys());
                Where keyWhere = CompareWhere.compare(groupValues, keyExprValues);
                return createKeyEqual(restGroup.result, query.translateExpr(new PartialKeyExprTranslator(keyExprValues, true), query.partitions)).and(keyWhere);
            }
        }
        return createRemoveValues(group, query);
    }

    private static Expr createRemoveValues(ImMap<KeyExpr, BaseExpr> group, Query query) {
        ImMap<BaseExpr, BaseExpr> exprValues = query.getWhere().getExprValues();// keys'ов уже очевидно нет

        MOrderFilterMap<Expr, Boolean> mRemovedOrders = MapFact.mOrderFilter(query.orders);
        Where removeWhere = Where.TRUE();
        for(int i=0,size=query.orders.size();i<size;i++) {
            Expr exprValue;
            Expr orderExpr = query.orders.getKey(i);
            if(orderExpr.isValue()) // ищем VALUE
                exprValue = orderExpr;
            else
                exprValue = exprValues.getObject(orderExpr); // ищем EXPRVALUE
            if(exprValue!=null) {
                if(query.ordersNotNull)
                    removeWhere = removeWhere.and(exprValue.getWhere());
            } else
                mRemovedOrders.keep(orderExpr, query.orders.getValue(i));
        }
        ImOrderMap<Expr, Boolean> removedOrders = MapFact.imOrderFilter(mRemovedOrders, query.orders);
        if(removedOrders.size() < query.orders.size()) // оптимизация
            return BaseExpr.create(new PartitionExpr(new Query(query.exprs, removedOrders, query.ordersNotNull, query.partitions, query.type, query.noInnerFollows), group)).and(removeWhere);

        assert removeWhere.isTrue();
        return BaseExpr.create(new PartitionExpr(query, group));
    }

    public static Expr create(final PartitionType partitionType, final ImList<Expr> exprs, final ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull, final ImSet<? extends Expr> partitions, final ImMap<KeyExpr, ? extends Expr> group, final PullExpr noPull, boolean noInnerFollows) {
        ImMap<KeyExpr, KeyExpr> pullKeys = BaseUtils.<ImSet<KeyExpr>>immutableCast(getOuterColKeys(exprs.getCol()).merge(getOuterSetKeys(orders.keys())).merge(getOuterSetKeys(partitions))).filterFn(key -> key instanceof PullExpr && !group.containsKey(key) && !key.equals(noPull)).toMap();
        return create(partitionType, exprs, orders, ordersNotNull, partitions, MapFact.addExcl(group, pullKeys), noInnerFollows);
    }

    public static Expr create(final PartitionType partitionType, final ImList<Expr> exprs, final ImOrderMap<Expr, Boolean> orders, final boolean ordersNotNull, final ImSet<? extends Expr> partitions, ImMap<KeyExpr, ? extends Expr> group) {
        return create(partitionType, exprs, orders, ordersNotNull, partitions, group, false);
    }
    public static Expr create(final PartitionType partitionType, final ImList<Expr> exprs, final ImOrderMap<Expr, Boolean> orders, final boolean ordersNotNull, final ImSet<? extends Expr> partitions, ImMap<KeyExpr, ? extends Expr> group, boolean noInnerFollows) {
        return create(new Query(exprs, orders, ordersNotNull, (ImSet<Expr>) partitions, partitionType, noInnerFollows), group);
    }

    public static Expr create(Query query, ImMap<KeyExpr, ? extends Expr> group) {
        if(Settings.get().isTransformPartitionExprsToKeys()) {
            Result<ImSet<Expr>> rKeyPartitions = new Result<>();
            ImSet<Expr> exprs = query.partitions.split(element -> !(element instanceof KeyExpr), rKeyPartitions);
            if (!exprs.isEmpty()) {
                KeyExprTranslator keyExprTranslator = new KeyExprTranslator(group);

                ImRevMap<KeyExpr, Expr> virtualKeysMap = KeyExpr.getMapKeys(exprs).reverse();
                ImSet<KeyExpr> virtualKeys = virtualKeysMap.keys();

                Where andWhere = CompareWhere.compare(virtualKeysMap);
                ImSet<Expr> newPartitions = rKeyPartitions.result.addExcl(virtualKeys);
                query = query.and(andWhere, newPartitions);
                group = keyExprTranslator.translate(virtualKeysMap).addExcl(group);
            }
        }
        
        return createExpr(query, group);
    }

    private static Expr createExpr(final Query query, ImMap<KeyExpr, ? extends Expr> group) {
        return new ExprPullWheres<KeyExpr>() {
            protected Expr proceedBase(ImMap<KeyExpr, BaseExpr> map) {
                return createBase(map, query);
            }
        }.proceed(group);
    }


    // можно было бы вообще hasNotNull завязать на canBeNull (а их на hasNotNull), но так как hasNotNull используется по сути в логике следствий, можно оставить так как есть пока
    // все же включим, так как есть проблема с AfterTranslateAspect, когда initTranslate два раза для такого не "настоящего" NotNull полученного в getWhere
    @Override
    public boolean hasNotNull() {
        return AggrType.canBeNull(query.type, group.keys());
    }

    @Override
    public AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and) {
        if (!hasNotNull()) { // так как calculateNotNullWhere может вернуть случайно DataWhere (одного из своих joins) в getAndClassSet может потеряться класс
            Type type = getInner().getType();
            if(type instanceof DataClass)
                return (AndClassSet) type;
            else {
                ImFilterValueMap<KeyExpr, AndClassSet> mvKeyClasses = group.mapFilterValues();
                for(int i=0,size=group.size();i<size;i++) {
                    AndClassSet classSet = group.getValue(i).getAndClassSet(and);
                    if(classSet!=null)
                        mvKeyClasses.mapValue(i, classSet);
                }
                final ClassExprWhere keyWhere = new ClassExprWhere(mvKeyClasses.immutableValue());

                return new ExclExprPullWheres<AndClassSet>() {
                    protected AndClassSet initEmpty() {
                        return null;
                    }
                    protected AndClassSet proceedBase(Where data, BaseExpr baseExpr) {
                        return data.getClassWhere().and(keyWhere).getAndClassSet(baseExpr);
                    }
                    protected AndClassSet add(AndClassSet op1, AndClassSet op2) {
                        if(op1 == null)
                            return op2;
                        if(op2 == null)
                            return op1;
                        return op1.or(op2);
                    }
                }.proceed(query.getWhere(), query.getMainExpr());
            }
        } else
            return super.getAndClassSet(and);
    }

    @IdentityInstanceLazy
    public PartitionJoin getInnerJoin() {
        boolean hasLimitOffset = WindowExpr.has(group.keys());

        ImOrderMap<Expr, Boolean> limitOrders = hasLimitOffset ? query.orders : MapFact.EMPTYORDER();

        return new PartitionJoin(getInner().getQueryKeys(), getInner().getInnerValues(), getInner().getInnerFollows(), query.getWhere(), query.partitions,group, limitOrders);
    }
}
