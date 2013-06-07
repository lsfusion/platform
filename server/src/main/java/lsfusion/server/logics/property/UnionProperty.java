package lsfusion.server.logics.property;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.classes.ValueClass;

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
        ImCol<CalcPropertyInterfaceImplement<Interface>> operands = getOperands();
        return or(interfaces, operands.toList(), ListFact.toList(commonValue, operands.size()));
    }
}
