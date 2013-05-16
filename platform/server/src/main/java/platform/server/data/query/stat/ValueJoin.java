package platform.server.data.query.stat;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.InnerExpr;
import platform.server.data.expr.NotNullExpr;
import platform.server.data.expr.query.Stat;

public class ValueJoin implements InnerBaseJoin<Object> {

    private ValueJoin() {
    }
    public final static ValueJoin instance = new ValueJoin();

    public ImMap<Object, BaseExpr> getJoins() {
        return MapFact.EMPTY();
    }

    public StatKeys<Object> getStatKeys(KeyStat keyStat) {
        return new StatKeys<Object>(SetFact.<Object>EMPTY(), Stat.ONE);
    }

    public ImSet<NotNullExpr> getExprFollows(boolean recursive) {
        return InnerExpr.getExprFollows(this, recursive);
    }
}
