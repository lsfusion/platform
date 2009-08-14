package platform.server.logics.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

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

    protected void fillDepends(Set<Property> depends) {
        fillDepends(depends,getOperands());
    }
}
