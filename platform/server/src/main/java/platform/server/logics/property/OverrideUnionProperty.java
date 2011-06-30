package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.MapDataChanges;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OverrideUnionProperty extends CaseUnionProperty {

    public OverrideUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    public void addOperand(PropertyMapImplement<?,Interface> operand) {
        operands.add(operand);
        addCase(operand, operand, true);
    }

    private List<PropertyMapImplement<?,Interface>> operands = new ArrayList<PropertyMapImplement<?, Interface>>();

    @Override
    protected Collection<PropertyMapImplement<?, Interface>> getOperands() {
        return operands;
    }

    @Override
    protected <U extends Changes<U>> U calculateUsedDataChanges(Modifier<U> modifier) {
        return modifier.getUsedDataChanges(getDepends());
    }

    @Override
    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
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
