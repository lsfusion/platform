package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.*;

abstract public class UnionProperty extends ComplexIncrementProperty<UnionProperty.Interface> {

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
    public void fillDepends(Set<Property> depends, boolean derived) {
        fillDepends(depends,getOperands());
    }
}
