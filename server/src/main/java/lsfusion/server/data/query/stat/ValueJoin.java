package lsfusion.server.data.query.stat;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.InnerExpr;
import lsfusion.server.data.expr.NotNullExpr;
import lsfusion.server.data.expr.query.Stat;

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

    public ImSet<NotNullExpr> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
        return InnerExpr.getExprFollows(this, includeInnerWithoutNotNull, recursive);
    }

    public boolean hasExprFollowsWithoutNotNull() {
        return InnerExpr.hasExprFollowsWithoutNotNull(this);
    }
}
