package platform.server.data.expr.query;

import platform.base.*;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import platform.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import platform.server.Settings;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.DataClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.*;
import platform.server.data.expr.where.pull.ExclExprPullWheres;
import platform.server.data.expr.where.pull.ExprPullWheres;
import platform.server.data.query.CompileSource;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.PartialQueryTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

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

        @Override
        public boolean twins(TwinImmutableObject o) {
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

    private PartitionExpr(PartitionType partitionType, ImMap<KeyExpr, BaseExpr> group, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull, ImSet<Expr> partitions) {
        this(new Query(exprs, orders, ordersNotNull, partitions, partitionType), group);
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
        ImMap<KeyExpr, Expr> packedGroup = packPushFollowFalse(group, falseWhere);
        Query packedQuery = query.pack();
        if(!(BaseUtils.hashEquals(packedQuery, query) && BaseUtils.hashEquals(packedGroup,group)))
            return create(query.type, packedQuery.exprs, packedQuery.orders, packedQuery.ordersNotNull, packedQuery.partitions, packedGroup);
        else
            return this;
    }

    protected static Expr createBase(PartitionType partitionType, ImMap<KeyExpr, BaseExpr> group, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull, final ImSet<Expr> partitions) {
        // проверим если в group есть ключи которые ссылаются на ValueExpr и они есть в partition'е - убираем их из partition'а
        Result<ImMap<KeyExpr, BaseExpr>> restGroup = new Result<ImMap<KeyExpr, BaseExpr>>();
        ImMap<KeyExpr, BaseExpr> translate = group.splitKeys(new GetKeyValue<Boolean, KeyExpr, BaseExpr>() {
            public Boolean getMapValue(KeyExpr key, BaseExpr value) {
                return value.isValue() && partitions.contains(key);
            }
        }, restGroup);

        ImSet<Expr> restPartitions = partitions.remove(translate.keys());

        if(translate.size()>0) {
            QueryTranslator translator = new PartialQueryTranslator(translate);
            exprs = translator.translate(exprs);
            orders = translator.translate(orders);
            restPartitions = translator.translate(restPartitions);
        }

        return BaseExpr.create(new PartitionExpr(partitionType, restGroup.result, exprs, orders, ordersNotNull, restPartitions));
    }

    public static Expr create(final PartitionType partitionType, final ImList<Expr> exprs, final ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull, final ImSet<? extends Expr> partitions, final ImMap<KeyExpr, ? extends Expr> group, final PullExpr noPull) {
        ImMap<KeyExpr, KeyExpr> pullKeys = getOuterKeys(exprs.getCol()).merge(getOuterKeys(orders.keys())).merge(getOuterKeys(partitions)).filterFn(new SFunctionSet<KeyExpr>() {
            public boolean contains(KeyExpr key) {
                return key instanceof PullExpr && !group.containsKey(key) && !key.equals(noPull);
            }}).toMap();
        return create(partitionType, exprs, orders, ordersNotNull, partitions, MapFact.addExcl(group, pullKeys));
    }

    public static Expr create(final PartitionType partitionType, final ImList<Expr> exprs, final ImOrderMap<Expr, Boolean> orders, final boolean ordersNotNull, final ImSet<? extends Expr> partitions, ImMap<KeyExpr, ? extends Expr> group) {
        return new ExprPullWheres<KeyExpr>() {
            protected Expr proceedBase(ImMap<KeyExpr, BaseExpr> map) {
                return createBase(partitionType, map, exprs, orders, ordersNotNull, (ImSet<Expr>) partitions);
            }
        }.proceed(group);
    }

    @Override
    public AndClassSet getAndClassSet(ImMap<VariableClassExpr, AndClassSet> and) {
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
        return new PartitionJoin(getInner().getInnerKeys(), getInner().getInnerValues(),query.getWhere(), Settings.get().isPushOrderWhere() ?query.partitions:SetFact.<Expr>EMPTY(),group);
    }
}
