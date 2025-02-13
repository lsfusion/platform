package lsfusion.server.logics.action.flow;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ReturnAction extends ChangeFlowAction {
    public ReturnAction() {
        super(LocalizedString.create("return"));

        finalizeInit();
    }

    @Override
    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if(type == ChangeFlowType.RETURN)
            return true;
        return super.hasFlow(type, recursiveAbstracts);
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) {
        return FlowResult.RETURN;
    }

    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return PropertyFact.createNull();
    }
}
