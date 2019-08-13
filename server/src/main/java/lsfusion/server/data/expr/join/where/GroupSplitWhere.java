package lsfusion.server.data.expr.join.where;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.stat.StatKeys;
import lsfusion.server.data.where.Where;

// сделаем generics для обратной совместимости, хотя в общем то он не нужен
public class GroupSplitWhere<K> extends GroupWhere {

    public final StatKeys<K> stats;
    private final boolean exclusiveSimpleCount;

//    public static <K extends BaseExpr> StatKeys<K> getStatKeys(ImCol<GroupJoinsWhere> groupStats, final ImSet<K> groups, final StatType statType) {
//        return StatKeys.or(groupStats, new GetValue<StatKeys<K>, GroupJoinsWhere>() {
//            public StatKeys<K> getMapValue(GroupJoinsWhere value) {
//                return value.getStatKeys(groups, statType);
//            }}, groups);
//    }
//
    public GroupSplitWhere(KeyEqual keyEqual, StatKeys<K> stats, Where where) {
        this(keyEqual, stats, where, false);
    }
    public GroupSplitWhere(KeyEqual keyEqual, StatKeys<K> stats, Where where, boolean exclusiveSimpleCount) {
        super(keyEqual, where);
        this.stats = stats;
        this.exclusiveSimpleCount = exclusiveSimpleCount;

        assert exclusiveSimpleCount || where.getKeyEquals().singleKey().isEmpty();
    }

    public static <K, V> ImCol<GroupSplitWhere<V>> mapBack(ImCol<GroupSplitWhere<K>> col, final ImMap<V,K> map) {
        return col.mapColValues(group -> new GroupSplitWhere<>(group.keyEqual, group.stats.mapBack(map), group.where, group.exclusiveSimpleCount));
    }

    @Override
    public boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && stats.equals(((GroupSplitWhere) o).stats);
    }

    @Override
    public int immutableHashCode() {
        return 31 * super.immutableHashCode() + stats.hashCode();
    }

    public Object pack() {
        throw new RuntimeException("not supported yet");
    }
}
