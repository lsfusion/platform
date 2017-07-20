package lsfusion.server.logics.property.infer;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.AlgInfoType;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.PropertyInterface;

public class InferInfoType extends InferType implements AlgInfoType {

    public <P extends PropertyInterface> boolean isEmpty(CalcProperty<P> property) {
        return property.inferEmpty(this);
    }

    public <P extends PropertyInterface> boolean isFull(CalcProperty<P> property, ImCol<P> checkInterfaces) {
        return property.inferFull(checkInterfaces, this);
    }

    public <P extends PropertyInterface> boolean isNotNull(ImSet<P> checkInterfaces, CalcProperty<P> property) {
        return property.inferNotNull(checkInterfaces, this);
    }

    @Override
    public AlgInfoType getAlgInfo() {
        return this;
    }
}
