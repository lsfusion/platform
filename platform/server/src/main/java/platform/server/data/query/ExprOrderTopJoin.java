package platform.server.data.query;

import platform.base.TwinImmutableInterface;
import platform.interop.Compare;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.translator.MapTranslate;

import java.util.Collections;

public class ExprOrderTopJoin extends ExprJoin<ExprOrderTopJoin> {

    private final Compare compare;
    private final Expr compareExpr;
    private boolean key;

    @Override
    public String toString() {
        return baseExpr + " " + compare + " " + compareExpr;
    }

    public ExprOrderTopJoin(BaseExpr baseExpr, Compare compare, Expr compareExpr) {
        super(baseExpr);
        assert compareExpr.isValue();
        this.compareExpr = compareExpr;
        this.compare = compare;
    }

    public StatKeys<Integer> getStatKeys(KeyStat keyStat) {
        return new StatKeys<Integer>(Collections.singleton(0), Stat.ONE);
    }

    protected int hash(HashContext hashContext) {
        return 31 * (31 * super.hash(hashContext) + compare.hashCode()) + compareExpr.hashOuter(hashContext) + 13;
    }

    protected ExprOrderTopJoin translate(MapTranslate translator) {
        return new ExprOrderTopJoin(baseExpr.translateOuter(translator), compare, compareExpr);
    }

    public boolean twins(TwinImmutableInterface o) {
        return super.twins(o) && compare.equals(((ExprOrderTopJoin)o).compare) && compareExpr.equals(((ExprOrderTopJoin)o).compareExpr);
    }

    public boolean isKey() {
        return baseExpr instanceof KeyExpr;
    }
}
