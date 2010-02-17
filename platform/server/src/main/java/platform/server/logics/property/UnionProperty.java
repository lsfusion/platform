package platform.server.logics.property;

import java.util.*;

abstract public class UnionProperty extends FunctionProperty<UnionProperty.Interface> {

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    static List<Interface> getInterfaces(int intNum) {
        List<Interface> interfaces = new ArrayList<Interface>();
        for(int i=0;i<intNum;i++)
            interfaces.add(new Interface(i));
        return interfaces;
    }

    protected UnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, getInterfaces(intNum));
    }

    protected abstract Collection<PropertyMapImplement<?, Interface>> getOperands();

    @Override
    public void fillDepends(Set<Property> depends) {
        fillDepends(depends,getOperands());
    }
}
