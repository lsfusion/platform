package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.util.ArrayList;
import java.util.List;

public class OverrideUnionProperty extends CaseUnionProperty {

    private static List<Case> getCases(List<CalcPropertyMapImplement<?, Interface>> operands) {
        List<Case> result = new ArrayList<Case>();
        for(CalcPropertyMapImplement<?, Interface> operand : operands)
            result.add(new Case(operand, operand));
        return BaseUtils.reverseThis(result);
    }
    public OverrideUnionProperty(String sID, String caption, List<Interface> interfaces, List<CalcPropertyMapImplement<?, Interface>> operands) {
        super(sID, caption, interfaces, getCases(operands));
        this.operands = operands;
    }

    private List<CalcPropertyMapImplement<?,Interface>> operands = new ArrayList<CalcPropertyMapImplement<?, Interface>>();

    @Override
    protected QuickSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        return propChanges.getUsedDataChanges(getDepends());
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        DataChanges result = new DataChanges();
        for(CalcPropertyMapImplement<?, Interface> operand : BaseUtils.reverse(operands)) {
            WhereBuilder operandWhere = new WhereBuilder();
            result = result.add(operand.mapDataChanges(change, operandWhere, propChanges));
            change = change.and(operandWhere.toWhere().not());
            if(changedWhere!=null) changedWhere.add(operandWhere.toWhere());
        }
        return result;
    }

    @Override
    protected boolean checkWhere() {
        return false;
    }
}
