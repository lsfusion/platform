package platform.server.data.query.innerjoins;

import platform.base.TwinImmutableInterface;
import platform.server.caches.ManualLazy;
import platform.server.data.expr.Expr;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.Where;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

// сделаем generics для обратной совместимости, хотя в общем то он не нужен
public class GroupStatWhere<K> extends GroupWhere {

    public final StatKeys<K> stats;

    public GroupStatWhere(KeyEqual keyEqual, StatKeys<K> stats, Where where) {
        super(keyEqual, where);
        this.stats = stats;
    }

    public static <K, V> Collection<GroupStatWhere<V>> mapBack(Collection<GroupStatWhere<K>> col, Map<V,K> map) {
        Collection<GroupStatWhere<V>> result = new ArrayList<GroupStatWhere<V>>();
        for(GroupStatWhere<K> group : col)
            result.add(new GroupStatWhere<V>(group.keyEqual, group.stats.mapBack(map), group.where));
        return result;
    }

    @Override
    public boolean twins(TwinImmutableInterface o) {
        return super.twins(o) && stats.equals(((GroupStatWhere) o).stats);
    }

    @Override
    public int immutableHashCode() {
        return 31 * super.immutableHashCode() + stats.hashCode();
    }
}
