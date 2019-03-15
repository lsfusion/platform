package lsfusion.server.data.expr.join.stat;

import lsfusion.server.data.expr.ParamExpr;
import lsfusion.server.data.expr.query.stat.Stat;

public interface KeyStat {

    Stat getKeyStat(ParamExpr key, boolean forJoin);
}
