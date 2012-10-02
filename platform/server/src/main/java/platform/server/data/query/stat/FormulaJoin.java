package platform.server.data.query.stat;

import platform.base.TwinImmutableInterface;
import platform.server.data.expr.BaseExpr;

import java.util.Map;

public class FormulaJoin<K> extends CalculateJoin<K> {

    public final Map<K, BaseExpr> params;

    public FormulaJoin(Map<K, BaseExpr> params) {
        this.params = params;
    }

    public Map<K, BaseExpr> getJoins() {
        return params;
    }

    public boolean twins(TwinImmutableInterface o) {
        return params.equals(((FormulaJoin)o).params);
    }

    public int immutableHashCode() {
        return params.hashCode();
    }
}
