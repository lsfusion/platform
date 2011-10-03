package platform.server.data.expr.query;

import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.*;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.SourceJoin;
import platform.server.data.query.innerjoins.KeyEqual;
import platform.server.data.query.innerjoins.StatInterface;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.translator.HashLazy;
import platform.server.data.translator.MapTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

import java.util.Map;
import java.util.Set;

public class GroupJoin extends QueryJoin<BaseExpr, GroupJoin.Query> {

    public static class Query extends AbstractOuterContext<Query> {
        private final Where where;
        private final StatInterface<?> joins;
        private final Map<KeyExpr, Stat> keyStats;
        private final Map<KeyExpr, Type> keyTypes;

        public Query(Where where, StatInterface joins, Map<KeyExpr, Stat> keyStats, Map<KeyExpr, Type> keyTypes) {
            this.where = where;
            this.joins = joins;
            this.keyStats = keyStats;
            this.keyTypes = keyTypes;
        }

        public boolean twins(TwinImmutableInterface o) {
            return joins.equals(((Query) o).joins) && where.equals(((Query) o).where) && keyStats.equals(((Query) o).keyStats) && keyTypes.equals(((Query) o).keyTypes);
        }

        @HashLazy
        public int hashOuter(HashContext hashContext) {
            int hash = 0;
            for(Map.Entry<KeyExpr, Stat> keyType : keyStats.entrySet())
                hash += keyType.getKey().hashOuter(hashContext) ^ keyType.getValue().hashCode();
            hash = hash * 31;
            for(Map.Entry<KeyExpr, Type> keyType : keyTypes.entrySet())
                hash += keyType.getKey().hashOuter(hashContext) ^ keyType.getValue().hashCode();
            return (31 * hash + where.hashOuter(hashContext))* 31 + joins.hashOuter(hashContext);
        }

        public Query translateOuter(MapTranslate translator) {
            return new Query(where.translateOuter(translator), joins.translateOuter(translator), translator.translateMapKeys(keyStats), translator.translateMapKeys(keyTypes));
        }

        public SourceJoin[] getEnum() {
            throw new RuntimeException("not supported");
        }
    }

    // дублируем аналогичную логику GroupExpr'а
    private GroupJoin(GroupJoin join, MapTranslate translator) {
        super(join, translator);
    }

    public InnerJoin translateOuter(MapTranslate translator) {
        return new GroupJoin(this, translator);
    }

    public GroupJoin(Map<KeyExpr, Stat> keyStats, Map<KeyExpr, Type> keyTypes, Set<Value> values, Where where, StatInterface joins, Map<BaseExpr, BaseExpr> group) {
        super(keyStats.keySet(),values,new Query(where, joins, keyStats, keyTypes),group);
    }

    public GroupJoin(Set<KeyExpr> keys, Set<Value> values, Query inner, Map<BaseExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }

    protected QueryJoin<BaseExpr, Query> createThis(Set<KeyExpr> keys, Set<Value> values, Query query, Map<BaseExpr, BaseExpr> group) {
        return new GroupJoin(keys, values, query, group);
    }

    @Override
    public StatKeys<BaseExpr> getStatKeys(KeyStat keyStat) {
        return query.joins.getStatKeys(group.keySet(), new KeyStat() {
            public Stat getKeyStat(KeyExpr key) {
                return query.keyStats.get(key);
            }
        });
    }
}
