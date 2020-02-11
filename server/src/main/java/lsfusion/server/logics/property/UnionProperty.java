package lsfusion.server.logics.property;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.function.IntFunction;

abstract public class UnionProperty extends ComplexIncrementProperty<UnionProperty.Interface> {

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    public static IntFunction<Interface> genInterface = Interface::new;
    public static ImOrderSet<Interface> getInterfaces(int intNum) {
        return SetFact.toOrderExclSet(intNum, genInterface);
    }

    protected UnionProperty(LocalizedString caption, ImOrderSet<Interface> interfaces) {
        super(caption, interfaces);
    }

    public abstract ImCol<PropertyInterfaceImplement<Interface>> getOperands();

    @Override
    public void fillDepends(MSet<Property> depends, boolean events) {
        fillDepends(depends,getOperands());
    }

    @Override
    public Inferred<Interface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        ImCol<PropertyInterfaceImplement<Interface>> operands = getOperands();
        return op(operands.toList(), ListFact.toList(commonValue, operands.size()), operands.size(), -1, inferType, true);
    }
    public boolean calcNeedInferredForValueClass(InferType inferType) {
        return opNeedInferForValueClass(getOperands(), inferType);
    }
    public ExClassSet calcInferValueClass(ImMap<Interface, ExClassSet> inferred, InferType inferType) {
        return opInferValueClasses(getOperands(), inferred, true, inferType);
    }
}
