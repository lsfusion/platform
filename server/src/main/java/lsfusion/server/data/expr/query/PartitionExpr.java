package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.SFunctionSet;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MOrderFilterMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.server.Settings;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.ParamLazy;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.expr.*;
import lsfusion.server.data.expr.where.extra.CompareWhere;
import lsfusion.server.data.expr.where.pull.ExclExprPullWheres;
import lsfusion.server.data.expr.where.pull.ExprPullWheres;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.innerjoins.KeyEqual;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.PartialQueryTranslator;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;

public class PartitionExpr extends AggrExpr<KeyExpr, PartitionType, PartitionExpr.Query, PartitionJoin, PartitionExpr, PartitionExpr.QueryInnerContext> {

    public static class Query extends AggrExpr.Query<PartitionType, Query> {
        public ImSet<Expr> partitions;

        public Query(ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull, ImSet<Expr> partitions, PartitionType type) {
            super(exprs, orders, ordersNotNull, type);
            this.partitions = partitions;
        }

        public Query(Query query, MapTranslate translate) {
            super(query, translate);
            this.partitions = translate.translate(query.partitions);
        }

        protected Query translate(MapTranslate translator) {
            return new Query(this, translator);
        }

        private Query(Query query, QueryTranslator translator, ImSet<Expr> restPartitions) {
            super(query, translator);
            this.partitions = translator.translate(restPartitions);
        }

        public Query translateQuery(QueryTranslator translator, ImSet<Expr> restPartitions) {
            return new Query(this, translator, restPartitions);
        }


        @Override
        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && partitions.equals(((Query) o).partitions);
        }

        protected int hash(HashContext hashContext) {
            return super.hash(hashContext) * 31 + hashOuter(partitions, hashContext);
        }

        public Stat getTypeStat(boolean forJoin) {
            return getMainExpr().getTypeStat(getWhere(), forJoin);
        }

        @IdentityLazy
        public Type getType() {
            return getMainExpr().getType(getWhere());
        }

        @Override
        public String toString() {
            return "INNER(" + exprs + "," + orders + "," + partitions + "," + type + ")";
        }

        @Override
        public Query calculatePack() {
            return new Query(Expr.pack(exprs), Expr.pack(orders), ordersNotNull, Expr.pack(partitions), type);
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

        public Type getType() {
            return thisObj.query.getType();
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
        return where; //query.type.canBeNull() ? Where.TRUE : getInner().getFullWhere().map(group);
    }

    public Where calculateNotNullWhere() {
        return query.type.canBeNull() ? new NotNull() : super.calculateNotNullWhere();
    }

    public String getSource(CompileSource compile) {
        return compile.getSource(this);
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return create(query, translator.translate(group));
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
            return create(packedQuery, packedGroup);
        else
            return this;
    }

    protected static Expr createBase(ImMap<KeyExpr, BaseExpr> group, Query query) {
        // проверим если в group есть ключи которые ссылаются на ValueExpr и они есть в partition'е - убираем их из partition'а
        Result<ImMap<KeyExpr, BaseExpr>> restGroup = new Result<>();
        final Query fQuery = query;
        ImMap<KeyExpr, BaseExpr> translate = group.splitKeys(new GetKeyValue<Boolean, KeyExpr, BaseExpr>() {
            public Boolean getMapValue(KeyExpr key, BaseExpr value) {
                return value.isValue() && fQuery.partitions.contains(key);
            }
        }, restGroup);

        ImSet<Expr> restPartitions = query.partitions.remove(translate.keys());

        if(translate.size()>0)
            query = query.translateQuery(new PartialQueryTranslator(translate, true), restPartitions);
        else
            assert BaseUtils.hashEquals(restPartitions, query.partitions);

        return createKeyEqual(restGroup.result, query);
    }

    // нижние оптимизации важны так как некоторые SQL SERVER'а константы где не надо не любят
    private static Expr createKeyEqual(ImMap<KeyExpr, BaseExpr> group, Query query) {
        final KeyEqual keyEqual = query.getWhere().getKeyEquals().getSingle();
        if(!keyEqual.isEmpty()) {
            Result<ImMap<KeyExpr,BaseExpr>> restGroup = new Result<>();
            Where keyWhere = CompareWhere.compare(group.splitKeys(new SFunctionSet<KeyExpr>() {
                public boolean contains(KeyExpr element) {
                    return keyEqual.keyExprs.containsKey(element);
                }
            }, restGroup), keyEqual.keyExprs);

            return createKeyEqual(restGroup.result, query.translateQuery(keyEqual.getTranslator(), query.partitions)).and(keyWhere);
        }
        return createRemoveValues(group, query);
    }

    private static Expr createRemoveValues(ImMap<KeyExpr, BaseExpr> group, Query query) {
        ImMap<BaseExpr, BaseExpr> exprValues = query.getWhere().getExprValues();// keys'ов уже очевидно нет

        MOrderFilterMap<Expr, Boolean> mRemovedOrders = MapFact.mOrderFilter(query.orders);
        Where removeWhere = Where.TRUE;
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
            return BaseExpr.create(new PartitionExpr(new Query(query.exprs, removedOrders, query.ordersNotNull, query.partitions, query.type), group)).and(removeWhere);

        assert removeWhere.isTrue();
        return BaseExpr.create(new PartitionExpr(query, group));
    }

    public static Expr create(final PartitionType partitionType, final ImList<Expr> exprs, final ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull, final ImSet<? extends Expr> partitions, final ImMap<KeyExpr, ? extends Expr> group, final PullExpr noPull) {
        ImMap<KeyExpr, KeyExpr> pullKeys = BaseUtils.<ImSet<KeyExpr>>immutableCast(getOuterColKeys(exprs.getCol()).merge(getOuterSetKeys(orders.keys())).merge(getOuterSetKeys(partitions))).filterFn(new SFunctionSet<KeyExpr>() {
            public boolean contains(KeyExpr key) {
                return key instanceof PullExpr && !group.containsKey(key) && !key.equals(noPull);
            }}).toMap();
        return create(partitionType, exprs, orders, ordersNotNull, partitions, MapFact.addExcl(group, pullKeys));
    }

    public static Expr create(final PartitionType partitionType, final ImList<Expr> exprs, final ImOrderMap<Expr, Boolean> orders, final boolean ordersNotNull, final ImSet<? extends Expr> partitions, ImMap<KeyExpr, ? extends Expr> group) {
        return create(new Query(exprs, orders, ordersNotNull, (ImSet<Expr>) partitions, partitionType), group);
    }

    public static Expr create(final Query query, ImMap<KeyExpr, ? extends Expr> group) {
        return new ExprPullWheres<KeyExpr>() {
            protected Expr proceedBase(ImMap<KeyExpr, BaseExpr> map) {
                return createBase(map, query);
            }
        }.proceed(group);
    }

    @Override
    public AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and) {
        if (!hasNotNull()) {
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
        return new PartitionJoin(getInner().getQueryKeys(), getInner().getInnerValues(), getInner().getInnerFollows(), query.getWhere(), Settings.get().isPushOrderWhere() ?query.partitions:SetFact.<Expr>EMPTY(),group);
    }
}
