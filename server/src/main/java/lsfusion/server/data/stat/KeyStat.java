package lsfusion.server.data.stat;

import lsfusion.server.data.expr.key.ParamExpr;

public interface KeyStat {

    Stat getKeyStat(ParamExpr key, boolean forJoin);
}
