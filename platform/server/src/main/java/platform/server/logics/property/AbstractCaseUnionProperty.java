package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.session.MapDataChanges;
import platform.server.session.PropertyChange;
import platform.base.BaseUtils;

import java.util.*;

public abstract class AbstractCaseUnionProperty extends IncrementUnionProperty {

    protected class Case {
        PropertyMapImplement<?, Interface> where;
        PropertyMapImplement<?, Interface> property;

        protected Case(PropertyMapImplement<?, Interface> where, PropertyMapImplement<?, Interface> property) {
            this.where = where;
            this.property = property;
        }
    }

    public AbstractCaseUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    @Override
    protected Collection<PropertyMapImplement<?, Interface>> getOperands() {
        return BaseUtils.mergeSet(getWheres(), getProps());
    }

    protected abstract Collection<Case> getCases();

    protected Set<PropertyMapImplement<?, Interface>> getWheres() {
        Set<PropertyMapImplement<?, Interface>> operands = new HashSet<PropertyMapImplement<?,Interface>>();
        for(Case propCase : getCases())
            operands.add(propCase.where);
        return operands;
    }
    protected Set<PropertyMapImplement<?, Interface>> getProps() {
        Set<PropertyMapImplement<?, Interface>> operands = new HashSet<PropertyMapImplement<?,Interface>>();
        for(Case propCase : getCases())
            operands.add(propCase.property);
        return operands;
    }

    protected <U extends Changes<U>> U calculateUsedDataChanges(Modifier<U> modifier) {
        Set<Property> propValues = new HashSet<Property>(); fillDepends(propValues, getProps());
        Set<Property> propWheres = new HashSet<Property>(); fillDepends(propWheres, getWheres());
        return modifier.getUsedDataChanges(propValues).add(modifier.getUsedChanges(propValues));
    }
}
