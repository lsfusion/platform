package platform.server.data.expr.query;

import platform.server.caches.AbstractOuterContext;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.*;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.JoinSet;
import platform.server.data.query.SourceJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

import java.util.Map;
import java.util.Set;

public class GroupJoin extends QueryJoin<BaseExpr, GroupJoin.Query> implements InnerJoin {

    public static class Query extends AbstractOuterContext<Query> {
        private final Where where;
        private final JoinSet joins;

        public Query(Where where, JoinSet joins) {
            this.where = where;
            this.joins = joins;
        }

        @IdentityLazy
        public int hashOuter(HashContext hashContext) {
            return where.hashOuter(hashContext) * 31 + joins.hashContext(hashContext);
        }

        public Query translateOuter(MapTranslate translator) {
            return new Query(where.translateOuter(translator), joins.translateOuter(translator));
        }

        public SourceJoin[] getEnum() {
            throw new RuntimeException("not supported");
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Query && joins.equals(((Query) o).joins) && where.equals(((Query) o).where);
        }
    }

    public VariableExprSet getJoinFollows() {
        return InnerExpr.getExprFollows(group);
    }

    // дублируем аналогичную логику GroupExpr'а
    private GroupJoin(GroupJoin join, MapTranslate translator) {
        super(join, translator);
    }

    public InnerJoin translateOuter(MapTranslate translator) {
        return new GroupJoin(this, translator);
    }

    public GroupJoin(Set<KeyExpr> keys, Set<ValueExpr> values, Where where, JoinSet joins, Map<BaseExpr, BaseExpr> group) {
        super(keys,values,new Query(where,joins),group);
    }

    public GroupJoin(Set<KeyExpr> keys, Set<ValueExpr> values, Query inner, Map<BaseExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    protected QueryJoin<BaseExpr, Query> createThis(Set<KeyExpr> keys, Set<ValueExpr> values, Query query, Map<BaseExpr, BaseExpr> group) {
        return new GroupJoin(keys, values, query, group);
    }

    @IdentityLazy
    public int hashOuter(final HashContext hashContext) {
        return new QueryInnerHashContext() {
            protected int hashOuterExpr(BaseExpr outerExpr) {
                return outerExpr.hashOuter(hashContext);
            }
        }.hashInner(hashContext.values);
    }

    public boolean isIn(VariableExprSet set) {
        for(int i=0;i<set.size;i++) {
            VariableClassExpr expr = set.get(i);
            if(expr instanceof GroupExpr && equals(((GroupExpr)expr).getGroupJoin()))
                return true;
        }
        return false;
    }
}
