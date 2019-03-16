package lsfusion.server.logics.action.flow;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class BreakAction extends ChangeFlowAction {
    public BreakAction() {
        super(LocalizedString.create("break"));

        finalizeInit();
    }

    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.BREAK)
            return true;
        return super.hasFlow(type);
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        return FlowResult.BREAK;
    }

    @Override
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return PropertyFact.createNull();
    }
}
