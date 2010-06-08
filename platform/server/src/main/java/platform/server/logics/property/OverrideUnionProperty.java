package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.session.*;
import platform.server.data.where.WhereBuilder;
import platform.base.BaseUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OverrideUnionProperty extends UnionProperty {

    public OverrideUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    public List<PropertyMapImplement<?,Interface>> operands = new ArrayList<PropertyMapImplement<?, Interface>>();

    protected Collection<PropertyMapImplement<?, Interface>> getOperands() {
        return operands;
    }

    public Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {

        Expr result = null;
        for(PropertyMapImplement<?, Interface> operand : operands) {
            Expr operandExpr = operand.mapExpr(joinImplement, modifier, changedWhere);
            if(result==null)
                result = operandExpr;
            else
                result = operandExpr.nvl(result);
        }
        return result;
    }

    @Override
    public <U extends Changes<U>> U getUsedDataChanges(Modifier<U> modifier) {
        return modifier.getUsedDataChanges(getDepends());
    }

    @Override
    public MapDataChanges<Interface> getDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        MapDataChanges<Interface> result = new MapDataChanges<Interface>();
        for(PropertyMapImplement<?, Interface> operand : BaseUtils.reverse(operands)) {
            WhereBuilder operandWhere = new WhereBuilder();
            result = result.add(operand.mapDataChanges(change, operandWhere, modifier));
            change = change.and(operandWhere.toWhere().not());
            if(changedWhere!=null) changedWhere.add(operandWhere.toWhere());
        }
        return result;
    }
}
