package lsfusion.server.logics.property.actions.flow;

public abstract class KeepContextActionProperty extends FlowActionProperty {

    protected KeepContextActionProperty(String caption, int size) {
        super(caption, size);
    }

    @Override
    public boolean ignoreReadOnlyPolicy() {
        return false;
    }
}
