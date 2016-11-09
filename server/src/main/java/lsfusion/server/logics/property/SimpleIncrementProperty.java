package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;

public abstract class SimpleIncrementProperty<T extends PropertyInterface> extends FunctionProperty<T> {

    protected SimpleIncrementProperty(String caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        return true;
    }
}
