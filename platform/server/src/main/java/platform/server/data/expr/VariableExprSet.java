package platform.server.data.expr;

import platform.base.QuickSet;
import platform.server.data.where.DataWhereSet;

import java.util.Collection;

public class VariableExprSet extends QuickSet<VariableClassExpr> {

    public VariableExprSet() {
    }

    public VariableExprSet(VariableExprSet set) {
        super(set);
    }

    public VariableExprSet(VariableExprSet[] sets) {
        super(sets);
    }

    public VariableExprSet(VariableClassExpr expr) {
        add(expr);
    }

    public VariableExprSet(BaseExpr expr1, BaseExpr expr2) {
        addAll(expr1.getExprFollows());
        addAll(expr2.getExprFollows());
    }

    public VariableExprSet(Collection<BaseExpr> exprs) {
        for(BaseExpr expr : exprs)
            addAll(expr.getExprFollows());
    }
}
