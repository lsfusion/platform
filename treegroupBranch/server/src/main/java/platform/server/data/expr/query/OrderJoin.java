package platform.server.data.expr.query;

import platform.server.caches.AbstractOuterContext;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.SourceJoin;
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

        @IdentityLazy
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

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Query && partitions.equals(((Query) o).partitions) && where.equals(((Query) o).where);
        }
    }

    public OrderJoin(Set<KeyExpr> keys, Set<ValueExpr> values, Where inner, Set<Expr> partitions, Map<KeyExpr, BaseExpr> group) {
        super(keys, values, new Query(inner, partitions), group);
    }

    private OrderJoin(Set<KeyExpr> keys, Set<ValueExpr> values, Query inner, Map<KeyExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    protected QueryJoin<KeyExpr, Query> createThis(Set<KeyExpr> keys, Set<ValueExpr> values, Query query, Map<KeyExpr, BaseExpr> group) {
        return new OrderJoin(keys, values, query, group);
    }
}
