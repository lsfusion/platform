package platform.server.logics.properties;

import platform.server.session.DataChanges;
import platform.server.data.types.Type;
import platform.base.BaseUtils;

import java.util.*;

abstract public class UnionProperty extends FunctionProperty<PropertyInterface> {

    static Collection<PropertyInterface> getInterfaces(int intNum) {
        Collection<PropertyInterface> interfaces = new ArrayList<PropertyInterface>();
        for(int i=0;i<intNum;i++)
            interfaces.add(new PropertyInterface(i));
        return interfaces;
    }

    protected UnionProperty(String iSID, int intNum) {
        super(iSID, getInterfaces(intNum));
    }

    protected abstract Collection<PropertyMapImplement<PropertyInterface,PropertyInterface>> getOperands();

    protected boolean fillDependChanges(List<Property> changedProperties, DataChanges changes, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) {
        boolean changed = false;
        for(PropertyMapImplement<PropertyInterface, PropertyInterface> operand : getOperands())
            changed = operand.mapFillChanges(changedProperties, changes, noUpdateProps, defaultProps) || changed;
        return changed;
    }
}
