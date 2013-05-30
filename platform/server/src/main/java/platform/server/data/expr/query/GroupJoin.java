package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.translator.MapTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

public class GroupJoin extends QueryJoin<Expr, GroupJoin.Query, GroupJoin, GroupJoin.QueryOuterContext> {

    public static class Query extends AbstractOuterContext<Query> {
        private final Where where;
        private final StatKeys<Expr> stats;
        private final ImMap<KeyExpr, Type> keyTypes; // чтобы не сливало группировки с разными типами

        public Query(Where where, StatKeys<Expr> stats, ImMap<KeyExpr, Type> keyTypes) {
            this.where = where;
            this.stats = stats;
            this.keyTypes = keyTypes;
        }

        public boolean twins(TwinImmutableObject o) {
            return stats.equals(((Query) o).stats) && where.equals(((Query) o).where) && keyTypes.equals(((Query) o).keyTypes);
        }

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashContext hashContext) {
            return (31 * hashKeysOuter(keyTypes, hashContext) + where.hashOuter(hashContext))* 31 + StatKeys.hashOuter(stats, hashContext);
        }

        protected Query translate(MapTranslate translator) {
            return new Query(where.translateOuter(translator), StatKeys.translateOuter(stats, translator), translator.translateExprKeys(keyTypes));
        }

        public ImSet<OuterContext> calculateOuterDepends() {
            return BaseUtils.<ImSet<OuterContext>>immutableCast(stats.getKeys()).merge(keyTypes.keys()).merge(where);
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

    public GroupJoin(ImMap<KeyExpr, Type> keyTypes, ImSet<Value> values, Where where, StatKeys<Expr> joins, ImMap<Expr, BaseExpr> group) {
        super(keyTypes.keys(),values,new Query(where, joins, keyTypes),group);
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
