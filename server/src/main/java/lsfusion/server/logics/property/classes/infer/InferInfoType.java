package lsfusion.server.logics.property.classes.infer;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class InferInfoType extends InferType implements AlgInfoType {

    public <P extends PropertyInterface> boolean isEmpty(Property<P> property) {
        return property.inferEmpty(this);
    }

    public <P extends PropertyInterface> boolean isFull(Property<P> property, ImCol<P> checkInterfaces) {
        return property.inferFull(checkInterfaces, this);
    }

    public <P extends PropertyInterface> boolean isNotNull(ImSet<P> checkInterfaces, Property<P> property) {
        return property.inferNotNull(checkInterfaces, this);
    }

    @Override
    public AlgInfoType getAlgInfo() {
        return this;
    }
}
