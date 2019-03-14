package lsfusion.server.data.query.stat;

import lsfusion.server.data.expr.ParamExpr;
import lsfusion.server.data.expr.query.Stat;

public interface KeyStat {

    Stat getKeyStat(ParamExpr key, boolean forJoin);
}
