package lsfusion.server.logics.property.infer;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public interface AlgInfoType extends AlgType {

    <P extends PropertyInterface> boolean isEmpty(CalcProperty<P> property);

    <P extends PropertyInterface> boolean isNotNull(ImSet<P> checkInterfaces, CalcProperty<P> property);

    <P extends PropertyInterface> boolean isFull(CalcProperty<P> property, ImCol<P> checkInterfaces);
}
