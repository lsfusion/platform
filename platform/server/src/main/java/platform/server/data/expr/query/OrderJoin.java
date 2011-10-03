package platform.server.data.expr.query;

import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.translator.HashLazy;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

import java.util.Map;
import java.util.Set;

public class OrderJoin extends QueryJoin<KeyExpr, OrderJoin.Query> {

    public static class Query extends AbstractOuterContext<Query> {
        private final Where where;
        private final Set<Expr> partitions;

        public Query(Where where, Set<Expr> partitions) {
            this.where = where;
            this.partitions = partitions;
        }

        public boolean twins(TwinImmutableInterface o) {
            return partitions.equals(((Query) o).partitions) && where.equals(((Query) o).where);
        }

        @HashLazy
        public int hashOuter(HashContext hashContext) {
            int hash = 0;
            for(Expr partition : partitions)
                hash += partition.hashOuter(hashContext);
            return hash * 31 + where.hashOuter(hashContext);
        }

        public Query translateOuter(MapTranslate translator) {
            return new Query(where.translateOuter(translator),translator.translate(partitions));
        }

        public SourceJoin[] getEnum() {
            throw new RuntimeException("not supported");
        }
    }

    public OrderJoin(Set<KeyExpr> keys, Set<Value> values, Where inner, Set<Expr> partitions, Map<KeyExpr, BaseExpr> group) {
        super(keys, values, new Query(inner, partitions), group);
    }

    private OrderJoin(Set<KeyExpr> keys, Set<Value> values, Query inner, Map<KeyExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    protected QueryJoin<KeyExpr, Query> createThis(Set<KeyExpr> keys, Set<Value> values, Query query, Map<KeyExpr, BaseExpr> group) {
        return new OrderJoin(keys, values, query, group);
    }

    private OrderJoin(OrderJoin orderJoin, MapTranslate translator) {
        super(orderJoin, translator);
    }

    public InnerJoin translateOuter(MapTranslate translator) {
        return new OrderJoin(this, translator);
    }

    @Override
    public StatKeys<KeyExpr> getStatKeys(KeyStat keyStat) {
        return query.where.getStatKeys(keys);
    }

    public Where getWhere() {
        return query.where;
    }

    public Set<Expr> getPartitions() {
        return query.partitions;
    }
}
