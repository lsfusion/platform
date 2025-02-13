package lsfusion.server.logics.action.flow;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ContinueAction extends ChangeFlowAction {
    public ContinueAction() {
        super(LocalizedString.create("continue"));

        finalizeInit();
    }

    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if(type == ChangeFlowType.CONTINUE)
            return true;
        return super.hasFlow(type, recursiveAbstracts);
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) {
        return FlowResult.CONTINUE;
    }

    @Override
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return PropertyFact.createNull();
    }
}
