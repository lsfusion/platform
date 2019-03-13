package lsfusion.server.data.query.stat;

import lsfusion.server.base.caches.ParamExpr;
import lsfusion.server.data.expr.query.Stat;

public interface KeyStat {

    Stat getKeyStat(ParamExpr key, boolean forJoin);
}
