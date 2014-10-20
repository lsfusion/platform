package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImRevMap;

public interface AlgInfoType extends AlgType {

    <P extends PropertyInterface> boolean isEmpty(CalcProperty<P> property);

    <P extends PropertyInterface> boolean isNotNull(CalcProperty<P> property);

    <P extends PropertyInterface> boolean isFull(CalcProperty<P> property, ImCol<P> checkInterfaces);
}
