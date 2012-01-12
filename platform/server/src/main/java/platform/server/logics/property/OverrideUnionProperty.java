package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.util.ArrayList;
import java.util.List;

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
    protected PropertyChanges calculateUsedDataChanges(PropertyChanges propChanges) {
        return propChanges.getUsedDataChanges(getDepends());
    }

    @Override
    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        MapDataChanges<Interface> result = new MapDataChanges<Interface>();
        for(PropertyMapImplement<?, Interface> operand : BaseUtils.reverse(operands)) {
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
