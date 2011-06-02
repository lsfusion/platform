package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SumUnionProperty extends UnionProperty {

    public SumUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    public Map<PropertyMapImplement<?,Interface>,Integer> operands = new HashMap<PropertyMapImplement<?, Interface>, Integer>();

    protected Collection<PropertyMapImplement<?, Interface>> getOperands() {
        return operands.keySet();
    }

    public Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {

        Expr result = null;
        for(Map.Entry<PropertyMapImplement<?,Interface>,Integer> operandCoeff : operands.entrySet()) {
            Expr operandExpr = operandCoeff.getKey().mapExpr(joinImplement, modifier, changedWhere).scale(operandCoeff.getValue());
            if(result==null)
                result = operandExpr;
            else
                result = result.sum(operandExpr);
        }
        return result;
    }

}
