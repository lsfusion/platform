package lsfusion.server.data.query.compile.where;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.translator.JoinExprTranslator;
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
    public Where getWhere(JoinExprTranslator translator) {
        return JoinExprTranslator.translateExpr((Expr)expr, translator).getWhere();
    }
}
