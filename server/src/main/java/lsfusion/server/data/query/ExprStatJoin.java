package lsfusion.server.data.query;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.translator.MapTranslate;

public class ExprStatJoin extends ExprJoin<ExprStatJoin> {

    private final Stat stat;
    private final InnerJoins valueJoins;

    @Override
    public String toString() {
        return baseExpr + " - " + stat.toString() + " " + valueJoins;
    }

    protected Stat getStat() {
        return stat;
    }

    public ExprStatJoin(BaseExpr baseExpr, Stat stat, BaseExpr valueExpr) {
        this(baseExpr, stat, getInnerJoins(valueExpr));
        assert valueExpr.isValue();
    }

    public ExprStatJoin(BaseExpr baseExpr, Stat stat) {
        this(baseExpr, stat, InnerJoins.EMPTY);
    }
    
    public boolean depends(InnerJoin join) {
        return valueJoins.containsAll(join);
    }

    public ExprStatJoin(BaseExpr baseExpr, Stat stat, InnerJoins valueJoins) {
        super(baseExpr);

        this.stat = stat;
        this.valueJoins = valueJoins;
    }

    @Override
    public InnerJoins getInnerJoins() {
        return super.getInnerJoins().and(valueJoins);
    }

    public StatKeys<Integer> getStatKeys(KeyStat keyStat) {
        return new StatKeys<Integer>(SetFact.singleton(0), getStat());
    }

    protected int hash(HashContext hashContext) {
        return 31 * (31 * super.hash(hashContext) + stat.hashCode()) + 5;
    }

    protected ExprStatJoin translate(MapTranslate translator) {
        return new ExprStatJoin(baseExpr.translateOuter(translator), stat, valueJoins.translate(translator.mapValues()));
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && stat.equals(((ExprStatJoin)o).stat) && valueJoins.equals(((ExprStatJoin)o).valueJoins);
    }
}
