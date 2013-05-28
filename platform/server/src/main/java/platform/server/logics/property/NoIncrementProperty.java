package platform.server.logics.property;

import platform.base.col.interfaces.immutable.ImOrderSet;

public abstract class NoIncrementProperty<T extends PropertyInterface> extends FunctionProperty<T> {

    public NoIncrementProperty(String sID, String caption, ImOrderSet<T> interfaces) {
        super(sID, caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        throw new RuntimeException("not supported"); // не может быть stored / modified
    }
}
