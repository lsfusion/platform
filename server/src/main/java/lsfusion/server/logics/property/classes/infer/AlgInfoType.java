package lsfusion.server.logics.property.classes.infer;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public interface AlgInfoType extends AlgType {

    <P extends PropertyInterface> boolean isEmpty(Property<P> property);

    <P extends PropertyInterface> boolean isNotNull(ImSet<P> checkInterfaces, Property<P> property);

    <P extends PropertyInterface> boolean isFull(Property<P> property, ImCol<P> checkInterfaces);
}
