package platform.server.data.query;

import platform.base.TwinImmutableInterface;
import platform.interop.Compare;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.NotNullExprSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.translator.MapTranslate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExprOrderTopJoin extends ExprJoin<ExprOrderTopJoin> {

    private final Compare compare;
    private final Expr compareExpr;
    private boolean not;

    @Override
    public String toString() {
        return baseExpr + " " + compare + " " + compareExpr + " " + not;
    }

    public ExprOrderTopJoin(BaseExpr baseExpr, Compare compare, Expr compareExpr, boolean not) {
        super(baseExpr);
        assert compareExpr.isValue();
        assert baseExpr.isTableIndexed();
        this.compareExpr = compareExpr;
        this.compare = compare;
        this.not = not;
    }

    public StatKeys<Integer> getStatKeys(KeyStat keyStat) {
        if(not)
            return new StatKeys<Integer>(new ArrayList<Integer>(), Stat.ONE);
        else
            if(compare.equals(Compare.EQUALS))
                return new StatKeys<Integer>(Collections.singleton(0), Stat.ONE);
            else
                return new StatKeys<Integer>(Collections.singleton(0), baseExpr.getTypeStat(keyStat));
    }

    protected int hash(HashContext hashContext) {
        return 31 * (31 * super.hash(hashContext) + compare.hashCode()) + compareExpr.hashOuter(hashContext) + 13 + (not?1:0);
    }

    protected ExprOrderTopJoin translate(MapTranslate translator) {
        return new ExprOrderTopJoin(baseExpr.translateOuter(translator), compare, compareExpr, not);
    }

    public boolean twins(TwinImmutableInterface o) {
        return super.twins(o) && compare.equals(((ExprOrderTopJoin)o).compare) && compareExpr.equals(((ExprOrderTopJoin)o).compareExpr) && not==((ExprOrderTopJoin)o).not;
    }

    @Override
    public NotNullExprSet getExprFollows(boolean recursive) {
        if(not)
            return new NotNullExprSet();
        return super.getExprFollows(recursive);
    }

    @Override
    public Map<Integer, BaseExpr> getJoins() {
        if(not)
            return new HashMap<Integer, BaseExpr>();
        return super.getJoins();
    }

    @Override
    public InnerJoins getInnerJoins() {
        if(not)
            return new InnerJoins();
        return super.getInnerJoins();
    }

    public boolean givesNoKeys() {
        return not || baseExpr instanceof KeyExpr;
    }
}
