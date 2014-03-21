package lsfusion.server.data.query.stat;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.BaseExpr;

public class FormulaJoin<K> extends CalculateJoin<K> {

    public final ImMap<K, BaseExpr> params;

    public FormulaJoin(ImMap<K, BaseExpr> params) {
        this.params = params;
    }

    public ImMap<K, BaseExpr> getJoins() {
        return params;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return params.equals(((FormulaJoin)o).params);
    }

    public int immutableHashCode() {
        return params.hashCode();
    }
}
