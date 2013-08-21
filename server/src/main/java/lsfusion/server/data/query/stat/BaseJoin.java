package lsfusion.server.data.query.stat;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.NotNullExpr;

public interface BaseJoin<K> {

    ImSet<NotNullExpr> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive);

    ImMap<K, BaseExpr> getJoins();
    StatKeys<K> getStatKeys(KeyStat keyStat);
}
