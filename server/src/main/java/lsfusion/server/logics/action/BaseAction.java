package lsfusion.server.logics.action;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class BaseAction<P extends PropertyInterface> extends Action<P> {

    protected BaseAction(LocalizedString caption, ImOrderSet<P> interfaces) {
        super(caption, interfaces);
    }

    public ImSet<Action> getDependActions() {
        return SetFact.EMPTY();
    }

    @Override
    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if(type == ChangeFlowType.ANYEFFECT)
            return true;
        return super.hasFlow(type, recursiveAbstracts);
    }
}
