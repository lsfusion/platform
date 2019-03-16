package lsfusion.server.data.expr.join.inner;

import lsfusion.server.data.expr.join.base.BaseJoin;

public interface InnerBaseJoin<K> extends BaseJoin<K> {

    boolean hasExprFollowsWithoutNotNull(); // для оптимизации
}
