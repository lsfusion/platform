package platform.server.data.expr.query;

import platform.server.caches.AbstractTranslateContext;
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

    public static class Query extends AbstractTranslateContext<Query> {
        private final Where where;
        private final JoinSet joins;

        public Query(Where where, JoinSet joins) {
            this.where = where;
            this.joins = joins;
        }

        @IdentityLazy
        public int hashContext(HashContext hashContext) {
            return where.hashContext(hashContext) * 31 + joins.hashContext(hashContext);
        }

        public Query translate(MapTranslate translator) {
            return new Query(where.translate(translator), joins.translate(translator));
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

    public InnerJoin translate(MapTranslate translator) {
        return new GroupJoin(this, translator);
    }

    public GroupJoin(Set<KeyExpr> keys, Set<ValueExpr> values, Where where, JoinSet joins, Map<BaseExpr, BaseExpr> group) {
        super(keys,values,new Query(where,joins),group);
    }

    public int hashContext(HashContext hashContext) {
        return hashes.hashContext(hashContext);
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
