package platform.server.data.query.stat;

import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.InnerExpr;
import platform.server.data.expr.NotNullExprSet;
import platform.server.data.expr.query.Stat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ValueJoin implements InnerBaseJoin<Object> {

    private ValueJoin() {
    }
    public final static ValueJoin instance = new ValueJoin();

    public Map<Object, BaseExpr> getJoins() {
        return new HashMap<Object, BaseExpr>();
    }

    public StatKeys<Object> getStatKeys(KeyStat keyStat) {
        return new StatKeys<Object>(new HashSet<Object>(), Stat.ONE);
    }

    public NotNullExprSet getExprFollows(boolean recursive) {
        return InnerExpr.getExprFollows(this, recursive);
    }
}
