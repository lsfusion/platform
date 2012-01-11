package platform.server.data.query.innerjoins;

import platform.server.caches.ManualLazy;
import platform.server.data.expr.Expr;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.Where;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

// сделаем generics для обратной совместимости, хотя в общем то он не нужен
public class GroupStatWhere<K> {

    public final KeyEqual keyEqual;
    public final StatKeys<K> stats;
    public final Where where; // !! where не включает keyEqual но включает все notNull baseExpr'ов его

    public GroupStatWhere(KeyEqual keyEqual, StatKeys<K> stats, Where where) {
        this.keyEqual = keyEqual;
        this.stats = stats;
        this.where = where;

        assert where.getKeyEquals().getSingleKey().isEmpty();
    }

    public static <K, V> Collection<GroupStatWhere<V>> mapBack(Collection<GroupStatWhere<K>> col, Map<V,K> map) {
        Collection<GroupStatWhere<V>> result = new ArrayList<GroupStatWhere<V>>();
        for(GroupStatWhere<K> group : col)
            result.add(new GroupStatWhere<V>(group.keyEqual, group.stats.mapBack(map), group.where));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GroupStatWhere && keyEqual.equals(((GroupStatWhere) o).keyEqual) && stats.equals(((GroupStatWhere) o).stats) && where.equals(((GroupStatWhere) o).where);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * keyEqual.hashCode() + stats.hashCode()) + where.hashCode();
    }
}
