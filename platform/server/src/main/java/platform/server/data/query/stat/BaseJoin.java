package platform.server.data.query.stat;

import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.InnerExprSet;

import java.util.Map;

public interface BaseJoin<K> {

    InnerExprSet getExprFollows(boolean recursive);

    Map<K, BaseExpr> getJoins();
    StatKeys<K> getStatKeys(KeyStat keyStat);
}
