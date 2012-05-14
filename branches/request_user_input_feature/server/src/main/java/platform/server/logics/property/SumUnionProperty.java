package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SumUnionProperty extends IncrementUnionProperty {

    public SumUnionProperty(String sID, String caption, List<Interface> interfaces, Map<CalcPropertyMapImplement<?, Interface>, Integer> operands) {
        super(sID, caption, interfaces);
        this.operands = operands;

        finalizeInit();
    }

    private final Map<CalcPropertyMapImplement<?,Interface>,Integer> operands;

    protected Collection<CalcPropertyMapImplement<?, Interface>> getOperands() {
        return operands.keySet();
    }

    protected Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Expr result = Expr.NULL;
        for(Map.Entry<CalcPropertyMapImplement<?,Interface>,Integer> operandCoeff : operands.entrySet())
            result = result.sum(operandCoeff.getKey().mapExpr(joinImplement, propClasses, propChanges, changedWhere).scale(operandCoeff.getValue()));
        return result;
    }

    protected Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        Expr result = prevExpr;
        for(Map.Entry<CalcPropertyMapImplement<?,Interface>,Integer> operandCoeff : operands.entrySet()) {
            WhereBuilder changedOperandWhere = new WhereBuilder();
            Expr newOperandExpr = operandCoeff.getKey().mapExpr(joinImplement, propChanges, changedOperandWhere);
            Expr prevOperandExpr = operandCoeff.getKey().mapExpr(joinImplement);
            result = result.sum(newOperandExpr.diff(prevOperandExpr).and(changedOperandWhere.toWhere()).scale(operandCoeff.getValue()));
            if(changedWhere!=null) changedWhere.add(changedOperandWhere.toWhere());
        }
        return result;
    }
}
