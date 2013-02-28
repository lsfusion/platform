package platform.server.data.query;

import platform.base.TwinImmutableObject;
import platform.base.col.SetFact;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.translator.MapTranslate;

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
        this(baseExpr, stat, new InnerJoins());
    }
    
    public boolean depends(InnerJoin join) {
        return valueJoins.means(join);
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

    public boolean twins(TwinImmutableObject o) {
        return super.twins(o) && stat.equals(((ExprStatJoin)o).stat) && valueJoins.equals(((ExprStatJoin)o).valueJoins);
    }
}
