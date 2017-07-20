package lsfusion.server.data.query.stat;

import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.BaseExpr;

public class FormulaJoin<K> extends CalculateJoin<K> {

    public final ImMap<K, BaseExpr> params;
    private final boolean concatenate; // для того чтобы не рушился assertion в getStatKeys, когда сливается cross-column используется ConcatenateExpr, и важно чтобы его join случайно не совпал с одним из join'ом который сливается (потому как получается цикл, ссылка саму на себе)

    public FormulaJoin(ImMap<K, BaseExpr> params, boolean concatenate) {
        this.params = params;
        this.concatenate = concatenate;
    }

    public ImMap<K, BaseExpr> getJoins() {
        return params;
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return params.equals(((FormulaJoin)o).params) && concatenate == ((FormulaJoin)o).concatenate;
    }

    public int immutableHashCode() {
        return 31 * params.hashCode() + (concatenate ? 1 : 0);
    }
}
