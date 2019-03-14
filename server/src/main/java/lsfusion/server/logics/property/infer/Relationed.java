package lsfusion.server.logics.property.infer;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class Relationed<T extends PropertyInterface> extends Compared<T> {

    public Relationed(PropertyInterfaceImplement<T> first, PropertyInterfaceImplement<T> second) {
        super(first, second);
    }

    protected <P extends PropertyInterface> Compared<P> create(PropertyInterfaceImplement<P> first, PropertyInterfaceImplement<P> second) {
        return new Relationed<>(first, second);
    }

    public ExClassSet resolveInferred(PropertyInterfaceImplement<T> operand, ImMap<T, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.removeValues(ExClassSet.toNotNull(operand.mapInferValueClass(inferred, inferType)));
    }

    public Inferred<T> inferResolved(PropertyInterfaceImplement<T> operand, ExClassSet classSet, InferType inferType) {
        return operand.mapInferInterfaceClasses(classSet, inferType);
    }
}
