package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.StructChanges;

import java.util.*;

public abstract class AbstractCaseUnionProperty extends IncrementUnionProperty {

    public static class Case {
        CalcPropertyInterfaceImplement<Interface> where;
        CalcPropertyInterfaceImplement<Interface> property;

        public Case(CalcPropertyInterfaceImplement<Interface> where, CalcPropertyInterfaceImplement<Interface> property) {
            this.where = where;
            this.property = property;
        }
    }

    protected AbstractCaseUnionProperty(String sID, String caption, List<Interface> interfaces) {
        super(sID, caption, interfaces);
    }

    @Override
    protected Collection<CalcPropertyInterfaceImplement<Interface>> getOperands() {
        return BaseUtils.mergeSet(getWheres(), getProps());
    }

    protected abstract Iterable<Case> getCases();

    protected Set<CalcPropertyInterfaceImplement<Interface>> getWheres() {
        Set<CalcPropertyInterfaceImplement<Interface>> operands = new HashSet<CalcPropertyInterfaceImplement<Interface>>();
        for(Case propCase : getCases())
            operands.add(propCase.where);
        return operands;
    }
    protected Set<CalcPropertyInterfaceImplement<Interface>> getProps() {
        Set<CalcPropertyInterfaceImplement<Interface>> operands = new HashSet<CalcPropertyInterfaceImplement<Interface>>();
        for(Case propCase : getCases())
            operands.add(propCase.property);
        return operands;
    }

    protected QuickSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        Set<CalcProperty> propValues = new HashSet<CalcProperty>(); fillDepends(propValues, getProps());
        Set<CalcProperty> propWheres = new HashSet<CalcProperty>(); fillDepends(propWheres, getWheres());
        return QuickSet.add(propChanges.getUsedDataChanges(propValues),propChanges.getUsedChanges(propValues));
    }

    protected boolean checkWhere() {
        return true;
    }

    @Override
    @IdentityLazy
    public ActionPropertyMapImplement<?, Interface> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        // нужно создать List - if(where[classes]) {getEditAction(); return;}
        boolean ifClasses = !checkWhere();
        ActionPropertyMapImplement<?, Interface> result = null;
        for(Case propCase : BaseUtils.reverse(getCases())) {
            ActionPropertyMapImplement<?, Interface> editAction = propCase.property.mapEditAction(editActionSID, filterProperty);
            if (editAction != null) {
                if (result == null && ifClasses) {
                    result = editAction;
                } else {
                    result = DerivedProperty.createIfAction(interfaces, (CalcPropertyMapImplement<?,Interface>) propCase.where, editAction, result, ifClasses);
                }
            }
        }
        return result;
    }

    @Override
    @IdentityLazy
    public Collection<DataProperty> getChangeProps() {
        Set<DataProperty> result = new HashSet<DataProperty>();
        for(Case operand : getCases())
            result.addAll(operand.property.mapChangeProps());
        return result;
    }
}
