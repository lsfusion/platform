package platform.server.logics.property;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.server.classes.ValueClass;

abstract public class UnionProperty extends ComplexIncrementProperty<UnionProperty.Interface> {

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    public static GetIndex<Interface> genInterface = new GetIndex<Interface>() {
        public Interface getMapValue(int i) {
            return new Interface(i);
        }};
    public static ImOrderSet<Interface> getInterfaces(int intNum) {
        return SetFact.toOrderExclSet(intNum, genInterface);
    }

    protected UnionProperty(String sID, String caption, ImOrderSet<Interface> interfaces) {
        super(sID, caption, interfaces);
    }

    public abstract ImCol<CalcPropertyInterfaceImplement<Interface>> getOperands();

    @Override
    public void fillDepends(MSet<CalcProperty> depends, boolean events) {
        fillDepends(depends,getOperands());
    }

    @Override
    public ImMap<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        ImMap<Interface, ValueClass> result = MapFact.EMPTY();
        for(CalcPropertyInterfaceImplement<Interface> operand : getOperands())
            result = or(interfaces, result, operand.mapInterfaceCommonClasses(commonValue));
        return result;
    }
}
