package lsfusion.server.logics.action;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public abstract class BaseAction<P extends PropertyInterface> extends Action<P> {

    protected BaseAction(LocalizedString caption, ImOrderSet<P> interfaces) {
        super(caption, interfaces);
    }

    public ImSet<Action> getDependActions() {
        return SetFact.EMPTY();
    }
}
