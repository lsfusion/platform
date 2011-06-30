package platform.server.logics.property;

import java.util.List;

public abstract class SimpleIncrementProperty<T extends PropertyInterface> extends FunctionProperty<T> {

    protected SimpleIncrementProperty(String sID, String caption, List<T> interfaces) {
        super(sID, caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        return true;
    }
}
