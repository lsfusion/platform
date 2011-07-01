package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.MapDataChanges;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.util.*;

// чисто для оптимизации
public class ExclusiveUnionProperty extends UnionProperty {

    public Set<PropertyMapImplement<?,Interface>> operands = new HashSet<PropertyMapImplement<?, Interface>>();

    @Override
    protected Collection<PropertyMapImplement<?, Interface>> getOperands() {
        return operands;
    }

    @Override
    protected Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Expr result = Expr.NULL;
        for(PropertyMapImplement<?, Interface> operand : operands)
            result = operand.mapExpr(joinImplement, modifier, changedWhere).nvl(result);
        return result;
    }

    @Override
    protected Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, Expr prevExpr, WhereBuilder changedWhere) {
        CaseExprInterface cases = Expr.newCases();
        for(PropertyMapImplement<?, Interface> operand : operands) {
            WhereBuilder changedOperandWhere = new WhereBuilder();
            Expr newOperandExpr = operand.mapExpr(joinImplement, modifier, changedOperandWhere);
            cases.add(changedOperandWhere.toWhere(), newOperandExpr);
        }
        if(changedWhere!=null) changedWhere.add(cases.getUpWhere());
        cases.add(Where.TRUE, prevExpr);
        return cases.getFinal();
    }

    public ExclusiveUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    @Override
    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        MapDataChanges<Interface> result = new MapDataChanges<Interface>();
        for(PropertyMapImplement<?, Interface> operand : operands)
            result = result.add(operand.mapDataChanges(change, changedWhere, modifier));
        return result;
    }
}
