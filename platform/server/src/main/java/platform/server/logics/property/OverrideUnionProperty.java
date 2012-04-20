package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.data.where.WhereBuilder;
import platform.server.session.MapDataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.util.ArrayList;
import java.util.List;

public class OverrideUnionProperty extends CaseUnionProperty {

    private static List<Case> getCases(List<PropertyMapImplement<?, Interface>> operands) {
        List<Case> result = new ArrayList<Case>();
        for(PropertyMapImplement<?, Interface> operand : operands)
            result.add(new Case(operand, operand));
        return BaseUtils.reverseThis(result);
    }
    public OverrideUnionProperty(String sID, String caption, List<Interface> interfaces, List<PropertyMapImplement<?, Interface>> operands) {
        super(sID, caption, interfaces, getCases(operands));
        this.operands = operands;
    }

    private List<PropertyMapImplement<?,Interface>> operands = new ArrayList<PropertyMapImplement<?, Interface>>();

    @Override
    protected QuickSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
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
