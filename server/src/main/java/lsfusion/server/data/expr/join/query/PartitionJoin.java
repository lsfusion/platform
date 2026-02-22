package lsfusion.server.data.expr.join.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.join.classes.InnerExprFollows;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.stat.StatKeys;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.translate.KeyExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.Where;

public class PartitionJoin extends QueryJoin<KeyExpr, PartitionJoin.Query, PartitionJoin, PartitionJoin.QueryOuterContext> {

    public PartitionJoin(ImSet<KeyExpr> keys, ImSet<Value> values, InnerExprFollows<KeyExpr> innerFollows, Where inner, ImSet<Expr> partitions, ImMap<KeyExpr, BaseExpr> group, ImOrderMap<Expr, Boolean> limitOrders) {
        super(keys, values, new Query(innerFollows, inner, partitions, limitOrders), group);
    }
    
    @IdentityLazy
    public Where getOrWhere() {
        return query.where.mapWhere(group);
    }

    public static StatKeys<KeyExpr> getStatKeys(Where where, ImSet<KeyExpr> keys, StatType type, StatKeys<KeyExpr> pushStatKeys) {
        return where.getPushedStatKeys(keys, type, pushStatKeys);
    }

    private PartitionJoin(ImSet<KeyExpr> keys, ImSet<Value> values, Query inner, ImMap<KeyExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    protected PartitionJoin createThis(ImSet<KeyExpr> keys, ImSet<Value> values, Query query, ImMap<KeyExpr, BaseExpr> group) {
        return new PartitionJoin(keys, values, query, group);
    }

    public static class QueryOuterContext extends QueryJoin.QueryOuterContext<KeyExpr, PartitionJoin.Query, PartitionJoin, PartitionJoin.QueryOuterContext> {
        public QueryOuterContext(PartitionJoin thisObj) {
            super(thisObj);
        }

        public PartitionJoin translateThis(MapTranslate translator) {
            return new PartitionJoin(thisObj, translator);
        }
    }
    protected QueryOuterContext createOuterContext() {
        return new QueryOuterContext(this);
    }

    private PartitionJoin(PartitionJoin partitionJoin, MapTranslate translator) {
        super(partitionJoin, translator);
    }

    @IdentityLazy
    private GroupExprWhereJoins<Expr> getGroupWhereJoins(StatType type, ImSet<Expr> usedPartitions) {
        return query.where.getGroupExprWhereJoins(usedPartitions, type, false);
    }

    @IdentityLazy
    public StatKeys<KeyExpr> getPushedStatKeys(StatType type, StatKeys<KeyExpr> pushStatKeys) {

        if(pushStatKeys != StatKeys.<KeyExpr>NOPUSH()) {
            ImSet<KeyExpr> usedKeys = pushStatKeys.getKeys();
            Result<ImSet<Expr>> usedPartitions = new Result<>();
            Result<Boolean> useWhere = new Result<>();
            filterFullKeys(usedKeys, usedPartitions, useWhere, true);

            GroupExprWhereJoins<Expr> groupWhereJoins = getGroupWhereJoins(type, usedPartitions.result); // так как в partitions expr'ы, а нужны baseExpr'ы
            return groupWhereJoins.getPartitionStatKeys(query.where, type, pushStatKeys, useWhere.result, keys);
        } else
            return getStatKeys(query.where, keys, type, pushStatKeys); // maybe makes sense to make where.getOuterKeys in the getStatKey

        // через self join не правильно, так как на самом деле через группировку проталкивается (смотри )
//        Pair<Where, ImRevMap<KeyExpr, KeyExpr>> selfJoin = getSelfJoin(pushStatKeys.getKeys());
//
//        StatKeys<KeyExpr> mappedStatKeys = pushStatKeys.mapBack(selfJoin.second.reverse());// mapp'им
//        return selfJoin.first.getStatKeys(this.keys, type, mappedStatKeys); // не full так как статистика по key' не интересует
    }

    private ImSet<KeyExpr> filterFullKeys(ImSet<KeyExpr> keys, Result<ImSet<Expr>> usedPartitions, Result<Boolean> useWhere, boolean assertFull) {
        MSet<KeyExpr> mAllPartitionKeys = SetFact.mSet();
        MExclSet<Expr> mUsedPartitions = usedPartitions == null ? null : SetFact.mExclSet();
        ImSet<Expr> partitions = getPartitions();
        for(Expr partition : partitions) {
            ImSet<KeyExpr> partitionKeys = BaseUtils.immutableCast(partition.getOuterKeys());
            if(keys.containsAll(partitionKeys)) {
                mAllPartitionKeys.addAll(partitionKeys);
                if(mUsedPartitions != null)
                    mUsedPartitions.exclAdd(partition);
            } else {
                if(keys.intersect(partitionKeys) && usePartitionPushWhere) { // докидываем where и возвращаем все partitions
                    if(usedPartitions != null)
                        usedPartitions.set(partitions);
                    if(useWhere != null)
                        useWhere.set(true);
                    return keys;
                } // иначе просто выкидываем
            }
        }

        if(usedPartitions != null)
            usedPartitions.set(mUsedPartitions.immutable());
        if(useWhere != null)
            useWhere.set(false);
        ImSet<KeyExpr> result = mAllPartitionKeys.immutable();
        assert !assertFull || BaseUtils.hashEquals(result, keys); // выкинули уже в getPushKeys
        return result; // не берем ключи, которые не входят ни в один partition (хотя это и не критично)
    }

//    @IdentityLazy // результат map старые на новые
//    private Pair<Where, ImRevMap<KeyExpr, KeyExpr>> getSelfJoin(ImSet<KeyExpr> usedKeys) {
//        //        identityLazy делаем
//        Result<ImSet<Expr>> rUsedPartitions = new Result<>();
//        ImSet<KeyExpr> patchedKeys = filterFullKeys(usedKeys, rUsedPartitions);
//        assert BaseUtils.hashEquals(patchedKeys, usedKeys);
//        ImSet<Expr> usedPartitions = rUsedPartitions.result;
//
//        ImRevMap<KeyExpr, KeyExpr> mapKeys = KeyExpr.getMapKeys(usedKeys);
//
//        // добавляем pe=pe'
//        MapTranslator partitionTranslator = new MapTranslator(BaseUtils.<ImRevMap<ParamExpr, ParamExpr>>immutableCast(mapKeys), MapValuesTranslator.noTranslate(AbstractOuterContext.getOuterColValues(usedPartitions)));
//        ImMap<Expr, Expr> mapPartitions = usedPartitions.toMap();
//        Where partitionWhere = CompareWhere.compare(mapPartitions, partitionTranslator.translateMap(mapPartitions));
//        return new Pair<>(query.where.and(partitionWhere), mapKeys);
//    }

    // доки
    // оставляет только полные partition'ы

    public final boolean usePartitionPushWhere = false;

//    если partition не пересекается выкидываем
//    если полностью принадлежит - берем
//    если смешано или добавляем where и все partition'ы или тоже выкидываем

    @Override
    public ImMap<Expr, ? extends Expr> getPushGroup(ImMap<KeyExpr, ? extends Expr> group, Result<Where> pushExtraWhere) {
        ImSet<Expr> usedPartitions;
        Result<ImSet<Expr>> rUsedPartitions = new Result<>(); Result<Boolean> rUseWhere = new Result<>();
        filterFullKeys(group.keys(), rUsedPartitions, rUseWhere, true);
        usedPartitions = rUsedPartitions.result;
        Where extraWhere = Where.TRUE();
        if(rUseWhere.result)
            extraWhere = getWhere();

        KeyExprTranslator translator = new KeyExprTranslator(group);
        if(pushExtraWhere != null)
            pushExtraWhere.set(extraWhere.translateExpr(translator));
        return translator.translate(usedPartitions.toMap());
    }

    public ImOrderMap<Expr, Boolean> getOrders() {
        return query.orders;
    }

    @Override
    public ImSet<KeyExpr> getPushKeys(ImSet<KeyExpr> pushKeys) {
//            checkPush.set(true); // по идее не надо
        ImSet<KeyExpr> fullKeys = filterFullKeys(pushKeys, null, null, false);
        assert pushKeys.containsAll(fullKeys);
        return fullKeys;
    }

    public Where getWhere() {
        return query.where;
    }

    public ImSet<Expr> getPartitions() {
        return query.partitions;
    }

    public static class Query extends QueryJoin.Query<KeyExpr, Query> {
        private final Where where;
        private final ImOrderMap<Expr, Boolean> orders;
        private final ImSet<Expr> partitions;

        public Query(InnerExprFollows<KeyExpr> follows, Where where, ImSet<Expr> partitions, ImOrderMap<Expr, Boolean> limitOrders) {
            super(follows);
            this.where = where;
            this.partitions = partitions;
            this.orders = limitOrders;
        }

        public Query(Query query, MapTranslate translate) {
            super(query, translate);
            where = query.where.translateOuter(translate);
            partitions = translate.translate(query.partitions);
            orders = translate.translate(query.orders);
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && partitions.equals(((Query) o).partitions) && where.equals(((Query) o).where) && orders.equals(((Query) o).orders);
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashContext hashContext) {
            return 31 * ((31 * super.hash(hashContext) + hashOuter(partitions, hashContext)) * 31 + where.hashOuter(hashContext)) + hashOuter(orders, hashContext);
        }

        protected Query translate(MapTranslate translator) {
            return new Query(this, translator);
        }

        public ImSet<OuterContext> calculateOuterDepends() {
            return super.calculateOuterDepends().merge(SetFact.<OuterContext>merge(partitions, where)).merge(orders.keys());
        }
    }
}
