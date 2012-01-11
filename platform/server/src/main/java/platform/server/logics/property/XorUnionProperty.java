package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class XorUnionProperty extends IncrementUnionProperty {

    public XorUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }
    public List<PropertyMapImplement<?,Interface>> operands = new ArrayList<PropertyMapImplement<?, Interface>>();

    protected Collection<PropertyMapImplement<?, Interface>> getOperands() {
        return operands;
    }

    @Override
    protected Expr calculateNewExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Where xorWhere = Where.FALSE;
        for(PropertyMapImplement<?, Interface> operand : operands)
            xorWhere = xorWhere.xor(operand.mapExpr(joinImplement, propChanges, changedWhere).getWhere());
        return ValueExpr.get(xorWhere);
    }

    @Override
    protected Expr calculateIncrementExpr(Map<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, Expr prevExpr, WhereBuilder changedWhere) {
        Where resultWhere = prevExpr.getWhere();
        for(PropertyMapImplement<?, Interface> operand : operands) {
            WhereBuilder changedOperandWhere = new WhereBuilder();
            Where newOperandWhere = operand.mapExpr(joinImplement, propChanges, changedOperandWhere).getWhere();
            Where prevOperandWhere = operand.mapExpr(joinImplement).getWhere();
            resultWhere = resultWhere.xor(newOperandWhere.xor(prevOperandWhere).and(changedOperandWhere.toWhere()));
            if(changedWhere!=null) changedWhere.add(changedOperandWhere.toWhere());
        }
        return ValueExpr.get(resultWhere);
    }

    @Override
    protected PropertyChanges calculateUsedDataChanges(PropertyChanges propChanges) {
        return propChanges.getUsedDataChanges(getDepends()).add(propChanges.getUsedChanges(getDepends()));
    }

    @Override
    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        MapDataChanges<Interface> result = new MapDataChanges<Interface>();
        for(PropertyMapImplement<?, Interface> operand : BaseUtils.reverse(operands)) {
            Where siblingWhere = Where.FALSE;
            for(PropertyMapImplement<?, Interface> siblingOperand : operands) // считаем where сиблингов и потом ими xor'им change
                if(siblingOperand!=operand)
                    siblingWhere = siblingWhere.xor(siblingOperand.mapExpr(change.mapKeys, propChanges).getWhere());
            WhereBuilder operandWhere = new WhereBuilder();
            result = result.add(operand.mapDataChanges(new PropertyChange<Interface>(change.mapKeys, ValueExpr.get(change.expr.getWhere().xor(siblingWhere)), change.where), operandWhere, propChanges));
            change = change.and(operandWhere.toWhere().not());
            if(changedWhere!=null) changedWhere.add(operandWhere.toWhere());
        }
        return result;
    }
}
