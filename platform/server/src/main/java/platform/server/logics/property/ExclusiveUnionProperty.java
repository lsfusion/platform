package platform.server.logics.property;

import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.util.*;

// чисто для оптимизации
public class ExclusiveUnionProperty extends ExclusiveCaseUnionProperty {

    private final Collection<PropertyMapImplement<?,Interface>> operands;

    @IdentityLazy
    protected Iterable<Case> getCases() {
        Collection<Case> result = new ArrayList<Case>();
        for(PropertyMapImplement<?, Interface> operand : operands)
            result.add(new Case(operand, operand));
        return result;
    }

    public ExclusiveUnionProperty(String sID, String caption, List<Interface> interfaces, Collection<PropertyMapImplement<?, Interface>> operands) {
        super(sID, caption, interfaces);
        this.operands = operands;

        finalizeInit();
    }

    @Override
    protected QuickSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
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
