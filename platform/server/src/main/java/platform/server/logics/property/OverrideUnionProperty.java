package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.util.ArrayList;
import java.util.List;

public class OverrideUnionProperty extends CaseUnionProperty {

    private static List<Case> getCases(List<CalcPropertyInterfaceImplement<Interface>> operands) {
        List<Case> result = new ArrayList<Case>();
        for(CalcPropertyInterfaceImplement<Interface> operand : operands)
            result.add(new Case(operand, operand));
        return BaseUtils.reverseThis(result);
    }
    public OverrideUnionProperty(String sID, String caption, List<Interface> interfaces, List<CalcPropertyInterfaceImplement<Interface>> operands) {
        super(sID, caption, interfaces, getCases(operands));
        this.operands = operands;
    }

    private List<CalcPropertyInterfaceImplement<Interface>> operands = new ArrayList<CalcPropertyInterfaceImplement<Interface>>();

    @Override
    protected QuickSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        return propChanges.getUsedDataChanges(getDepends());
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        DataChanges result = new DataChanges();
        for(CalcPropertyInterfaceImplement<Interface> operand : BaseUtils.reverse(operands)) {
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
