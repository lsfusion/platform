package lsfusion.server.data.expr.join.query;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.join.classes.InnerExprFollows;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatKeys;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.Where;

public class GroupJoin extends QueryJoin<Expr, GroupJoin.Query, GroupJoin, GroupJoin.QueryOuterContext> {

    public GroupJoin(ImSet<KeyExpr> keys, ImSet<Value> values, ImMap<KeyExpr, Type> keyTypes, InnerExprFollows<Expr> innerFollows, Where where, GroupExprWhereJoins<Expr> groupJoins, ImMap<Expr, BaseExpr> group, ImOrderMap<Expr, Boolean> limitOrders) {
        super(keys,values,new Query(innerFollows, where, keyTypes, groupJoins, limitOrders),group);
    }

    public static class QueryOuterContext extends QueryJoin.QueryOuterContext<Expr, GroupJoin.Query, GroupJoin, GroupJoin.QueryOuterContext> {
        public QueryOuterContext(GroupJoin thisObj) {
            super(thisObj);
        }

        public GroupJoin translateThis(MapTranslate translator) {
            return new GroupJoin(thisObj, translator);
        }
    }
    protected QueryOuterContext createOuterContext() {
        return new QueryOuterContext(this);
    }

    // дублируем аналогичную логику GroupExpr'а
    private GroupJoin(GroupJoin join, MapTranslate translator) {
        super(join, translator);
    }

    public static class Query extends QueryJoin.Query<Expr, Query> {
        private final Where where;
        private final ImOrderMap<Expr, Boolean> orders;
        private final ImMap<KeyExpr, Type> keyTypes;
        private final GroupExprWhereJoins<Expr> joins;// чтобы не сливало группировки со всем разными join'ами

        public Query(InnerExprFollows<Expr> follows, Where where, ImMap<KeyExpr, Type> keyTypes, GroupExprWhereJoins<Expr> joins, ImOrderMap<Expr, Boolean> limitOrders) {
            super(follows);
            this.where = where;
            this.orders = limitOrders;
            this.keyTypes = keyTypes;
            this.joins = joins;
            assert !keyTypes.containsNull();
        }

        public Query(Query query, MapTranslate translator) {
            super(query, translator);
            where = query.where.translateOuter(translator);
            orders = translator.translate(query.orders);
            keyTypes = translator.translateExprKeys(query.keyTypes);
            joins = query.joins.translateOuter(translator);
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && where.equals(((Query) o).where) && keyTypes.equals(((Query) o).keyTypes) && joins.equals(((Query) o).joins) && orders.equals(((Query) o).orders);
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashContext hashContext) {
            return 31 * ((31 * (31 * super.hash(hashContext) + hashKeysOuter(keyTypes, hashContext)) + where.hashOuter(hashContext))* 31 + joins.hashOuter(hashContext)) + hashOuter(orders, hashContext);
        }

        protected Query translate(MapTranslate translator) {
            return new Query(this, translator);
        }

        public ImSet<OuterContext> calculateOuterDepends() {
            return super.calculateOuterDepends().merge(keyTypes.keys()).merge(where).merge(joins).merge(orders.keys());
        }
    }

    private GroupJoin(ImSet<KeyExpr> keys, ImSet<Value> values, Query inner, ImMap<Expr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    protected GroupJoin createThis(ImSet<KeyExpr> keys, ImSet<Value> values, Query query, ImMap<Expr, BaseExpr> group) {
        return new GroupJoin(keys, values, query, group);
    }

    @IdentityLazy
    public StatKeys<Expr> getPushedStatKeys(StatType type, StatKeys<Expr> pushStatKeys) {
        return query.joins.getStatKeys((key, forJoin) -> {
            Type keyType = query.keyTypes.get((KeyExpr) key);
            if(keyType == null) // при висячих ключах бывает
                return Stat.ALOT;
            return keyType.getTypeStat(forJoin);
        }, type, pushStatKeys, group.keys());
    }
}
