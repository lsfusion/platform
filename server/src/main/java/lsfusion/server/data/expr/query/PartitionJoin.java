package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.InnerExprFollows;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.translator.*;
import lsfusion.server.data.where.Where;

public class PartitionJoin extends QueryJoin<KeyExpr, PartitionJoin.Query, PartitionJoin, PartitionJoin.QueryOuterContext> {

    public static class Query extends QueryJoin.Query<KeyExpr, Query> {
        private final Where where;
        private final ImSet<Expr> partitions;

        public Query(InnerExprFollows<KeyExpr> follows, Where where, ImSet<Expr> partitions) {
            super(follows);
            this.where = where;
            this.partitions = partitions;
        }

        public Query(Query query, MapTranslate translate) {
            super(query, translate);
            where = query.where.translateOuter(translate);
            partitions = translate.translate(query.partitions);
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && partitions.equals(((Query) o).partitions) && where.equals(((Query) o).where);
        }

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashContext hashContext) {
            return (31 * super.hash(hashContext) + hashOuter(partitions, hashContext)) * 31 + where.hashOuter(hashContext);
        }

        protected Query translate(MapTranslate translator) {
            return new Query(this, translator);
        }

        public ImSet<OuterContext> calculateOuterDepends() {
            return super.calculateOuterDepends().merge(SetFact.<OuterContext>merge(partitions, where));
        }
    }
    
    @IdentityLazy
    public Where getOrWhere() {
        return query.where.mapWhere(group);
    }

    public PartitionJoin(ImSet<KeyExpr> keys, ImSet<Value> values, InnerExprFollows<KeyExpr> innerFollows, Where inner, ImSet<Expr> partitions, ImMap<KeyExpr, BaseExpr> group) {
        super(keys, values, new Query(innerFollows, inner, partitions), group);
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
        return query.where.getGroupWhereJoins(usedPartitions, type);
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
            return query.where.getPushedStatKeys(keys, type, pushStatKeys);

        // через self join не правильно, так как на самом деле через группировку проталкивается (смотри )
//        Pair<Where, ImRevMap<KeyExpr, KeyExpr>> selfJoin = getSelfJoin(pushStatKeys.getKeys());
//
//        StatKeys<KeyExpr> mappedStatKeys = pushStatKeys.mapBack(selfJoin.second.reverse());// mapp'им
//        return selfJoin.first.getStatKeys(this.keys, type, mappedStatKeys); // не full так как статистика по key' не интересует
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

    private ImSet<KeyExpr> filterFullKeys(ImSet<KeyExpr> keys, Result<ImSet<Expr>> usedPartitions, Result<Boolean> useWhere, boolean assertFull) {
        MSet<KeyExpr> mAllPartitionKeys = SetFact.mSet();
        MExclSet<Expr> mUsedPartitions = usedPartitions == null ? null : SetFact.<Expr>mExclSet();
        for(Expr partition : getPartitions()) {
            ImSet<KeyExpr> partitionKeys = BaseUtils.<ImSet<KeyExpr>>immutableCast(partition.getOuterKeys());
            if(keys.containsAll(partitionKeys)) { // пересекается
                mAllPartitionKeys.addAll(partitionKeys);
                if(mUsedPartitions != null)
                    mUsedPartitions.exclAdd(partition);
            } else {
                if(keys.intersect(partitionKeys) && usePartitionPushWhere) { // докидываем where и возвращаем все partitions
                    if(usedPartitions != null)
                        usedPartitions.set(getPartitions());
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

    @Override
    public ImMap<Expr, ? extends Expr> getPushGroup(ImMap<KeyExpr, ? extends Expr> group, boolean newPush, Result<Where> pushExtraWhere) {
        ImSet<Expr> usedPartitions;
        Where extraWhere = Where.TRUE;
        if (newPush) {
            Result<ImSet<Expr>> rUsedPartitions = new Result<>(); Result<Boolean> rUseWhere = new Result<>();
            filterFullKeys(group.keys(), rUsedPartitions, rUseWhere, true);
            usedPartitions = rUsedPartitions.result;
            if(rUseWhere.result)
                extraWhere = getWhere();
        } else {
            usedPartitions = getPartitions();
            group = getJoins().filterIncl(BaseUtils.<ImSet<KeyExpr>>immutableCast(AbstractOuterContext.getOuterSetKeys(usedPartitions))); // берем все ключи, хотя формально можно было бы брать только смежные от переданных, но эта ветка все равно уйдет
        }

        KeyExprTranslator translator = new KeyExprTranslator(group);
        if(pushExtraWhere != null)
            pushExtraWhere.set(extraWhere.translateExpr(translator));
        return translator.translate(usedPartitions.toMap());
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
}
