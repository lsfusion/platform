package platform.server.logics.property;

import java.util.List;

public abstract class NoIncrementProperty<T extends PropertyInterface> extends FunctionProperty<T> {

    public NoIncrementProperty(String sID, String caption, List<T> interfaces) {
        super(sID, caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        throw new RuntimeException("not supported"); // не может быть stored / modified
    }
}
