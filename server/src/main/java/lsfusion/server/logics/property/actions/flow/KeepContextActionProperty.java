package lsfusion.server.logics.property.actions.flow;

import lsfusion.server.logics.i18n.LocalizedString;

public abstract class KeepContextActionProperty extends FlowActionProperty {

    protected KeepContextActionProperty(LocalizedString caption, int size) {
        super(caption, size);
    }

    @Override
    public boolean ignoreReadOnlyPolicy() {
        return false;
    }
}
