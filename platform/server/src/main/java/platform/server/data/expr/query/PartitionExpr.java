package platform.server.data.expr.query;

import platform.base.*;
import platform.server.Settings;
import platform.server.classes.sets.AndClassSet;
import platform.server.classes.DataClass;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.*;
import platform.server.data.expr.where.pull.ExclExprPullWheres;
import platform.server.data.expr.where.pull.ExprPullWheres;
import platform.server.data.query.*;
import platform.server.data.translator.*;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.*;

import static platform.base.BaseUtils.toMap;

public class PartitionExpr extends AggrExpr<KeyExpr, PartitionType, PartitionExpr.Query, PartitionJoin, PartitionExpr, PartitionExpr.QueryInnerContext> {

    public static class Query extends AggrExpr.Query<PartitionType, Query> {
        public Set<Expr> partitions;

        public Query(List<Expr> exprs, OrderedMap<Expr, Boolean> orders, boolean ordersNotNull, Set<Expr> partitions, PartitionType type) {
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

        @Override
        public boolean twins(TwinImmutableInterface o) {
            return super.twins(o) && partitions.equals(((Query) o).partitions);
        }

        protected int hash(HashContext hashContext) {
            return super.hash(hashContext) * 31 + hashOuter(partitions, hashContext);
        }

        public Stat getTypeStat() {
            return getMainExpr().getTypeStat(getWhere());
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
        public Set<Expr> getExprs() { // получает все выражения
            return BaseUtils.mergeSet(super.getExprs(), partitions);
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

    private PartitionExpr(PartitionType partitionType, Map<KeyExpr, BaseExpr> group, List<Expr> exprs, OrderedMap<Expr, Boolean> orders, boolean ordersNotNull, Set<Expr> partitions) {
        this(new Query(exprs, orders, ordersNotNull, partitions, partitionType), group);
    }

    // трансляция
    private PartitionExpr(PartitionExpr partitionExpr, MapTranslate translator) {
        super(partitionExpr, translator);
    }

    protected PartitionExpr translate(MapTranslate translator) {
        return new PartitionExpr(this,translator);
    }

    private PartitionExpr(Query query, Map<KeyExpr, BaseExpr> group) {
        super(query, group);
    }

    protected PartitionExpr createThis(Query query, Map<KeyExpr, BaseExpr> group) {
        return new PartitionExpr(query, group);
    }

    public class NotNull extends QueryExpr.NotNull {
    }

    public Where calculateOrWhere() {
        return getInner().getFullWhere().map(group); //query.type.canBeNull() ? Where.TRUE : getInner().getFullWhere().map(group);
    }

    public Where calculateNotNullWhere() {
        return query.type.canBeNull() ? new NotNull() : super.calculateNotNullWhere();
    }

    public String getSource(CompileSource compile) {
        return compile.getSource(this);
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return create(query.type, query.exprs, query.orders, query.ordersNotNull, query.partitions, translator.translate(group));
    }

    @Override
    public String toString() {
        return "ORDER(" + query + "," + group + ")";
    }

    @Override
    public Expr packFollowFalse(Where falseWhere) {
        Map<KeyExpr, Expr> packedGroup = packPushFollowFalse(group, falseWhere);
        Query packedQuery = query.pack();
        if(!(BaseUtils.hashEquals(packedQuery, query) && BaseUtils.hashEquals(packedGroup,group)))
            return create(query.type, packedQuery.exprs, packedQuery.orders, packedQuery.ordersNotNull, packedQuery.partitions, packedGroup);
        else
            return this;
    }

    protected static Expr createBase(PartitionType partitionType, Map<KeyExpr, BaseExpr> group, List<Expr> exprs, OrderedMap<Expr, Boolean> orders, boolean ordersNotNull, Set<Expr> partitions) {
        // проверим если в group есть ключи которые ссылаются на ValueExpr и они есть в partition'е - убираем их из partition'а
        Map<KeyExpr,BaseExpr> restGroup = new HashMap<KeyExpr, BaseExpr>();
        Set<Expr> restPartitions = new HashSet<Expr>(partitions);
        Map<KeyExpr,BaseExpr> translate = new HashMap<KeyExpr, BaseExpr>();
        for(Map.Entry<KeyExpr,BaseExpr> groupKey : group.entrySet())
            if(groupKey.getValue().isValue() && restPartitions.remove(groupKey.getKey()))
                translate.put(groupKey.getKey(), groupKey.getValue());
            else
                restGroup.put(groupKey.getKey(), groupKey.getValue());
        if(translate.size()>0) {
            QueryTranslator translator = new PartialQueryTranslator(translate);
            exprs = translator.translate(exprs);
            orders = translator.translate(orders);
            restPartitions = translator.translate(restPartitions);
        }

        return BaseExpr.create(new PartitionExpr(partitionType, restGroup, exprs, orders, ordersNotNull, restPartitions));
    }

    public static Expr create(final PartitionType partitionType, final List<Expr> exprs, final OrderedMap<Expr, Boolean> orders, boolean ordersNotNull, final Set<? extends Expr> partitions, Map<KeyExpr, ? extends Expr> group, PullExpr noPull) {
        Map<KeyExpr, Expr> pullGroup = new HashMap<KeyExpr, Expr>(group);
        for(KeyExpr key : getOuterKeys(exprs).merge(getOuterKeys(orders.keySet())).merge(getOuterKeys(partitions)))
            if(key instanceof PullExpr && !group.containsKey(key) && !key.equals(noPull))
                pullGroup.put(key, key);

        return create(partitionType, exprs, orders, ordersNotNull, partitions, pullGroup);
    }

    public static Expr create(final PartitionType partitionType, final List<Expr> exprs, final OrderedMap<Expr, Boolean> orders, final boolean ordersNotNull, final Set<? extends Expr> partitions, Map<KeyExpr, ? extends Expr> group) {
        return new ExprPullWheres<KeyExpr>() {
            protected Expr proceedBase(Map<KeyExpr, BaseExpr> map) {
                return createBase(partitionType, map, exprs, orders, ordersNotNull, (Set<Expr>) partitions);
            }
        }.proceed(group);
    }

    @Override
    public AndClassSet getAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and) {
        if (!hasNotNull()) {
            Type type = getInner().getType();
            if(type instanceof DataClass)
                return (AndClassSet) type;
            else {
                QuickMap<KeyExpr, AndClassSet> keyClasses = new SimpleMap<KeyExpr, AndClassSet>();
                for(Map.Entry<KeyExpr, BaseExpr> groupEntry : group.entrySet()) {
                    AndClassSet classSet = groupEntry.getValue().getAndClassSet(and);
                    if(classSet!=null)
                        keyClasses.add(groupEntry.getKey(), classSet);
                }
                final ClassExprWhere keyWhere = new ClassExprWhere(keyClasses);

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

    @IdentityLazy
    public PartitionJoin getInnerJoin() {
        return new PartitionJoin(getInner().getInnerKeys(), getInner().getInnerValues(),query.getWhere(), Settings.instance.isPushOrderWhere() ?query.partitions:new HashSet<Expr>(),group);
    }
}
