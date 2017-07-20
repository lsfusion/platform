package lsfusion.server.data.query.innerjoins;

import lsfusion.base.TwinImmutableObject;
import lsfusion.server.data.expr.InnerExpr;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;

public class InnerUpWhere extends AbstractUpWhere<InnerUpWhere> {

    private final InnerExpr expr;

    public InnerUpWhere(InnerExpr expr) {
        this.expr = expr;
    }

    public int immutableHashCode() {
        return expr.hashCode();
    }

    protected boolean calcTwins(TwinImmutableObject o) {
        return expr.equals(((InnerUpWhere)o).expr);
    }

    @Override
    public Where getWhere() {
        return expr.getWhere();
    }
}
