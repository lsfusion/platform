package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.session.PropertyChanges;

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

    protected PropertyChanges calculateUsedDataChanges(PropertyChanges propChanges) {
        Set<Property> propValues = new HashSet<Property>(); fillDepends(propValues, getProps());
        Set<Property> propWheres = new HashSet<Property>(); fillDepends(propWheres, getWheres());
        return propChanges.getUsedDataChanges(propValues).add(propChanges.getUsedChanges(propValues));
    }
}
