package platform.server.data.query.stat;

import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.expr.BaseExpr;

public class FormulaJoin<K> extends CalculateJoin<K> {

    public final ImMap<K, BaseExpr> params;

    public FormulaJoin(ImMap<K, BaseExpr> params) {
        this.params = params;
    }

    public ImMap<K, BaseExpr> getJoins() {
        return params;
    }

    public boolean twins(TwinImmutableObject o) {
        return params.equals(((FormulaJoin)o).params);
    }

    public int immutableHashCode() {
        return params.hashCode();
    }
}
