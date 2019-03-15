package lsfusion.server.data.expr.join.stat;

public interface InnerBaseJoin<K> extends BaseJoin<K> {

    boolean hasExprFollowsWithoutNotNull(); // для оптимизации
}
