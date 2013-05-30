package platform.server.data.query.stat;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.NotNullExpr;

public interface BaseJoin<K> {

    ImSet<NotNullExpr> getExprFollows(boolean recursive);

    ImMap<K, BaseExpr> getJoins();
    StatKeys<K> getStatKeys(KeyStat keyStat);
}
