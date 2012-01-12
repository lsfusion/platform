package platform.server.logics.property;

import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.util.*;

// чисто для оптимизации
public class ExclusiveUnionProperty extends ExclusiveCaseUnionProperty {

    private Set<PropertyMapImplement<?,Interface>> operands = new HashSet<PropertyMapImplement<?, Interface>>();
    public void addOperand(PropertyMapImplement<?,Interface> operand) {
        operands.add(operand);
        addCase(operand, operand);
    }

    public ExclusiveUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    @Override
    protected PropertyChanges calculateUsedDataChanges(PropertyChanges propChanges) {
        return propChanges.getUsedDataChanges(getDepends());
    }

    @Override
    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        MapDataChanges<Interface> result = new MapDataChanges<Interface>();
        for(PropertyMapImplement<?, Interface> operand : operands)
            result = result.add(operand.mapDataChanges(change, changedWhere, propChanges));
        return result;
    }

    @Override
    protected boolean checkWhere() {
        return false;
    }
}
