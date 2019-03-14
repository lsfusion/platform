package lsfusion.server.logics.action.flow;

import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class KeepContextAction extends FlowAction {

    protected KeepContextAction(LocalizedString caption, int size) {
        super(caption, size);
    }
}
