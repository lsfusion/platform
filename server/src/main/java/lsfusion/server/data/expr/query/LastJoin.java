package lsfusion.server.data.expr.query;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.IdentityQuickLazy;
import lsfusion.server.base.caches.hash.HashContext;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.InnerExprFollows;
import lsfusion.server.data.query.stat.Cost;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.translator.MapTranslate;

public class LastJoin extends QueryJoin<KeyExpr, LastJoin.Query, LastJoin, LastJoin.QueryOuterContext> {

    public static class Query extends QueryJoin.Query<KeyExpr, Query> {
        private final Cost costPerStat;
        private final Cost costMax; // оптимизация - выше этого cost'а идти смысла нет

        public Query(InnerExprFollows<KeyExpr> follows, Cost costPerStat, Cost costMax) {
            super(follows);
            this.costPerStat = costPerStat;
            this.costMax = costMax;
        }

        public Query(Query query, MapTranslate translate) {
            super(query, translate);
            this.costPerStat = query.costPerStat;
            this.costMax = query.costMax;
        }

        protected Query translate(MapTranslate translator) {
            return new Query(this, translator);
        }
        
        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && costPerStat.equals(((Query) o).costPerStat) && costMax.equals(((Query) o).costMax);
        }

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashContext hashContext) {
            return 31 * (31 * super.hash(hashContext) + costPerStat.hashCode()) + costMax.hashCode();
        }

    }

    public LastJoin(LastJoin join, MapTranslate translator) {
        super(join, translator);
    }

    public static class QueryOuterContext extends QueryJoin.QueryOuterContext<KeyExpr, LastJoin.Query, LastJoin, LastJoin.QueryOuterContext> {
        public QueryOuterContext(LastJoin thisObj) {
            super(thisObj);
        }

        public LastJoin translateThis(MapTranslate translator) {
            return new LastJoin(thisObj, translator);
        }
    }

    @Override
    protected QueryOuterContext createOuterContext() {
        return new QueryOuterContext(this);
    }

    public LastJoin(Cost costPerStat, Cost costMax, ImMap<KeyExpr, BaseExpr> group) {
        this(SetFact.<KeyExpr>EMPTY(), SetFact.<Value>EMPTY(), new Query(InnerExprFollows.<KeyExpr>EMPTYEXPR(), costPerStat, costMax), group);
    }

    public LastJoin(ImSet<KeyExpr> keys, ImSet<Value> values, Query inner, ImMap<KeyExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    @IdentityQuickLazy
    public StatKeys<KeyExpr> getMaxStatKeys() {
        Stat rows = query.costMax.rows;
        return StatKeys.create(query.costMax, rows, new DistinctKeys<>(group.keys().toMap(rows)));
    }

    // если cost <, при этом for <= - оставляем, так как когда не надо делать GROUP LAST, и материализовать общую таблицу почему-то быстрее работает
    public static Cost calcCost(Cost cost, Stat stat, Cost costPerStat, Cost costMax) {
        Cost forCost = costPerStat.mult(stat);
        if(cost.less(costMax) && forCost.equals(costMax)) // ( < costMax, но rows * CPS = costMax)
            return forCost.div(Stat.ONESTAT);
        return forCost.or(cost);
    }
    
    @IdentityLazy
    public StatKeys<KeyExpr> getPushedStatKeys(StatType type, StatKeys<KeyExpr> pushStatKeys) {
        if (pushStatKeys == StatKeys.<KeyExpr>NOPUSH() || pushStatKeys.getKeys().size() != group.size())
            return getMaxStatKeys(); // нужны все ключи, запрещаем выбор по сути

        Cost adjustCost = calcCost(pushStatKeys.getCost(), pushStatKeys.getRows(), query.costPerStat, query.costMax);
        // min costMax, чтобы выйти если cost превысит costMax (дальше нет смысла смотреть)
        return pushStatKeys.replaceCost(adjustCost.min(query.costMax));
    }

    @Override
    protected LastJoin createThis(ImSet<KeyExpr> keys, ImSet<Value> values, Query query, ImMap<KeyExpr, BaseExpr> group) {
        return new LastJoin(keys, values, query, group);
    }
}
