package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public abstract class SimpleRequestInput<T extends PropertyInterface> {

    public abstract SimpleRequestInput<T> newSession();

    public abstract <P extends PropertyInterface> SimpleRequestInput<P> map(ImRevMap<T, P> mapping);
    public abstract <P extends PropertyInterface> SimpleRequestInput<P> mapInner(ImRevMap<T, P> mapping);
    public abstract <P extends PropertyInterface> SimpleRequestInput<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping);

    public abstract SimpleRequestInput<T> merge(SimpleRequestInput<T> input);
}
