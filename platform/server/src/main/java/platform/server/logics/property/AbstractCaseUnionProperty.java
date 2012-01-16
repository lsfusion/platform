package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.base.Result;
import platform.server.logics.DataObject;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import platform.server.session.StructChanges;

import java.sql.SQLException;
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
    public PropertyMapImplement<?, Interface> modifyChangeImplement(Result<Property> aggProp, Map<Interface, DataObject> interfaceValues, DataSession session, Modifier modifier) throws SQLException {
        for(Case propCase : getCases()) {
            if(!checkWhere() || propCase.where.read(session, interfaceValues, modifier)!=null) {
                PropertyMapImplement<?, Interface> operandImplement = propCase.property.mapChangeImplement(interfaceValues, session, modifier);
                if(operandImplement!=null)
                    return operandImplement;
            }
        }
        return super.modifyChangeImplement(aggProp, interfaceValues, session, modifier);
    }
}
