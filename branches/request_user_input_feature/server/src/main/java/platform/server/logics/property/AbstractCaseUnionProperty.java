package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.StructChanges;

import java.util.*;

public abstract class AbstractCaseUnionProperty extends IncrementUnionProperty {

    public static class Case {
        PropertyMapImplement<?, Interface> where;
        PropertyMapImplement<?, Interface> property;

        public Case(PropertyMapImplement<?, Interface> where, PropertyMapImplement<?, Interface> property) {
            this.where = where;
            this.property = property;
        }
    }

    protected AbstractCaseUnionProperty(String sID, String caption, List<Interface> interfaces) {
        super(sID, caption, interfaces);
    }

    @Override
    protected Collection<PropertyMapImplement<?, Interface>> getOperands() {
        return BaseUtils.mergeSet(getWheres(), getProps());
    }

    protected abstract Iterable<Case> getCases();

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

    protected QuickSet<Property> calculateUsedDataChanges(StructChanges propChanges) {
        Set<Property> propValues = new HashSet<Property>(); fillDepends(propValues, getProps());
        Set<Property> propWheres = new HashSet<Property>(); fillDepends(propWheres, getWheres());
        return QuickSet.add(propChanges.getUsedDataChanges(propValues),propChanges.getUsedChanges(propValues));
    }

    protected boolean checkWhere() {
        return true;
    }

    @Override
    public PropertyMapImplement<ClassPropertyInterface, Interface> getDefaultEditAction(String editActionSID, Property filterProperty) {
        // нужно создать List - if(where[classes]) {getEditAction(); return;}
        boolean ifClasses = !checkWhere();
        PropertyMapImplement<ClassPropertyInterface, Interface> result = null;
        for(Case propCase : BaseUtils.reverse(getCases())) {
            PropertyMapImplement<ClassPropertyInterface, Interface> editAction = propCase.property.mapEditAction(editActionSID, filterProperty);
            if(result==null && ifClasses)
                result = editAction;
            else
                result = DerivedProperty.createIfAction(interfaces, propCase.where, editAction, result, ifClasses);
        }
        return result;
    }
}
