package platform.server.data.query.stat;

import platform.server.caches.ParamExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.Stat;

public interface KeyStat {

    Stat getKeyStat(ParamExpr key);
}
