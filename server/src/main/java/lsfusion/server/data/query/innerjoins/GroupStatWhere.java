package lsfusion.server.data.query.innerjoins;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.where.Where;

// сделаем generics для обратной совместимости, хотя в общем то он не нужен
public class GroupStatWhere<K> extends GroupWhere {

    public final StatKeys<K> stats;

    public GroupStatWhere(KeyEqual keyEqual, StatKeys<K> stats, Where where) {
        super(keyEqual, where);
        this.stats = stats;

        assert where.getKeyEquals().singleKey().isEmpty();
    }

    public static <K, V> ImCol<GroupStatWhere<V>> mapBack(ImCol<GroupStatWhere<K>> col, final ImMap<V,K> map) {
        return col.mapColValues(new GetValue<GroupStatWhere<V>, GroupStatWhere<K>>() {
            public GroupStatWhere<V> getMapValue(GroupStatWhere<K> group) {
                return new GroupStatWhere<V>(group.keyEqual, group.stats.mapBack(map), group.where);
            }});
    }

    @Override
    public boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && stats.equals(((GroupStatWhere) o).stats);
    }

    @Override
    public int immutableHashCode() {
        return 31 * super.immutableHashCode() + stats.hashCode();
    }

    public Object pack() {
        throw new RuntimeException("not supported yet");
    }
}
