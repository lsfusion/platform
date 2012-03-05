package platform.server.data.query.stat;

import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.NotNullExprSet;

import java.util.Map;

public interface BaseJoin<K> {

    NotNullExprSet getExprFollows(boolean recursive);

    Map<K, BaseExpr> getJoins();
    StatKeys<K> getStatKeys(KeyStat keyStat);
}
