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
    protected <U extends Changes<U>> U calculateUsedDataChanges(Modifier<U> modifier) {
        return modifier.getUsedDataChanges(getDepends());
    }

    @Override
    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        MapDataChanges<Interface> result = new MapDataChanges<Interface>();
        for(PropertyMapImplement<?, Interface> operand : operands)
            result = result.add(operand.mapDataChanges(change, changedWhere, modifier));
        return result;
    }
}
