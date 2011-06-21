package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.*;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.innerjoins.GroupJoinSet;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class GroupJoin extends QueryJoin<BaseExpr, GroupJoin.Query> implements InnerJoin<BaseExpr> {

    public static class Query extends AbstractOuterContext<Query> {
        private final Where where;
        private final GroupJoinSet<?> joins;

        public Query(Where where, GroupJoinSet joins) {
            this.where = where;
            this.joins = joins;
        }

        public boolean twins(TwinImmutableInterface o) {
            return joins.equals(((Query) o).joins) && where.equals(((Query) o).where);
        }

        @IdentityLazy
        public int hashOuter(HashContext hashContext) {
            return where.hashOuter(hashContext) * 31 + joins.hashOuter(hashContext);
        }

        public Query translateOuter(MapTranslate translator) {
            return new Query(where.translateOuter(translator), joins.translateOuter(translator));
        }

        public SourceJoin[] getEnum() {
            throw new RuntimeException("not supported");
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

    public GroupJoin(Set<KeyExpr> keys, Set<Value> values, Where where, GroupJoinSet joins, Map<BaseExpr, BaseExpr> group) {
        super(keys,values,new Query(where,joins),group);
    }

    public GroupJoin(Set<KeyExpr> keys, Set<Value> values, Query inner, Map<BaseExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    protected QueryJoin<BaseExpr, Query> createThis(Set<KeyExpr> keys, Set<Value> values, Query query, Map<BaseExpr, BaseExpr> group) {
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

    @IdentityLazy
    public StatKeys<BaseExpr> getStatKeys() {
        return getStat(query.joins.getStatKeys(keys), BaseUtils.toMap(group.keySet()));
    }

    public static <K> StatKeys<K> getStat(StatKeys<KeyExpr> statKeys, Map<K, BaseExpr> exprs) {
        StatKeys<K> result = new StatKeys<K>();
        for(Map.Entry<K, BaseExpr> groupKey : exprs.entrySet())
            result.add(groupKey.getKey(), statKeys.getMaxStat(AbstractSourceJoin.enumKeys(groupKey.getValue())));
        return result;
    }

    public Map<BaseExpr, BaseExpr> getJoins() {
        return group;
    }
}
