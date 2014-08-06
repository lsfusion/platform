package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImRevMap;

public interface AlgInfoType extends AlgType {

    <T extends PropertyInterface, P extends PropertyInterface> boolean intersectFull(CalcProperty<T> property, CalcProperty<P> intersect, ImRevMap<P, T> map);

    <P extends PropertyInterface> boolean isEmpty(CalcProperty<P> property);

    <P extends PropertyInterface> boolean isNotNull(CalcProperty<P> property);

    <P extends PropertyInterface> boolean isFull(CalcProperty<P> property, ImCol<P> checkInterfaces);
}
