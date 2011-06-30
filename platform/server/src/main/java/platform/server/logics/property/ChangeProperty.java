package platform.server.logics.property;

import java.util.List;

public abstract class ChangeProperty<T extends PropertyInterface> extends AggregateProperty<T> {

    public ChangeProperty(String SID, String caption, List<T> interfaces) {
        super(SID, caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        throw new RuntimeException("not supported"); // can not be stored / modified;
    }
}
