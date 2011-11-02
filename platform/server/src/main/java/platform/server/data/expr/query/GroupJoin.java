package platform.server.data.expr.query;

import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.*;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.translator.HashLazy;
import platform.server.data.translator.HashOuterLazy;
import platform.server.data.translator.MapTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

import java.util.Map;
import java.util.Set;

public class GroupJoin extends QueryJoin<Expr, GroupJoin.Query> {

    public static class Query extends AbstractOuterContext<Query> {
        private final Where where;
        private final StatKeys<Expr> stats;
        private final Map<KeyExpr, Type> keyTypes; // чтобы не сливало группировки с разными типами

        public Query(Where where, StatKeys<Expr> stats, Map<KeyExpr, Type> keyTypes) {
            this.where = where;
            this.stats = stats;
            this.keyTypes = keyTypes;
        }

        public boolean twins(TwinImmutableInterface o) {
            return stats.equals(((Query) o).stats) && where.equals(((Query) o).where) && keyTypes.equals(((Query) o).keyTypes);
        }

        @HashOuterLazy
        public int hashOuter(HashContext hashContext) {
            return (31 * hashKeysOuter(keyTypes, hashContext) + where.hashOuter(hashContext))* 31 + StatKeys.hashOuter(stats, hashContext);
        }

        public Query translateOuter(MapTranslate translator) {
            return new Query(where.translateOuter(translator), StatKeys.translateOuter(stats, translator), translator.translateMapKeys(keyTypes));
        }

        public SourceJoin[] getEnum() {
            return where.getEnum();
        }
    }

    // дублируем аналогичную логику GroupExpr'а
    private GroupJoin(GroupJoin join, MapTranslate translator) {
        super(join, translator);
    }

    public InnerJoin translateOuter(MapTranslate translator) {
        return new GroupJoin(this, translator);
    }

    public GroupJoin(Map<KeyExpr, Type> keyTypes, Set<Value> values, Where where, StatKeys<Expr> joins, Map<Expr, BaseExpr> group) {
        super(keyTypes.keySet(),values,new Query(where, joins, keyTypes),group);
    }

    public GroupJoin(Set<KeyExpr> keys, Set<Value> values, Query inner, Map<Expr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    protected GroupJoin createThis(Set<KeyExpr> keys, Set<Value> values, Query query, Map<Expr, BaseExpr> group) {
        return new GroupJoin(keys, values, query, group);
    }

    @Override
    public StatKeys<Expr> getStatKeys(KeyStat keyStat) {
        return query.stats;
    }
}
