package lsfusion.server.data.expr.join.query;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.join.classes.InnerExprFollows;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.stat.StatKeys;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.Where;

public class SubQueryJoin extends QueryJoin<KeyExpr, SubQueryJoin.Query, SubQueryJoin, SubQueryJoin.QueryOuterContext> {

    public SubQueryJoin(ImSet<KeyExpr> keys, ImSet<Value> values, InnerExprFollows<KeyExpr> innerFollows, Where inner, int top, ImMap<KeyExpr, BaseExpr> group) {
        this(keys, values, new Query(innerFollows, inner, top), group);
    }

    protected SubQueryJoin(ImSet<KeyExpr> keys, ImSet<Value> values, Query inner, ImMap<KeyExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    public static class Query extends QueryJoin.Query<KeyExpr, Query> {
        private final Where where;
        private final int top;

        public Query(InnerExprFollows<KeyExpr> follows, Where where, int top) {
            super(follows);
            this.where = where;
            this.top = top;
        }

        public Query(Query query, MapTranslate translate) {
            super(query, translate);
            this.where = query.where.translateOuter(translate);
            this.top = query.top;
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && where.equals(((Query) o).where) && top == ((Query) o).top;
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashContext hashContext) {
            return 31 * (31 * super.hash(hashContext) + where.hashOuter(hashContext)) + top;
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

    @IdentityLazy
    public StatKeys<KeyExpr> getPushedStatKeys(StatType type, StatKeys<KeyExpr> pushStatKeys) {
        return query.where.getPushedStatKeys(keys, type, pushStatKeys); // формально full, но у full пока нет смысла добавлять инфраструктуру pushStatKeys
    }

    public Where getWhere() {
        return query.where;
    }

    public int getTop() {
        return query.top;
    }
}
