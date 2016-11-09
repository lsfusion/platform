package lsfusion.server.data.expr.query;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.InnerExprFollows;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.where.Where;

public class SubQueryJoin extends QueryJoin<KeyExpr, SubQueryJoin.Query, SubQueryJoin, SubQueryJoin.QueryOuterContext> {

    public SubQueryJoin(ImSet<KeyExpr> keys, ImSet<Value> values, InnerExprFollows<KeyExpr> innerFollows, Where inner, ImMap<KeyExpr, BaseExpr> group) {
        this(keys, values, new Query(innerFollows, inner), group);
    }

    protected SubQueryJoin(ImSet<KeyExpr> keys, ImSet<Value> values, Query inner, ImMap<KeyExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    public static class Query extends QueryJoin.Query<KeyExpr, Query> {
        private final Where where;

        public Query(InnerExprFollows<KeyExpr> follows, Where where) {
            super(follows);
            this.where = where;
        }

        public Query(Query query, MapTranslate translate) {
            super(query, translate);
            this.where = query.where.translateOuter(translate);
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && where.equals(((Query) o).where);
        }

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashContext hashContext) {
            return 31 * super.hash(hashContext) + where.hashOuter(hashContext);
        }

        protected Query translate(MapTranslate translator) {
            return new Query(this, translator);
        }

        public ImSet<OuterContext> calculateOuterDepends() {
            return super.calculateOuterDepends().merge(SetFact.<OuterContext>singleton(where));
        }
    }

    public static class QueryOuterContext extends QueryJoin.QueryOuterContext<KeyExpr, Query, SubQueryJoin, SubQueryJoin.QueryOuterContext> {
        public QueryOuterContext(SubQueryJoin thisObj) {
            super(thisObj);
        }

        public SubQueryJoin translateThis(MapTranslate translator) {
            return new SubQueryJoin(thisObj, translator);
        }
    }
    protected QueryOuterContext createOuterContext() {
        return new QueryOuterContext(this);
    }

    protected SubQueryJoin createThis(ImSet<KeyExpr> keys, ImSet<Value> values, SubQueryJoin.Query query, ImMap<KeyExpr, BaseExpr> group) {
        return new SubQueryJoin(keys, values, query, group);
    }

    private SubQueryJoin(SubQueryJoin partitionJoin, MapTranslate translator) {
        super(partitionJoin, translator);
    }

    public StatKeys<KeyExpr> getStatKeys() {
        return query.where.getFullStatKeys(keys);
    }

    // кэшить
    public StatKeys<KeyExpr> getStatKeys(KeyStat keyStat) {
        return getStatKeys();
    }

    public Where getWhere() {
        return query.where;
    }
}
