package platform.server.data.expr.query;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.caches.HashContext;
import platform.server.caches.TranslateContext;
import platform.server.caches.AbstractTranslateContext;
import platform.server.data.query.SourceJoin;
import platform.server.data.translator.KeyTranslator;

import java.util.Set;
import java.util.Map;

public class OrderJoin extends QueryJoin<KeyExpr, OrderJoin.Query> {

    public static class Query extends AbstractTranslateContext<Query> {
        private final Where where;
        private final Set<Expr> partitions;

        public Query(Where where, Set<Expr> partitions) {
            this.where = where;
            this.partitions = partitions;
        }

        public int hashContext(HashContext hashContext) {
            int hash = 0;
            for(Expr partition : partitions)
                hash += partition.hashContext(hashContext);
            return hash * 31 + where.hashContext(hashContext);
        }

        public Query translateDirect(KeyTranslator translator) {
            return new Query(where.translateDirect(translator),translator.translate(partitions));
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
}
