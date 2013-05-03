package platform.server.data.expr.query;

import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
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
import platform.server.data.where.Where;

public class PartitionJoin extends QueryJoin<KeyExpr, PartitionJoin.Query, PartitionJoin, PartitionJoin.QueryOuterContext> {

    public static class Query extends AbstractOuterContext<Query> {
        private final Where where;
        private final ImSet<Expr> partitions;

        public Query(Where where, ImSet<Expr> partitions) {
            this.where = where;
            this.partitions = partitions;
        }

        public boolean twins(TwinImmutableObject o) {
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

        public ImSet<OuterContext> calculateOuterDepends() {
            return SetFact.<OuterContext>merge(partitions, where);
        }
    }

    public PartitionJoin(ImSet<KeyExpr> keys, ImSet<Value> values, Where inner, ImSet<Expr> partitions, ImMap<KeyExpr, BaseExpr> group) {
        super(keys, values, new Query(inner, partitions), group);
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

    public StatKeys<KeyExpr> getStatKeys(KeyStat keyStat) {
        return query.where.getFullStatKeys(keys);
    }

    public Where getWhere() {
        return query.where;
    }

    public ImSet<Expr> getPartitions() {
        return query.partitions;
    }
}
