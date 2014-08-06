package lsfusion.server.logics.property.infer;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;

public class Equals<T extends PropertyInterface> extends Compared<T> {

    public Equals(CalcPropertyInterfaceImplement<T> first, CalcPropertyInterfaceImplement<T> second) {
        super(first, second);
    }

    protected <P extends PropertyInterface> Compared<P> create(CalcPropertyInterfaceImplement<P> first, CalcPropertyInterfaceImplement<P> second) {
        return new Equals<P>(first, second);
    }

    public ExClassSet resolveInferred(CalcPropertyInterfaceImplement<T> operand, ImMap<T, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.toNotNull(operand.mapInferValueClass(inferred, inferType));
    }

    public Inferred<T> inferResolved(CalcPropertyInterfaceImplement<T> operand, ExClassSet classSet, InferType inferType) {
        return operand.mapInferInterfaceClasses(classSet, inferType);
    }
}
