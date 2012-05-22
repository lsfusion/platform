package platform.server.logics.property;

import platform.server.classes.ValueClass;

import java.util.*;

abstract public class UnionProperty extends ComplexIncrementProperty<UnionProperty.Interface> {

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    public static List<Interface> getInterfaces(int intNum) {
        List<Interface> interfaces = new ArrayList<Interface>();
        for(int i=0;i<intNum;i++)
            interfaces.add(new Interface(i));
        return interfaces;
    }

    protected UnionProperty(String sID, String caption, List<Interface> interfaces) {
        super(sID, caption, interfaces);
    }

    protected abstract Collection<CalcPropertyInterfaceImplement<Interface>> getOperands();

    @Override
    public void fillDepends(Set<CalcProperty> depends, boolean events) {
        fillDepends(depends,getOperands());
    }

    @Override
    public Map<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        Map<Interface, ValueClass> result = new HashMap<Interface, ValueClass>();
        for(CalcPropertyInterfaceImplement<Interface> operand : getOperands())
            result = or(interfaces, result, operand.mapInterfaceCommonClasses(commonValue));
        return result;
    }
}
