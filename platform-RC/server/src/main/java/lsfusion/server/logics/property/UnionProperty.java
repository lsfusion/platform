package lsfusion.server.logics.property;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;
import lsfusion.server.logics.property.infer.Inferred;

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

    protected UnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces) {
        super(caption, interfaces);
    }

    public abstract ImCol<CalcPropertyInterfaceImplement<Interface>> getOperands();

    @Override
    public void fillDepends(MSet<CalcProperty> depends, boolean events) {
        fillDepends(depends,getOperands());
    }

    @Override
    public Inferred<Interface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        ImCol<CalcPropertyInterfaceImplement<Interface>> operands = getOperands();
        return op(operands.toList(), ListFact.toList(commonValue, operands.size()), operands.size(), -1, inferType, true);
    }
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        return opInferValueClasses(getOperands(), inferred, true, inferType);
    }
}
