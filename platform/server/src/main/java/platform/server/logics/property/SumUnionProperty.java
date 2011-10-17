package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SumUnionProperty extends IncrementUnionProperty {

    public SumUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    public Map<PropertyMapImplement<?,Interface>,Integer> operands = new HashMap<PropertyMapImplement<?, Interface>, Integer>();

    protected Collection<PropertyMapImplement<?, Interface>> getOperands() {
        return operands.keySet();
    }

    protected Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Expr result = Expr.NULL;
        for(Map.Entry<PropertyMapImplement<?,Interface>,Integer> operandCoeff : operands.entrySet())
            result = result.sum(operandCoeff.getKey().mapExpr(joinImplement, modifier, changedWhere).scale(operandCoeff.getValue()));
        return result;
    }

    protected Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, Expr prevExpr, WhereBuilder changedWhere) {
        Expr result = prevExpr;
        for(Map.Entry<PropertyMapImplement<?,Interface>,Integer> operandCoeff : operands.entrySet()) {
            WhereBuilder changedOperandWhere = new WhereBuilder();
            Expr newOperandExpr = operandCoeff.getKey().mapExpr(joinImplement, modifier, changedOperandWhere);
            Expr prevOperandExpr = operandCoeff.getKey().mapExpr(joinImplement);
            result = result.sum(newOperandExpr.sum(prevOperandExpr.scale(-1)).and(changedOperandWhere.toWhere()).scale(operandCoeff.getValue()));
            if(changedWhere!=null) changedWhere.add(changedOperandWhere.toWhere());
        }
        return result;
    }
}
