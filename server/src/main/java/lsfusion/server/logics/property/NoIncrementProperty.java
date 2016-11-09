package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;

public abstract class NoIncrementProperty<T extends PropertyInterface> extends FunctionProperty<T> {

    public NoIncrementProperty(String caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        throw new RuntimeException("not supported"); // не может быть stored / modified
    }
}
