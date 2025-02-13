package lsfusion.server.logics.action.flow;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class BreakAction extends ChangeFlowAction {
    public BreakAction() {
        super(LocalizedString.create("break"));

        finalizeInit();
    }

    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if(type == ChangeFlowType.BREAK)
            return true;
        return super.hasFlow(type, recursiveAbstracts);
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) {
        return FlowResult.BREAK;
    }

    @Override
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return PropertyFact.createNull();
    }
}
