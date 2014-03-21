package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.query.stat.WhereJoins;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;

public class GroupJoin extends QueryJoin<Expr, GroupJoin.Query, GroupJoin, GroupJoin.QueryOuterContext> {

    public static class Query extends AbstractOuterContext<Query> {
        private final Where where;
        private final StatKeys<Expr> stats;
        private final ImMap<KeyExpr, Type> keyTypes; // чтобы не сливало группировки с разными типами
        private final WhereJoins joins;// чтобы не сливало группировки со всем разными join'ами

        public Query(Where where, StatKeys<Expr> stats, ImMap<KeyExpr, Type> keyTypes, WhereJoins joins) {
            this.where = where;
            this.stats = stats;
            this.keyTypes = keyTypes;
            this.joins = joins; 
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return stats.equals(((Query) o).stats) && where.equals(((Query) o).where) && keyTypes.equals(((Query) o).keyTypes) && joins.equals(((Query) o).joins);
        }

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashContext hashContext) {
            return 31 * ((31 * hashKeysOuter(keyTypes, hashContext) + where.hashOuter(hashContext))* 31 + StatKeys.hashOuter(stats, hashContext)) + joins.hashOuter(hashContext);
        }

        protected Query translate(MapTranslate translator) {
            return new Query(where.translateOuter(translator), StatKeys.translateOuter(stats, translator), translator.translateExprKeys(keyTypes), joins.translateOuter(translator));
        }

        public ImSet<OuterContext> calculateOuterDepends() {
            return BaseUtils.<ImSet<OuterContext>>immutableCast(stats.getKeys()).merge(keyTypes.keys()).merge(where).merge(joins);
        }
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

    public GroupJoin(ImSet<KeyExpr> keys, ImSet<Value> values, ImMap<KeyExpr, Type> extKeyTypes, Where where, WhereJoins groupJoins, StatKeys<Expr> joins, ImMap<Expr, BaseExpr> group) {
        super(keys,values,new Query(where, joins, extKeyTypes, groupJoins),group);
    }

    private GroupJoin(ImSet<KeyExpr> keys, ImSet<Value> values, Query inner, ImMap<Expr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    protected GroupJoin createThis(ImSet<KeyExpr> keys, ImSet<Value> values, Query query, ImMap<Expr, BaseExpr> group) {
        return new GroupJoin(keys, values, query, group);
    }

    @Override
    public StatKeys<Expr> getStatKeys(KeyStat keyStat) {
        return query.stats;
    }
}
