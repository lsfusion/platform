package platform.server.data.expr.query;

import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
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
import platform.server.data.where.Where;

import java.util.Map;
import java.util.Set;

public class PartitionJoin extends QueryJoin<KeyExpr, PartitionJoin.Query, PartitionJoin, PartitionJoin.QueryOuterContext> {

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

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashContext hashContext) {
            return hashOuter(partitions, hashContext) * 31 + where.hashOuter(hashContext);
        }

        protected Query translate(MapTranslate translator) {
            return new Query(where.translateOuter(translator),translator.translate(partitions));
        }

        public QuickSet<OuterContext> calculateOuterDepends() {
            return new QuickSet<OuterContext>(partitions, where);
        }
    }

    public PartitionJoin(QuickSet<KeyExpr> keys, QuickSet<Value> values, Where inner, Set<Expr> partitions, Map<KeyExpr, BaseExpr> group) {
        super(keys, values, new Query(inner, partitions), group);
    }

    private PartitionJoin(QuickSet<KeyExpr> keys, QuickSet<Value> values, Query inner, Map<KeyExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    protected PartitionJoin createThis(QuickSet<KeyExpr> keys, QuickSet<Value> values, Query query, Map<KeyExpr, BaseExpr> group) {
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
