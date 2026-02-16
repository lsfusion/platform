package lsfusion.server.data.expr.join.select;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.stat.*;
import lsfusion.server.data.type.Type;

public class KeyExprCompareJoin extends ExprCompareJoin<KeyExpr, KeyExprCompareJoin> {

    public final Compare compare;

    public KeyExprCompareJoin(KeyExpr left, Compare compare, BaseExpr right) {
        super(left, right);

        this.compare = compare;
        assert isInterval(left, compare, right);
    }

    public static boolean isInterval(KeyExpr keyExpr, Compare compare, BaseExpr compareExpr) {
        if(!(compare == Compare.GREATER || compare == Compare.GREATER_EQUALS || compare == Compare.LESS || compare == Compare.LESS_EQUALS))
            return false;

        Type intervalValueType = getIntervalValueType(keyExpr, compareExpr);
        return intervalValueType != null && intervalValueType.getIntervalStep() != null;
    }

    public StatKeys<Integer> getStatKeys(KeyStat keyStat, StatType type) {
        Stat expr1Stat = expr1.getTypeStat(keyStat, false);
        Stat expr2Stat = expr2.getTypeStat(keyStat, false);
        return new StatKeys<>(expr1Stat.mult(expr2Stat), new DistinctKeys<>(MapFact.toMap(0, expr1Stat, 1,expr2Stat)));
    }

    public static Type getIntervalValueType(KeyExpr keyExpr, BaseExpr compareExpr) {
        return compareExpr.getSelfType();
    }

    @Override
    protected KeyExprCompareJoin createThis(KeyExpr expr1, BaseExpr expr2) {
        return new KeyExprCompareJoin(expr1, compare, expr2);
    }

    public int hash(HashContext hashContext) {
        return 31 * super.hash(hashContext) + compare.hashCode();
    }

    @Override
    public boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && compare == ((KeyExprCompareJoin)o).compare;
    }
}
