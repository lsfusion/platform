package lsfusion.server.data.query;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.Compare;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.NotNullExprInterface;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.translator.MapTranslate;

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
            return new StatKeys<Integer>(SetFact.<Integer>EMPTY(), Stat.ONE);
        else
            if(compare.equals(Compare.EQUALS) && !givesNoKeys()) // если не дает ключей, нельзя уменьшать статистику, так как паковка может съесть другие join'ы и тогда будет висячий ключ
                return new StatKeys<Integer>(SetFact.singleton(0), Stat.ONE);
            else
                return new StatKeys<Integer>(SetFact.singleton(0), baseExpr.getTypeStat(keyStat, true));
    }

    protected int hash(HashContext hashContext) {
        return 31 * (31 * super.hash(hashContext) + compare.hashCode()) + compareExpr.hashOuter(hashContext) + 13 + (not?1:0);
    }

    protected ExprOrderTopJoin translate(MapTranslate translator) {
        return new ExprOrderTopJoin(baseExpr.translateOuter(translator), compare, compareExpr, not);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && compare.equals(((ExprOrderTopJoin)o).compare) && compareExpr.equals(((ExprOrderTopJoin)o).compareExpr) && not==((ExprOrderTopJoin)o).not;
    }

    @Override
    public ImSet<NotNullExprInterface> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
        if(not)
            return SetFact.EMPTY();
        return super.getExprFollows(includeInnerWithoutNotNull, recursive);
    }

    @Override
    public ImMap<Integer, BaseExpr> getJoins() {
        if(not)
            return MapFact.EMPTY();
        return super.getJoins();
    }

    @Override
    public InnerJoins getInnerJoins() {
        if(not)
            return InnerJoins.EMPTY;
        return super.getInnerJoins();
    }

    public boolean givesNoKeys() {
        return not || baseExpr instanceof KeyExpr;
    }
}
